plugins {
    alias(libs.plugins.jellymusic.jvm.library)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    // paging-common is pure JVM: lets repository interfaces expose Flow<PagingData<…>>.
    api(libs.androidx.paging.common)
    implementation(libs.kotlinx.coroutines.core)
}
