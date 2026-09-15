plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.ktlint)
}

// Release signing comes from the environment only (AGENTS.md rule 5): CI maps GitHub secrets to
// these four variables. When any of them is missing the release build type stays unsigned, so a
// plain `assembleRelease` still works locally and the debug build is never affected.
val releaseSigningEnv =
    listOf("MARQUE_KEYSTORE_PATH", "MARQUE_KEYSTORE_PASSWORD", "MARQUE_KEY_ALIAS", "MARQUE_KEY_PASSWORD")
        .associateWith { providers.environmentVariable(it).orNull }
        .takeIf { env -> env.values.none { it.isNullOrBlank() } }
        ?.mapValues { it.value!! }

android {
    namespace = "io.github.ardaulas.marque"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.ardaulas.marque"
        minSdk = 26
        targetSdk = 36
        // versionCode must increase monotonically with every release; bump it by hand with versionName.
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        releaseSigningEnv?.let { env ->
            create("release") {
                storeFile = file(env.getValue("MARQUE_KEYSTORE_PATH"))
                storePassword = env.getValue("MARQUE_KEYSTORE_PASSWORD")
                keyAlias = env.getValue("MARQUE_KEY_ALIAS")
                keyPassword = env.getValue("MARQUE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Null (unsigned) unless the environment above provided a keystore.
            signingConfig = signingConfigs.findByName("release")
        }
    }

    // With AGP's built-in Kotlin, kotlin.compilerOptions.jvmTarget defaults to targetCompatibility.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // BuildConfig.DEBUG gates the OkHttp logging interceptor.
        buildConfig = true
    }
}

// The Room Gradle plugin wires schema export (exportSchema = true) to this directory; the JSON
// files are committed so schema changes show up in review.
room {
    schemaDirectory("$projectDir/schemas")
}

ktlint {
    version.set(libs.versions.ktlint.get())
    android.set(true)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Registers the activity that createComposeRule() hosts stateless screens in.
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Network: Retrofit + kotlinx.serialization converter over OkHttp.
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // Pins the coroutines runtime in the app APK to the same version as kotlinx-coroutines-test.
    // AndroidX pulls in an older transitive version; the androidTest APK shares the app's copy,
    // and 1.11.0's test builders call into core APIs that the older version lacks.
    implementation(libs.kotlinx.coroutines.android)

    // Local cache: Room (room-ktx was merged into room-runtime in 2.7, so it is not declared).
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockwebserver3)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
