package com.v7878.llvm;

import static com.v7878.llvm.LibLLVM.LLVM;
import static com.v7878.llvm._Utils.addressToLLVMString;
import static com.v7878.llvm._Utils.addressToString;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.BOOL_AS_INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;

import androidx.annotation.Keep;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.AddressValue;
import com.v7878.llvm.Types.LLVMMemoryBufferRef;
import com.v7878.unsafe.AndroidUnsafe;
import com.v7878.unsafe.Utils.FineClosable;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;

public class ObjectFile {

    /*
     * @defgroup LLVMCObject Object file reading and writing
     * @ingroup LLVMC
     */

    public static final class LLVMObjectFileRef extends AddressValue implements FineClosable {

        private LLVMObjectFileRef(long value) {
            super(value);
        }

        public static LLVMObjectFileRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMObjectFileRef of 0");
            }
            return new LLVMObjectFileRef(value);
        }

        public static LLVMObjectFileRef ofNullable(long value) {
            return value == 0 ? null : new LLVMObjectFileRef(value);
        }

        @Override
        public void close() {
            LLVMDisposeObjectFile(this);
        }
    }

    public static final class LLVMSectionIteratorRef extends AddressValue implements FineClosable {

        private LLVMSectionIteratorRef(long value) {
            super(value);
        }

        public static LLVMSectionIteratorRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMSectionIteratorRef of 0");
            }
            return new LLVMSectionIteratorRef(value);
        }

        public static LLVMSectionIteratorRef ofNullable(long value) {
            return value == 0 ? null : new LLVMSectionIteratorRef(value);
        }

        @Override
        public void close() {
            LLVMDisposeSectionIterator(this);
        }
    }

    public static final class LLVMSymbolIteratorRef extends AddressValue implements FineClosable {

        private LLVMSymbolIteratorRef(long value) {
            super(value);
        }

        public static LLVMSymbolIteratorRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMSymbolIteratorRef of 0");
            }
            return new LLVMSymbolIteratorRef(value);
        }

        public static LLVMSymbolIteratorRef ofNullable(long value) {
            return value == 0 ? null : new LLVMSymbolIteratorRef(value);
        }

        @Override
        public void close() {
            LLVMDisposeSymbolIterator(this);
        }
    }

    public static final class LLVMRelocationIteratorRef extends AddressValue implements FineClosable {

        private LLVMRelocationIteratorRef(long value) {
            super(value);
        }

        public static LLVMRelocationIteratorRef of(long value) {
            if (value == 0) {
                throw new IllegalStateException("LLVMRelocationIteratorRef of 0");
            }
            return new LLVMRelocationIteratorRef(value);
        }

        public static LLVMRelocationIteratorRef ofNullable(long value) {
            return value == 0 ? null : new LLVMRelocationIteratorRef(value);
        }

        @Override
        public void close() {
            LLVMDisposeRelocationIterator(this);
        }
    }

    @Keep
    private abstract static class Native {

        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol("LLVMCreateObjectFile")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMCreateObjectFile(long MemBuf);

        @LibrarySymbol("LLVMDisposeObjectFile")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposeObjectFile(long ObjectFile);

        @LibrarySymbol("LLVMGetSections")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetSections(long ObjectFile);

        @LibrarySymbol("LLVMDisposeSectionIterator")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposeSectionIterator(long SI);

        @LibrarySymbol("LLVMIsSectionIteratorAtEnd")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMIsSectionIteratorAtEnd(long ObjectFile, long SI);

        @LibrarySymbol("LLVMMoveToNextSection")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMMoveToNextSection(long SI);

        @LibrarySymbol("LLVMMoveToContainingSection")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract void LLVMMoveToContainingSection(long Sect, long Sym);

        @LibrarySymbol("LLVMGetSymbols")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetSymbols(long ObjectFile);

        @LibrarySymbol("LLVMDisposeSymbolIterator")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposeSymbolIterator(long SI);

        @LibrarySymbol("LLVMIsSymbolIteratorAtEnd")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMIsSymbolIteratorAtEnd(long ObjectFile, long SI);

        @LibrarySymbol("LLVMMoveToNextSymbol")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMMoveToNextSymbol(long SI);

        @LibrarySymbol("LLVMGetSectionName")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetSectionName(long SI);

        @LibrarySymbol("LLVMGetSectionSize")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD})
        abstract long LLVMGetSectionSize(long SI);

        @LibrarySymbol("LLVMGetSectionContents")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetSectionContents(long SI);

        @LibrarySymbol("LLVMGetSectionAddress")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD})
        abstract long LLVMGetSectionAddress(long SI);

        @LibrarySymbol("LLVMGetSectionContainsSymbol")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMGetSectionContainsSymbol(long SI, long Sym);

        @LibrarySymbol("LLVMGetRelocations")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetRelocations(long Section);

        @LibrarySymbol("LLVMDisposeRelocationIterator")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMDisposeRelocationIterator(long RI);

        @LibrarySymbol("LLVMIsRelocationIteratorAtEnd")
        @CallSignature(type = CRITICAL, ret = BOOL_AS_INT, args = {LONG_AS_WORD, LONG_AS_WORD})
        abstract boolean LLVMIsRelocationIteratorAtEnd(long Section, long RI);

        @LibrarySymbol("LLVMMoveToNextRelocation")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMMoveToNextRelocation(long RI);

        @LibrarySymbol("LLVMGetSymbolName")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetSymbolName(long SI);

        @LibrarySymbol("LLVMGetSymbolAddress")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD})
        abstract long LLVMGetSymbolAddress(long SI);

        @LibrarySymbol("LLVMGetSymbolSize")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD})
        abstract long LLVMGetSymbolSize(long SI);

        @LibrarySymbol("LLVMGetRelocationOffset")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD})
        abstract long LLVMGetRelocationOffset(long RI);

        @LibrarySymbol("LLVMGetRelocationSymbol")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetRelocationSymbol(long RI);

        @LibrarySymbol("LLVMGetRelocationType")
        @CallSignature(type = CRITICAL, ret = LONG, args = {LONG_AS_WORD})
        abstract long LLVMGetRelocationType(long RI);

        @LibrarySymbol("LLVMGetRelocationTypeName")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetRelocationTypeName(long RI);

        @LibrarySymbol("LLVMGetRelocationValueString")
        @CallSignature(type = CRITICAL, ret = LONG_AS_WORD, args = {LONG_AS_WORD})
        abstract long LLVMGetRelocationValueString(long RI);

        static final Native INSTANCE = AndroidUnsafe.allocateInstance(
                BulkLinker.processSymbols(SCOPE, Native.class, LLVM));
    }

    // ObjectFile creation

    public static LLVMObjectFileRef LLVMCreateObjectFile(LLVMMemoryBufferRef MemBuf) {
        return LLVMObjectFileRef.ofNullable(Native.INSTANCE.LLVMCreateObjectFile(MemBuf.value()));
    }

    public static void LLVMDisposeObjectFile(LLVMObjectFileRef ObjectFile) {
        Native.INSTANCE.LLVMDisposeObjectFile(ObjectFile.value());
    }

    // ObjectFile Section iterators

    public static LLVMSectionIteratorRef LLVMGetSections(LLVMObjectFileRef ObjectFile) {
        return LLVMSectionIteratorRef.ofNullable(Native.INSTANCE.LLVMGetSections(ObjectFile.value()));
    }

    public static void LLVMDisposeSectionIterator(LLVMSectionIteratorRef SI) {
        Native.INSTANCE.LLVMDisposeSectionIterator(SI.value());
    }

    public static boolean LLVMIsSectionIteratorAtEnd(LLVMObjectFileRef ObjectFile, LLVMSectionIteratorRef SI) {
        return Native.INSTANCE.LLVMIsSectionIteratorAtEnd(ObjectFile.value(), SI.value());
    }

    public static void LLVMMoveToNextSection(LLVMSectionIteratorRef SI) {
        Native.INSTANCE.LLVMMoveToNextSection(SI.value());
    }

    public static void LLVMMoveToContainingSection(LLVMSectionIteratorRef Sect, LLVMSymbolIteratorRef Sym) {
        Native.INSTANCE.LLVMMoveToContainingSection(Sect.value(), Sym.value());
    }

    // ObjectFile Symbol iterators

    public static LLVMSymbolIteratorRef LLVMGetSymbols(LLVMObjectFileRef ObjectFile) {
        return LLVMSymbolIteratorRef.ofNullable(Native.INSTANCE.LLVMGetSymbols(ObjectFile.value()));
    }

    public static void LLVMDisposeSymbolIterator(LLVMSymbolIteratorRef SI) {
        Native.INSTANCE.LLVMDisposeSymbolIterator(SI.value());
    }

    public static boolean LLVMIsSymbolIteratorAtEnd(LLVMObjectFileRef ObjectFile, LLVMSymbolIteratorRef SI) {
        return Native.INSTANCE.LLVMIsSymbolIteratorAtEnd(ObjectFile.value(), SI.value());
    }

    public static void LLVMMoveToNextSymbol(LLVMSymbolIteratorRef SI) {
        Native.INSTANCE.LLVMMoveToNextSymbol(SI.value());
    }

    // SectionRef accessors

    public static String LLVMGetSectionName(LLVMSectionIteratorRef SI) {
        return addressToString(Native.INSTANCE.LLVMGetSectionName(SI.value()));
    }

    public static long /* uint64_t */ LLVMGetSectionSize(LLVMSectionIteratorRef SI) {
        return Native.INSTANCE.LLVMGetSectionSize(SI.value());
    }

    /* package-private */
    static long LLVMGetSectionContents(LLVMSectionIteratorRef SI) {
        return Native.INSTANCE.LLVMGetSectionContents(SI.value());
    }

    // Port-added
    public static MemorySegment LLVMGetSectionSegment(LLVMSectionIteratorRef SI) {
        long address = LLVMGetSectionContents(SI);
        long size = LLVMGetSectionSize(SI);
        return MemorySegment.ofAddress(address).reinterpret(size).asReadOnly();
    }

    public static long /* uint64_t */ LLVMGetSectionAddress(LLVMSectionIteratorRef SI) {
        return Native.INSTANCE.LLVMGetSectionAddress(SI.value());
    }

    public static boolean LLVMGetSectionContainsSymbol(LLVMSectionIteratorRef SI, LLVMSymbolIteratorRef Sym) {
        return Native.INSTANCE.LLVMGetSectionContainsSymbol(SI.value(), Sym.value());
    }

    // Section Relocation iterators

    public static LLVMRelocationIteratorRef LLVMGetRelocations(LLVMSectionIteratorRef Section) {
        return LLVMRelocationIteratorRef.ofNullable(Native.INSTANCE.LLVMGetRelocations(Section.value()));
    }

    public static void LLVMDisposeRelocationIterator(LLVMRelocationIteratorRef RI) {
        Native.INSTANCE.LLVMDisposeRelocationIterator(RI.value());
    }

    public static boolean LLVMIsRelocationIteratorAtEnd(LLVMSectionIteratorRef Section, LLVMRelocationIteratorRef RI) {
        return Native.INSTANCE.LLVMIsRelocationIteratorAtEnd(Section.value(), RI.value());
    }

    public static void LLVMMoveToNextRelocation(LLVMRelocationIteratorRef RI) {
        Native.INSTANCE.LLVMMoveToNextRelocation(RI.value());
    }

    // SymbolRef accessors

    public static String LLVMGetSymbolName(LLVMSymbolIteratorRef SI) {
        return addressToString(Native.INSTANCE.LLVMGetSymbolName(SI.value()));
    }

    public static long /* uint64_t */ LLVMGetSymbolAddress(LLVMSymbolIteratorRef SI) {
        return Native.INSTANCE.LLVMGetSymbolAddress(SI.value());
    }

    public static long /* uint64_t */ LLVMGetSymbolSize(LLVMSymbolIteratorRef SI) {
        return Native.INSTANCE.LLVMGetSymbolSize(SI.value());
    }

    // RelocationRef accessors

    public static long /* uint64_t */ LLVMGetRelocationOffset(LLVMRelocationIteratorRef RI) {
        return Native.INSTANCE.LLVMGetRelocationOffset(RI.value());
    }

    public static LLVMSymbolIteratorRef LLVMGetRelocationSymbol(LLVMRelocationIteratorRef RI) {
        return LLVMSymbolIteratorRef.ofNullable(Native.INSTANCE.LLVMGetRelocationSymbol(RI.value()));
    }

    public static long /* uint64_t */ LLVMGetRelocationType(LLVMRelocationIteratorRef RI) {
        return Native.INSTANCE.LLVMGetRelocationType(RI.value());
    }

    // NOTE: Caller takes ownership of returned string of the two
    // following functions.

    public static String LLVMGetRelocationTypeName(LLVMRelocationIteratorRef RI) {
        // NOTE: using addressToLLVMString is wrong, but it just frees the pointer, that's ok
        return addressToLLVMString(Native.INSTANCE.LLVMGetRelocationTypeName(RI.value()));
    }

    public static String LLVMGetRelocationValueString(LLVMRelocationIteratorRef RI) {
        // NOTE: using addressToLLVMString is wrong, but it just frees the pointer, that's ok
        return addressToLLVMString(Native.INSTANCE.LLVMGetRelocationValueString(RI.value()));
    }
}
