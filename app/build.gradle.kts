plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val cinderhellVersionName =
    providers.environmentVariable("CINDERHELL_VERSION_NAME").orElse("0.1.0-dev").get()
val cinderhellVersionCode =
    providers.environmentVariable("CINDERHELL_VERSION_CODE").orElse("1000000").get().toInt()
val previewSigningValues = listOf(
    "CINDERHELL_PREVIEW_KEYSTORE_FILE",
    "CINDERHELL_PREVIEW_KEYSTORE_PASSWORD",
    "CINDERHELL_PREVIEW_KEY_ALIAS",
    "CINDERHELL_PREVIEW_KEY_PASSWORD",
).associateWith { providers.environmentVariable(it).orNull }
val configuredPreviewSigning = previewSigningValues.values.all { !it.isNullOrBlank() }
val partiallyConfiguredPreviewSigning = previewSigningValues.values.any { !it.isNullOrBlank() }
val requirePreviewSigning =
    providers.environmentVariable("CINDERHELL_REQUIRE_PREVIEW_SIGNING").orNull == "true"

check(!partiallyConfiguredPreviewSigning || configuredPreviewSigning) {
    "Preview signing must provide all CINDERHELL_PREVIEW_* environment variables."
}
check(!requirePreviewSigning || configuredPreviewSigning) {
    "A release preview requires the dedicated Cinderhell preview signing identity."
}

android {
    namespace = "dev.cinderhell"
    compileSdk = 37
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "io.github.anthonystainer.cinderhell"
        minSdk = 26
        targetSdk = 35
        versionCode = cinderhellVersionCode
        versionName = cinderhellVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += "arm64-v8a"
        }

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }

    signingConfigs {
        if (configuredPreviewSigning) {
            create("previewRelease") {
                storeFile = file(checkNotNull(previewSigningValues["CINDERHELL_PREVIEW_KEYSTORE_FILE"]))
                storePassword =
                    checkNotNull(previewSigningValues["CINDERHELL_PREVIEW_KEYSTORE_PASSWORD"])
                keyAlias = checkNotNull(previewSigningValues["CINDERHELL_PREVIEW_KEY_ALIAS"])
                keyPassword = checkNotNull(previewSigningValues["CINDERHELL_PREVIEW_KEY_PASSWORD"])
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("preview") {
            initWith(getByName("release"))
            applicationIdSuffix = ".preview"
            signingConfig = if (configuredPreviewSigning) {
                signingConfigs.getByName("previewRelease")
            } else {
                signingConfigs.getByName("debug")
            }
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
        prefab = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    externalNativeBuild {
        cmake {
            path = file("../native/CMakeLists.txt")
            version = "3.31.6"
        }
    }
}

dependencies {
    implementation(files("libs/SDL3-3.4.10.aar"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
}
