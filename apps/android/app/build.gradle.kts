plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.centwise"
    compileSdk = 35

    val releaseKeystoreFile = System.getenv("CENTWISE_KEYSTORE_FILE")
    val releaseKeystorePassword = System.getenv("CENTWISE_KEYSTORE_PASSWORD")
    val releaseKeyAlias = System.getenv("CENTWISE_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("CENTWISE_KEY_PASSWORD")
    val releaseSigningConfigured = listOf(
        releaseKeystoreFile,
        releaseKeystorePassword,
        releaseKeyAlias,
        releaseKeyPassword
    ).all { !it.isNullOrBlank() }

    if (gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) } &&
        !releaseSigningConfigured
    ) {
        throw GradleException(
            "Release signing is not configured. Set CENTWISE_KEYSTORE_FILE, " +
                "CENTWISE_KEYSTORE_PASSWORD, CENTWISE_KEY_ALIAS, and " +
                "CENTWISE_KEY_PASSWORD."
        )
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseKeystoreFile!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.centwise"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    sourceSets["main"].jniLibs.directories.add("src/main/jniLibs")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.biometric)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime.ktx)
    implementation("net.java.dev.jna:jna:5.16.0@aar")

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation("junit:junit:4.13.2")
}
