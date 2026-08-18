plugins {
    id("com.android.application") version "9.3.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
}

android {
    namespace = "com.omadroid.launcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.omadroid.launcher"
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
        kotlin.directories.add(layout.projectDirectory.dir("../shell/lib/theme").asFile.path)
        assets.directories.add(layout.projectDirectory.dir("../shell/themes").asFile.path)
        assets.directories.add(layout.buildDirectory.dir("generated/pluginAssets").get().asFile.path)
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

val stagePluginAssets by tasks.registering(Copy::class) {
    from(layout.projectDirectory.dir("../shell/plugins")) {
        exclude("README.md")
    }
    into(layout.buildDirectory.dir("generated/pluginAssets/plugins"))
}

tasks.named("preBuild") {
    dependsOn(stagePluginAssets)
}

dependencies {
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.foundation:foundation:1.8.2")
    implementation("androidx.compose.runtime:runtime:1.8.2")
    implementation("androidx.compose.ui:ui:1.8.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
