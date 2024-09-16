# com.v7878.r8.annotations.DoNotShrinkFull
-dontwarn com.v7878.r8.annotations.DoNotShrinkFull
-keep, allowoptimization, allowobfuscation
    @com.v7878.r8.annotations.DoNotShrinkFull class * {*;}
-keepclasseswithmembers, allowoptimization, allowobfuscation class * {
    @com.v7878.r8.annotations.DoNotShrinkFull <init>(...);
    @com.v7878.r8.annotations.DoNotShrinkFull <methods>;
    @com.v7878.r8.annotations.DoNotShrinkFull <fields>;
}

# com.v7878.r8.annotations.DoNotShrink
-dontwarn com.v7878.r8.annotations.DoNotShrink
-keepclassmembers, allowoptimization, allowobfuscation
    @com.v7878.r8.annotations.DoNotShrink class * {*;}
-keepclassmembers, allowoptimization, allowobfuscation class * {
    @com.v7878.r8.annotations.DoNotShrink <init>(...);
    @com.v7878.r8.annotations.DoNotShrink <methods>;
    @com.v7878.r8.annotations.DoNotShrink <fields>;
}

# com.v7878.r8.annotations.DoNotShrinkType
-dontwarn com.v7878.r8.annotations.DoNotShrinkType
-keep, allowoptimization, allowobfuscation
    @com.v7878.r8.annotations.DoNotShrinkType class * {}

# com.v7878.r8.annotations.DoNotObfuscateFull
-dontwarn com.v7878.r8.annotations.DoNotObfuscateFull
-keep, allowshrinking, allowoptimization
    @com.v7878.r8.annotations.DoNotObfuscateFull class * {*;}
-keepclasseswithmembers, allowshrinking, allowoptimization class * {
    @com.v7878.r8.annotations.DoNotObfuscateFull <init>(...);
    @com.v7878.r8.annotations.DoNotObfuscateFull <methods>;
    @com.v7878.r8.annotations.DoNotObfuscateFull <fields>;
}

# com.v7878.r8.annotations.DoNotObfuscate
-dontwarn com.v7878.r8.annotations.DoNotObfuscate
-keepclassmembers, allowshrinking, allowoptimization
    @com.v7878.r8.annotations.DoNotObfuscate class * {*;}
-keepclassmembers, allowshrinking, allowoptimization class * {
    @com.v7878.r8.annotations.DoNotObfuscate <init>(...);
    @com.v7878.r8.annotations.DoNotObfuscate <methods>;
    @com.v7878.r8.annotations.DoNotObfuscate <fields>;
}

# com.v7878.r8.annotations.DoNotObfuscateType
-dontwarn com.v7878.r8.annotations.DoNotObfuscateType
-keep, allowshrinking, allowoptimization
    @com.v7878.r8.annotations.DoNotObfuscateType class * {}

# com.v7878.r8.annotations.DoNotOptimize
-dontwarn com.v7878.r8.annotations.DoNotOptimize
-keepclassmembers, allowshrinking, allowobfuscation
    @com.v7878.r8.annotations.DoNotOptimize class * {*;}
-keepclassmembers, allowshrinking, allowobfuscation class * {
    @com.v7878.r8.annotations.DoNotOptimize <init>(...);
    @com.v7878.r8.annotations.DoNotOptimize <methods>;
    @com.v7878.r8.annotations.DoNotOptimize <fields>;
}

# com.v7878.r8.annotations.KeepCodeAttribute
-dontwarn com.v7878.r8.annotations.KeepCodeAttribute
-keepclassmembers, allowshrinking, allowobfuscation, allowoptimization, includecode
    @com.v7878.r8.annotations.KeepCodeAttribute class * {*;}
-keepclassmembers, allowshrinking, allowobfuscation, allowoptimization, includecode class * {
    @com.v7878.r8.annotations.KeepCodeAttribute <init>(...);
    @com.v7878.r8.annotations.KeepCodeAttribute <methods>;
}

# com.v7878.r8.annotations.KeepAttributes
-dontwarn com.v7878.r8.annotations.KeepAttributes
-keep, allowshrinking, allowobfuscation, allowoptimization
    @com.v7878.r8.annotations.KeepAttributes class * {*;}
-keepclassmembers, allowshrinking, allowobfuscation, allowoptimization class * {
    @com.v7878.r8.annotations.KeepAttributes <init>(...);
    @com.v7878.r8.annotations.KeepAttributes <methods>;
    @com.v7878.r8.annotations.KeepAttributes <fields>;
}

# com.v7878.r8.annotations.NoSideEffects
-dontwarn com.v7878.r8.annotations.NoSideEffects
-assumenosideeffects @com.v7878.r8.annotations.NoSideEffects class * {*;}
-assumenosideeffects class * {
    @com.v7878.r8.annotations.NoSideEffects <init>(...);
    @com.v7878.r8.annotations.NoSideEffects <methods>;
    @com.v7878.r8.annotations.NoSideEffects <fields>;
}

# R8 specific annotations

# com.v7878.r8.annotations.AlwaysInline
-dontwarn com.v7878.r8.annotations.AlwaysInline
-alwaysinline class * {
    @com.v7878.r8.annotations.AlwaysInline <init>(...);
    @com.v7878.r8.annotations.AlwaysInline <methods>;
}

# com.v7878.r8.annotations.CheckDiscard
-dontwarn com.v7878.r8.annotations.CheckDiscard
-checkdiscard @com.v7878.r8.annotations.CheckDiscard class *
-checkdiscard class * {
    @com.v7878.r8.annotations.CheckDiscard <init>(...);
    @com.v7878.r8.annotations.CheckDiscard <methods>;
    @com.v7878.r8.annotations.CheckDiscard <fields>;
}