## About

`PanamaPort` is a library implementing the [Foreign Function & Memory API](https://openjdk.org/jeps/454) for Android 8.0+ (API level 26)

### Components

This project contains 4 sublibraries:

- Core - the entire public API and implementation of the Panama project
- Unsafe - low-level implementation details and additional capabilities not provided by the original API
- LLVM - bindings to the built-in libLLVM.so Android library
- VarHandles - backport of java.lang.invoke.VarHandle which didn't exist in android 8.x

### Requirements

- JDK 21+
- Gradle 8.7+
- Android Gradle plugin 8.6.0+
- compileSdk 35+

### Get started

Just add this library to the list of dependencies:

```
dependencies {
    implementation 'io.github.vova7878.panama:Core:v0.1.0'
}
```

---

### Supported features

PanamaPort implements full Foreign Function & Memory API support, including:

- Downcall handles - allows Java code to call foreign functions
- Upcall stubs - allows foreign functions to call Java method handles
- Linker options such as captureCallState("errno") and even critical(allowHeapAccess: true)
- Access to native memory via VarHandles
- Management of memory segments via Arenas
- And much more

### Differences from the original API

- Package java.lang.foreign moved to com.v7878.foreign package because it is a port and not part of the Java core library
- WrongThreadException class is also in the com.v7878.foreign package because it doesn't exist on android
- My implementation of VarHandle is used instead of java.lang.invoke.VarHandle

### What was added

All non-standard apis added to the port are marked with `@PortAPI` annotation

This includes just useful methods:

- `MemoryLayout.valueLayout(Class<?>, ByteOrder)`
- `MemoryLayout.sequenceLayout(MemoryLayout)`
- `MemoryLayout.paddedStructLayout(MemoryLayout...)`
- `MemorySegment.nativeAddress()`

Android-specific linker options:

- `Option.allowExceptions()` and `Option.JNIEnvArg(int)`

Port of methods outside the java.lang.foreign package:

- `FileChannelUtils.map(FileChannel, MapMode, long, long, Arena)`

### Interaction with jextract

Usually, to use the output of jextract, it is enough to fix package names. If something doesn't work properly, please create an issue

### Examples and tests

Examples are available in [this](https://github.com/vova7878/PanamaExamples) repository. The same repository is where tests are ported from the OpenJ9 and Hotspot implementations
