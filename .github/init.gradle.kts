// GitHub Actions Gradle init script to handle Maven Central 403 errors
allprojects {
    buildscript {
        repositories {
            mavenCentral()
            gradlePluginPortal()
            google()
        }
    }

    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}