# Preserve Room database & entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Preserve ViewModels
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Preserve line numbers for crash diagnostics
-keepattributes SourceFile,LineNumberTable
