plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.claw.motogp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.claw.motogp"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../motogp.keystore")
            storePassword = "motogp2026"
            keyAlias = "motogp"
            keyPassword = "motogp2026"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // JSoup for HTML scraping
    implementation("org.jsoup:jsoup:1.17.2")

    // Google Calendar intents
    implementation("com.google.android.gms:play-services-location:21.2.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
