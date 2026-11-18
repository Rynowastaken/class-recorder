plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ryno.classrecorder"
    compileSdk = 36

    defaultConfig {
        // The real package. Release/user installs keep this identity and its data.
        applicationId = "com.ryno.classrecorder"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    // ---------------------------------------------------------------------
    // Side-by-side test install.
    //
    // "classrecorderTest" gets its own applicationId, so it installs
    // alongside the real app with a completely separate data directory. It
    // never sees, migrates, or overwrites the recordings the user already
    // has under com.ryno.classrecorder - which means a UI session can be
    // tested, screenshotted, and even wrecked without touching real data.
    //
    //   ./gradle :app:assembleClassrecorderTestDebug
    //   adb install -r app-classrecorderTest-debug.apk
    //
    // Source package (namespace) stays com.ryno.classrecorder, so no Kotlin
    // file or manifest entry needs to change.
    // ---------------------------------------------------------------------
    flavorDimensions += "channel"
    productFlavors {
        create("classrecorder") {
            dimension = "channel"
            isDefault = true
        }
        create("classrecorderTest") {
            dimension = "channel"
            applicationIdSuffix = ".uitest"
            versionNameSuffix = "-uitest"
            resValue("string", "app_name", "ClassRecorder UI Test")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    // Material 3 1.4.0 is the first stable release carrying the expressive
    // system (MaterialExpressiveTheme, MotionScheme, expressive button groups,
    // split buttons, floating toolbar, loading indicator). It is NOT in a BOM
    // yet, so it is pinned directly.
    //
    // Compose is aligned on the 2025.09.01 BOM because M3 1.4.0 expects
    // foundation/ui 1.8.x+. Mixing a newer runtime (which transitively resolves
    // to 1.9.0) with older ui produced "Cannot access ... it is internal in
    // file" errors from mismatched Kotlin metadata.
    implementation(platform("androidx.compose:compose-bom:2025.09.01"))
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite")

    // Shizuku client. Lets us make one privileged binder call as shell uid
    // (INotificationManager.setCanBePromoted) that no app can make for itself.
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
