plugins {
    alias(libs.plugins.jellymusic.android.feature)
}

android {
    namespace = "dev.themselg.jellymusic.feature.player"
}

dependencies {
    implementation(libs.wavy.slider)
}
