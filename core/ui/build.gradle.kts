plugins {
    alias(libs.plugins.jellymusic.android.library.compose)
    alias(libs.plugins.jellymusic.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.themselg.jellymusic.ui"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:player"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Image loading + dynamic color from album art
    implementation(libs.coil.compose)
    implementation(libs.materialkolor)

    // Custom Compose cast chooser / volume overlay
    implementation(libs.androidx.media3.cast)
    implementation(libs.play.services.cast.framework)
    implementation(libs.androidx.mediarouter)
}
