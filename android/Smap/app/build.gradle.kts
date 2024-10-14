plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.dmonster.smap"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dmonster.smap"
        minSdk = 24
        targetSdk = 34
        versionCode = 14
        versionName = "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        val baseUrl: String = project.properties["baseUrl"] as String
        val baseUrlDebug: String = project.properties["baseUrlDebug"] as String
        val baseApiUrl: String = project.properties["baseApiUrl"] as String
        val baseApiUrlDebug: String = project.properties["baseApiUrlDebug"] as String
        val baseFileApiUrl: String = project.properties["baseFileApiUrl"] as String
        val baseFileApiUrlDebug: String = project.properties["baseFileApiUrlDebug"] as String

        getByName("debug") {
            isMinifyEnabled = true
            isJniDebuggable = true
            //applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "BASE_URL", baseUrlDebug)
            buildConfigField("String", "BASE_API_URL", baseApiUrlDebug)
            buildConfigField("String", "BASE_FILE_API_URL", baseFileApiUrlDebug)

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("release") {
            isMinifyEnabled = true
            buildConfigField("String", "BASE_URL", baseUrl)
            buildConfigField("String", "BASE_API_URL", baseApiUrl)
            buildConfigField("String", "BASE_FILE_API_URL", baseFileApiUrl)

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            //signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.44")
    kapt("com.google.dagger:hilt-android-compiler:2.44")

    // androidX <
    implementation("androidx.activity:activity-ktx:1.6.1")
    implementation("androidx.fragment:fragment-ktx:1.5.5")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")

    // Coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

    // Coil
    implementation("io.coil-kt:coil:2.3.0")

    //glide
    implementation("com.github.bumptech.glide:glide:4.13.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.13.0")

    // Google Location
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-vision:20.1.3")

    //Retrofit
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.3.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.9.0")
    implementation("com.squareup.okio:okio:2.8.0")
    implementation("org.jsoup:jsoup:1.15.2")

    //room
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    //crop
    implementation("com.vanniktech:android-image-cropper:4.5.0")

    //firebase
    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:32.2.3"))

    // Also add the dependency for the Google Play services library and specify its version
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    // Firebase Cloud Messaging (Kotlin)
    implementation("com.google.firebase:firebase-messaging-ktx")

    implementation("com.android.billingclient:billing:6.2.0")

    //AdMob
    implementation("com.google.android.gms:play-services-ads:22.6.0")
}

kapt {
    correctErrorTypes = true
}