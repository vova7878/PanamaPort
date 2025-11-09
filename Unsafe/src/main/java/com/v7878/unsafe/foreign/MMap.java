package com.v7878.unsafe.foreign;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MMap {
    public record MMapEntry(long start, String path) {
    }

    private static final String PATTERN =
            "^(?<start>[0-9A-Fa-f]+)-(?<end>[0-9A-Fa-f]+)\\s+"
                    + "(?<perms>[rwxsp\\-]{4})\\s+"
                    + "(?<offset>[0-9A-Fa-f]+)\\s+"
                    + "(?<dev>[0-9A-Fa-f]+:[0-9A-Fa-f]+)\\s+"
                    + "(?<inode>[0-9]+)\\s+"
                    + "(?<path>%s)$";

    public static MMapEntry findFirstByPath(String path) {
        File maps = new File("/proc/self/maps");
        if (!maps.isFile()) {
            throw new IllegalStateException("File " + maps + " does not exist");
        }
        String data;
        try {
            byte[] tmp = Files.readAllBytes(maps.toPath());
            data = new String(tmp);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        Matcher match = Pattern.compile(String.format(PATTERN, path), Pattern.MULTILINE).matcher(data);
        if (!match.find()) {
            throw new IllegalStateException("Can`t find mmap entry with path " + path);
        }
        long start = Long.parseUnsignedLong(Objects.requireNonNull(match.group("start")), 16);
        path = match.group("path");
        return new MMapEntry(start, path);
    }
}
