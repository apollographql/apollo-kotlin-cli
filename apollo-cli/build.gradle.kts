import com.gradleup.librarian.gradle.librarianModule

plugins {
  alias(libs.plugins.serialization)
  alias(libs.plugins.kotlin)
  id("application")
}

dependencies {
  implementation(libs.clikt)
  implementation(libs.serialization.json)
  implementation(libs.apollo.tooling)
  implementation(libs.kotlinx.coroutines)
  implementation(libs.kotlinx.datetime)
  implementation(libs.okhttp)
  implementation(libs.jsonpathkt)
  implementation(libs.mordant)
}

librarianModule(true)

application {
  mainClass.set("com.apollographql.cli.MainKt")
  applicationName = "apollo-kotlin-cli"
}