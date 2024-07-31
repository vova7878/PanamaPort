package com.v7878.unsafe.llvm;

import static com.v7878.llvm.Analysis.LLVMVerifyModule;
import static com.v7878.llvm.Core.LLVMBuildAdd;
import static com.v7878.llvm.Core.LLVMBuildIntToPtr;
import static com.v7878.llvm.Core.LLVMBuildLoad;
import static com.v7878.llvm.Core.LLVMBuildNeg;
import static com.v7878.llvm.Core.LLVMBuildPtrToInt;
import static com.v7878.llvm.Core.LLVMBuildTrunc;
import static com.v7878.llvm.Core.LLVMBuildZExtOrBitCast;
import static com.v7878.llvm.Core.LLVMConstAdd;
import static com.v7878.llvm.Core.LLVMConstInt;
import static com.v7878.llvm.Core.LLVMConstIntOfArbitraryPrecision;
import static com.v7878.llvm.Core.LLVMConstIntToPtr;
import static com.v7878.llvm.Core.LLVMCreateBuilderInContext;
import static com.v7878.llvm.Core.LLVMCreatePassManager;
import static com.v7878.llvm.Core.LLVMGetBasicBlockParent;
import static com.v7878.llvm.Core.LLVMGetGlobalParent;
import static com.v7878.llvm.Core.LLVMGetInsertBlock;
import static com.v7878.llvm.Core.LLVMGetModuleContext;
import static com.v7878.llvm.Core.LLVMModuleCreateWithNameInContext;
import static com.v7878.llvm.Core.LLVMRunPassManager;
import static com.v7878.llvm.ObjectFile.LLVMCreateObjectFile;
import static com.v7878.llvm.ObjectFile.LLVMGetSectionSegment;
import static com.v7878.llvm.ObjectFile.LLVMGetSections;
import static com.v7878.llvm.ObjectFile.LLVMGetSymbolAddress;
import static com.v7878.llvm.ObjectFile.LLVMGetSymbolName;
import static com.v7878.llvm.ObjectFile.LLVMGetSymbolSize;
import static com.v7878.llvm.ObjectFile.LLVMGetSymbols;
import static com.v7878.llvm.ObjectFile.LLVMIsSymbolIteratorAtEnd;
import static com.v7878.llvm.ObjectFile.LLVMMoveToContainingSection;
import static com.v7878.llvm.ObjectFile.LLVMMoveToNextSymbol;
import static com.v7878.llvm.PassManagerBuilder.LLVMPassManagerBuilderCreate;
import static com.v7878.llvm.PassManagerBuilder.LLVMPassManagerBuilderPopulateModulePassManager;
import static com.v7878.llvm.TargetMachine.LLVMCodeGenFileType.LLVMObjectFile;
import static com.v7878.llvm.TargetMachine.LLVMTargetMachineEmitToMemoryBuffer;
import static com.v7878.unsafe.Utils.shouldNotHappen;
import static com.v7878.unsafe.llvm.LLVMGlobals.int128_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int16_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int1_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int32_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int64_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.int8_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.intptr_t;
import static com.v7878.unsafe.llvm.LLVMGlobals.newContext;
import static com.v7878.unsafe.llvm.LLVMGlobals.newDefaultMachine;
import static com.v7878.unsafe.llvm.LLVMGlobals.ptr_t;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.ValueLayout;
import com.v7878.llvm.LLVMException;
import com.v7878.llvm.ObjectFile.LLVMObjectFileRef;
import com.v7878.llvm.Types.LLVMBuilderRef;
import com.v7878.llvm.Types.LLVMContextRef;
import com.v7878.llvm.Types.LLVMMemoryBufferRef;
import com.v7878.llvm.Types.LLVMModuleRef;
import com.v7878.llvm.Types.LLVMTypeRef;
import com.v7878.llvm.Types.LLVMValueRef;
import com.v7878.unsafe.NativeCodeBlob;
import com.v7878.unsafe.VM;

import java.util.List;
import java.util.Objects;

public class LLVMUtils {

    public static MemorySegment[] getFunctionsCode(LLVMObjectFileRef obj, String... names) {
        Objects.requireNonNull(obj);
        Objects.requireNonNull(names);
        if (names.length == 0) {
            return new MemorySegment[0];
        }

        List<String> l_names = List.of(names);

        MemorySegment[] out = new MemorySegment[names.length];

        try (var sym = LLVMGetSymbols(obj); var section = LLVMGetSections(obj)) {
            for (; !LLVMIsSymbolIteratorAtEnd(obj, sym); LLVMMoveToNextSymbol(sym)) {
                String name = LLVMGetSymbolName(sym);
                int index = l_names.indexOf(name);
                if (index >= 0) {
                    long offset = LLVMGetSymbolAddress(sym);
                    long size = LLVMGetSymbolSize(sym);

                    LLVMMoveToContainingSection(section, sym);
                    out[index] = LLVMGetSectionSegment(section).asSlice(offset, size);
                }
            }
        }

        for (int i = 0; i < names.length; i++) {
            if (out[i] == null) {
                throw new IllegalArgumentException("Can`t find code for function: \"" + names[i] + "\"");
            }
        }

        return out;
    }

    public static MemorySegment getFunctionCode(LLVMObjectFileRef obj, String name) {
        return getFunctionsCode(obj, name)[0];
    }

    public static LLVMModuleRef getBuilderModule(LLVMBuilderRef builder) {
        return LLVMGetGlobalParent(LLVMGetBasicBlockParent(LLVMGetInsertBlock(builder)));
    }

    public static LLVMContextRef getModuleContext(LLVMModuleRef module) {
        return LLVMGetModuleContext(module);
    }

    public static LLVMContextRef getBuilderContext(LLVMBuilderRef builder) {
        return LLVMGetModuleContext(getBuilderModule(builder));
    }

    public static LLVMValueRef buildRawObjectToAddress(LLVMBuilderRef builder, LLVMValueRef base, LLVMValueRef offset) {
        if (VM.isPoisonReferences()) {
            base = LLVMBuildNeg(builder, base, "");
        }
        base = LLVMBuildZExtOrBitCast(builder, base, intptr_t(getBuilderContext(builder)), "");
        return LLVMBuildAdd(builder, base, offset, "");
    }

    public static LLVMValueRef buildRawObjectToPointer(LLVMBuilderRef builder, LLVMValueRef base, LLVMValueRef offset, LLVMTypeRef type) {
        return LLVMBuildIntToPtr(builder, buildRawObjectToAddress(builder, base, offset), ptr_t(type), "");
    }

    public static LLVMValueRef buildAddressToRawObject(LLVMBuilderRef builder, LLVMValueRef base, LLVMValueRef offset) {
        base = LLVMBuildAdd(builder, base, offset, "");
        base = LLVMBuildTrunc(builder, base, int32_t(getBuilderContext(builder)), "");
        if (VM.isPoisonReferences()) {
            base = LLVMBuildNeg(builder, base, "");
        }
        return base;
    }

    public static LLVMValueRef buildPointerToRawObject(LLVMBuilderRef builder, LLVMValueRef base, LLVMValueRef offset) {
        base = LLVMBuildPtrToInt(builder, base, intptr_t(getBuilderContext(builder)), "");
        return buildAddressToRawObject(builder, base, offset);
    }

    public static LLVMValueRef const_int128(LLVMContextRef context, long low, long high) {
        return LLVMConstIntOfArbitraryPrecision(int128_t(context), low, high);
    }

    public static LLVMValueRef const_intptr(LLVMContextRef context, long value) {
        return LLVMConstInt(intptr_t(context), value, false);
    }

    public static LLVMValueRef const_int64(LLVMContextRef context, long value) {
        return LLVMConstInt(int64_t(context), value, false);
    }

    public static LLVMValueRef const_int32(LLVMContextRef context, int value) {
        return LLVMConstInt(int32_t(context), value, false);
    }

    public static LLVMValueRef const_int16(LLVMContextRef context, short value) {
        return LLVMConstInt(int16_t(context), value, false);
    }

    public static LLVMValueRef const_int8(LLVMContextRef context, byte value) {
        return LLVMConstInt(int8_t(context), value, false);
    }

    public static LLVMValueRef const_int1(LLVMContextRef context, boolean value) {
        return LLVMConstInt(int1_t(context), value ? 1 : 0, false);
    }

    public static LLVMValueRef const_ptr(LLVMContextRef context, LLVMTypeRef type, long value, long offset) {
        var ptr = LLVMConstAdd(const_intptr(context, value), const_intptr(context, offset));
        return LLVMConstIntToPtr(ptr, ptr_t(type));
    }

    public static LLVMValueRef const_ptr(LLVMContextRef context, LLVMTypeRef type, long value) {
        return const_ptr(context, type, value, 0);
    }

    public static LLVMValueRef const_load_ptr(LLVMBuilderRef builder, LLVMTypeRef type, long value, long offset) {
        var context = getBuilderContext(builder);
        var ptr = const_ptr(context, ptr_t(type), value, offset);
        return LLVMBuildLoad(builder, ptr, "");
    }

    public static LLVMValueRef const_load_ptr(LLVMBuilderRef builder, LLVMTypeRef type, long value) {
        return const_load_ptr(builder, type, value, 0);
    }

    public interface Generator {
        void generate(LLVMContextRef context, LLVMModuleRef module, LLVMBuilderRef builder);
    }

    public static LLVMMemoryBufferRef generateModuleToBuffer(Generator generator) throws LLVMException {
        Objects.requireNonNull(generator);

        try (var context = newContext(); var builder = LLVMCreateBuilderInContext(context);
             var module = LLVMModuleCreateWithNameInContext("generic", context)) {

            generator.generate(context, module, builder);

            LLVMVerifyModule(module);

            try (var pass_manager = LLVMCreatePassManager()) {
                try (var pmb = LLVMPassManagerBuilderCreate()) {
                    LLVMPassManagerBuilderPopulateModulePassManager(pmb, pass_manager);
                }
                LLVMRunPassManager(pass_manager, module);
            }

            try (var machine = newDefaultMachine()) {
                return LLVMTargetMachineEmitToMemoryBuffer(machine, module, LLVMObjectFile);
            }
        }
    }

    public static byte[] generateFunctionCodeArray(Generator generator, String name) {
        try {
            var buf = generateModuleToBuffer(generator);
            try (var of = LLVMCreateObjectFile(buf)) {
                return getFunctionCode(of, name).toArray(ValueLayout.JAVA_BYTE);
            }
        } catch (LLVMException e) {
            throw shouldNotHappen(e);
        }
    }

    public static MemorySegment generateFunctionCodeSegment(Generator generator, String name, Arena scope) {
        try {
            var buf = generateModuleToBuffer(generator);
            try (var of = LLVMCreateObjectFile(buf)) {
                MemorySegment code = getFunctionCode(of, name);
                return NativeCodeBlob.makeCodeBlob(scope, code)[0];
            }
        } catch (LLVMException e) {
            throw shouldNotHappen(e);
        }
    }
}
