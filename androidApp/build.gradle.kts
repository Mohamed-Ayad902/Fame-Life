import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.koin.android)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "org.mayad.fame_life"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.mayad.fame_life"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        // The Pulse backend (backend/README.md). Set pulse.backendUrl in local.properties; without it the app uses
        // the phone's speech recognizer and the on-device audience.
        val localProperties = Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
        }
        val backendUrl = localProperties.getProperty("pulse.backendUrl", "").trim()
        buildConfigField("String", "PULSE_BACKEND_URL", "\"$backendUrl\"")

        // AdMob rewarded ads for earning points (docs/ARCHITECTURE.md, "Points economy"). Set pulse.admobAppId and
        // pulse.admobRewardedUnitId in local.properties once the AdMob app exists. Without a unit id the ad offer is
        // hidden; the SDK still needs an app id at start-up, so Google's published sample app id stands in.
        val admobAppId = localProperties.getProperty("pulse.admobAppId", "").trim().ifEmpty { "ca-app-pub-3940256099942544~3347511713" }
        manifestPlaceholders["admobAppId"] = admobAppId
        val rewardedUnitId = localProperties.getProperty("pulse.admobRewardedUnitId", "").trim()
        buildConfigField("String", "PULSE_REWARDED_AD_UNIT", "\"$rewardedUnitId\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}