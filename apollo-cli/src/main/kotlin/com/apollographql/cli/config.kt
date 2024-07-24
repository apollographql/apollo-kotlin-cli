package com.apollographql.cli

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

@Serializable
class Config(
  val lastCheck: String,
  val latestVersion: String,
  val minimalVersion: String,
)

private val latestVersion = "latest-version.json"
private var theConfig: Config? = null
private var initialized = false

private fun configRead(): Config? {
  return installDir.resolve(latestVersion).run {
    if (exists()) {
      inputStream().use {
        Json.decodeFromStream(it)
      }
    } else {
      null
    }
  }
}

@Synchronized
fun config(): Config? {
  if (!initialized) {
    initialized = true
    return configRead()
  } else {
    return theConfig
  }
}

@Synchronized
fun configSetLatestVersion(latestVersion: String, lastCheck: String) {
  theConfig = Config(
    lastCheck = lastCheck,
    latestVersion = latestVersion,
    minimalVersion = theConfig?.minimalVersion ?: VERSION
  )
  configWrite(theConfig!!)
}

@Synchronized
fun configSetMinimalVersion(minimalVersion: String) {
  if (theConfig != null) {
    theConfig = Config(
      lastCheck = theConfig!!.lastCheck,
      latestVersion = theConfig!!.latestVersion,
      minimalVersion = minimalVersion
    )
    configWrite(theConfig!!)
  }
}


private fun configWrite(config: Config) {
  installDir.resolve(latestVersion).outputStream().use {
    Json.encodeToStream(config, it)
  }
}