import com.gradleup.librarian.gradle.librarianModule

plugins {
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  implementation(libs.clikt)
  implementation(libs.serialization.json)
  implementation(libs.apollo.tooling)
}

librarianModule(true)
