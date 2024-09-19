package com.v7878.unsafe.llvm;

import static com.v7878.llvm.Core.LLVMDoubleTypeInContext;
import static com.v7878.llvm.Core.LLVMFP128TypeInContext;
import static com.v7878.llvm.Core.LLVMFloatTypeInContext;
import static com.v7878.llvm.Core.LLVMFunctionType;
import static com.v7878.llvm.Core.LLVMHalfTypeInContext;
import static com.v7878.llvm.Core.LLVMInt128TypeInContext;
import static com.v7878.llvm.Core.LLVMInt16TypeInContext;
import static com.v7878.llvm.Core.LLVMInt1TypeInContext;
import static com.v7878.llvm.Core.LLVMInt32TypeInContext;
import static com.v7878.llvm.Core.LLVMInt64TypeInContext;
import static com.v7878.llvm.Core.LLVMInt8TypeInContext;
import static com.v7878.llvm.Core.LLVMPointerType;
import static com.v7878.llvm.Core.LLVMVoidTypeInContext;
import static com.v7878.llvm.Types.LLVMContextRef;
import static com.v7878.llvm.Types.LLVMTypeRef;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;

public class LLVMTypes {
    public static LLVMTypeRef ptr_t(LLVMTypeRef type) {
        return LLVMPointerType(type, 0);
    }

    public static LLVMTypeRef void_t(LLVMContextRef context) {
        return LLVMVoidTypeInContext(context);
    }

    public static LLVMTypeRef void_ptr_t(LLVMContextRef context) {
        return ptr_t(void_t(context));
    }

    public static LLVMTypeRef int1_t(LLVMContextRef context) {
        return LLVMInt1TypeInContext(context);
    }

    public static LLVMTypeRef int8_t(LLVMContextRef context) {
        return LLVMInt8TypeInContext(context);
    }

    public static LLVMTypeRef int16_t(LLVMContextRef context) {
        return LLVMInt16TypeInContext(context);
    }

    public static LLVMTypeRef int32_t(LLVMContextRef context) {
        return LLVMInt32TypeInContext(context);
    }

    public static LLVMTypeRef int64_t(LLVMContextRef context) {
        return LLVMInt64TypeInContext(context);
    }

    public static LLVMTypeRef int128_t(LLVMContextRef context) {
        return LLVMInt128TypeInContext(context);
    }

    public static LLVMTypeRef intptr_t(LLVMContextRef context) {
        // TODO?: get from target
        return IS64BIT ? int64_t(context) : int32_t(context);
    }

    public static LLVMTypeRef half_t(LLVMContextRef context) {
        return LLVMHalfTypeInContext(context);
    }

    public static LLVMTypeRef float_t(LLVMContextRef context) {
        return LLVMFloatTypeInContext(context);
    }

    public static LLVMTypeRef double_t(LLVMContextRef context) {
        return LLVMDoubleTypeInContext(context);
    }

    public static LLVMTypeRef fp128_t(LLVMContextRef context) {
        return LLVMFP128TypeInContext(context);
    }

    public static LLVMTypeRef function_t(LLVMTypeRef ret, LLVMTypeRef... args) {
        return LLVMFunctionType(ret, args, false);
    }

    public static LLVMTypeRef function_ptr_t(LLVMTypeRef ret, LLVMTypeRef... args) {
        return ptr_t(function_t(ret, args));
    }

    public static LLVMTypeRef variadic_function_t(LLVMTypeRef ret, LLVMTypeRef... args) {
        return LLVMFunctionType(ret, args, true);
    }

    public static LLVMTypeRef variadic_function_ptr_t(LLVMTypeRef ret, LLVMTypeRef... args) {
        return ptr_t(variadic_function_t(ret, args));
    }
}
