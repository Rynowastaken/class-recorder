# Keep Rust JNI entrypoints and Kotlin callback methods invoked from Rust
-keep class com.ryno.classrecorder.core.RustCore { *; }
-keep interface com.ryno.classrecorder.core.RecordingBridge { *; }
-keepclassmembers class * implements com.ryno.classrecorder.core.RecordingBridge {
    public java.lang.String startRecordingFromAgent(java.lang.String);
    public java.lang.String stopRecordingFromAgent();
}

# Shizuku provider & binder IPC
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-dontwarn moe.shizuku.**
-dontwarn rikka.shizuku.**
