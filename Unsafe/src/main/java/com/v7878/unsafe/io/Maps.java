package com.v7878.unsafe.io;

import static com.v7878.unsafe.Utils.shouldNotHappen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Maps {
    public record MMapEntry(long start, long end, String perms,
                            long offset, int dev_major, int dev_minor,
                            long inode, String path) {
    }

    public static Stream<MMapEntry> maps(String pid) {
        var file = Paths.get((String.format("/proc/%s/maps", pid)));

        try {
            //noinspection resource
            return Files.lines(file).map(line -> {
                var parts = line.split(" ", 6);
                assert parts.length >= 5;

                var range = parts[0].split("-");
                long start = Long.parseUnsignedLong(range[0], 16);
                long end = Long.parseUnsignedLong(range[1], 16);

                String parms = parts[1];

                long offset = Long.parseUnsignedLong(parts[2], 16);

                var dev = parts[3].split(":");
                int devMajor = Integer.parseUnsignedInt(dev[0], 16);
                int devMinor = Integer.parseUnsignedInt(dev[1], 16);

                long inode = Long.parseUnsignedLong(parts[4], 16);

                String path;
                if (parts.length < 6) {
                    path = null;
                } else {
                    path = parts[5].trim();
                    if (path.isEmpty()) {
                        path = null;
                    }
                }

                return new MMapEntry(start, end, parms, offset, devMajor, devMinor, inode, path);
            });
        } catch (IOException e) {
            throw shouldNotHappen(e);
        }
    }

    public static MMapEntry findFirstByPath(String path) {
        var pattern = Pattern.compile(path);
        try (var maps = maps("self")) {
            return maps.filter(entry -> entry.path() != null
                            && pattern.matcher(entry.path()).matches())
                    .findFirst().orElse(null);
        }
    }

    public static MMapEntry findFirst(Predicate<MMapEntry> test) {
        try (var maps = maps("self")) {
            return maps.filter(test).findFirst().orElse(null);
        }
    }
}
