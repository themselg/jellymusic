plugins {
    alias(libs.plugins.jellymusic.android.feature)
}

android {
    namespace = "dev.themselg.jellymusic.feature.home"
}

dependencies {
    implementation(libs.androidx.paging.compose)
}
