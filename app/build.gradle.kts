plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "mentat.music.com.publicarbluesky"
    compileSdk = 34 // He vuelto a 34, 36 es una versión inestable. Usa 34.

    defaultConfig {
        applicationId = "mentat.music.com.publicarbluesky"
        minSdk = 24
        targetSdk = 34 // He vuelto a 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

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
        // Para Java 11 necesitas cambiar esto en el archivo gradle.properties
        // Por ahora lo dejamos en 1.8 que es lo estándar y más estable.
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        // Esta es la forma correcta y más robusta.
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    // Añade este bloque si no lo tienes. Evita conflictos de duplicados.
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Tus dependencias están bien
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // La dependencia de room-compiler debe ser "ksp(libs.androidx.room.compiler)" o "kapt(...)"
    // Por ahora la comento para que no de error
    // implementation(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3") // Versión estable
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3") // Versión estable

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("org.jsoup:jsoup:1.18.1") // Versión más reciente

    implementation("androidx.work:work-runtime-ktx:2.9.0") // Versión estable

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
