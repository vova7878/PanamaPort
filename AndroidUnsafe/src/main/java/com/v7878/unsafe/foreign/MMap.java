package com.v7878.unsafe.foreign;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MMap {

    public static class MMapEntry {
        public final long start;
        public final String path;

        public MMapEntry(long start, String path) {
            this.start = start;
            this.path = path;
        }
    }

    private static final String PATTERN =
            "(?<start>[0-9A-Fa-f]+)-(?<end>[0-9A-Fa-f]+)\\s+"
                    + "(?<perms>[rwxsp\\-]{4})\\s+"
                    + "(?<offset>[0-9A-Fa-f]+)\\s+"
                    + "(?<dev>[0-9A-Fa-f]+:[0-9A-Fa-f]+)\\s+"
                    + "(?<inode>[0-9]+)\\s+"
                    + "(?<path>%s)";

    public static MMapEntry findFirstByPath(String path) {
        File maps = new File("/proc/self/maps");
        if (!maps.isFile()) {
            throw new IllegalArgumentException("file " + maps + " does not exist");
        }
        String data;
        try {
            byte[] tmp = Files.readAllBytes(maps.toPath());
            data = new String(tmp);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        Matcher match = Pattern.compile(String.format(PATTERN, path)).matcher(data);
        if (!match.find()) {
            throw new IllegalStateException("Can`t find mmap entry with path " + path);
        }
        long start = Long.parseLong(match.group("start"), 16);
        path = match.group("path");
        return new MMapEntry(start, path);
    }
}
