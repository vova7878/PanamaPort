package com.v7878.llvm;

import com.v7878.foreign.MemorySegment;
import com.v7878.llvm.Types.LLVMMemoryBufferRef;

public class ObjectFile {

    public static final class LLVMObjectFileRef {
        private LLVMObjectFileRef() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static final class LLVMSectionIteratorRef {
        private LLVMSectionIteratorRef() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static final class LLVMSymbolIteratorRef {
        private LLVMSymbolIteratorRef() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static final class LLVMRelocationIteratorRef {
        private LLVMRelocationIteratorRef() {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public static LLVMObjectFileRef LLVMCreateObjectFile(LLVMMemoryBufferRef MemBuf) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeObjectFile(LLVMObjectFileRef ObjectFile) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMSectionIteratorRef LLVMGetSections(LLVMObjectFileRef ObjectFile) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeSectionIterator(LLVMSectionIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsSectionIteratorAtEnd(LLVMObjectFileRef ObjectFile, LLVMSectionIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMMoveToNextSection(LLVMSectionIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMMoveToContainingSection(LLVMSectionIteratorRef Sect, LLVMSymbolIteratorRef Sym) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMSymbolIteratorRef LLVMGetSymbols(LLVMObjectFileRef ObjectFile) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeSymbolIterator(LLVMSymbolIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsSymbolIteratorAtEnd(LLVMObjectFileRef ObjectFile, LLVMSymbolIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMMoveToNextSymbol(LLVMSymbolIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetSectionName(LLVMSectionIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long /* uint64_t */ LLVMGetSectionSize(LLVMSectionIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static MemorySegment LLVMGetSectionSegment(LLVMSectionIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long /* uint64_t */ LLVMGetSectionAddress(LLVMSectionIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMGetSectionContainsSymbol(LLVMSectionIteratorRef SI, LLVMSymbolIteratorRef Sym) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMRelocationIteratorRef LLVMGetRelocations(LLVMSectionIteratorRef Section) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMDisposeRelocationIterator(LLVMRelocationIteratorRef RI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static boolean LLVMIsRelocationIteratorAtEnd(LLVMSectionIteratorRef Section, LLVMRelocationIteratorRef RI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static void LLVMMoveToNextRelocation(LLVMRelocationIteratorRef RI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetSymbolName(LLVMSymbolIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long /* uint64_t */ LLVMGetSymbolAddress(LLVMSymbolIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long /* uint64_t */ LLVMGetSymbolSize(LLVMSymbolIteratorRef SI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long /* uint64_t */ LLVMGetRelocationOffset(LLVMRelocationIteratorRef RI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static LLVMSymbolIteratorRef LLVMGetRelocationSymbol(LLVMRelocationIteratorRef RI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static long /* uint64_t */ LLVMGetRelocationType(LLVMRelocationIteratorRef RI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetRelocationTypeName(LLVMRelocationIteratorRef RI) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String LLVMGetRelocationValueString(LLVMRelocationIteratorRef RI) {
        throw new UnsupportedOperationException("Stub!");
    }
}
