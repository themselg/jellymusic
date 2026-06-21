// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/** Shared Android + Kotlin config applied to every Android module (lib or app). */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = 35
        defaultConfig {
            minSdk = 31
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
        // FOSS vs Chromecast editions. Declared in every Android module so cross-module
        // variant matching is automatic; app-specific id/version suffixes live in :app.
        flavorDimensions += "edition"
        productFlavors {
            create("libre")
            create("proprietary")
        }
    }
    extensions.configure<KotlinAndroidProjectExtension> {
        jvmToolchain(21)
    }
}
