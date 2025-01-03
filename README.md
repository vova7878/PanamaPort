## PanamaPort

`PanamaPort` is a library implementing the [Foreign Function & Memory API](https://openjdk.org/jeps/454) from JDK 22+ for Android since Oreo (API level 26)

### Components

The project contains 4 sublibraries:

- AndroidPanama - the entire public API and implementation of the Panama project
- AndroidUnsafe - low-level implementation details and additional capabilities not provided by the original API
- LLVM - bindings to the built-in libLLVM.so Android library
- VarHandleApi - backport of java.lang.invoke.VarHandle, but without polymorphic behavior. Unfortunately, it did not exist before Android 9

### Get started

To use the library you need at least JDK 21 and compileSdk 34

Before you add the library to your project, make sure it supports the dependencies from jitpack.io

Add it in your root settings.gradle at the end of repositories:

```
dependencyResolutionManagement {
    repositories {
        <...>
        maven { url 'https://jitpack.io' }
    }
}
```

After this, you can add the library to the list of dependencies:

```
dependencies {
    implementation 'com.github.vova7878.PanamaPort:<sublibrary>:<version>'
}
```

For example:

```
dependencies {
    implementation 'com.github.vova7878.PanamaPort:AndroidPanama:v0.0.6-preview'
}
```

---

### Supported features and differences from the original API

PanamaPort implements FULL Foreign Function & Memory API support, including:

- Downcall handles - allows Java code to call foreign functions
- Upcall stubs - allows foreign functions to call Java method handles
- Linker options such as captureCallState("errno") and even critical(allowHeapAccess: true)
- Volatile access to native memory via VarHandles (including compareAndSet, getAndSet, etc.)
- Management of memory segments via Arenas (and all Arena.ofAuto(), global(), ofConfined() and ofShared() methods)
- And much more

Important differences:

- All classes from the java.lang.foreign package are in the com.v7878.foreign package because it is a port and not part of the Java core library
- Due to technical difficulties, my implementation of VarHandle is used instead of java.lang.invoke.VarHandle
- Added MemoryLayout.valueLayout() and paddedStructLayout() methods, which were removed in recent versions of the source API

### Interaction with jextract

I have not tested this tool in practice, but all classes it generates should work after manually replacing java.lang.foreign with com.v7878.foreign and java.lang.invoke.VarHandle with com.v7878.invoke.VarHandle

### Examples and tests

Examples are available in [this](https://github.com/vova7878/PanamaExamples) repository. The same repository is where tests are ported from the OpenJ9 and Hotspot implementations
