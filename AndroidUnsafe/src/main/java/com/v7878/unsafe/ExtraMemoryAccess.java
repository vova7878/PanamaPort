package com.v7878.unsafe;

import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.NativeCodeBlob.InstructionSet.ARM;
import static com.v7878.unsafe.NativeCodeBlob.InstructionSet.ARM64;
import static com.v7878.unsafe.NativeCodeBlob.InstructionSet.RISCV64;
import static com.v7878.unsafe.NativeCodeBlob.InstructionSet.X86;
import static com.v7878.unsafe.NativeCodeBlob.InstructionSet.X86_64;
import static com.v7878.unsafe.NativeCodeBlob.processASM;

import androidx.annotation.Keep;

import com.v7878.unsafe.NativeCodeBlob.ASM;

import dalvik.annotation.optimization.CriticalNative;

public class ExtraMemoryAccess {
    static {
        processASM(IS64BIT ? Swaps64.class : Swaps32.class);
    }

    @Keep
    private static class Swaps32 {
        @ASM(iset = X86 /*, TODO*/)
        @ASM(iset = ARM /*, TODO*/)
        @SuppressWarnings("JavaJniMissingFunction")
        @CriticalNative
        static native void swapShorts(int dst, int src, int count);

        @ASM(iset = X86 /*, TODO*/)
        @ASM(iset = ARM /*, TODO*/)
        @SuppressWarnings("JavaJniMissingFunction")
        @CriticalNative
        static native void swapInts(int dst, int src, int count);

        @ASM(iset = X86 /*, TODO*/)
        @ASM(iset = ARM /*, TODO*/)
        @SuppressWarnings("JavaJniMissingFunction")
        @CriticalNative
        static native void swapLongs(int dst, int src, int count);
    }

    @Keep
    private static class Swaps64 {
        @ASM(iset = X86_64, code = {
                0x49, (byte) 0x89, (byte) 0xf8,          // mov     r8, rdi
                0x48, (byte) 0x89, (byte) 0xd7,          // mov     rdi, rdx
                0x48, (byte) 0xd1, (byte) 0xef,          // shr     rdi, 1
                0x48, (byte) 0x83, (byte) 0xfa, 0x01,    // cmp     rdx, 0x1
                0x76, 0x31,                              // jbe     0x40
                (byte) 0xb9, 0x00, 0x00, 0x00, 0x00,     // mov     ecx, 0x0
                (byte) 0x8b, 0x04, (byte) 0x8e,          // mov     eax, DWORD PTR [rsi+rcx*4]
                0x0f, (byte) 0xc8,                       // bswap   eax
                (byte) 0xc1, (byte) 0xc0, 0x10,          // rol     eax, 0x10
                0x41, (byte) 0x89, 0x04, (byte) 0x88,    // mov     DWORD PTR [r8+rcx*4], eax
                0x48, (byte) 0x83, (byte) 0xc1, 0x01,    // add     rcx, 0x1
                0x48, 0x39, (byte) 0xf9,                 // cmp     rcx, rdi
                0x72, (byte) 0xeb,                       // jb      0x14
                0x48, (byte) 0xc1, (byte) 0xe7, 0x02,    // shl     rdi, 0x2
                0x48, 0x01, (byte) 0xfe,                 // add     rsi, rdi
                0x48, (byte) 0x83, (byte) 0xfa, 0x01,    // cmp     rdx, 0x1
                (byte) 0xb8, 0x04, 0x00, 0x00, 0x00,     // mov     eax, 0x4
                0x48, 0x0f, 0x46, (byte) 0xf8,           // cmovbe  rdi, rax
                0x49, 0x01, (byte) 0xf8,                 // add     r8, rdi
                (byte) 0xf6, (byte) 0xc2, 0x01,          // test    dl, 0x1
                0x74, 0x0b,                              // je      0x50
                0x0f, (byte) 0xb7, 0x06,                 // movzx   eax, WORD PTR [rsi]
                0x66, (byte) 0xc1, (byte) 0xc0, 0x08,    // rol     ax, 0x8
                0x66, 0x41, (byte) 0x89, 0x00,           // mov     WORD PTR [r8], ax
                (byte) 0xc3                              // ret
        })
        @ASM(iset = ARM64 /*, TODO*/)
        @ASM(iset = RISCV64 /*, TODO*/)
        @SuppressWarnings("JavaJniMissingFunction")
        @CriticalNative
        static native void swapShorts(long dst, long src, long count);

        @ASM(iset = X86_64, code = {
                0x48, (byte) 0x85, (byte) 0xd2,       // test   rdx, rdx
                0x74, 0x16,                           // je     0x1b
                (byte) 0xb8, 0x00, 0x00, 0x00, 0x00,  // mov    eax, 0x0
                (byte) 0x8b, 0x0c, (byte) 0x86,       // mov    ecx, DWORD PTR [rsi+rax*4]
                0x0f, (byte) 0xc9,                    // bswap  ecx
                (byte) 0x89, 0x0c, (byte) 0x87,       // mov    DWORD PTR [rdi+rax*4], ecx
                0x48, (byte) 0x83, (byte) 0xc0, 0x01, // add    rax, 0x1
                0x48, 0x39, (byte) 0xc2,              // cmp    rdx, rax
                0x75, (byte) 0xef,                    // jne    0xa
                (byte) 0xc3                           // ret
        })
        @ASM(iset = ARM64 /*, TODO*/)
        @ASM(iset = RISCV64 /*, TODO*/)
        @SuppressWarnings("JavaJniMissingFunction")
        @CriticalNative
        static native void swapInts(long dst, long src, long count);

        @ASM(iset = X86_64, code = {
                0x48, (byte) 0x85, (byte) 0xd2,             // test   rdx, rdx
                0x74, 0x23,                                 // je     0x28
                (byte) 0xb8, 0x00, 0x00, 0x00, 0x00,        // mov    eax, 0x0
                (byte) 0x8b, 0x0c, (byte) 0xc6,             // mov    ecx, DWORD PTR [rsi+rax*8]
                0x44, (byte) 0x8b, 0x44, (byte) 0xc6, 0x04, // mov    r8d, DWORD PTR [rsi+rax*8+0x4]
                0x41, 0x0f, (byte) 0xc8,                    // bswap  r8d
                0x44, (byte) 0x89, 0x04, (byte) 0xc7,       // mov    DWORD PTR [rdi+rax*8], r8d
                0x0f, (byte) 0xc9,                          // bswap  ecx
                (byte) 0x89, 0x4c, (byte) 0xc7, 0x04,       // mov    DWORD PTR [rdi+rax*8+0x4], ecx
                0x48, (byte) 0x83, (byte) 0xc0, 0x01,       // add    rax, 0x1
                0x48, 0x39, (byte) 0xc2,                    // cmp    rdx, rax
                0x75, (byte) 0xe2,                          // jne    0xa
                (byte) 0xc3,                                // ret
        })
        @ASM(iset = ARM64 /*, TODO*/)
        @ASM(iset = RISCV64 /*, TODO*/)
        @SuppressWarnings("JavaJniMissingFunction")
        @CriticalNative
        static native void swapLongs(long dst, long src, long count);
    }

    public static void swapShorts(long dst, long src, long count) {
        if (IS64BIT)
            Swaps64.swapShorts(dst, src, count);
        else
            Swaps32.swapShorts((int) dst, (int) src, (int) count);
    }

    public static void swapInts(long dst, long src, long count) {
        if (IS64BIT)
            Swaps64.swapInts(dst, src, count);
        else
            Swaps32.swapInts((int) dst, (int) src, (int) count);
    }

    public static void swapLongs(long dst, long src, long count) {
        if (IS64BIT)
            Swaps64.swapLongs(dst, src, count);
        else
            Swaps32.swapLongs((int) dst, (int) src, (int) count);
    }

    public void copySwapMemory(long srcAddress, long dstAddress,
                               long bytes, long elemSize) {
        if (bytes == 0) {
            return;
        }

        switch ((int) elemSize) {
            case 2:
                swapShorts(dstAddress, srcAddress, bytes / 2);
                break;
            case 4:
                swapInts(dstAddress, srcAddress, bytes / 4);
                break;
            case 8:
                swapLongs(dstAddress, srcAddress, bytes / 8);
                break;
            default:
                throw new IllegalArgumentException("Illegal element size: " + elemSize);
        }
    }
}
