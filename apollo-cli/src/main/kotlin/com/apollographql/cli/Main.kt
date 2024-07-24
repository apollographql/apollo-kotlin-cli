package com.apollographql.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.Terminal
import kotlin.system.exitProcess

/**
 * A placeholder command to test that the auto-update runs in the background
 */
class NoOpCommand: CliktCommand(hidden = true) {
  override fun run() {}
}

private class MainCommand(name: String) : CliktCommand(name = name, invokeWithoutSubcommand = true) {
  val version by option().flag()

  init {
    subcommands(DownloadSchemaCommand(), PublishSchemaCommand(), InstallCommand(), NoOpCommand())
  }

  override fun run() {
    if (version) {
      println("apollo-kotlin-cli $VERSION")
      exitProcess(0)
    }

    val subcommand = currentContext.invokedSubcommand
    if (subcommand == null) {
      println(getFormattedHelp())
      exitProcess(1)
    }

    val newVersion = checkVersion()
    if (subcommand !is InstallCommand && newVersion != null) {
      val terminal = Terminal()
      val update = terminal.prompt(
        "A new version is available ($newVersion > $VERSION). Do you want to update",
        default = "y",
        choices = listOf("y", "n")
      )
      if (update == "y") {
        overwrite(newVersion)
      } else {
        configSetMinimalVersion(newVersion)
      }
    }
  }
}

fun main(args: Array<String>) {
  MainCommand("apollo-kotlin-cli")
    .main(args)
}

