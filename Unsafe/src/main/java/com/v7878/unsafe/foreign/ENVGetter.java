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
import static com.v7878.llvm.Core.LLVMBuildUnreachable;
import static com.v7878.llvm.Core.LLVMIntPredicate.LLVMIntEQ;
import static com.v7878.llvm.Core.LLVMPositionBuilderAtEnd;
import static com.v7878.llvm.Types.LLVMBuilderRef;
import static com.v7878.llvm.Types.LLVMContextRef;
import static com.v7878.llvm.Types.LLVMValueRef;
import static com.v7878.unsafe.JNIUtils.getJNIInvokeInterfaceOffset;
import static com.v7878.unsafe.foreign.LibArt.ART;
import static com.v7878.unsafe.llvm.LLVMBuilder.call;
import static com.v7878.unsafe.llvm.LLVMBuilder.const_int32;
import static com.v7878.unsafe.llvm.LLVMBuilder.const_intptr;
import static com.v7878.unsafe.llvm.LLVMBuilder.fnptr_factory;
import static com.v7878.unsafe.llvm.LLVMBuilder.intptr_factory;
import static com.v7878.unsafe.llvm.LLVMBuilder.load_ptr;
import static com.v7878.unsafe.llvm.LLVMBuilder.ptr_factory;
import static com.v7878.unsafe.llvm.LLVMTypes.fn_t;
import static com.v7878.unsafe.llvm.LLVMTypes.int32_t;
import static com.v7878.unsafe.llvm.LLVMTypes.intptr_t;
import static com.v7878.unsafe.llvm.LLVMTypes.ptr_t;
import static com.v7878.unsafe.llvm.LLVMTypes.var_fn_t;
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
            intptr_factory(SCOPE.allocateFrom(Utils.LOG_TAG));

    private static final Function<LLVMContextRef, LLVMValueRef> LOG_GET_ENV_MSG =
            intptr_factory(SCOPE.allocateFrom("Could not get JNIEnv for upcall. JNI error code: %d"));

    private static final Function<LLVMContextRef, LLVMValueRef> LOG_ATTACH_MSG =
            intptr_factory(SCOPE.allocateFrom("Could not attach thread for upcall. JNI error code: %d"));

    private static final Function<LLVMContextRef, LLVMValueRef> LOG_SET_SPECIFIC =
            intptr_factory(SCOPE.allocateFrom("Could not set JNIEnv. Error code: %d"));

    private static final Function<LLVMContextRef, LLVMValueRef> JVM = intptr_factory(JNIUtils.getJavaVM());

    private static final Function<LLVMBuilderRef, LLVMValueRef> LOG_ASSERT =
            fnptr_factory(SCOPE, ART.findOrThrow("__android_log_assert"), context ->
                    var_fn_t(void_t(context), intptr_t(context),
                            intptr_t(context), intptr_t(context)));

    private static final Function<LLVMBuilderRef, LLVMValueRef> GET_SPECIFIC =
            fnptr_factory(SCOPE, ART.findOrThrow("pthread_getspecific"), context ->
                    fn_t(intptr_t(context), int32_t(context)));

    private static final Function<LLVMBuilderRef, LLVMValueRef> SET_SPECIFIC =
            fnptr_factory(SCOPE, ART.findOrThrow("pthread_setspecific"), context ->
                    fn_t(int32_t(context), int32_t(context), intptr_t(context)));

    private static final BiFunction<LLVMBuilderRef, LLVMValueRef, LLVMValueRef> GET_ENV =
            ptr_factory(getJNIInvokeInterfaceOffset("GetEnv"), context ->
                    fn_t(int32_t(context), intptr_t(context),
                            ptr_t(intptr_t(context)), int32_t(context)));

    private static final BiFunction<LLVMBuilderRef, LLVMValueRef, LLVMValueRef> ATTACH =
            ptr_factory(getJNIInvokeInterfaceOffset("AttachCurrentThreadAsDaemon"), context ->
                    fn_t(int32_t(context), intptr_t(context),
                            ptr_t(intptr_t(context)), intptr_t(context)));


    // Note: Destructor is not needed, since the thread itself will release the JVM resources upon completion
    private static final int KEY = PThread.pthread_key_create(0);

    private static final MemorySegment GETTER;

    static {
        GETTER = generateFunctionCodeSegment((context, module, builder) -> {
            var nullptr = const_intptr(context, 0);
            var zero32 = const_int32(context, 0);
            var jni_edetached = const_int32(context, -2);
            var jni_version = const_int32(context, /* JNI_VERSION_1_6 */ 0x00010006);

            var f_type = fn_t(intptr_t(context));
            var function = LLVMAddFunction(module, "getter", f_type);

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
            var jvm_iface = load_ptr(builder, intptr_t(context), jvm_ptr);
            var pthread_key = const_int32(context, KEY);
            LLVMBuildBr(builder, get_cached_env);

            LLVMPositionBuilderAtEnd(builder, abort);
            var abort_msg = LLVMBuildPhi(builder, intptr_t(context), "");
            var abort_code = LLVMBuildPhi(builder, int32_t(context), "");
            call(builder, LOG_ASSERT.apply(builder), const_intptr(context, 0),
                    LOG_TAG.apply(context), abort_msg, abort_code);
            LLVMBuildUnreachable(builder);

            LLVMPositionBuilderAtEnd(builder, exit);
            var ret_env = LLVMBuildPhi(builder, intptr_t(context), "");
            LLVMBuildRet(builder, ret_env);

            LLVMPositionBuilderAtEnd(builder, cache);
            var cache_env_ptr = LLVMBuildPhi(builder, ptr_t(intptr_t(context)), "");
            var cache_env = LLVMBuildLoad(builder, cache_env_ptr, "");
            LLVMAddIncoming(ret_env, cache_env, cache);
            var status = call(builder, SET_SPECIFIC.apply(builder), pthread_key, cache_env);
            var test = LLVMBuildICmp(builder, LLVMIntEQ, status, zero32, "");
            LLVMAddIncoming(abort_msg, LOG_SET_SPECIFIC.apply(context), cache);
            LLVMAddIncoming(abort_code, status, cache);
            LLVMBuildCondBr(builder, test, exit, abort);

            LLVMPositionBuilderAtEnd(builder, get_cached_env);
            var env = call(builder, GET_SPECIFIC.apply(builder), pthread_key);
            LLVMAddIncoming(ret_env, env, get_cached_env);
            test = LLVMBuildICmp(builder, LLVMIntEQ, env, nullptr, "");
            LLVMBuildCondBr(builder, test, get_env, exit);

            LLVMPositionBuilderAtEnd(builder, get_env);
            var env_ptr = LLVMBuildAlloca(builder, intptr_t(context), "");
            status = call(builder, GET_ENV.apply(builder, jvm_iface),
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
            status = call(builder, ATTACH.apply(builder, jvm_iface),
                    jvm_ptr, env_ptr, nullptr);
            test = LLVMBuildICmp(builder, LLVMIntEQ, status, zero32, "");
            LLVMAddIncoming(abort_msg, LOG_ATTACH_MSG.apply(context), attach);
            LLVMAddIncoming(abort_code, status, attach);
            LLVMAddIncoming(cache_env_ptr, env_ptr, attach);
            LLVMBuildCondBr(builder, test, cache, abort);

            return function;
        }, SCOPE);
    }

    public static final MemorySegment INSTANCE = MemorySegment.ofAddress(GETTER.nativeAddress());
}
