@file:OptIn(ExperimentalSerializationApi::class)

package com.apollographql.cli

import com.nfeld.jsonpathkt.kotlinx.resolvePathAsStringOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.zip.ZipInputStream
import kotlin.time.Duration.Companion.days

internal val logFile = installDir.resolve("logs")

internal fun log(message: String) {
  logFile.parentFile.mkdirs()
  if (logFile.exists() && logFile.length() > 10_000_000) {
    logFile.writeText("")
  }

  logFile.appendText(message)
}

internal fun log(throwable: Throwable) {
  log("${Clock.System.now()}\n")
  log(throwable.stackTraceToString())
  log("\n")
}

internal fun checkVersion(): String? {
  val config: Config? = config()

  log("minimalVersion=${config?.minimalVersion} - latestVersion=${config?.latestVersion}\n")
  if (config != null && config.latestVersion > config.minimalVersion) {
    return config.latestVersion
  }
  val lastCheck = config?.lastCheck?.let { Instant.parse(it) } ?: Instant.DISTANT_PAST

  if (Clock.System.now() < lastCheck + 1.days) {
    return null
  }

  val thread = Thread {
    try {
      log("get latest version\n")
      getLatestVersion()
    } catch (e: Exception) {
      log(e)
    }
  }
  thread.start()

  return null
}

/**
 * @throws Exception
 */
private fun getLatestVersion() {
  val request = Request.Builder()
    .get()
    .url("https://api.github.com/repos/apollographql/apollo-kotlin-cli/releases/latest")
    .build()

  val response = OkHttpClient()
    .newCall(request)
    .execute()

  if (!response.isSuccessful) {
    throw Exception("Getting ${request.url} failed (${response.code}): '${response.body?.string()}'")
  }

  val json = response.body!!.string().let {
    Json.parseToJsonElement(it)
  }

  val tagName = json.resolvePathAsStringOrNull("$.tag_name")

  if (tagName == null) {
    throw Exception("Cannot find tag_name in '$json'")
  }

  if (!tagName.startsWith('v')) {
    throw Exception("tag_name must start with 'v' (found '$tagName')")
  }

  // Remember for the next launch
  configSetLatestVersion(
    latestVersion = tagName.substring(1),
    lastCheck = Clock.System.now().toString(),
  )
}

/**
 * Overwrites the current installation in ~/.apollo-kotlin-cli:
 * - gets a new zip
 * - copies everything to ~/.apollo-kotlin-cli
 * - does not touch .zshrc/.bashrc
 * - TODO: do we need to remove files before overwriting them?
 */
internal fun overwrite(version: String) {
  val url = "https://github.com/apollographql/apollo-kotlin-cli/releases/download/v$version/apollo-kotlin-cli-$version.zip"

  val request = Request.Builder()
    .get()
    .url(url)
    .build()

  println("downloading $url...")
  val response = OkHttpClient().newCall(request).execute()
  if (!response.isSuccessful) {
    throw Exception("Getting '$url' failed (${response.code}): '${response.body?.string()}'")
  }

  ZipInputStream(response.body!!.source().inputStream()).use { inputStream ->
    while (true) {
      val entry = inputStream.nextEntry
      if (entry == null) {
        break
      }

      if (!entry.isDirectory) {
        /**
         * The distribution zip contains a first folder, drop it
         * project/lib/foo.jar
         * project/bin/foo
         */
        val output = installDir.resolve(entry.name.substringAfter('/'))
        output.parentFile.mkdirs()
        output.outputStream().use { outputStream ->
          inputStream.copyTo(outputStream)
        }
      }

      inputStream.closeEntry()
    }
  }
  installDir.resolve("bin").walk()
    .filter { it.isFile }
    .forEach {
      it.setExecutable(true)
    }
  configSetMinimalVersion(version)

  println("version $version is installed ✅")
}