package com.v7878.unsafe.access;

import static com.v7878.unsafe.Reflection.getHiddenConstructor;
import static com.v7878.unsafe.Reflection.getHiddenMethod;
import static com.v7878.unsafe.Reflection.unreflect;
import static com.v7878.unsafe.Utils.nothrows_run;

import com.v7878.unsafe.DexFileUtils;

import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;

import dalvik.system.DexFile;

public class DexFileAccess {
    public static DexFile openDexFile(ByteBuffer[] bufs, ClassLoader loader) {
        class Holder {
            static final MethodHandle handle;

            static {
                Class<?> elements = DexFileUtils.forName(
                        "[Ldalvik.system.DexPathList$Element;", null);
                handle = unreflect(getHiddenConstructor(DexFile.class,
                        ByteBuffer[].class, ClassLoader.class, elements));
            }
        }
        return nothrows_run(() -> (DexFile) Holder.handle.invoke(bufs, loader, null));
    }

    public static DexFile openDexFile(ByteBuffer buf) {
        class Holder {
            static final MethodHandle handle;

            static {
                handle = unreflect(getHiddenConstructor(DexFile.class, ByteBuffer.class));
            }
        }
        return nothrows_run(() -> (DexFile) Holder.handle.invoke(buf));
    }

    public static Object openCookie(ByteBuffer[] bufs, ClassLoader loader) {
        class Holder {
            static final MethodHandle handle;

            static {
                Class<?> elements = DexFileUtils.forName(
                        "[Ldalvik.system.DexPathList$Element;", null);
                handle = unreflect(getHiddenMethod(DexFile.class, "openInMemoryDexFiles",
                        ByteBuffer[].class, ClassLoader.class, elements));
            }
        }
        return nothrows_run(() -> Holder.handle.invoke(bufs, loader, null));
    }

    public static Object openCookie(ByteBuffer buf) {
        class Holder {
            static final MethodHandle handle;

            static {
                handle = unreflect(getHiddenMethod(DexFile.class,
                        "openInMemoryDexFile", ByteBuffer.class));
            }
        }
        return nothrows_run(() -> Holder.handle.invoke(buf));
    }

    public static Class<?> defineClassNative(
            String name, ClassLoader loader, Object cookie, DexFile dexFile) {
        class Holder {
            static final MethodHandle handle;

            static {
                handle = unreflect(getHiddenMethod(DexFile.class, "defineClassNative",
                        String.class, ClassLoader.class, Object.class, DexFile.class));
            }
        }
        return nothrows_run(() -> (Class<?>) Holder.handle.invoke(name, loader, cookie, dexFile));
    }

    public static String[] getClassNameList(Object cookie) {
        class Holder {
            static final MethodHandle handle;

            static {
                handle = unreflect(getHiddenMethod(DexFile.class,
                        "getClassNameList", Object.class));
            }
        }
        return nothrows_run(() -> (String[]) Holder.handle.invoke(cookie));
    }
}
