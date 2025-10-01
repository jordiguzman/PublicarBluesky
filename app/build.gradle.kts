plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "mentat.music.com.publicarbluesky"
    compileSdk = 36

    defaultConfig {
        applicationId = "mentat.music.com.publicarbluesky"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ✅ BuildConfig fields para tus credenciales
        buildConfigField(
            "String",
            "BLUESKY_USERNAME",
            "\"${project.findProperty("BLUESKY_USERNAME") ?: ""}\""
        )
        buildConfigField(
            "String",
            "BLUESKY_PASSWORD",
            "\"${project.findProperty("BLUESKY_PASSWORD") ?: ""}\""
        )
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4") // O la última versión estable
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    implementation("org.jsoup:jsoup:1.21.2")

    implementation("androidx.work:work-runtime-ktx:2.10.5") // O la última versión estable

    implementation("com.squareup.okhttp3:okhttp:5.1.0") // O la versión que estés usando de okhttp
    implementation("com.squareup.okhttp3:logging-interceptor:5.1.0") // <--- ESTA ES LA IMPORTANTE para HttpLoggingInterceptor

}
