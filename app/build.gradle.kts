plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.sweetlove.directdetection"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sweetlove.directdetection"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}
buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0")
    }
}
dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-auth")
    // https://mvnrepository.com/artifact/com.google.firebase/firebase-firestore
    implementation("com.google.firebase:firebase-firestore:25.1.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Also add the dependencies for the Credential Manager libraries and specify their versions
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation(libs.camera.view)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.core.ktx)


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.play.services.auth)

    // CameraX core
    implementation(libs.androidx.camera.core)
    // CameraX Camera2 implementation
    implementation(libs.androidx.camera.camera2)
    // CameraX lifecycle
    implementation(libs.androidx.camera.lifecycle)
    // CameraX view (cho PreviewView)
    implementation(libs.camera.view.v134)

    implementation("io.socket:socket.io-client:2.1.0")
    implementation ("com.google.guava:guava:31.0.1-android")
    implementation ("org.java-websocket:Java-WebSocket:1.5.4")

    implementation("com.google.firebase:firebase-appcheck-playintegrity:17.1.2")
    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation("com.google.firebase:firebase-firestore:24.10.3")


    implementation ("androidx.navigation:navigation-fragment:2.7.7")
    implementation ("androidx.navigation:navigation-ui:2.7.7")
    implementation ("com.google.android.material:material:1.11.0")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")


}