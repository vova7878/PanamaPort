package com.v7878.unsafe;

import static android.system.Os.mmap;
import static android.system.Os.munmap;
import static com.v7878.misc.Math.roundUpL;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_BYTE_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.copyMemory;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Reflection.getDeclaredMethods;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.VM.getCurrentInstructionSet;
import static java.lang.annotation.ElementType.METHOD;

import android.system.OsConstants;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.unsafe.access.JavaForeignAccess;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class NativeCodeBlob {
    public enum InstructionSet {
        ARM(8),
        ARM64(16),
        X86(16),
        X86_64(16),
        RISCV64(16);

        private final int code_alignment;

        InstructionSet(int code_alignment) {
            this.code_alignment = code_alignment;
        }

        public int codeAlignment() {
            return code_alignment;
        }
    }

    public static final InstructionSet CURRENT_INSTRUCTION_SET;

    static {
        String iset = getCurrentInstructionSet();
        switch (iset) {
            case "arm" -> CURRENT_INSTRUCTION_SET = InstructionSet.ARM;
            case "arm64" -> CURRENT_INSTRUCTION_SET = InstructionSet.ARM64;
            case "x86" -> CURRENT_INSTRUCTION_SET = InstructionSet.X86;
            case "x86_64" -> CURRENT_INSTRUCTION_SET = InstructionSet.X86_64;
            case "riscv64" -> CURRENT_INSTRUCTION_SET = InstructionSet.RISCV64;
            default -> throw new IllegalStateException("unsupported instruction set: " + iset);
        }
    }

    private static final int CODE_PROT = OsConstants.PROT_READ | OsConstants.PROT_WRITE | OsConstants.PROT_EXEC;
    private static final int MAP_ANONYMOUS = 0x20;
    private static final int CODE_FLAGS = OsConstants.MAP_PRIVATE | MAP_ANONYMOUS;

    private static final int CODE_ALIGNMENT = CURRENT_INSTRUCTION_SET.codeAlignment();

    static MemorySegment[] makeCodeBlobInternal(Arena arena, byte[]... code) {
        int count = code.length;
        long size = 0;
        long[] offsets = new long[count];
        for (int i = 0; i < count; i++) {
            size = roundUpL(size, CODE_ALIGNMENT);
            offsets[i] = size;
            size += code[i].length;
        }

        long finalSize = size;
        long address = nothrows_run(() -> mmap(0, finalSize,
                CODE_PROT, CODE_FLAGS, null, 0));
        JavaForeignAccess.addOrCleanupIfFail(arena.scope(),
                () -> nothrows_run(() -> munmap(address, finalSize)));
        MemorySegment data = MemorySegment.ofAddress(address)
                .reinterpret(finalSize, arena, null);

        MemorySegment[] out = new MemorySegment[count];
        for (int i = 0; i < count; i++) {
            MemorySegment tmp = data.asSlice(offsets[i], code[i].length);
            copyMemory(code[i], ARRAY_BYTE_BASE_OFFSET,
                    null, tmp.address(), code[i].length);
            out[i] = tmp;
        }
        return out;
    }

    public static MemorySegment[] makeCodeBlob(Arena arena, byte[]... code) {
        Objects.requireNonNull(arena);
        if (code.length == 0) {
            return new MemorySegment[0];
        }
        return makeCodeBlobInternal(arena, code.clone());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @Repeatable(ASMs.class)
    public @interface ASM {
        InstructionSet iset();

        byte[] code() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    public @interface ASMs {
        ASM[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    public @interface ASM_GENERATOR {
        Class<?> declaring_class() default void.class;

        String value(); // generator method name
    }

    private static final byte[] NOT_FOUND = new byte[0];

    private static Optional<byte[]> getCode(ASM asm) {
        byte[] code = asm.code();
        if (code.length == 0) {
            return Optional.of(NOT_FOUND);
        }
        return Optional.of(code);
    }

    private static Optional<byte[]> getCode(ASM_GENERATOR asm, Class<?> clazz) {
        if (asm.declaring_class() != void.class) {
            clazz = asm.declaring_class();
        }
        Method generator = getDeclaredMethod(clazz, asm.value(), InstructionSet.class);
        if (!Modifier.isStatic(generator.getModifiers())) {
            throw new IllegalArgumentException("asm generator method is not static: " + generator);
        }
        if (generator.getReturnType() != byte[].class) {
            throw new IllegalArgumentException("return type of asm generator method is not byte[]: " + generator);
        }
        byte[] code = (byte[]) nothrows_run(() -> generator.invoke(null, CURRENT_INSTRUCTION_SET));
        if (code == null) {
            return Optional.empty();
        }
        if (code.length == 0) {
            return Optional.of(NOT_FOUND);
        }
        return Optional.of(code);
    }

    public static void processASM() {
        processASM(Stack.getStackClass1());
    }

    public static void processASM(Class<?> clazz) {
        Objects.requireNonNull(clazz);
        Method[] methods = getDeclaredMethods(clazz);

        Map<Method, byte[]> work = new HashMap<>(methods.length);

        for (Method method : methods) {
            ASM[] data = method.getDeclaredAnnotationsByType(ASM.class);
            ASM_GENERATOR generator = method.getDeclaredAnnotation(ASM_GENERATOR.class);
            if (data.length != 0 || generator != null) {
                if (!Modifier.isNative(method.getModifiers())) {
                    throw new IllegalArgumentException("Non-native method annotated with ASM: " + method);
                }
                Optional<byte[]> code = Optional.empty();
                for (ASM tmp : data) {
                    if (tmp.iset() == CURRENT_INSTRUCTION_SET) {
                        code = getCode(tmp);
                        if (code.isPresent()) {
                            break;
                        }
                    }
                }
                if (!code.isPresent() && generator != null) {
                    code = getCode(generator, clazz);
                }
                if (code.isPresent()) {
                    if (code.get() == NOT_FOUND) {
                        throw new IllegalStateException("Unable to find ASM for " +
                                CURRENT_INSTRUCTION_SET + " instruction set for method " + method);
                    } else {
                        work.put(method, code.get());
                    }
                }
            }
        }

        methods = new Method[work.size()];
        byte[][] code = new byte[methods.length][];

        {
            int i = 0;
            for (Map.Entry<Method, byte[]> tmp : work.entrySet()) {
                methods[i] = tmp.getKey();
                code[i] = tmp.getValue();
                i++;
            }
        }

        //TODO: add arena parameter
        MemorySegment[] ptrs = makeCodeBlobInternal(Arena.global(), code);

        for (int i = 0; i < methods.length; i++) {
            registerNativeMethod(methods[i], ptrs[i].address());
        }
    }
}
