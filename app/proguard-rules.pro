-keep class androidx.room.RoomDatabase_Impl { *; }
-keep @kotlinx.serialization.Serializable class ** { *; }

# SDL registers Java bridge methods by their source names from JNI_OnLoad.
# SDL 3.4.10's AAR consumer rules omit newer native callbacks such as
# onNativeFileDialog, so preserve the complete bridge package in minified APKs.
-keep,includedescriptorclasses,allowoptimization class org.libsdl.app.** { *; }
