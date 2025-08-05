package com.v7878.llvm;

import static com.v7878.foreign.MemoryLayout.paddingLayout;
import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.llvm._LibLLVM.LLVM;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;

import android.annotation.SuppressLint;

import com.v7878.foreign.Arena;
import com.v7878.foreign.FunctionDescriptor;
import com.v7878.foreign.Linker;
import com.v7878.foreign.MemorySegment;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.LibrarySymbol;

import java.lang.invoke.MethodHandle;

/*===-- llvm-c/ErrorHandling.h - Error Handling C Interface -------*- C -*-===*\
|*                                                                            *|
|* This file defines the C interface to LLVM's error handling mechanism.      *|
|*                                                                            *|
\*===----------------------------------------------------------------------===*/
@SuppressLint("WrongCommentType")
public final class ErrorHandling {
    private ErrorHandling() {
    }

    @FunctionalInterface
    public interface LLVMFatalErrorHandler {
        void invoke(String reason);
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        @LibrarySymbol(name = "LLVMInstallFatalErrorHandler")
        @CallSignature(type = CRITICAL, ret = VOID, args = {LONG_AS_WORD})
        abstract void LLVMInstallFatalErrorHandler(long Handler);

        @LibrarySymbol(name = "LLVMResetFatalErrorHandler")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMResetFatalErrorHandler();

        @LibrarySymbol(name = "LLVMEnablePrettyStackTrace")
        @CallSignature(type = CRITICAL, ret = VOID, args = {})
        abstract void LLVMEnablePrettyStackTrace();

        static final Native INSTANCE = BulkLinker.generateImpl(SCOPE, Native.class, LLVM);
    }

    private static class ErrorHandlerHolder {
        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();
        public static final MemorySegment NATIVE_HANDLER;
        public static volatile LLVMFatalErrorHandler JAVA_HANDLER;

        static {
            MethodHandle target = unreflect(getDeclaredMethod(
                    ErrorHandlerHolder.class, "invoke", MemorySegment.class));
            NATIVE_HANDLER = Linker.nativeLinker().upcallStub(target, FunctionDescriptor.ofVoid(
                    ADDRESS.withTargetLayout(paddingLayout(Long.MAX_VALUE))), SCOPE);
        }

        @DoNotShrink
        @DoNotObfuscate
        @SuppressWarnings("unused")
        private static void invoke(MemorySegment msg) {
            String jmsg = msg.getString(0);
            JAVA_HANDLER.invoke(jmsg);
        }
    }

    /**
     * Install a fatal error handler. By default, if LLVM detects a fatal error, it
     * will call exit(1). This may not be appropriate in many contexts. For example,
     * doing exit(1) will bypass many crash reporting/tracing system tools. This
     * function allows you to install a callback that will be invoked prior to the
     * call to exit(1).
     */
    public static void LLVMInstallFatalErrorHandler(LLVMFatalErrorHandler Handler) {
        if (Handler == null) {
            LLVMResetFatalErrorHandler();
            return;
        }
        synchronized (ErrorHandlerHolder.class) {
            ErrorHandlerHolder.JAVA_HANDLER = Handler;
            Native.INSTANCE.LLVMInstallFatalErrorHandler(
                    ErrorHandlerHolder.NATIVE_HANDLER.nativeAddress());
        }
    }

    /**
     * Reset the fatal error handler. This resets LLVM's fatal error handling
     * behavior to the default.
     */
    public static void LLVMResetFatalErrorHandler() {
        synchronized (ErrorHandlerHolder.class) {
            Native.INSTANCE.LLVMResetFatalErrorHandler();
            ErrorHandlerHolder.JAVA_HANDLER = null;
        }
    }

    /**
     * Enable LLVM's built-in stack trace code. This intercepts the OS's crash
     * signals and prints which component of LLVM you were in at the time if the
     * crash.
     */
    public static void LLVMEnablePrettyStackTrace() {
        Native.INSTANCE.LLVMEnablePrettyStackTrace();
    }
}
