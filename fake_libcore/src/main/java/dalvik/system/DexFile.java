package dalvik.system;

import java.nio.ByteBuffer;

public class DexFile {
    public DexFile(ByteBuffer[] bufs, ClassLoader loader, DexPathList.Element[] elements) {
        throw new UnsupportedOperationException("Stub!");
    }

    public DexFile(ByteBuffer buf) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static Object openInMemoryDexFiles(
            ByteBuffer[] bufs, ClassLoader loader, DexPathList.Element[] elements) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static Object openInMemoryDexFile(ByteBuffer buf) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static Class<?> defineClassNative(
            String name, ClassLoader loader, Object cookie, DexFile dexFile) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static String[] getClassNameList(Object cookie) {
        throw new UnsupportedOperationException("Stub!");
    }
}
