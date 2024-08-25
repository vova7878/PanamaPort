package com.v7878.libcore;

import static com.v7878.unsafe.DexFileUtils.setTrusted;
import static com.v7878.unsafe.Reflection.getDeclaredConstructor;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;

import com.v7878.unsafe.ArtMethodUtils;
import com.v7878.unsafe.DexFileUtils;
import com.v7878.unsafe.Utils.NopConsumer;

import java.nio.ByteBuffer;

import dalvik.system.DexFile;

public class DexFileAccess {
    static {
        setTrusted(DexFileAccess.class);
    }

    public static DexFile openDexFile(ByteBuffer[] bufs, ClassLoader loader) {
        class Holder {
            static final boolean done;

            static {
                Class<?> elements = DexFileUtils.forName(
                        "[Ldalvik.system.DexPathList$Element;", null);
                var constructor = getDeclaredConstructor(DexFile.class,
                        ByteBuffer[].class, ClassLoader.class, elements);
                ArtMethodUtils.makeExecutablePublic(constructor);
                done = true;
            }
        }
        NopConsumer.consume(Holder.done);
        return new DexFile(bufs, loader, null);
    }

    public static DexFile openDexFile(ByteBuffer buf) {
        class Holder {
            static final boolean done;

            static {
                var constructor = getDeclaredConstructor(DexFile.class, ByteBuffer.class);
                ArtMethodUtils.makeExecutablePublic(constructor);
                done = true;
            }
        }
        NopConsumer.consume(Holder.done);
        return new DexFile(buf);
    }

    public static Object openCookie(ByteBuffer[] bufs, ClassLoader loader) {
        class Holder {
            static final boolean done;

            static {
                Class<?> elements = DexFileUtils.forName(
                        "[Ldalvik.system.DexPathList$Element;", null);
                var method = getDeclaredMethod(DexFile.class, "openInMemoryDexFiles",
                        ByteBuffer[].class, ClassLoader.class, elements);
                ArtMethodUtils.makeExecutablePublic(method);
                done = true;
            }
        }
        NopConsumer.consume(Holder.done);
        return DexFile.openInMemoryDexFiles(bufs, loader, null);
    }

    public static Object openCookie(ByteBuffer buf) {
        class Holder {
            static final boolean done;

            static {
                var method = getDeclaredMethod(DexFile.class,
                        "openInMemoryDexFile", ByteBuffer.class);
                ArtMethodUtils.makeExecutablePublic(method);
                done = true;
            }
        }
        NopConsumer.consume(Holder.done);
        return DexFile.openInMemoryDexFile(buf);
    }

    public static Class<?> defineClassNative(
            String name, ClassLoader loader, Object cookie, DexFile dexFile) {
        class Holder {
            static final boolean done;

            static {
                var method = getDeclaredMethod(DexFile.class, "defineClassNative",
                        String.class, ClassLoader.class, Object.class, DexFile.class);
                ArtMethodUtils.makeExecutablePublic(method);
                done = true;
            }
        }
        NopConsumer.consume(Holder.done);
        return DexFile.defineClassNative(name, loader, cookie, dexFile);
    }

    public static String[] getClassNameList(Object cookie) {
        class Holder {
            static final boolean done;

            static {
                var method = getDeclaredMethod(DexFile.class,
                        "getClassNameList", Object.class);
                ArtMethodUtils.makeExecutablePublic(method);
                done = true;
            }
        }
        NopConsumer.consume(Holder.done);
        return DexFile.getClassNameList(cookie);
    }

    //TODO: getCookie/setCookie
    //TODO: getInternalCookie/setInternalCookie
    //TODO: getFileName/setFileName
}
