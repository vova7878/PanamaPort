package com.v7878.llvm;

import static com.v7878.llvm.LibLLVM.LLVM;
import static com.v7878.llvm.LibLLVM.LLVM_SCOPE;
import static com.v7878.llvm.Types.LLVMBool;
import static com.v7878.llvm.Types.cLLVMMemoryBufferRef;
import static com.v7878.llvm._Utils.CONST_CHAR_PTR;
import static com.v7878.llvm._Utils.UINT64_T;
import static com.v7878.llvm._Utils.VOID_PTR;
import static com.v7878.llvm._Utils.addressToLLVMString;
import static com.v7878.llvm._Utils.addressToString;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.llvm.Types.AddressValue;
import com.v7878.llvm.Types.LLVMMemoryBufferRef;
import com.v7878.unsafe.foreign.SimpleBulkLinker;
import com.v7878.unsafe.foreign.SimpleBulkLinker.SymbolHolder2;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class ObjectFile {

    /*
     * @defgroup LLVMCObject Object file reading and writing
     * @ingroup LLVMC
     */

    static final Class<?> cLLVMObjectFileRef = VOID_PTR;
    static final Class<?> cLLVMSectionIteratorRef = VOID_PTR;
    static final Class<?> cLLVMSymbolIteratorRef = VOID_PTR;
    static final Class<?> cLLVMRelocationIteratorRef = VOID_PTR;

    public static final class LLVMObjectFileRef extends AddressValue {

        LLVMObjectFileRef(long value) {
            super(value);
        }
    }

    public static final class LLVMSectionIteratorRef extends AddressValue {
        LLVMSectionIteratorRef(long value) {
            super(value);
        }
    }

    public static final class LLVMSymbolIteratorRef extends AddressValue {
        LLVMSymbolIteratorRef(long value) {
            super(value);
        }
    }

    public static final class LLVMRelocationIteratorRef extends AddressValue {
        LLVMRelocationIteratorRef(long value) {
            super(value);
        }
    }

    private enum Function implements SymbolHolder2 {
        LLVMCreateObjectFile(cLLVMObjectFileRef, cLLVMMemoryBufferRef),
        LLVMDisposeObjectFile(void.class, cLLVMObjectFileRef),
        LLVMGetSections(cLLVMSectionIteratorRef, cLLVMObjectFileRef),
        LLVMDisposeSectionIterator(void.class, cLLVMSectionIteratorRef),
        LLVMIsSectionIteratorAtEnd(LLVMBool, cLLVMObjectFileRef, cLLVMSectionIteratorRef),
        LLVMMoveToNextSection(void.class, cLLVMSectionIteratorRef),
        LLVMMoveToContainingSection(void.class, cLLVMSectionIteratorRef, cLLVMSymbolIteratorRef),
        LLVMGetSymbols(cLLVMSymbolIteratorRef, cLLVMObjectFileRef),
        LLVMDisposeSymbolIterator(void.class, cLLVMSymbolIteratorRef),
        LLVMIsSymbolIteratorAtEnd(LLVMBool, cLLVMObjectFileRef, cLLVMSymbolIteratorRef),
        LLVMMoveToNextSymbol(void.class, cLLVMSymbolIteratorRef),
        LLVMGetSectionName(CONST_CHAR_PTR, cLLVMSectionIteratorRef),
        LLVMGetSectionSize(UINT64_T, cLLVMSectionIteratorRef),
        LLVMGetSectionContents(CONST_CHAR_PTR, cLLVMSectionIteratorRef),
        LLVMGetSectionAddress(UINT64_T, cLLVMSectionIteratorRef),
        LLVMGetSectionContainsSymbol(LLVMBool, cLLVMSectionIteratorRef, cLLVMSymbolIteratorRef),
        LLVMGetRelocations(cLLVMRelocationIteratorRef, cLLVMSectionIteratorRef),
        LLVMDisposeRelocationIterator(void.class, cLLVMRelocationIteratorRef),
        LLVMIsRelocationIteratorAtEnd(LLVMBool, cLLVMSectionIteratorRef, cLLVMRelocationIteratorRef),
        LLVMMoveToNextRelocation(void.class, cLLVMRelocationIteratorRef),
        LLVMGetSymbolName(CONST_CHAR_PTR, cLLVMSymbolIteratorRef),
        LLVMGetSymbolAddress(UINT64_T, cLLVMSymbolIteratorRef),
        LLVMGetSymbolSize(UINT64_T, cLLVMSymbolIteratorRef),
        LLVMGetRelocationOffset(UINT64_T, cLLVMRelocationIteratorRef),
        LLVMGetRelocationSymbol(cLLVMSymbolIteratorRef, cLLVMRelocationIteratorRef),
        LLVMGetRelocationType(UINT64_T, cLLVMRelocationIteratorRef),
        LLVMGetRelocationTypeName(CONST_CHAR_PTR, cLLVMRelocationIteratorRef),
        LLVMGetRelocationValueString(CONST_CHAR_PTR, cLLVMRelocationIteratorRef);

        static {
            SimpleBulkLinker.processSymbols(LLVM, LLVM_SCOPE, Function.values());
        }

        private final MethodType type;

        private LongSupplier symbol;
        private Supplier<MethodHandle> handle;

        Function(Class<?> rtype, Class<?>... atypes) {
            this.type = MethodType.methodType(rtype, atypes);
        }

        @Override
        public MethodType type() {
            return type;
        }

        @Override
        public void setSymbol(LongSupplier symbol) {
            this.symbol = symbol;
        }

        @Override
        public void setHandle(Supplier<MethodHandle> handle) {
            this.handle = handle;
        }

        public long symbol() {
            return symbol.getAsLong();
        }

        public MethodHandle handle() {
            return Objects.requireNonNull(handle.get());
        }

        @Override
        public String toString() {
            return name() + "{" +
                    "type=" + type +
                    ", symbol=" + symbol() +
                    ", handle=" + handle() + '}';
        }
    }

    // ObjectFile creation

    public static LLVMObjectFileRef LLVMCreateObjectFile(LLVMMemoryBufferRef MemBuf) {
        return nothrows_run(() -> new LLVMObjectFileRef((long) Function.LLVMCreateObjectFile.handle().invoke(MemBuf.value())));
    }

    public static void LLVMDisposeObjectFile(LLVMObjectFileRef ObjectFile) {
        nothrows_run(() -> Function.LLVMDisposeObjectFile.handle().invoke(ObjectFile.value()));
    }

    // ObjectFile Section iterators

    public static LLVMSectionIteratorRef LLVMGetSections(LLVMObjectFileRef ObjectFile) {
        return nothrows_run(() -> new LLVMSectionIteratorRef((long) Function.LLVMGetSections.handle().invoke(ObjectFile.value())));
    }

    public static void LLVMDisposeSectionIterator(LLVMSectionIteratorRef SI) {
        nothrows_run(() -> Function.LLVMDisposeSectionIterator.handle().invoke(SI.value()));
    }

    public static boolean LLVMIsSectionIteratorAtEnd(LLVMObjectFileRef ObjectFile, LLVMSectionIteratorRef SI) {
        return nothrows_run(() -> (boolean) Function.LLVMIsSectionIteratorAtEnd.handle().invoke(ObjectFile.value(), SI.value()));
    }

    public static void LLVMMoveToNextSection(LLVMSectionIteratorRef SI) {
        nothrows_run(() -> Function.LLVMMoveToNextSection.handle().invoke(SI.value()));
    }

    public static void LLVMMoveToContainingSection(LLVMSectionIteratorRef Sect, LLVMSymbolIteratorRef Sym) {
        nothrows_run(() -> Function.LLVMMoveToContainingSection.handle().invoke(Sect.value(), Sym.value()));
    }

    // ObjectFile Symbol iterators

    public static LLVMSymbolIteratorRef LLVMGetSymbols(LLVMObjectFileRef ObjectFile) {
        return nothrows_run(() -> new LLVMSymbolIteratorRef((long) Function.LLVMGetSymbols.handle().invoke(ObjectFile.value())));
    }

    public static void LLVMDisposeSymbolIterator(LLVMSymbolIteratorRef SI) {
        nothrows_run(() -> Function.LLVMDisposeSymbolIterator.handle().invoke(SI.value()));
    }

    public static boolean LLVMIsSymbolIteratorAtEnd(LLVMObjectFileRef ObjectFile, LLVMSymbolIteratorRef SI) {
        return nothrows_run(() -> (boolean) Function.LLVMIsSymbolIteratorAtEnd.handle().invoke(ObjectFile.value(), SI.value()));
    }

    public static void LLVMMoveToNextSymbol(LLVMSymbolIteratorRef SI) {
        nothrows_run(() -> Function.LLVMMoveToNextSymbol.handle().invoke(SI.value()));
    }

    // SectionRef accessors

    public static String LLVMGetSectionName(LLVMSectionIteratorRef SI) {
        return nothrows_run(() -> addressToString((long) Function.LLVMGetSectionName.handle().invoke(SI.value())));
    }

    public static long /* uint64_t */ LLVMGetSectionSize(LLVMSectionIteratorRef SI) {
        return nothrows_run(() -> (long) Function.LLVMGetSectionSize.handle().invoke(SI.value()));
    }

    public static String LLVMGetSectionContents(LLVMSectionIteratorRef SI) {
        return nothrows_run(() -> addressToString((long) Function.LLVMGetSectionContents.handle().invoke(SI.value())));
    }

    public static long /* uint64_t */ LLVMGetSectionAddress(LLVMSectionIteratorRef SI) {
        return nothrows_run(() -> (long) Function.LLVMGetSectionAddress.handle().invoke(SI.value()));
    }

    public static boolean LLVMGetSectionContainsSymbol(LLVMSectionIteratorRef SI, LLVMSymbolIteratorRef Sym) {
        return nothrows_run(() -> (boolean) Function.LLVMGetSectionContainsSymbol.handle().invoke(SI.value(), Sym.value()));
    }

    // Section Relocation iterators

    public static LLVMRelocationIteratorRef LLVMGetRelocations(LLVMSectionIteratorRef Section) {
        return nothrows_run(() -> new LLVMRelocationIteratorRef((long) Function.LLVMGetRelocations.handle().invoke(Section.value())));
    }

    public static void LLVMDisposeRelocationIterator(LLVMRelocationIteratorRef RI) {
        nothrows_run(() -> Function.LLVMDisposeRelocationIterator.handle().invoke(RI.value()));
    }

    public static boolean LLVMIsRelocationIteratorAtEnd(LLVMSectionIteratorRef Section, LLVMRelocationIteratorRef RI) {
        return nothrows_run(() -> (boolean) Function.LLVMIsRelocationIteratorAtEnd.handle().invoke(Section.value(), RI.value()));
    }

    public static void LLVMMoveToNextRelocation(LLVMRelocationIteratorRef RI) {
        nothrows_run(() -> Function.LLVMMoveToNextRelocation.handle().invoke(RI.value()));
    }

    // SymbolRef accessors

    public static String LLVMGetSymbolName(LLVMSymbolIteratorRef SI) {
        return nothrows_run(() -> addressToString((long) Function.LLVMGetSymbolName.handle().invoke(SI.value())));
    }

    public static long /* uint64_t */ LLVMGetSymbolAddress(LLVMSymbolIteratorRef SI) {
        return nothrows_run(() -> (long) Function.LLVMGetSymbolAddress.handle().invoke(SI.value()));
    }

    public static long /* uint64_t */ LLVMGetSymbolSize(LLVMSymbolIteratorRef SI) {
        return nothrows_run(() -> (long) Function.LLVMGetSymbolSize.handle().invoke(SI.value()));
    }

    // RelocationRef accessors

    public static long /* uint64_t */ LLVMGetRelocationOffset(LLVMRelocationIteratorRef RI) {
        return nothrows_run(() -> (long) Function.LLVMGetRelocationOffset.handle().invoke(RI.value()));
    }

    public static LLVMSymbolIteratorRef LLVMGetRelocationSymbol(LLVMRelocationIteratorRef RI) {
        return nothrows_run(() -> new LLVMSymbolIteratorRef((long) Function.LLVMGetRelocationSymbol.handle().invoke(RI.value())));
    }

    public static long /* uint64_t */ LLVMGetRelocationType(LLVMRelocationIteratorRef RI) {
        return nothrows_run(() -> (long) Function.LLVMGetRelocationType.handle().invoke(RI.value()));
    }

    // NOTE: Caller takes ownership of returned string of the two
    // following functions.

    public static String LLVMGetRelocationTypeName(LLVMRelocationIteratorRef RI) {
        // NOTE: using addressToLLVMString is wrong, but it just frees the pointer, that's ok
        return nothrows_run(() -> addressToLLVMString((long) Function.LLVMGetRelocationTypeName.handle().invoke(RI.value())));
    }

    public static String LLVMGetRelocationValueString(LLVMRelocationIteratorRef RI) {
        // NOTE: using addressToLLVMString is wrong, but it just frees the pointer, that's ok
        return nothrows_run(() -> addressToLLVMString((long) Function.LLVMGetRelocationValueString.handle().invoke(RI.value())));
    }
}
