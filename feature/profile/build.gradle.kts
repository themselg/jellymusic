plugins {
    alias(libs.plugins.jellymusic.android.feature)
}

android {
    namespace = "dev.themselg.jellymusic.feature.profile"
}

dependencies {
    implementation(libs.androidx.appcompat)
}
