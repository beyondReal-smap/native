// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("org.jetbrains.kotlin.android") apply false version "1.8.20"
    id ("androidx.navigation.safeargs.kotlin") apply false
    id ("com.google.dagger.hilt.android") apply false
    id ("com.google.gms.google-services") apply false version "4.3.15"
}