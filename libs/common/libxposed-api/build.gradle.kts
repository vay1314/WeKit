import com.android.build.api.dsl.LibraryExtension

plugins {
    id("com.android.base")
    id("com.android.library")
}

configure<LibraryExtension> {
    namespace = "io.github.libxposed.api"

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        lint.targetSdk = libs.versions.targetSdk.get().toInt()
        compileSdk = libs.versions.targetSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jdk.get().toInt())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jdk.get().toInt())
    }
}

dependencies {
    compileOnly(libs.androidx.annotation)
}
