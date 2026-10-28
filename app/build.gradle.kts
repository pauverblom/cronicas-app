plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

// La sincronización usa Firebase. Su configuración (google-services.json) no está en el repositorio: la publicación
// la pone en src/release/ desde un secreto. Sin ella la app compila igual y la sincronización no aparece.
googleServices {
    missingGoogleServicesStrategy = com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy.WARN
}

// Release signing comes from environment variables (set by the release workflow from repo secrets).
// Without them, assembleRelease produces an unsigned APK.
val releaseKeystore: String? = System.getenv("CRONICAS_KEYSTORE_PATH")

android {
    namespace = "io.github.pauverblom.cronicas"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.pauverblom.cronicas"
        minSdk = 26
        targetSdk = 35
        // Overridden by CI from the release tag: -PversionName=1.2.3 -PversionCode=10203
        versionCode = (findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (findProperty("versionName") as String?) ?: "0.1.0"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("CRONICAS_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("CRONICAS_KEY_ALIAS")?.trim()
                // Keys made with keytool -storetype PKCS12 share the store password;
                // Android Studio's wizard can give the key its own (CRONICAS_KEY_PASSWORD).
                keyPassword = System.getenv("CRONICAS_KEY_PASSWORD").takeUnless { it.isNullOrEmpty() }
                    ?: System.getenv("CRONICAS_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        // Las compilaciones de depuración son otra app: se instalan al lado de la de GitHub/Obtainium (firmada con
        // la clave de publicación y con un versionCode mayor) en vez de chocar con ella
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            if (releaseKeystore != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.androidx.palette)
    implementation(libs.material.kolor)
    implementation(libs.image.cropper)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
