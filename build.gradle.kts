// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.2" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
    id("io.ktor.plugin") version "2.3.8"

}

application {
    mainClass.set("com.example.jc.App")
}