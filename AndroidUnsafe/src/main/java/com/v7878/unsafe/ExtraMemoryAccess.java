package com.v7878.unsafe;

import static com.v7878.dex.bytecode.CodeBuilder.InvokeKind.STATIC;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_BOOLEAN_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_BYTE_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_CHAR_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_DOUBLE_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_FLOAT_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_INT_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_LONG_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.ARRAY_SHORT_INDEX_SCALE;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.AndroidUnsafe.allocateInstance;
import static com.v7878.unsafe.ArtMethodUtils.getExecutableData;
import static com.v7878.unsafe.ClassUtils.setClassStatus;
import static com.v7878.unsafe.DexFileUtils.loadClass;
import static com.v7878.unsafe.DexFileUtils.openDexFile;
import static com.v7878.unsafe.NativeCodeBlob.InstructionSet.ARM;
import static com.v7878.unsafe.NativeCodeBlob.InstructionSet.ARM64;
import static com.v7878.unsafe.NativeCodeBlob.InstructionSet.RISCV64;
import static com.v7878.unsafe.NativeCodeBlob.InstructionSet.X86;
import static com.v7878.unsafe.NativeCodeBlob.InstructionSet.X86_64;
import static com.v7878.unsafe.NativeCodeBlob.processASM;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;
import static com.v7878.unsafe.Utils.ensureClassInitialized;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.VM.kPoisonReferences;

import androidx.annotation.Keep;

import com.v7878.dex.ClassDef;
import com.v7878.dex.Dex;
import com.v7878.dex.EncodedMethod;
import com.v7878.dex.MethodId;
import com.v7878.dex.ProtoId;
import com.v7878.dex.TypeId;
import com.v7878.foreign.Linker;
import com.v7878.unsafe.NativeCodeBlob.ASM;

import java.lang.reflect.Modifier;

import dalvik.annotation.optimization.CriticalNative;
import dalvik.system.DexFile;

public class ExtraMemoryAccess {

    @Keep
    private static class Swaps {
        static {
            processASM();
        }

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
        @CriticalNative
        static native void swapShorts32(int dst, int src, int count);

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
        @CriticalNative
        static native void swapInts32(int dst, int src, int count);

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
        @CriticalNative
        static native void swapLongs32(int dst, int src, int count);

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
        @CriticalNative
        static native void swapShorts64(long dst, long src, long count);

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
        @CriticalNative
        static native void swapInts64(long dst, long src, long count);

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
        @CriticalNative
        static native void swapLongs64(long dst, long src, long count);
    }

    private static void swapShorts(long dst, long src, long count) {
        if (IS64BIT)
            Swaps.swapShorts64(dst, src, count);
        else
            Swaps.swapShorts32((int) dst, (int) src, (int) count);
    }

    private static void swapInts(long dst, long src, long count) {
        if (IS64BIT)
            Swaps.swapInts64(dst, src, count);
        else
            Swaps.swapInts32((int) dst, (int) src, (int) count);
    }

    private static void swapLongs(long dst, long src, long count) {
        if (IS64BIT)
            Swaps.swapLongs64(dst, src, count);
        else
            Swaps.swapLongs32((int) dst, (int) src, (int) count);
    }

    public void copySwapMemory(long srcAddress, long dstAddress,
                               long bytes, long elemSize) {
        if (bytes == 0) {
            return;
        }

        switch ((int) elemSize) {
            case 2 -> swapShorts(dstAddress, srcAddress, bytes / 2);
            case 4 -> swapInts(dstAddress, srcAddress, bytes / 4);
            case 8 -> swapLongs(dstAddress, srcAddress, bytes / 8);
            default -> throw new IllegalArgumentException("Illegal element size: " + elemSize);
        }
    }

    @Keep
    static abstract class CopyInvoker {
        private static final long MEMCPY = Linker.nativeLinker()
                .defaultLookup().find("memcpy").get().address();
        private static final long COPY_SWAP_SHORTS;
        private static final long COPY_SWAP_INTS;
        private static final long COPY_SWAP_LONGS;

        static {
            processASM();

            ensureClassInitialized(Swaps.class);

            Class<?> word = IS64BIT ? long.class : int.class;
            String suffix = IS64BIT ? "64" : "32";
            COPY_SWAP_SHORTS = getExecutableData(getDeclaredMethod(Swaps.class,
                    "swapShorts" + suffix, word, word, word));
            COPY_SWAP_INTS = getExecutableData(getDeclaredMethod(Swaps.class,
                    "swapInts" + suffix, word, word, word));
            COPY_SWAP_LONGS = getExecutableData(getDeclaredMethod(Swaps.class,
                    "swapLongs" + suffix, word, word, word));
        }

        @ASM(iset = X86, code = {
                (byte) 0x8b, 0x4c, 0x24, 0x14, // mov    ecx, DWORD PTR [esp+0x14]
                (byte) 0x8b, 0x54, 0x24, 0x0c, // mov    edx, DWORD PTR [esp+0xc]
                (byte) 0x89, 0x4c, 0x24, 0x0c, // mov    DWORD PTR [esp+0xc], ecx
                (byte) 0x8b, 0x4c, 0x24, 0x10, // mov    ecx, DWORD PTR [esp+0x10]
                0x01, (byte) 0xca,             // add    edx, ecx
                (byte) 0x8b, 0x44, 0x24, 0x08, // mov    eax, DWORD PTR [esp+0x8]
                (byte) 0x89, 0x54, 0x24, 0x08, // mov    DWORD PTR [esp+0x8], edx
                (byte) 0x8b, 0x54, 0x24, 0x04, // mov    edx, DWORD PTR [esp+0x4]
                0x01, (byte) 0xd0,             // add    eax, edx
                (byte) 0x89, 0x44, 0x24, 0x04, // mov    DWORD PTR [esp+0x4], eax
                (byte) 0xff, 0x64, 0x24, 0x18, // jmp    DWORD PTR [esp+0x18]
        })
        @ASM(iset = ARM /*, TODO*/)
        @CriticalNative
        @SuppressWarnings("unused")
        static native void invoke32n(int dst_ref, int dst_offset, int src_ref,
                                     int src_offset, int count, int symbol);

        @ASM(iset = X86_64, code = {
                (byte) 0x89, (byte) 0xFF,       // mov edi, edi
                0x48, 0x01, (byte) 0xF7,        // add rdi, rsi
                (byte) 0x89, (byte) 0xD6,       // mov esi, edx
                0x48, 0x01, (byte) 0xCE,        // add rsi, rcx
                0x4C, (byte) 0x89, (byte) 0xC2, // mov rdx, r8
                0x41, (byte) 0xFF, (byte) 0xE1, // jmp r9
        })
        @ASM(iset = ARM64, code = {
                0x20, 0x40, 0x20, (byte) 0x8B,        // add  x0, x1, w0, uxtw
                0x61, 0x40, 0x22, (byte) 0x8B,        // add  x1, x3, w2, uxtw
                (byte) 0xE2, 0x03, 0x04, (byte) 0xAA, // mov  x2, x4
                (byte) 0xA0, 0x00, 0x1F, (byte) 0xD6  // br   x5
        })
        @ASM(iset = RISCV64 /*, TODO*/)
        @CriticalNative
        @SuppressWarnings("unused")
        static native void invoke64n(int dst_ref, long dst_offset, int src_ref,
                                     long src_offset, long count, long symbol);

        @ASM(iset = X86, code = {
                (byte) 0x8B, 0x4C, 0x24, 0x14, // mov ecx, dword ptr [esp + 0x14]
                (byte) 0x8B, 0x54, 0x24, 0x0C, // mov edx, dword ptr [esp + 0xc]
                (byte) 0x89, 0x4C, 0x24, 0x0C, // mov dword ptr [esp + 0xc], ecx
                (byte) 0x8B, 0x4C, 0x24, 0x10, // mov ecx, dword ptr [esp + 0x10]
                (byte) 0x8B, 0x44, 0x24, 0x08, // mov eax, dword ptr [esp + 8]
                0x29, (byte) 0xD1,             // sub ecx, edx
                (byte) 0x8B, 0x54, 0x24, 0x04, // mov edx, dword ptr [esp + 4]
                (byte) 0x89, 0x4C, 0x24, 0x08, // mov dword ptr [esp + 8], ecx
                0x29, (byte) 0xD0,             // sub eax, edx
                (byte) 0x89, 0x44, 0x24, 0x04, // mov dword ptr [esp + 4], eax
                (byte) 0xFF, 0x64, 0x24, 0x18, // jmp dword ptr [esp + 0x18]
        })
        @ASM(iset = ARM /*, TODO*/)
        @CriticalNative
        @SuppressWarnings("unused")
        static native void invoke32p(int dst_ref, int dst_offset, int src_ref,
                                     int src_offset, int count, int symbol);

        @ASM(iset = X86_64, code = {
                (byte) 0xF7, (byte) 0xDF,       // neg edi
                0x48, 0x01, (byte) 0xF7,        // add rdi, rsi
                (byte) 0xF7, (byte) 0xDA,       // neg edx
                0x48, (byte) 0x8D, 0x34, 0x0A,  // lea rsi, [rdx + rcx]
                0x4C, (byte) 0x89, (byte) 0xC2, // mov rdx, r8
                0x41, (byte) 0xFF, (byte) 0xE1, // jmp r9
        })
        @ASM(iset = ARM64, code = {
                (byte) 0xE8, 0x03, 0x00, 0x4B,        // neg w8, w0
                0x00, 0x01, 0x01, (byte) 0x8B,        // add x0, x8, x1
                (byte) 0xE8, 0x03, 0x02, 0x4B,        // neg w8, w2
                0x01, 0x01, 0x03, (byte) 0x8B,        // add x1, x8, x3
                (byte) 0xE2, 0x03, 0x04, (byte) 0xAA, // mov x2, x4
                (byte) 0xA0, 0x00, 0x1F, (byte) 0xD6, // br  x5
        })
        @ASM(iset = RISCV64 /*, TODO*/)
        @CriticalNative
        @SuppressWarnings("unused")
        static native void invoke64p(int dst_ref, long dst_offset, int src_ref,
                                     long src_offset, long count, long symbol);

        abstract void invoke32(Object dst_ref, int dst_offset, Object src_ref,
                               int src_offset, int count, int symbol);

        abstract void invoke64(Object dst_ref, long dst_offset, Object src_ref,
                               long src_offset, long count, long symbol);

        public static void invoke(Object src_ref, long src_offset, Object dst_ref,
                                  long dst_offset, long count, long symbol) {
            if (IS64BIT) {
                INSTANCE.invoke64(dst_ref, dst_offset, src_ref, src_offset, count, symbol);
            } else {
                INSTANCE.invoke32(dst_ref, (int) dst_offset, src_ref,
                        (int) src_offset, (int) count, (int) symbol);
            }
        }

        private static final CopyInvoker INSTANCE = nothrows_run(() -> {
            Class<?> word = IS64BIT ? long.class : int.class;
            TypeId word_id = TypeId.of(word);
            String suffix = IS64BIT ? "64" : "32";
            String suffix2 = suffix + (kPoisonReferences.get() ? "p" : "n");

            String impl_name = CopyInvoker.class.getName() + "$Impl";
            TypeId impl_id = TypeId.of(impl_name);
            ClassDef impl_def = new ClassDef(impl_id);
            impl_def.setSuperClass(TypeId.of(CopyInvoker.class));

            MethodId raw_invoke_id = new MethodId(TypeId.of(CopyInvoker.class), new ProtoId(TypeId.V,
                    TypeId.I, word_id, TypeId.I, word_id, word_id, word_id), "invoke" + suffix2);
            MethodId invoke_id = new MethodId(impl_id, new ProtoId(TypeId.V, TypeId.of(Object.class),
                    word_id, TypeId.of(Object.class), word_id, word_id, word_id), "invoke" + suffix);

            // note: it's broken - object is cast to int
            impl_def.getClassData().getVirtualMethods().add(new EncodedMethod(
                    invoke_id, Modifier.PUBLIC).withCode(0, b -> b
                    .invoke_range(STATIC, raw_invoke_id, IS64BIT ? 10 : 6, b.p(0))
                    .return_void()
            ));

            DexFile dex = openDexFile(new Dex(impl_def).compile());
            Class<?> impl = loadClass(dex, impl_name, CopyInvoker.class.getClassLoader());
            setClassStatus(impl, ClassUtils.ClassStatus.Verified);

            return (CopyInvoker) allocateInstance(impl);
        });

        static final boolean inited;

        static {
            inited = true;
        }
    }

    public static void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
        if (bytes == 0) {
            return;
        }

        if (CopyInvoker.inited) {
            CopyInvoker.invoke(srcBase, srcOffset, destBase, destOffset, bytes, CopyInvoker.MEMCPY);
        } else {
            AndroidUnsafe.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
        }
    }

    public static void copySwapMemory(Object srcBase, long srcOffset, Object destBase,
                                      long destOffset, long bytes, long elemSize) {
        if (bytes == 0) {
            return;
        }

        switch ((int) elemSize) {
            case 2 -> CopyInvoker.invoke(srcBase, srcOffset, destBase,
                    destOffset, bytes / 2, CopyInvoker.COPY_SWAP_SHORTS);
            case 4 -> CopyInvoker.invoke(srcBase, srcOffset, destBase,
                    destOffset, bytes / 4, CopyInvoker.COPY_SWAP_INTS);
            case 8 -> CopyInvoker.invoke(srcBase, srcOffset, destBase,
                    destOffset, bytes / 8, CopyInvoker.COPY_SWAP_LONGS);
            default -> throw new IllegalArgumentException("Illegal element size: " + elemSize);
        }
    }

    public static final int SOFT_MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    public static final int LOG2_ARRAY_BOOLEAN_INDEX_SCALE = exactLog2(ARRAY_BOOLEAN_INDEX_SCALE);
    public static final int LOG2_ARRAY_BYTE_INDEX_SCALE = exactLog2(ARRAY_BYTE_INDEX_SCALE);
    public static final int LOG2_ARRAY_CHAR_INDEX_SCALE = exactLog2(ARRAY_CHAR_INDEX_SCALE);
    public static final int LOG2_ARRAY_SHORT_INDEX_SCALE = exactLog2(ARRAY_SHORT_INDEX_SCALE);
    public static final int LOG2_ARRAY_INT_INDEX_SCALE = exactLog2(ARRAY_INT_INDEX_SCALE);
    public static final int LOG2_ARRAY_LONG_INDEX_SCALE = exactLog2(ARRAY_LONG_INDEX_SCALE);
    public static final int LOG2_ARRAY_FLOAT_INDEX_SCALE = exactLog2(ARRAY_FLOAT_INDEX_SCALE);
    public static final int LOG2_ARRAY_DOUBLE_INDEX_SCALE = exactLog2(ARRAY_DOUBLE_INDEX_SCALE);

    private static final int LOG2_BYTE_BIT_SIZE = exactLog2(Byte.SIZE);

    private static int exactLog2(int scale) {
        if ((scale & (scale - 1)) != 0)
            throw new Error("data type scale not a power of two");
        return Integer.numberOfTrailingZeros(scale);
    }

    public static int vectorizedMismatch(Object a, long aOffset,
                                         Object b, long bOffset,
                                         int length,
                                         int log2ArrayIndexScale) {
        // assert a.getClass().isArray();
        // assert b.getClass().isArray();
        // assert 0 <= length <= sizeOf(a)
        // assert 0 <= length <= sizeOf(b)
        // assert 0 <= log2ArrayIndexScale <= 3

        int log2ValuesPerWidth = LOG2_ARRAY_LONG_INDEX_SCALE - log2ArrayIndexScale;
        int wi = 0;
        for (; wi < length >> log2ValuesPerWidth; wi++) {
            long bi = ((long) wi) << LOG2_ARRAY_LONG_INDEX_SCALE;
            long av = AndroidUnsafe.getLongUnaligned(a, aOffset + bi);
            long bv = AndroidUnsafe.getLongUnaligned(b, bOffset + bi);
            if (av != bv) {
                long x = av ^ bv;
                int o = AndroidUnsafe.IS_BIG_ENDIAN
                        ? Long.numberOfLeadingZeros(x) >> (LOG2_BYTE_BIT_SIZE + log2ArrayIndexScale)
                        : Long.numberOfTrailingZeros(x) >> (LOG2_BYTE_BIT_SIZE + log2ArrayIndexScale);
                return (wi << log2ValuesPerWidth) + o;
            }
        }

        // Calculate the tail of remaining elements to check
        int tail = length - (wi << log2ValuesPerWidth);

        if (log2ArrayIndexScale < LOG2_ARRAY_INT_INDEX_SCALE) {
            int wordTail = 1 << (LOG2_ARRAY_INT_INDEX_SCALE - log2ArrayIndexScale);
            // Handle 4 bytes or 2 chars in the tail using int width
            if (tail >= wordTail) {
                long bi = ((long) wi) << LOG2_ARRAY_LONG_INDEX_SCALE;
                int av = AndroidUnsafe.getIntUnaligned(a, aOffset + bi);
                int bv = AndroidUnsafe.getIntUnaligned(b, bOffset + bi);
                if (av != bv) {
                    int x = av ^ bv;
                    int o = AndroidUnsafe.IS_BIG_ENDIAN
                            ? Integer.numberOfLeadingZeros(x) >> (LOG2_BYTE_BIT_SIZE + log2ArrayIndexScale)
                            : Integer.numberOfTrailingZeros(x) >> (LOG2_BYTE_BIT_SIZE + log2ArrayIndexScale);
                    return (wi << log2ValuesPerWidth) + o;
                }
                tail -= wordTail;
            }
        }
        return ~tail;
    }
}
