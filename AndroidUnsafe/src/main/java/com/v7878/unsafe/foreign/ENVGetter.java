package com.v7878.unsafe.foreign;

import static com.v7878.llvm.Core.LLVMAddFunction;
import static com.v7878.llvm.Core.LLVMAddIncoming;
import static com.v7878.llvm.Core.LLVMAppendBasicBlock;
import static com.v7878.llvm.Core.LLVMBuildAlloca;
import static com.v7878.llvm.Core.LLVMBuildBr;
import static com.v7878.llvm.Core.LLVMBuildCondBr;
import static com.v7878.llvm.Core.LLVMBuildICmp;
import static com.v7878.llvm.Core.LLVMBuildLoad;
import static com.v7878.llvm.Core.LLVMBuildPhi;
import static com.v7878.llvm.Core.LLVMBuildRet;
import static com.v7878.llvm.Core.LLVMBuildRetVoid;
import static com.v7878.llvm.Core.LLVMBuildUnreachable;
import static com.v7878.llvm.Core.LLVMIntPredicate.LLVMIntEQ;
import static com.v7878.llvm.Core.LLVMPositionBuilderAtEnd;
import static com.v7878.llvm.Types.LLVMBuilderRef;
import static com.v7878.llvm.Types.LLVMContextRef;
import static com.v7878.llvm.Types.LLVMValueRef;
import static com.v7878.unsafe.JNIUtils.getJNIInvokeInterfaceOffset;
import static com.v7878.unsafe.foreign.LibArt.ART;
import static com.v7878.unsafe.llvm.LLVMBuilder.build_call;
import static com.v7878.unsafe.llvm.LLVMBuilder.build_load_ptr;
import static com.v7878.unsafe.llvm.LLVMBuilder.const_int32;
import static com.v7878.unsafe.llvm.LLVMBuilder.const_intptr;
import static com.v7878.unsafe.llvm.LLVMBuilder.functionPointerFactory;
import static com.v7878.unsafe.llvm.LLVMBuilder.intptrFactory;
import static com.v7878.unsafe.llvm.LLVMBuilder.pointerFactory;
import static com.v7878.unsafe.llvm.LLVMTypes.function_ptr_t;
import static com.v7878.unsafe.llvm.LLVMTypes.function_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int32_t;
import static com.v7878.unsafe.llvm.LLVMTypes.intptr_t;
import static com.v7878.unsafe.llvm.LLVMTypes.ptr_t;
import static com.v7878.unsafe.llvm.LLVMTypes.variadic_function_ptr_t;
import static com.v7878.unsafe.llvm.LLVMTypes.void_t;
import static com.v7878.unsafe.llvm.LLVMUtils.generateFunctionCodeSegment;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.JNIUtils;
import com.v7878.unsafe.Utils;

import java.util.function.BiFunction;
import java.util.function.Function;

public class ENVGetter {
    @DoNotShrink
    private static final Arena SCOPE = Arena.ofAuto();

    private static final Function<LLVMContextRef, LLVMValueRef> LOG_TAG =
            intptrFactory(SCOPE.allocateFrom(Utils.LOG_TAG));

    private static final Function<LLVMContextRef, LLVMValueRef> LOG_GET_ENV_MSG =
            intptrFactory(SCOPE.allocateFrom("Could not get JNIEnv for upcall. JNI error code: %d"));

    private static final Function<LLVMContextRef, LLVMValueRef> LOG_ATTACH_MSG =
            intptrFactory(SCOPE.allocateFrom("Could not attach thread for upcall. JNI error code: %d"));

    private static final Function<LLVMContextRef, LLVMValueRef> LOG_SET_SPECIFIC =
            intptrFactory(SCOPE.allocateFrom("Could not set JNIEnv. Error code: %d"));

    private static final Function<LLVMContextRef, LLVMValueRef> JVM = intptrFactory(JNIUtils.getJavaVM());

    private static final Function<LLVMBuilderRef, LLVMValueRef> LOG_ASSERT =
            functionPointerFactory(SCOPE, ART.findOrThrow("__android_log_assert"), context ->
                    variadic_function_ptr_t(void_t(context), intptr_t(context),
                            intptr_t(context), intptr_t(context)));

    private static final Function<LLVMBuilderRef, LLVMValueRef> GET_SPECIFIC =
            functionPointerFactory(SCOPE, ART.findOrThrow("pthread_getspecific"), context ->
                    function_ptr_t(intptr_t(context), int32_t(context)));

    private static final Function<LLVMBuilderRef, LLVMValueRef> SET_SPECIFIC =
            functionPointerFactory(SCOPE, ART.findOrThrow("pthread_setspecific"), context ->
                    function_ptr_t(int32_t(context), int32_t(context), intptr_t(context)));

    private static final BiFunction<LLVMBuilderRef, LLVMValueRef, LLVMValueRef> GET_ENV =
            pointerFactory(getJNIInvokeInterfaceOffset("GetEnv"), context ->
                    function_ptr_t(int32_t(context), intptr_t(context),
                            ptr_t(intptr_t(context)), int32_t(context)));

    private static final BiFunction<LLVMBuilderRef, LLVMValueRef, LLVMValueRef> ATTACH =
            pointerFactory(getJNIInvokeInterfaceOffset("AttachCurrentThreadAsDaemon"), context ->
                    function_ptr_t(int32_t(context), intptr_t(context),
                            ptr_t(intptr_t(context)), intptr_t(context)));

    private static final BiFunction<LLVMBuilderRef, LLVMValueRef, LLVMValueRef> DETACH =
            pointerFactory(getJNIInvokeInterfaceOffset("DetachCurrentThread"), context ->
                    function_ptr_t(int32_t(context), intptr_t(context)));

    private static final MemorySegment DESTRUCTOR;

    static {
        final String name = "detach";
        DESTRUCTOR = generateFunctionCodeSegment((context, module, builder) -> {
            var f_type = function_t(void_t(context), intptr_t(context));
            var function = LLVMAddFunction(module, name, f_type);

            LLVMPositionBuilderAtEnd(builder, LLVMAppendBasicBlock(function, ""));
            var jvm_ptr = JVM.apply(context);
            var jvm_iface = build_load_ptr(builder, intptr_t(context), jvm_ptr);
            build_call(builder, DETACH.apply(builder, jvm_iface), jvm_ptr);
            LLVMBuildRetVoid(builder);
        }, name, SCOPE);
    }

    private static final int KEY = PThread.pthread_key_create(DESTRUCTOR.nativeAddress());

    private static final MemorySegment GETTER;

    static {
        final String name = "getter";
        GETTER = generateFunctionCodeSegment((context, module, builder) -> {
            var nullptr = const_intptr(context, 0);
            var zero32 = const_int32(context, 0);
            var jni_edetached = const_int32(context, -2);
            var jni_version = const_int32(context, /* JNI_VERSION_1_6 */ 0x00010006);

            var f_type = function_t(intptr_t(context));
            var function = LLVMAddFunction(module, name, f_type);

            var init = LLVMAppendBasicBlock(function, "");
            var get_cached_env = LLVMAppendBasicBlock(function, "");
            var get_env = LLVMAppendBasicBlock(function, "");
            var check_detached = LLVMAppendBasicBlock(function, "");
            var attach = LLVMAppendBasicBlock(function, "");
            var cache = LLVMAppendBasicBlock(function, "");
            var exit = LLVMAppendBasicBlock(function, "");
            var abort = LLVMAppendBasicBlock(function, "");

            LLVMPositionBuilderAtEnd(builder, init);
            var jvm_ptr = JVM.apply(context);
            var jvm_iface = build_load_ptr(builder, intptr_t(context), jvm_ptr);
            var pthread_key = const_int32(context, KEY);
            LLVMBuildBr(builder, get_cached_env);

            LLVMPositionBuilderAtEnd(builder, abort);
            var abort_msg = LLVMBuildPhi(builder, intptr_t(context), "");
            var abort_code = LLVMBuildPhi(builder, int32_t(context), "");
            build_call(builder, LOG_ASSERT.apply(builder), const_intptr(context, 0),
                    LOG_TAG.apply(context), abort_msg, abort_code);
            LLVMBuildUnreachable(builder);

            LLVMPositionBuilderAtEnd(builder, exit);
            var ret_env = LLVMBuildPhi(builder, intptr_t(context), "");
            LLVMBuildRet(builder, ret_env);

            LLVMPositionBuilderAtEnd(builder, cache);
            var cache_env_ptr = LLVMBuildPhi(builder, ptr_t(intptr_t(context)), "");
            var cache_env = LLVMBuildLoad(builder, cache_env_ptr, "");
            LLVMAddIncoming(ret_env, cache_env, cache);
            var status = build_call(builder, SET_SPECIFIC.apply(builder), pthread_key, cache_env);
            var test = LLVMBuildICmp(builder, LLVMIntEQ, status, zero32, "");
            LLVMAddIncoming(abort_msg, LOG_SET_SPECIFIC.apply(context), cache);
            LLVMAddIncoming(abort_code, status, cache);
            LLVMBuildCondBr(builder, test, exit, abort);

            LLVMPositionBuilderAtEnd(builder, get_cached_env);
            var env = build_call(builder, GET_SPECIFIC.apply(builder), pthread_key);
            LLVMAddIncoming(ret_env, env, get_cached_env);
            test = LLVMBuildICmp(builder, LLVMIntEQ, env, nullptr, "");
            LLVMBuildCondBr(builder, test, get_env, exit);

            LLVMPositionBuilderAtEnd(builder, get_env);
            var env_ptr = LLVMBuildAlloca(builder, intptr_t(context), "");
            status = build_call(builder, GET_ENV.apply(builder, jvm_iface),
                    jvm_ptr, env_ptr, jni_version);
            test = LLVMBuildICmp(builder, LLVMIntEQ, status, zero32, "");
            LLVMAddIncoming(cache_env_ptr, env_ptr, get_env);
            LLVMBuildCondBr(builder, test, cache, check_detached);

            LLVMPositionBuilderAtEnd(builder, check_detached);
            test = LLVMBuildICmp(builder, LLVMIntEQ, status, jni_edetached, "");
            LLVMAddIncoming(abort_msg, LOG_GET_ENV_MSG.apply(context), check_detached);
            LLVMAddIncoming(abort_code, status, check_detached);
            LLVMBuildCondBr(builder, test, attach, abort);

            LLVMPositionBuilderAtEnd(builder, attach);
            status = build_call(builder, ATTACH.apply(builder, jvm_iface),
                    jvm_ptr, env_ptr, nullptr);
            test = LLVMBuildICmp(builder, LLVMIntEQ, status, zero32, "");
            LLVMAddIncoming(abort_msg, LOG_ATTACH_MSG.apply(context), attach);
            LLVMAddIncoming(abort_code, status, attach);
            LLVMAddIncoming(cache_env_ptr, env_ptr, attach);
            LLVMBuildCondBr(builder, test, cache, abort);
        }, name, SCOPE);
    }

    public static final MemorySegment INSTANCE = MemorySegment.ofAddress(GETTER.nativeAddress());
}
