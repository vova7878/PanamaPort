package com.v7878.unsafe;

public class Utils {
    public static class NopConsumer {
        public static void consume(boolean ignored) {
            throw new UnsupportedOperationException("Stub!");
        }
    }
}
