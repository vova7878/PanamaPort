package java.lang.foreign;

import com.v7878.unsafe.JNIUtils;
import com.v7878.unsafe.NativeCodeBlob;

import java.lang.reflect.Method;

public class _UpcallNativeStubs {

    private static final MemorySegment preallocated_fatal_msg =
            Arena.ofAuto().allocateUtf8String("Exception in upcall stub!");

    private static byte[] long_to_arr(long v) {
        return new byte[]{
                (byte) (v),
                (byte) (v >> 8),
                (byte) (v >> 16),
                (byte) (v >> 24),
                (byte) (v >> 32),
                (byte) (v >> 40),
                (byte) (v >> 48),
                (byte) (v >> 56)};
    }

    static MemorySegment makeUpcallNativeStub(
            Method function, char r_shorty, Arena arena) {
        long exit = 0; // TODO
        long clazz = JNIUtils.NewGlobalRef(function.getDeclaringClass(), arena);
        long method_id = JNIUtils.FromReflectedMethod(function);
        long jvm = JNIUtils.getJavaVMPtr().address();
        long msg = preallocated_fatal_msg.address();

        byte[] e = long_to_arr(exit);
        byte[] j = long_to_arr(jvm);
        byte[] c = long_to_arr(clazz);
        byte[] m = long_to_arr(method_id);
        byte[] s = long_to_arr(msg);

        byte[] code;

        //<type> stub(...) {
        //    void (*exit)(int) = <e>
        //    JavaVM* vm = <j>
        //    jclass clazz = <c>
        //    jmethodID m_id = <m>
        //    const char* msg = <s>
        //
        //    va_list args;
        //    va_start(args);
        //
        //    bool destroy = false;
        //    JNIEnv* env;
        //
        //    {
        //        int res = (*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_6);
        //        if (res != JNI_OK) {
        //            if (res == JNI_EDETACHED) {
        //                res = (*vm)->AttachCurrentThread(vm, &env, 0);
        //                destroy = true;
        //                if (res != JNI_OK) {
        //                    exit(res);
        //                }
        //            } else {
        //                exit(res);
        //            }
        //        }
        //    }
        //
        //    <type> r = (*env)->CallStatic##<Type>##MethodV(env, clazz, m_id, args);
        //
        //    if ((*env)->ExceptionCheck(env)) {
        //        (*env)->ExceptionDescribe(env);
        //        (*env)->FatalError(env, msg);
        //    }
        //
        //    if (destroy) {
        //        int res = (*vm)->DetachCurrentThread(vm);
        //        if (res != JNI_OK) {
        //            exit(res);
        //        }
        //    }
        //
        //    va_end(args);
        //
        //    return r;
        //}

        switch (NativeCodeBlob.CURRENT_INSTRUCTION_SET) {
            case X86_64:
                code = makeUpcallX86_64Stub(e, j, c, m, s, r_shorty);
                break;
            default:
                //TODO
                throw new UnsupportedOperationException("Unsupported yet");
        }
        return NativeCodeBlob.makeCodeBlob(arena, code)[0];
    }


    private static byte[] makeUpcallX86_64Stub(byte[] e, byte[] j, byte[] c,
                                               byte[] m, byte[] s, char r_shorty) {
        if (r_shorty == 'V') {
            return new byte[]{
                    0x55,                                                                                          // 0x000: push   rbp
                    0x53,                                                                                          // 0x001: push   rbx
                    0x48, (byte) 0x81, (byte) 0xEC, (byte) 0xE8, 0x00, 0x00, 0x00,                                 // 0x002: sub    rsp, 0xe8
                    0x48, (byte) 0x89, 0x7C, 0x24, 0x30,                                                           // 0x009: mov    qword ptr [rsp + 0x30], rdi
                    0x48, (byte) 0x89, 0x74, 0x24, 0x38,                                                           // 0x00e: mov    qword ptr [rsp + 0x38], rsi
                    0x48, (byte) 0x89, 0x54, 0x24, 0x40,                                                           // 0x013: mov    qword ptr [rsp + 0x40], rdx
                    0x48, (byte) 0x89, 0x4C, 0x24, 0x48,                                                           // 0x018: mov    qword ptr [rsp + 0x48], rcx
                    0x4C, (byte) 0x89, 0x44, 0x24, 0x50,                                                           // 0x01d: mov    qword ptr [rsp + 0x50], r8
                    0x4C, (byte) 0x89, 0x4C, 0x24, 0x58,                                                           // 0x022: mov    qword ptr [rsp + 0x58], r9
                    (byte) 0x84, (byte) 0xC0,                                                                      // 0x027: test   al, al
                    0x74, 0x3A,                                                                                    // 0x029: je     0x65
                    0x0F, 0x29, 0x44, 0x24, 0x60,                                                                  // 0x02b: movaps xmmword ptr [rsp + 0x60], xmm0
                    0x0F, 0x29, 0x4C, 0x24, 0x70,                                                                  // 0x030: movaps xmmword ptr [rsp + 0x70], xmm1
                    0x0F, 0x29, (byte) 0x94, 0x24, (byte) 0x80, 0x00, 0x00, 0x00,                                  // 0x035: movaps xmmword ptr [rsp + 0x80], xmm2
                    0x0F, 0x29, (byte) 0x9C, 0x24, (byte) 0x90, 0x00, 0x00, 0x00,                                  // 0x03d: movaps xmmword ptr [rsp + 0x90], xmm3
                    0x0F, 0x29, (byte) 0xA4, 0x24, (byte) 0xA0, 0x00, 0x00, 0x00,                                  // 0x045: movaps xmmword ptr [rsp + 0xa0], xmm4
                    0x0F, 0x29, (byte) 0xAC, 0x24, (byte) 0xB0, 0x00, 0x00, 0x00,                                  // 0x04d: movaps xmmword ptr [rsp + 0xb0], xmm5
                    0x0F, 0x29, (byte) 0xB4, 0x24, (byte) 0xC0, 0x00, 0x00, 0x00,                                  // 0x055: movaps xmmword ptr [rsp + 0xc0], xmm6
                    0x0F, 0x29, (byte) 0xBC, 0x24, (byte) 0xD0, 0x00, 0x00, 0x00,                                  // 0x05d: movaps xmmword ptr [rsp + 0xd0], xmm7
                    0x48, (byte) 0xBB, j[0], j[1], j[2], j[3], j[4], j[5], j[6], j[7],                             // 0x065: movabs rbx, <j>
                    0x48, (byte) 0x8D, 0x44, 0x24, 0x30,                                                           // 0x06f: lea    rax, [rsp + 0x30]
                    0x48, (byte) 0x89, 0x44, 0x24, 0x20,                                                           // 0x074: mov    qword ptr [rsp + 0x20], rax
                    0x48, (byte) 0x8D, (byte) 0x84, 0x24, 0x00, 0x01, 0x00, 0x00,                                  // 0x079: lea    rax, [rsp + 0x100]
                    0x48, (byte) 0x89, 0x44, 0x24, 0x18,                                                           // 0x081: mov    qword ptr [rsp + 0x18], rax
                    0x48, (byte) 0xB8, 0x00, 0x00, 0x00, 0x00, 0x30, 0x00, 0x00, 0x00,                             // 0x086: movabs rax, 0x3000000000
                    0x48, (byte) 0x89, 0x44, 0x24, 0x10,                                                           // 0x090: mov    qword ptr [rsp + 0x10], rax
                    0x48, (byte) 0x8B, 0x03,                                                                       // 0x095: mov    rax, qword ptr [rbx]
                    0x48, (byte) 0x8D, 0x74, 0x24, 0x08,                                                           // 0x098: lea    rsi, [rsp + 8]
                    0x48, (byte) 0x89, (byte) 0xDF,                                                                // 0x09d: mov    rdi, rbx
                    (byte) 0xBA, 0x06, 0x00, 0x01, 0x00,                                                           // 0x0a0: mov    edx, 0x10006
                    (byte) 0xFF, 0x50, 0x30,                                                                       // 0x0a5: call   qword ptr [rax + 0x30]
                    0x31, (byte) 0xED,                                                                             // 0x0a8: xor    ebp, ebp
                    (byte) 0x85, (byte) 0xC0,                                                                      // 0x0aa: test   eax, eax
                    0x74, 0x2A,                                                                                    // 0x0ac: je     0xd8
                    (byte) 0x83, (byte) 0xF8, (byte) 0xFE,                                                         // 0x0ae: cmp    eax, -2
                    0x75, 0x17,                                                                                    // 0x0b1: jne    0xca
                    0x48, (byte) 0x8B, 0x03,                                                                       // 0x0b3: mov    rax, qword ptr [rbx]
                    0x48, (byte) 0x8D, 0x74, 0x24, 0x08,                                                           // 0x0b6: lea    rsi, [rsp + 8]
                    0x48, (byte) 0x89, (byte) 0xDF,                                                                // 0x0bb: mov    rdi, rbx
                    0x31, (byte) 0xD2,                                                                             // 0x0be: xor    edx, edx
                    (byte) 0xFF, 0x50, 0x20,                                                                       // 0x0c0: call   qword ptr [rax + 0x20]
                    0x40, (byte) 0xB5, 0x01,                                                                       // 0x0c3: mov    bpl, 1
                    (byte) 0x85, (byte) 0xC0,                                                                      // 0x0c6: test   eax, eax
                    0x74, 0x0E,                                                                                    // 0x0c8: je     0xd8
                    0x48, (byte) 0xB9, e[0], e[1], e[2], e[3], e[4], e[5], e[6], e[7],                             // 0x0ca: movabs rcx, <e>
                    (byte) 0x89, (byte) 0xC7,                                                                      // 0x0d4: mov    edi, eax
                    (byte) 0xFF, (byte) 0xD1,                                                                      // 0x0d6: call   rcx
                    0x48, (byte) 0x8B, 0x7C, 0x24, 0x08,                                                           // 0x0d8: mov    rdi, qword ptr [rsp + 8]
                    0x48, (byte) 0x8B, 0x07,                                                                       // 0x0dd: mov    rax, qword ptr [rdi]
                    0x48, (byte) 0xBE, c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7],                             // 0x0e0: movabs rsi, <c>
                    0x48, (byte) 0xBA, m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7],                             // 0x0ea: movabs rdx, <m>
                    0x48, (byte) 0x8D, 0x4C, 0x24, 0x10,                                                           // 0x0f4: lea    rcx, [rsp + 0x10]
                    (byte) 0xFF, (byte) 0x90, 0x70, 0x04, 0x00, 0x00,                                              // 0x0f9: call   qword ptr [rax + 0x470]
                    0x48, (byte) 0x8B, 0x7C, 0x24, 0x08,                                                           // 0x0ff: mov    rdi, qword ptr [rsp + 8]
                    0x48, (byte) 0x8B, 0x07,                                                                       // 0x104: mov    rax, qword ptr [rdi]
                    (byte) 0xFF, (byte) 0x90, 0x20, 0x07, 0x00, 0x00,                                              // 0x107: call   qword ptr [rax + 0x720]
                    (byte) 0x84, (byte) 0xC0,                                                                      // 0x10d: test   al, al
                    0x74, 0x26,                                                                                    // 0x10f: je     0x137
                    0x48, (byte) 0x8B, 0x7C, 0x24, 0x08,                                                           // 0x111: mov    rdi, qword ptr [rsp + 8]
                    0x48, (byte) 0x8B, 0x07,                                                                       // 0x116: mov    rax, qword ptr [rdi]
                    (byte) 0xFF, (byte) 0x90, (byte) 0x80, 0x00, 0x00, 0x00,                                       // 0x119: call   qword ptr [rax + 0x80]
                    0x48, (byte) 0x8B, 0x7C, 0x24, 0x08,                                                           // 0x11f: mov    rdi, qword ptr [rsp + 8]
                    0x48, (byte) 0x8B, 0x07,                                                                       // 0x124: mov    rax, qword ptr [rdi]
                    0x48, (byte) 0xBE, s[0], s[1], s[2], s[3], s[4], s[5], s[6], s[7],                             // 0x127: movabs rsi, <s>
                    (byte) 0xFF, (byte) 0x90, (byte) 0x90, 0x00, 0x00, 0x00,                                       // 0x131: call   qword ptr [rax + 0x90]
                    0x40, (byte) 0x84, (byte) 0xED,                                                                // 0x137: test   bpl, bpl
                    0x74, 0x1B,                                                                                    // 0x13a: je     0x157
                    0x48, (byte) 0x8B, 0x03,                                                                       // 0x13c: mov    rax, qword ptr [rbx]
                    0x48, (byte) 0x89, (byte) 0xDF,                                                                // 0x13f: mov    rdi, rbx
                    (byte) 0xFF, 0x50, 0x28,                                                                       // 0x142: call   qword ptr [rax + 0x28]
                    (byte) 0x85, (byte) 0xC0,                                                                      // 0x145: test   eax, eax
                    0x74, 0x0E,                                                                                    // 0x147: je     0x157
                    0x48, (byte) 0xB9, e[0], e[1], e[2], e[3], e[4], e[5], e[6], e[7],                             // 0x149: movabs rcx, <e>
                    (byte) 0x89, (byte) 0xC7,                                                                      // 0x153: mov    edi, eax
                    (byte) 0xFF, (byte) 0xD1,                                                                      // 0x155: call   rcx
                    0x48, (byte) 0x81, (byte) 0xC4, (byte) 0xE8, 0x00, 0x00, 0x00,                                 // 0x157: add    rsp, 0xe8
                    0x5B,                                                                                          // 0x15e: pop    rbx
                    0x5D,                                                                                          // 0x15f: pop    rbp
                    (byte) 0xC3,                                                                                   // 0x160: ret
            };
        }
        throw new AssertionError("Unexpected return type: " + r_shorty);
    }
}
