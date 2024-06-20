import com.gradleup.librarian.core.librarianModule

plugins {
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  implementation(libs.clikt)
  implementation(libs.serialization.json)
  implementation(libs.apollo.tooling)
}

librarianModule()  
