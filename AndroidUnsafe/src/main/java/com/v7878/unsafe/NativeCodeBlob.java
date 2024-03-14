package com.v7878.unsafe;

import static com.v7878.misc.Math.roundUpL;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.InstructionSet.CURRENT_INSTRUCTION_SET;
import static com.v7878.unsafe.Reflection.getDeclaredMethods;
import static com.v7878.unsafe.Utils.shouldNotHappen;
import static com.v7878.unsafe.io.IOUtils.MAP_ANONYMOUS;
import static java.lang.annotation.ElementType.METHOD;

import android.system.ErrnoException;
import android.system.OsConstants;

import androidx.annotation.Keep;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.unsafe.io.IOUtils;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class NativeCodeBlob {

    private static final int CODE_PROT = OsConstants.PROT_READ | OsConstants.PROT_WRITE | OsConstants.PROT_EXEC;
    private static final int CODE_FLAGS = OsConstants.MAP_PRIVATE | MAP_ANONYMOUS;
    private static final int CODE_ALIGNMENT = CURRENT_INSTRUCTION_SET.codeAlignment();

    private static MemorySegment[] makeCodeBlobInternal(Arena arena, MemorySegment... code) {
        int count = code.length;
        long size = 0;
        long[] offsets = new long[count];
        for (int i = 0; i < count; i++) {
            size = roundUpL(size, CODE_ALIGNMENT);
            offsets[i] = size;
            size += code[i].byteSize();
        }

        MemorySegment data;
        try {
            data = IOUtils.mmap(null, null, 0, size, CODE_PROT, CODE_FLAGS, arena);
        } catch (ErrnoException e) {
            throw shouldNotHappen(e);
        }

        MemorySegment[] out = new MemorySegment[count];
        for (int i = 0; i < count; i++) {
            MemorySegment tmp = data.asSlice(offsets[i], code[i].byteSize());
            MemorySegment.copy(code[i], 0, tmp, 0, code[i].byteSize());
            out[i] = tmp;
        }
        return out;
    }

    public static MemorySegment[] makeCodeBlob(Arena arena, MemorySegment... code) {
        Objects.requireNonNull(arena);
        if (code.length == 0) {
            return new MemorySegment[0];
        }
        return makeCodeBlobInternal(arena, code.clone());
    }

    public static MemorySegment[] makeCodeBlob(Arena arena, byte[]... code) {
        Objects.requireNonNull(arena);
        if (code.length == 0) {
            return new MemorySegment[0];
        }
        return makeCodeBlobInternal(arena, Arrays.stream(code)
                .map(MemorySegment::ofArray).toArray(MemorySegment[]::new));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @Repeatable(ASMs.class)
    @Keep
    public @interface ASM {
        InstructionSet iset();

        byte[] code() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(METHOD)
    @Keep
    public @interface ASMs {
        ASM[] value();
    }

    private static final byte[] NOT_FOUND = new byte[0];

    private static Optional<byte[]> getCode(ASM asm) {
        byte[] code = asm.code();
        if (code.length == 0) {
            return Optional.of(NOT_FOUND);
        }
        return Optional.of(code);
    }

    public static void processASM(Arena arena) {
        processASM(arena, Stack.getStackClass1());
    }

    public static void processASM(Arena arena, Class<?> clazz) {
        Objects.requireNonNull(arena);
        Objects.requireNonNull(clazz);

        Method[] methods = getDeclaredMethods(clazz);
        Map<Method, byte[]> work = new HashMap<>(methods.length);

        for (Method method : methods) {
            ASM[] data = method.getDeclaredAnnotationsByType(ASM.class);
            if (data.length != 0) {
                if (!Modifier.isNative(method.getModifiers())) {
                    throw new IllegalArgumentException("Non-native method annotated with ASM: " + method);
                }
                Optional<byte[]> code = Optional.empty();
                for (ASM tmp : data) {
                    if (tmp.iset() == CURRENT_INSTRUCTION_SET) {
                        code = getCode(tmp);
                        break;
                    }
                }
                if (code.isPresent()) {
                    if (code.get() == NOT_FOUND) {
                        throw new IllegalStateException("Unable to find ASM for " +
                                CURRENT_INSTRUCTION_SET + " instruction set for method " + method);
                    }
                    work.put(method, code.get());
                }
            }
        }

        methods = new Method[work.size()];
        byte[][] code = new byte[methods.length][];

        {
            int i = 0;
            for (var tmp : work.entrySet()) {
                methods[i] = tmp.getKey();
                code[i] = tmp.getValue();
                i++;
            }
        }

        MemorySegment[] ptrs = makeCodeBlob(arena, code);

        for (int i = 0; i < methods.length; i++) {
            registerNativeMethod(methods[i], ptrs[i].nativeAddress());
        }
    }
}
