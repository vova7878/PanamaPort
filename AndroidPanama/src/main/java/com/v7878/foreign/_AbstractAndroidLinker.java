package com.v7878.foreign;

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_BOOLEAN;
import static com.v7878.foreign.ValueLayout.JAVA_BYTE;
import static com.v7878.foreign.ValueLayout.JAVA_CHAR;
import static com.v7878.foreign.ValueLayout.JAVA_DOUBLE;
import static com.v7878.foreign.ValueLayout.JAVA_FLOAT;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_LONG;
import static com.v7878.foreign.ValueLayout.JAVA_SHORT;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;

import com.v7878.unsafe.Utils.SoftReferenceCache;
import com.v7878.unsafe.access.JavaForeignAccess;
import com.v7878.unsafe.foreign.RawNativeLibraries;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

sealed abstract class _AbstractAndroidLinker implements Linker permits _AndroidLinkerImpl {

    public interface UpcallStubFactory {
        MemorySegment makeStub(MethodHandle target, Arena arena);
    }

    private static final class LinkRequest {
        private final FunctionDescriptor descriptor;
        private final _LinkerOptions options;

        private LinkRequest(FunctionDescriptor descriptor, _LinkerOptions options) {
            this.descriptor = descriptor;
            this.options = options;
        }

        public FunctionDescriptor descriptor() {
            return descriptor;
        }

        public _LinkerOptions options() {
            return options;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            return o instanceof LinkRequest that &&
                    Objects.equals(this.descriptor, that.descriptor) &&
                    Objects.equals(this.options, that.options);
        }

        @Override
        public int hashCode() {
            return Objects.hash(descriptor, options);
        }

        @Override
        public String toString() {
            return "LinkRequest[" +
                    "descriptor=" + descriptor + ", " +
                    "options=" + options + ']';
        }
    }

    private final SoftReferenceCache<LinkRequest, MethodHandle> DOWNCALL_CACHE = new SoftReferenceCache<>();
    private final SoftReferenceCache<LinkRequest, UpcallStubFactory> UPCALL_CACHE = new SoftReferenceCache<>();
    private final Set<MemoryLayout> CANONICAL_LAYOUTS_CACHE = new HashSet<>(canonicalLayouts().values());

    @Override
    public final MethodHandle downcallHandle(MemorySegment symbol, FunctionDescriptor function, Option... options) {
        _Utils.checkSymbol(symbol);
        return downcallHandle(function, options).bindTo(symbol);
    }

    @Override
    public final MethodHandle downcallHandle(FunctionDescriptor function, Option... options) {
        Objects.requireNonNull(function);
        Objects.requireNonNull(options);
        checkLayouts(function);
        function = stripNames(function);
        _LinkerOptions optionSet = _LinkerOptions.forDowncall(function, options);
        validateVariadicLayouts(function, optionSet);

        return DOWNCALL_CACHE.get(new LinkRequest(function, optionSet), request -> {
            FunctionDescriptor fd = request.descriptor();
            MethodType type = fd.toMethodType();
            MethodHandle handle = arrangeDowncall(type, fd, request.options());
            handle = _Utils.maybeCheckCaptureSegment(handle, request.options());
            return handle;
        });
    }

    protected abstract MethodHandle arrangeDowncall(MethodType inferredMethodType, FunctionDescriptor function, _LinkerOptions options);

    @Override
    public final MemorySegment upcallStub(MethodHandle target, FunctionDescriptor function, Arena arena, Option... options) {
        Objects.requireNonNull(arena);
        Objects.requireNonNull(target);
        Objects.requireNonNull(function);
        checkLayouts(function);
        //TODO? SharedUtils.checkExceptions(target);
        function = stripNames(function);
        _LinkerOptions optionSet = _LinkerOptions.forUpcall(function, options);

        MethodType type = function.toMethodType();
        if (!type.equals(target.type())) {
            throw new IllegalArgumentException("Wrong method handle type: " + target.type());
        }

        UpcallStubFactory factory = UPCALL_CACHE.get(new LinkRequest(function, optionSet), request ->
                arrangeUpcall(type, request.descriptor(), request.options()));
        return factory.makeStub(target, arena);
    }

    protected abstract UpcallStubFactory arrangeUpcall(MethodType targetType, FunctionDescriptor function, _LinkerOptions options);

    // C spec mandates that variadic arguments smaller than int are promoted to int,
    // and float is promoted to double
    // See: https://en.cppreference.com/w/c/language/conversion#Default_argument_promotions
    // We reject the corresponding layouts here, to avoid issues where unsigned values
    // are sign extended when promoted. (as we don't have a way to unambiguously represent signed-ness atm).
    private void validateVariadicLayouts(FunctionDescriptor function, _LinkerOptions optionSet) {
        if (optionSet.isVariadicFunction()) {
            List<MemoryLayout> argumentLayouts = function.argumentLayouts();
            List<MemoryLayout> variadicLayouts = argumentLayouts.subList(optionSet.firstVariadicArgIndex(), argumentLayouts.size());

            for (MemoryLayout variadicLayout : variadicLayouts) {
                if (variadicLayout.equals(ValueLayout.JAVA_BOOLEAN)
                        || variadicLayout.equals(ValueLayout.JAVA_BYTE)
                        || variadicLayout.equals(ValueLayout.JAVA_CHAR)
                        || variadicLayout.equals(ValueLayout.JAVA_SHORT)
                        || variadicLayout.equals(ValueLayout.JAVA_FLOAT)) {
                    throw new IllegalArgumentException("Invalid variadic argument layout: " + variadicLayout);
                }
            }
        }
    }

    private void checkLayouts(FunctionDescriptor descriptor) {
        descriptor.returnLayout().ifPresent(this::checkLayout);
        descriptor.argumentLayouts().forEach(this::checkLayout);
    }

    private void checkLayout(MemoryLayout layout) {
        // Note: we should not worry about padding layouts, as they cannot be present in a function descriptor
        if (layout instanceof SequenceLayout) {
            //TODO?: support it
            throw new IllegalArgumentException("Unsupported layout: " + layout);
        } else {
            checkLayoutRecursive(layout);
        }
    }

    // some ABIs have special handling for struct members
    protected void checkStructMember(MemoryLayout member, long offset) {
        checkLayoutRecursive(member);
    }

    private void checkLayoutRecursive(MemoryLayout layout) {
        if (layout instanceof ValueLayout vl) {
            checkSupported(vl);
        } else if (layout instanceof StructLayout sl) {
            checkHasNaturalAlignment(layout);
            long offset = 0;
            long lastUnpaddedOffset = 0;
            for (MemoryLayout member : sl.memberLayouts()) {
                // check element offset before recursing so that an error points at the
                // outermost layout first
                checkMemberOffset(sl, member, lastUnpaddedOffset, offset);
                checkStructMember(member, offset);

                offset += member.byteSize();
                if (!(member instanceof PaddingLayout)) {
                    lastUnpaddedOffset = offset;
                }
            }
            checkGroupSize(sl, lastUnpaddedOffset);
        } else if (layout instanceof UnionLayout ul) {
            checkHasNaturalAlignment(layout);
            long maxUnpaddedLayout = 0;
            for (MemoryLayout member : ul.memberLayouts()) {
                checkLayoutRecursive(member);
                if (!(member instanceof PaddingLayout)) {
                    maxUnpaddedLayout = Long.max(maxUnpaddedLayout, member.byteSize());
                }
            }
            checkGroupSize(ul, maxUnpaddedLayout);
        } else if (layout instanceof SequenceLayout sl) {
            checkHasNaturalAlignment(layout);
            checkLayoutRecursive(sl.elementLayout());
        }
    }

    // check for trailing padding
    private void checkGroupSize(GroupLayout gl, long maxUnpaddedOffset) {
        long expectedSize = _Utils.alignUp(maxUnpaddedOffset, gl.byteAlignment());
        if (gl.byteSize() != expectedSize) {
            throw new IllegalArgumentException("Layout '" + gl + "' has unexpected size: "
                    + gl.byteSize() + " != " + expectedSize);
        }
    }

    // checks both that there is no excess padding between 'memberLayout' and
    // the previous layout
    private void checkMemberOffset(StructLayout parent, MemoryLayout memberLayout,
                                   long lastUnpaddedOffset, long offset) {
        long expectedOffset = _Utils.alignUp(lastUnpaddedOffset, memberLayout.byteAlignment());
        if (expectedOffset != offset) {
            throw new IllegalArgumentException("Member layout '" + memberLayout + "', of '" + parent + "'" +
                    " found at unexpected offset: " + offset + " != " + expectedOffset);
        }
    }

    private void checkSupported(ValueLayout valueLayout) {
        if (valueLayout instanceof AddressLayout addressLayout) {
            valueLayout = addressLayout.withoutTargetLayout();
        }
        valueLayout = valueLayout.withoutName();
        if (!CANONICAL_LAYOUTS_CACHE.contains(valueLayout)) {
            throw new IllegalArgumentException("Unsupported layout: " + valueLayout);
        }
    }

    private void checkHasNaturalAlignment(MemoryLayout layout) {
        if (!((_AbstractLayout<?>) layout).hasNaturalAlignment()) {
            throw new IllegalArgumentException("Layout alignment must be natural alignment: " + layout);
        }
    }

    @SuppressWarnings("restricted")
    private static MemoryLayout stripNames(MemoryLayout ml) {
        // we don't care about transferring alignment and byte order here
        // since the linker already restricts those such that they will always be the same
        if (ml instanceof StructLayout sl) {
            return MemoryLayout.structLayout(stripNames(sl.memberLayouts()));
        } else if (ml instanceof UnionLayout ul) {
            return MemoryLayout.unionLayout(stripNames(ul.memberLayouts()));
        } else if (ml instanceof SequenceLayout sl) {
            return MemoryLayout.sequenceLayout(sl.elementCount(), stripNames(sl.elementLayout()));
        } else if (ml instanceof AddressLayout al) {
            // Port-changed
            //return al.targetLayout()
            //        .map(tl -> al.withoutName().withTargetLayout(stripNames(tl))) // restricted
            //        .orElseGet(al::withoutName);

            return al.targetLayout().map(tl -> al.withoutName().withTargetLayout(
                            MemoryLayout.sequenceLayout(tl.byteSize(), JAVA_BYTE)))
                    .orElseGet(al::withoutName);
        } else {
            return ml.withoutName(); // ValueLayout and PaddingLayout;
        }
    }

    private static MemoryLayout[] stripNames(List<MemoryLayout> layouts) {
        return layouts.stream()
                .map(_AbstractAndroidLinker::stripNames)
                .toArray(MemoryLayout[]::new);
    }

    private static FunctionDescriptor stripNames(FunctionDescriptor function) {
        return function.returnLayout()
                .map(rl -> FunctionDescriptor.of(stripNames(rl), stripNames(function.argumentLayouts())))
                .orElseGet(() -> FunctionDescriptor.ofVoid(stripNames(function.argumentLayouts())));
    }

    @Override
    public SymbolLookup defaultLookup() {
        return JavaForeignAccess.libraryLookup(RawNativeLibraries.DEFAULT, Arena.global());
    }

    @Override
    public Map<String, MemoryLayout> canonicalLayouts() {
        class Holder {
            static final Map<String, MemoryLayout> CANONICAL_LAYOUTS;

            static {
                MemoryLayout word = IS64BIT ? JAVA_LONG : JAVA_INT;

                CANONICAL_LAYOUTS = Map.ofEntries(
                        // specified canonical layouts
                        Map.entry("bool", JAVA_BOOLEAN),
                        Map.entry("char", JAVA_BYTE),
                        Map.entry("short", JAVA_SHORT),
                        Map.entry("int", JAVA_INT),
                        Map.entry("float", JAVA_FLOAT),
                        Map.entry("long", word),
                        Map.entry("long long", JAVA_LONG),
                        Map.entry("double", JAVA_DOUBLE),
                        Map.entry("void*", ADDRESS),
                        Map.entry("size_t", word),
                        //TODO?: Map.entry("wchar_t", ???),

                        // unspecified size-dependent layouts
                        Map.entry("int8_t", JAVA_BYTE),
                        Map.entry("int16_t", JAVA_SHORT),
                        Map.entry("int32_t", JAVA_INT),
                        Map.entry("int64_t", JAVA_LONG),
                        Map.entry("intptr_t", word),

                        // unspecified JNI layouts
                        Map.entry("jboolean", JAVA_BOOLEAN),
                        Map.entry("jchar", JAVA_CHAR),
                        Map.entry("jbyte", JAVA_BYTE),
                        Map.entry("jshort", JAVA_SHORT),
                        Map.entry("jint", JAVA_INT),
                        Map.entry("jlong", JAVA_LONG),
                        Map.entry("jfloat", JAVA_FLOAT),
                        Map.entry("jdouble", JAVA_DOUBLE)
                );
            }
        }

        return Holder.CANONICAL_LAYOUTS;
    }
}
