plugins {
    alias(libs.plugins.jellymusic.android.library)
    alias(libs.plugins.jellymusic.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.themselg.jellymusic.data"
}

dependencies {
    implementation(project(":core:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Paging 3 (PagingSource + Pager); paging-common is pure JVM.
    implementation(libs.androidx.paging.common)

    // Encrypted session storage + DataStore prefs
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    // Media3 offline downloads (DownloadManager + SimpleCache + DatabaseProvider)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.database)

    // Jellyfin SDK (repository implementations + DTO mappers)
    implementation(libs.jellyfin.core)
}
