package com.v7878.llvm;

import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.LLVMMemoryBufferRef;

public class ObjectFile {
    public static final class LLVMObjectFileRef implements AutoCloseable {
        public static LLVMObjectFileRef of(long value) {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMObjectFileRef ofNullable(long value) {
            throw new UnsupportedOperationException("Stub!");
        }

        public void close() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static final class LLVMSectionIteratorRef implements AutoCloseable {
        public static LLVMSectionIteratorRef of(long value) {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMSectionIteratorRef ofNullable(long value) {
            throw new UnsupportedOperationException("Stub!");
        }

        public void close() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static final class LLVMSymbolIteratorRef implements AutoCloseable {
        public static LLVMSymbolIteratorRef of(long value) {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMSymbolIteratorRef ofNullable(long value) {
            throw new UnsupportedOperationException("Stub!");
        }

        public void close() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static final class LLVMRelocationIteratorRef implements AutoCloseable {
        public static LLVMRelocationIteratorRef of(long value) {
            throw new UnsupportedOperationException("Stub!");
        }

        public static LLVMRelocationIteratorRef ofNullable(long value) {
            throw new UnsupportedOperationException("Stub!");
        }

        public void close() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static LLVMObjectFileRef LLVMCreateObjectFile(LLVMMemoryBufferRef MemBuf) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeObjectFile(LLVMObjectFileRef ObjectFile) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeRelocationIterator(LLVMRelocationIteratorRef RI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeSectionIterator(LLVMSectionIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeSymbolIterator(LLVMSymbolIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long LLVMGetRelocationOffset(LLVMRelocationIteratorRef RI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMSymbolIteratorRef LLVMGetRelocationSymbol(LLVMRelocationIteratorRef RI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long LLVMGetRelocationType(LLVMRelocationIteratorRef RI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetRelocationTypeName(LLVMRelocationIteratorRef RI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetRelocationValueString(LLVMRelocationIteratorRef RI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMRelocationIteratorRef LLVMGetRelocations(LLVMSectionIteratorRef Section) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long LLVMGetSectionAddress(LLVMSectionIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMGetSectionContainsSymbol(LLVMSectionIteratorRef SI, LLVMSymbolIteratorRef Sym) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetSectionName(LLVMSectionIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MemorySegment LLVMGetSectionSegment(LLVMSectionIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long LLVMGetSectionSize(LLVMSectionIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMSectionIteratorRef LLVMGetSections(LLVMObjectFileRef ObjectFile) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long LLVMGetSymbolAddress(LLVMSymbolIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetSymbolName(LLVMSymbolIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long LLVMGetSymbolSize(LLVMSymbolIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMSymbolIteratorRef LLVMGetSymbols(LLVMObjectFileRef ObjectFile) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsRelocationIteratorAtEnd(LLVMSectionIteratorRef Section, LLVMRelocationIteratorRef RI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsSectionIteratorAtEnd(LLVMObjectFileRef ObjectFile, LLVMSectionIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsSymbolIteratorAtEnd(LLVMObjectFileRef ObjectFile, LLVMSymbolIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMMoveToContainingSection(LLVMSectionIteratorRef Sect, LLVMSymbolIteratorRef Sym) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMMoveToNextRelocation(LLVMRelocationIteratorRef RI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMMoveToNextSection(LLVMSectionIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMMoveToNextSymbol(LLVMSymbolIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }
}
