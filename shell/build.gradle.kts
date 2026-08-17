plugins {
    id("com.android.application") version "9.3.0"
}

android {
    namespace = "com.omadroid.shell"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.omadroid.shell"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets.getByName("main") {
        assets.directories.add(layout.projectDirectory.dir("plugins").asFile.path)
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
