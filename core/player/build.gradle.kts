plugins {
    alias(libs.plugins.jellymusic.android.library)
    alias(libs.plugins.jellymusic.android.hilt)
}

android {
    namespace = "dev.themselg.jellymusic.player"
}

dependencies {
    implementation(project(":core:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Media3 playback + cast
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.cast)
    implementation(libs.play.services.cast.framework)
}
