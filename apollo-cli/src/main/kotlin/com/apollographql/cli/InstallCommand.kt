package com.apollographql.cli

import com.github.ajalt.clikt.core.CliktCommand
import java.io.File
import kotlin.system.exitProcess

private val initFilename = "init.sh"
val home = File(System.getProperty("user.home"))
val installDir = home.resolve(".apollo-kotlin-cli")

/**
 * The install command is run from the install.sh script and finishes installation:
 * - copies files to ~/.apollo-kotlin-cli
 * - add ~/.apollo-kotlin-cli/init.sh to the .zshrc/.bashrc
 */
internal class InstallCommand : CliktCommand(
  hidden = true,
  help = "installs apollo-kotlin-cli in your home directory"
) {

  private fun copyFiles() {
    val uri = this::class.java.protectionDomain.codeSource.location.toURI()
    if (uri.scheme != "file") {
      System.err.println("Cannot install from non-file location '$uri'")
      exitProcess(1)
    }

    val sourceDir = File(uri.path).parentFile.parentFile

    sourceDir.resolve("lib").apply {
      check(exists()) {
        "Cannot find '$path': was this installed from a distribution?"
      }
    }
    sourceDir.resolve("bin").apply {
      check(exists()) {
        "Cannot find '$path': was this installed from a distribution?"
      }
    }

    sourceDir.copyRecursively(installDir, overwrite = true)

    installDir.resolve("bin/apollo-kotlin-cli").setExecutable(true)

    println("apollo-kotlin-cli has been installed in '${installDir.path}' ✅")

    installDir.resolve(initFilename).writeText("""
      export PATH="$installDir/bin:${'$'}PATH"
    """.trimIndent())
  }

  override fun run() {
    copyFiles()

    val source = "source \"${installDir.resolve(initFilename)}\""
    var found = false
    listOf(".zshrc", ".bashrc").forEach {
      home.resolve(it).apply {
        if (exists()) {
          found = true
          if (!readText().contains(source)) {
            appendText("\n")
            appendText(source)
          }
        }
      }
    }

    if (!found) {
      println("Cannot detect your shell. Add the following to your init scripts:")
      println(source)
    } else {
      println("Open a new terminal or run the command below to start using apollo-kotlin-cli")
      println(source)
    }
  }
}
