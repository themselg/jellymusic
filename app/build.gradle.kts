import java.util.Properties

plugins {
    alias(libs.plugins.jellymusic.android.application)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing is read from a gitignored app/keystore.properties so NO secrets live in the
// repo. Without that file, `release` builds come out unsigned — clone the repo and you can build
// and test debug freely; to produce a signed APK, create your own keystore and keystore.properties
// (see keystore.properties.example). The signing key is never inherited from this repo.
val keystorePropsFile = file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "dev.themselg.jellymusic"

    defaultConfig {
        applicationId = "dev.themselg.jellymusic"
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Only define the release signing config when keystore.properties is present.
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    productFlavors {
        // libre: FOSS build, no Google Play Services / Chromecast. Distinct id so it can be
        // installed alongside the proprietary build.
        getByName("libre") {
            dimension = "edition"
            applicationIdSuffix = ".libre"
            versionNameSuffix = "-libre"
        }
        getByName("proprietary") {
            dimension = "edition"
            versionNameSuffix = "-proprietary"
        }
    }

    buildTypes {
        release {
            // null when keystore.properties is absent → release stays unsigned (build from scratch).
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        // compose is enabled by the convention plugin; buildConfig for BuildConfig.VERSION_NAME.
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Modules
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:player"))
    implementation(project(":core:ui"))
    implementation(project(":feature:login"))
    implementation(project(":feature:home"))
    implementation(project(":feature:search"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:detail"))
    implementation(project(":feature:player"))
    implementation(project(":feature:library"))

    // AndroidX core
    implementation(libs.androidx.core.ktx)
    // appcompat: only for AppCompatDelegate per-app locale switching (back-compat below API 33)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose (BOM + Compose plugin from the convention plugin)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Async (type-safe nav routes use kotlinx-serialization, declared in :core:ui)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Coil image loader provided app-wide (AppModule + SingletonImageLoader.Factory)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Cast: MainActivity routes the volume keys to the active Cast session — proprietary only
    "proprietaryImplementation"(libs.play.services.cast.framework)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
}
