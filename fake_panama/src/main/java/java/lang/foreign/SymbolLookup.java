package java.lang.foreign;

import java.nio.file.Path;
import java.util.Optional;

@FunctionalInterface
public interface SymbolLookup {
    Optional<MemorySegment> find(String name);

    default SymbolLookup or(SymbolLookup other) {
        throw new UnsupportedOperationException("Stub!");
    }

    static SymbolLookup loaderLookup() {
        throw new UnsupportedOperationException("Stub!");
    }

    static SymbolLookup libraryLookup(String name, Arena arena) {
        throw new UnsupportedOperationException("Stub!");
    }

    static SymbolLookup libraryLookup(Path path, Arena arena) {
        throw new UnsupportedOperationException("Stub!");
    }
}
