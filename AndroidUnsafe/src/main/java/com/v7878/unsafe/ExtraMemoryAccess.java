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
        @ASM(iset = X86, code = {
                0x56,                                 // push   esi
                0x53,                                 // push   ebx
                (byte) 0x8B, 0x4C, 0x24, 0x0C,        // mov    ecx, dword ptr [esp+0xc]
                (byte) 0x8B, 0x5C, 0x24, 0x10,        // mov    ebx, dword ptr [esp+0x10]
                (byte) 0x8B, 0x74, 0x24, 0x14,        // mov    esi, dword ptr [esp+0x14]
                (byte) 0xD1, (byte) 0xEE,             // shr    esi, 1
                0x74, 0x1C,                           // je     0x2e
                (byte) 0xBA, 0x00, 0x00, 0x00, 0x00,  // mov    edx, 0x0
                (byte) 0x8B, 0x04, (byte) 0x93,       // mov    eax, dword ptr [ebx+edx*4]
                0x0F, (byte) 0xC8,                    // bswap  eax
                (byte) 0xC1, (byte) 0xC0, 0x10,       // rol    eax, 0x10
                (byte) 0x89, 0x04, (byte) 0x91,       // mov    dword ptr [ecx+edx*4], eax
                0x42,                                 // inc    edx
                0x39, (byte) 0xF2,                    // cmp    edx, esi
                0x75, (byte) 0xF0,                    // jne    0x17
                (byte) 0xC1, (byte) 0xE2, 0x02,       // shl    edx, 2
                0x01, (byte) 0xD3,                    // add    ebx, edx
                0x01, (byte) 0xD1,                    // add    ecx, edx
                (byte) 0xF6, 0x44, 0x24, 0x14, 0x01,  // test   byte ptr [esp+0x14], 1
                0x74, 0x0A,                           // je     0x3f
                0x66, (byte) 0x8B, 0x03,              // mov    ax, word ptr [ebx]
                0x66, (byte) 0xC1, (byte) 0xC0, 0x08, // rol    ax, 8
                0x66, (byte) 0x89, 0x01,              // mov    word ptr [ecx], ax
                0x5B,                                 // pop    ebx
                0x5E,                                 // pop    esi
                (byte) 0xC3,                          // ret
        })
        @ASM(iset = ARM /*, TODO*/)
        @SuppressWarnings("JavaJniMissingFunction")
        @CriticalNative
        static native void swapShorts(int dst, int src, int count);

        @ASM(iset = X86, code = {
                0x56,                                //push   esi
                0x53,                                //push   ebx
                (byte) 0x8B, 0x5C, 0x24, 0x0C,       //mov    ebx, dword ptr [esp+0xc]
                (byte) 0x8B, 0x74, 0x24, 0x10,       //mov    esi, dword ptr [esp+0x10]
                (byte) 0x8B, 0x4C, 0x24, 0x14,       //mov    ecx, dword ptr [esp+0x14]
                (byte) 0x85, (byte) 0xC9,            //test   ecx, ecx
                0x74, 0x12,                          //je     0x24
                (byte) 0xB8, 0x00, 0x00, 0x00, 0x00, //mov    eax, 0x0
                (byte) 0x8B, 0x14, (byte) 0x86,      //mov    edx, dword ptr [esi+eax*4]
                0x0F, (byte) 0xCA,                   //bswap  edx
                (byte) 0x89, 0x14, (byte) 0x83,      //mov    dword ptr [ebx+eax*4], edx
                0x40,                                //inc    eax
                0x39, (byte) 0xC1,                   //cmp    ecx, eax
                0x75, (byte) 0xF3,                   //jne    0x17
                0x5B,                                //pop    ebx
                0x5E,                                //pop    esi
                (byte) 0xC3,                         //ret
        })
        @ASM(iset = ARM /*, TODO*/)
        @SuppressWarnings("JavaJniMissingFunction")
        @CriticalNative
        static native void swapInts(int dst, int src, int count);

        @ASM(iset = X86, code = {
                0x57,                                 // push   edi
                0x56,                                 // push   esi
                0x53,                                 // push   ebx
                (byte) 0x8B, 0x5C, 0x24, 0x10,        // mov    ebx, dword ptr [esp+0x10]
                (byte) 0x8B, 0x74, 0x24, 0x14,        // mov    esi, dword ptr [esp+0x14]
                (byte) 0x8B, 0x7C, 0x24, 0x18,        // mov    edi, dword ptr [esp+0x18]
                (byte) 0x85, (byte) 0xFF,             // test   edi, edi
                0x74, 0x1C,                           // je     0x2f
                (byte) 0xB8, 0x00, 0x00, 0x00, 0x00,  // mov    eax, 0x0
                (byte) 0x8B, 0x14, (byte) 0xC6,       // mov    edx, dword ptr [esi+eax*8]
                (byte) 0x8B, 0x4C, (byte) 0xC6, 0x04, // mov    ecx, dword ptr [esi+eax*8+4]
                0x0F, (byte) 0xC9,                    // bswap  ecx
                (byte) 0x89, 0x0C, (byte) 0xC3,       // mov    dword ptr [ebx+eax*8], ecx
                0x0F, (byte) 0xCA,                    // bswap  edx
                (byte) 0x89, 0x54, (byte) 0xC3, 0x04, // mov    dword ptr [ebx+eax*8+4], edx
                0x40,                                 // inc    eax
                0x39, (byte) 0xC7,                    // cmp    edi, eax
                0x75, (byte) 0xE9,                    // jne    0x18
                0x5B,                                 // pop    ebx
                0x5E,                                 // pop    esi
                0x5F,                                 // pop    edi
                (byte) 0xC3,                          // ret
        })
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
        @ASM(iset = ARM64, code = {
                0x5F, 0x08, 0x00, (byte) 0xF1,               // cmp   x2, #2
                0x23, 0x01, 0x00, 0x54,                      // b.lo  #0x28
                0x48, (byte) 0xFC, 0x41, (byte) 0xD3,        // lsr   x8, x2, #1
                0x1F, 0x05, 0x00, (byte) 0xF1,               // cmp   x8, #1
                0x08, (byte) 0x85, (byte) 0x9F, (byte) 0x9A, // csinc x8, x8, xzr, hi
                0x29, 0x44, 0x40, (byte) 0xB8,               // ldr   w9, [x1], #4
                0x08, 0x05, 0x00, (byte) 0xF1,               // subs  x8, x8, #1
                0x29, 0x05, (byte) 0xC0, 0x5A,               // rev16 w9, w9
                0x09, 0x44, 0x00, (byte) 0xB8,               // str   w9, [x0], #4
                (byte) 0x81, (byte) 0xFF, (byte) 0xFF, 0x54, // b.ne  #0x14
                (byte) 0xA2, 0x00, 0x00, 0x36,               // tbz   w2, #0, #0x3c
                0x28, 0x00, 0x40, 0x79,                      // ldrh  w8, [x1]
                0x08, 0x09, (byte) 0xC0, 0x5A,               // rev   w8, w8
                0x08, 0x7D, 0x10, 0x53,                      // lsr   w8, w8, #0x10
                0x08, 0x00, 0x00, 0x79,                      // strh  w8, [x0]
                (byte) 0xC0, 0x03, 0x5F, (byte) 0xD6         // ret
        })
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
        @ASM(iset = ARM64, code = {
                (byte) 0xC2, 0x00, 0x00, (byte) 0xB4,        // cbz  x2, #0x18
                0x28, 0x44, 0x40, (byte) 0xB8,               // ldr  w8, [x1], #4
                0x42, 0x04, 0x00, (byte) 0xF1,               // subs x2, x2, #1
                0x08, 0x09, (byte) 0xC0, 0x5A,               // rev  w8, w8
                0x08, 0x44, 0x00, (byte) 0xB8,               // str  w8, [x0], #4
                (byte) 0x81, (byte) 0xFF, (byte) 0xFF, 0x54, // b.ne #4
                (byte) 0xC0, 0x03, 0x5F, (byte) 0xD6         // ret
        })
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
        @ASM(iset = ARM64, code = {
                (byte) 0xE2, 0x00, 0x00, (byte) 0xB4, // cbz  x2, #0x1c
                0x29, 0x20, (byte) 0xC1, 0x28,        // ldp  w9, w8, [x1], #8
                0x42, 0x04, 0x00, (byte) 0xF1,        // subs x2, x2, #1
                0x08, 0x09, (byte) 0xC0, 0x5A,        // rev  w8, w8
                0x29, 0x09, (byte) 0xC0, 0x5A,        // rev  w9, w9
                0x08, 0x24, (byte) 0x81, 0x28,        // stp  w8, w9, [x0], #8
                0x61, (byte) 0xFF, (byte) 0xFF, 0x54, // b.ne #4
                (byte) 0xC0, 0x03, 0x5F, (byte) 0xD6  // ret
        })
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
