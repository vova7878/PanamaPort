# com.v7878.r8.annotations.DoNotShrink
-dontwarn com.v7878.r8.annotations.DoNotShrink
-keepclasseswithmembers, allowoptimization, allowobfuscation
    @com.v7878.r8.annotations.DoNotShrink class * {*;}
-keepclasseswithmembers, allowoptimization, allowobfuscation class * {
    @com.v7878.r8.annotations.DoNotShrink <init>(...);
    @com.v7878.r8.annotations.DoNotShrink <methods>;
    @com.v7878.r8.annotations.DoNotShrink <fields>;
}

# com.v7878.r8.annotations.DoNotShrinkMembers
-dontwarn com.v7878.r8.annotations.DoNotShrinkMembers
-keepclassmembers, allowoptimization, allowobfuscation
    @com.v7878.r8.annotations.DoNotShrinkMembers class * {*;}
-keepclassmembers, allowoptimization, allowobfuscation class * {
    @com.v7878.r8.annotations.DoNotShrinkMembers <init>(...);
    @com.v7878.r8.annotations.DoNotShrinkMembers <methods>;
    @com.v7878.r8.annotations.DoNotShrinkMembers <fields>;
}

# com.v7878.r8.annotations.DoNotObfuscate
-dontwarn com.v7878.r8.annotations.DoNotObfuscate
-keepclasseswithmembers, allowshrinking, allowoptimization
    @com.v7878.r8.annotations.DoNotObfuscate class * {*;}
-keepclasseswithmembers, allowshrinking, allowoptimization class * {
    @com.v7878.r8.annotations.DoNotObfuscate <init>(...);
    @com.v7878.r8.annotations.DoNotObfuscate <methods>;
    @com.v7878.r8.annotations.DoNotObfuscate <fields>;
}

# com.v7878.r8.annotations.DoNotObfuscateMembers
-dontwarn com.v7878.r8.annotations.DoNotObfuscateMembers
-keepclassmembers, allowshrinking, allowoptimization
    @com.v7878.r8.annotations.DoNotObfuscateMembers class * {*;}
-keepclassmembers, allowshrinking, allowoptimization class * {
    @com.v7878.r8.annotations.DoNotObfuscateMembers <init>(...);
    @com.v7878.r8.annotations.DoNotObfuscateMembers <methods>;
    @com.v7878.r8.annotations.DoNotObfuscateMembers <fields>;
}

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