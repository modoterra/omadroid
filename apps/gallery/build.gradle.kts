import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application") version "9.3.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
}

val sharedUi = layout.buildDirectory.dir("generated/omadroidUi")
val syncSharedUi by tasks.registering(Copy::class) {
    from(layout.projectDirectory.dir("../../launcher/src/main/java")) {
        include(
            "com/omadroid/compose/Compose.kt",
            "com/omadroid/compose/Input.kt",
            "com/omadroid/compose/Overscroll.kt",
            "com/omadroid/compose/Slot.kt",
            "com/omadroid/compose/Stack.kt",
            "com/omadroid/compose/Widgets.kt",
            "com/omadroid/launcher/widget/GridStyle.kt",
            "com/omadroid/launcher/widget/IconFonts.kt",
        )
    }
    into(sharedUi)
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(syncSharedUi)
}

android {
    namespace = "com.omadroid.gallery"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.omadroid.gallery"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets.getByName("main") {
        kotlin.directories.add(sharedUi.get().asFile.path)
        kotlin.directories.add(layout.projectDirectory.dir("../../shell/lib/theme").asFile.path)
        assets.directories.add(layout.projectDirectory.dir("../../shell/themes").asFile.path)
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.foundation:foundation:1.8.2")
    implementation("androidx.compose.runtime:runtime:1.8.2")
    implementation("androidx.compose.ui:ui:1.8.2")
    testImplementation("junit:junit:4.13.2")
}
