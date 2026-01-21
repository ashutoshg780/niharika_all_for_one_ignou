plugins {
    id("com.android.application") version "8.13.0"
    id("org.jetbrains.kotlin.android") version "2.2.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
    id("com.google.gms.google-services") version "4.4.3"
    id("com.google.firebase.crashlytics") version "3.0.6"
    id("com.google.firebase.firebase-perf")
}

android {
    namespace = "com.example.niharika_all_for_one"
    compileSdkVersion("android-36.1")

    defaultConfig {
        applicationId = "com.example.niharika_all_for_one"
        minSdk = 29                                 
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    buildFeatures {
        viewBinding = true
    }
    buildToolsVersion = "36.0.0"
    ndkVersion = "28.2.13676358"

//
//    aaptOptions {
//        ignoreAssetsPattern = "!.jpg:!.png:!.gif"
//    }

}

dependencies {

    // --- AndroidX Core ---
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-ktx:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.4")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.4")

    // --- Material Design ---
    implementation("com.google.android.material:material:1.13.0")

    // --- Compose ---
    implementation(platform("androidx.compose:compose-bom:2025.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.activity:activity:1.11.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // --- RecyclerView ---
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.recyclerview:recyclerview-selection:1.2.0")

    // --- Firebase (via BoM) ---
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
//    implementation("com.google.firebase:firebase-auth")
//    implementation("com.google.firebase:firebase-auth-ktx")
//    implementation("com.google.firebase:firebase-firestore")
//    implementation("com.google.firebase:firebase-database")
//    implementation("com.google.firebase:firebase-database-ktx")
//    implementation("com.google.firebase:firebase-analytics")
//    implementation("com.google.firebase:firebase-analytics-ktx")
//    implementation("com.google.firebase:firebase-crashlytics")
//    implementation("com.google.firebase:firebase-crashlytics-ktx")
//    implementation("com.google.firebase:firebase-storage")
//    implementation("com.google.firebase:firebase-storage-ktx")

    implementation("com.google.firebase:firebase-analytics:23.0.0")
    implementation("com.google.firebase:firebase-auth:24.0.1")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.firebase:firebase-database:22.0.1")
    implementation("com.google.firebase:firebase-ai:17.3.0")
    implementation("com.google.firebase:firebase-storage:22.0.1")
    implementation("com.google.firebase:firebase-functions:22.0.1")
    implementation("com.google.firebase:firebase-crashlytics:20.0.2")

//    implementation("com.google.firebase:firebase-ml-vision:24.1.0")
    implementation("com.google.firebase:firebase-perf:22.0.2")
    implementation("com.google.firebase:firebase-messaging:25.0.1")
    implementation("com.google.firebase:firebase-inappmessaging-display:22.0.1")




    implementation("com.google.firebase:firebase-firestore:26.0.1")

    // FirebaseUI
    implementation("com.firebaseui:firebase-ui-firestore:9.0.0")

    // --- Play Services ---
    implementation("com.google.android.play:integrity:1.5.0")

    // --- ML Kit (OCR) ---
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.1")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition-devanagari:16.0.1")

    // --- ML Kit (Barcode) ---
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")

    // --- ML Kit (Face) ---
    implementation("com.google.mlkit:face-detection:16.1.7")
    implementation("com.google.android.gms:play-services-mlkit-face-detection:17.1.0")

    // --- ML Kit (Document) ---
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")

    // --- Glide ---
    implementation("com.github.bumptech.glide:glide:5.0.5")

    // --- GIF Support ---
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.29")

    // --- Tests ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
