plugins {
    alias(libs.plugins.jellymusic.android.application)
    alias(libs.plugins.kotlin.serialization)
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
        // Self-signed release key (checked into the repo for this personal build). For a public
        // release, move these to a private keystore.properties / environment variables instead.
        create("release") {
            storeFile = file("jellymusic-release.keystore")
            storePassword = "REDACTED"
            keyAlias = "jellymusic"
            keyPassword = "REDACTED"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
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

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Async / serialization
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Storage
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    // Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.database)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)

    // Google Cast (Chromecast)
    implementation(libs.androidx.media3.cast)
    implementation(libs.play.services.cast.framework)
    implementation(libs.androidx.mediarouter)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Jellyfin SDK
    implementation(libs.jellyfin.core)

    // Dynamic color from album art
    implementation(libs.materialkolor)

    // Wavy (squiggly) seek bar, Android 13 media style
    implementation(libs.wavy.slider)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
}
