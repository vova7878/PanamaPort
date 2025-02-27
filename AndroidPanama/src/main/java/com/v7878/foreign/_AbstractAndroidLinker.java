package com.v7878.foreign;

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_BOOLEAN;
import static com.v7878.foreign.ValueLayout.JAVA_BYTE;
import static com.v7878.foreign.ValueLayout.JAVA_CHAR;
import static com.v7878.foreign.ValueLayout.JAVA_FLOAT;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_SHORT;

import com.v7878.unsafe.Utils.SoftReferenceCache;
import com.v7878.unsafe.access.InvokeAccess;
import com.v7878.unsafe.cpp_std.CLayouts;
import com.v7878.unsafe.foreign.RawNativeLibraries;
import com.v7878.unsafe.invoke.MethodHandlesFixes;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

sealed abstract class _AbstractAndroidLinker implements Linker permits _AndroidLinkerImpl {

    public interface UpcallStubFactory {
        MemorySegment makeStub(MethodHandle target, Arena arena);
    }

    private record LinkRequest(_FunctionDescriptorImpl descriptor, _LinkerOptions options) {
    }

    private final SoftReferenceCache<LinkRequest, MethodHandle> DOWNCALL_CACHE = new SoftReferenceCache<>();
    private final SoftReferenceCache<LinkRequest, UpcallStubFactory> UPCALL_CACHE = new SoftReferenceCache<>();
    private final Set<MemoryLayout> CANONICAL_LAYOUTS_CACHE = new HashSet<>(canonicalLayouts().values());

    @Override
    public final MethodHandle downcallHandle(MemorySegment symbol, FunctionDescriptor function, Option... options) {
        _Utils.checkSymbol(symbol);
        return MethodHandlesFixes.bindTo(downcallHandle(function, options), symbol);
    }

    @Override
    public final MethodHandle downcallHandle(FunctionDescriptor function, Option... options) {
        Objects.requireNonNull(function);
        Objects.requireNonNull(options);
        checkLayouts(function);
        function = stripNames(function, true);
        _LinkerOptions optionSet = _LinkerOptions.forDowncall(function, options);
        validateVariadicLayouts(function, optionSet);

        return DOWNCALL_CACHE.get(new LinkRequest((_FunctionDescriptorImpl) function, optionSet),
                request -> arrangeDowncall(request.descriptor(), request.options()));
    }

    protected abstract MethodHandle arrangeDowncall(_FunctionDescriptorImpl function, _LinkerOptions options);

    @Override
    public final MemorySegment upcallStub(MethodHandle target, FunctionDescriptor function, Arena arena, Option... options) {
        Objects.requireNonNull(arena);
        Objects.requireNonNull(target);
        Objects.requireNonNull(function);
        checkLayouts(function);
        function = stripNames(function, false);
        _LinkerOptions optionSet = _LinkerOptions.forUpcall(function, options);
        if (!optionSet.allowExceptions()) {
            checkExceptions(target);
        }

        MethodType type = function.toMethodType();
        if (!type.equals(target.type())) {
            throw new IllegalArgumentException("Wrong method handle type: " + target.type());
        }

        UpcallStubFactory factory = UPCALL_CACHE.get(
                new LinkRequest((_FunctionDescriptorImpl) function, optionSet), request ->
                        arrangeUpcall(type, request.descriptor(), request.options()));
        return factory.makeStub(target, arena);
    }

    protected abstract UpcallStubFactory arrangeUpcall(
            MethodType targetType, _FunctionDescriptorImpl function, _LinkerOptions options);

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

    private static void checkExceptions(MethodHandle target) {
        Class<?>[] exceptions = InvokeAccess.exceptionTypes(target);
        if (exceptions != null && exceptions.length != 0) {
            throw new IllegalArgumentException("Target handle may throw exceptions: " + Arrays.toString(exceptions));
        }
    }

    private void checkLayouts(FunctionDescriptor descriptor) {
        descriptor.returnLayout().ifPresent(this::checkLayout);
        descriptor.argumentLayouts().forEach(this::checkLayout);
    }

    private void checkLayout(MemoryLayout layout) {
        // Note: we should not worry about padding layouts, as they cannot be present in a function descriptor
        if (layout instanceof SequenceLayout) {
            throw new IllegalArgumentException("Unsupported layout: " + layout);
        } else {
            checkLayoutRecursive(layout);
        }
    }

    private void checkLayoutRecursive(MemoryLayout layout) {
        if (layout instanceof ValueLayout vl) {
            checkSupported(vl);
        } else if (layout instanceof StructLayout sl) {
            checkHasNaturalAlignment(layout);
            long offset = 0;
            long lastUnpaddedOffset = 0;
            PaddingLayout preceedingPadding = null;
            for (MemoryLayout member : sl.memberLayouts()) {
                // check element offset before recursing so that an error points at the
                // outermost layout first
                checkMemberOffset(sl, member, lastUnpaddedOffset, offset);
                checkLayoutRecursive(member);

                offset += member.byteSize();
                if (member instanceof PaddingLayout pl) {
                    if (preceedingPadding != null) {
                        throw new IllegalArgumentException("The padding layout " + pl +
                                " was preceded by another padding layout " + preceedingPadding +
                                " in " + sl);
                    }
                    preceedingPadding = pl;
                } else {
                    lastUnpaddedOffset = offset;
                    if (preceedingPadding != null) {
                        preceedingPadding = null;
                    }
                }
            }
            checkNotAllPadding(sl);
            checkGroup(sl, lastUnpaddedOffset);
        } else if (layout instanceof UnionLayout ul) {
            checkHasNaturalAlignment(layout);
            // We need to know this up front
            long maxUnpaddedLayout = ul.memberLayouts().stream()
                    .filter(l -> !(l instanceof PaddingLayout))
                    .mapToLong(MemoryLayout::byteSize)
                    .max()
                    .orElse(0);

            boolean hasPadding = false;

            for (MemoryLayout member : ul.memberLayouts()) {
                checkLayoutRecursive(member);
                if (member instanceof PaddingLayout pl) {
                    if (hasPadding) {
                        throw new IllegalArgumentException("More than one padding in " + ul);
                    }
                    hasPadding = true;
                    if (pl.byteSize() <= maxUnpaddedLayout) {
                        throw new IllegalArgumentException("Superfluous padding " + pl + " in " + ul);
                    }
                }
            }
            checkGroup(ul, maxUnpaddedLayout);
        } else if (layout instanceof SequenceLayout sl) {
            checkHasNaturalAlignment(layout);
            if (sl.elementLayout() instanceof PaddingLayout pl) {
                throw memberException(sl, pl,
                        "not supported because a sequence of a padding layout is not allowed");
            }
            checkLayoutRecursive(sl.elementLayout());
        }
    }

    // check elements are not all padding layouts
    private static void checkNotAllPadding(StructLayout sl) {
        if (!sl.memberLayouts().isEmpty() && sl.memberLayouts().stream().allMatch(e -> e instanceof PaddingLayout)) {
            throw new IllegalArgumentException("Layout '" + sl + "' is non-empty and only has padding layouts");
        }
    }

    // check trailing padding
    private static void checkGroup(GroupLayout gl, long maxUnpaddedOffset) {
        long expectedSize = _Utils.alignUp(maxUnpaddedOffset, gl.byteAlignment());
        if (gl.byteSize() != expectedSize) {
            throw new IllegalArgumentException("Layout '" + gl + "' has unexpected size: "
                    + gl.byteSize() + " != " + expectedSize);
        }
    }

    // checks both that there is no excess padding between 'memberLayout' and
    // the previous layout
    private static void checkMemberOffset(StructLayout parent, MemoryLayout memberLayout,
                                          long lastUnpaddedOffset, long offset) {
        long expectedOffset = _Utils.alignUp(lastUnpaddedOffset, memberLayout.byteAlignment());
        if (expectedOffset != offset) {
            throw memberException(parent, memberLayout,
                    "found at unexpected offset: " + offset + " != " + expectedOffset);
        }
    }

    private static IllegalArgumentException memberException(
            MemoryLayout parent, MemoryLayout member, String info) {
        return new IllegalArgumentException(
                "Member layout '" + member + "', of '" + parent + "' " + info);
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

    // Port-changed
    private static MemoryLayout stripNames(MemoryLayout ml, boolean nested) {
        // we don't care about transferring alignment and byte order here
        // since the linker already restricts those such that they will always be the same
        if (ml instanceof StructLayout sl) {
            MemoryLayout[] members = sl.memberLayouts().stream().flatMap(member -> {
                if (member.byteSize() == 0 || member instanceof PaddingLayout) {
                    return null;
                }
                member = stripNames(member, true);
                if (member instanceof StructLayout sl_sl) {
                    return sl_sl.memberLayouts().stream();
                }
                return Stream.of(member);
            }).toArray(MemoryLayout[]::new);
            if (nested && members.length == 1) {
                return members[0];
            }
            return MemoryLayout.paddedStructLayout(members);
        } else if (ml instanceof UnionLayout ul) {
            MemoryLayout[] members = ul.memberLayouts().stream().flatMap(member -> {
                if (member.byteSize() == 0 || member instanceof PaddingLayout) {
                    return null;
                }
                member = stripNames(member, true);
                if (member instanceof UnionLayout ul_ul) {
                    return ul_ul.memberLayouts().stream();
                }
                return Stream.of(member);
            }).toArray(MemoryLayout[]::new);
            if (nested && members.length == 1) {
                return members[0];
            }
            return MemoryLayout.unionLayout(members);
        } else if (ml instanceof SequenceLayout sl) {
            assert nested;
            MemoryLayout element = stripNames(sl.elementLayout(), true);
            if (element.byteSize() == 0) {
                return element;
            }
            long count = sl.elementCount();
            if (count == 1) {
                return element;
            }
            if (element instanceof SequenceLayout el_sl) {
                count *= el_sl.elementCount();
                element = el_sl.elementLayout();
            }
            return MemoryLayout.sequenceLayout(count, element);
        } else if (ml instanceof AddressLayout al) {
            al = al.withoutName();
            if (nested) {
                return al.withoutTargetLayout();
            }
            if (al.targetLayout().isPresent()) {
                MemoryLayout tl = al.targetLayout().get();
                return al.withTargetLayout(
                        MemoryLayout.paddingLayout(tl.byteSize())
                                .withByteAlignment(tl.byteAlignment()));
            }
            return al;
        }
        return ml.withoutName(); // ValueLayout and PaddingLayout
    }

    private static FunctionDescriptor stripNames(FunctionDescriptor function, boolean forDownCall) {
        var arg_layouts = function.argumentLayouts().stream();
        if (forDownCall) {
            arg_layouts = arg_layouts
                    .map(l -> l instanceof AddressLayout al ? al.withoutTargetLayout() : l);
        }
        arg_layouts = arg_layouts.map(layout -> stripNames(layout, false));
        MemoryLayout[] args = arg_layouts.toArray(MemoryLayout[]::new);
        return function.returnLayout()
                .map(rl -> FunctionDescriptor.of(stripNames(rl, false), args))
                .orElseGet(() -> FunctionDescriptor.ofVoid(args));
    }

    @Override
    public SymbolLookup defaultLookup() {
        return RawNativeLibraries.DEFAULT_LOOKUP;
    }

    @Override
    public Map<String, MemoryLayout> canonicalLayouts() {
        class Holder {
            static final Map<String, MemoryLayout> CANONICAL_LAYOUTS;

            static {
                CANONICAL_LAYOUTS = Map.ofEntries(
                        // specified canonical layouts
                        Map.entry("bool", JAVA_BOOLEAN),
                        Map.entry("char", JAVA_BYTE),
                        Map.entry("short", JAVA_SHORT),
                        Map.entry("int", JAVA_INT),
                        Map.entry("float", JAVA_FLOAT),
                        Map.entry("long", CLayouts.C_LONG),
                        Map.entry("long long", CLayouts.C_LONG_LONG),
                        Map.entry("double", CLayouts.C_DOUBLE),
                        Map.entry("void*", ADDRESS),
                        Map.entry("size_t", CLayouts.C_LONG),
                        Map.entry("wchar_t", CLayouts.C_WCHAR_T),

                        // unspecified size-dependent layouts
                        Map.entry("int8_t", JAVA_BYTE),
                        Map.entry("int16_t", JAVA_SHORT),
                        Map.entry("int32_t", JAVA_INT),
                        Map.entry("int64_t", CLayouts.C_LONG_LONG),
                        Map.entry("intptr_t", CLayouts.C_LONG),

                        // unspecified JNI layouts
                        Map.entry("jboolean", JAVA_BOOLEAN),
                        Map.entry("jchar", JAVA_CHAR),
                        Map.entry("jbyte", JAVA_BYTE),
                        Map.entry("jshort", JAVA_SHORT),
                        Map.entry("jint", JAVA_INT),
                        Map.entry("jlong", CLayouts.C_LONG_LONG),
                        Map.entry("jfloat", JAVA_FLOAT),
                        Map.entry("jdouble", CLayouts.C_DOUBLE)
                );
            }
        }

        return Holder.CANONICAL_LAYOUTS;
    }
}
