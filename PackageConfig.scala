package io.github.edadma.kit_cli

import io.github.edadma.path.Path
import scala.io.StdIn
import scala.util.{Try, Success, Failure}
import toml.derivation.auto._
import toml.Toml

// TOML structure for kit.toml
case class PackageConfig(
                          name: String,
                          version: String,
                          description: String,
                          repository: String,
                          keywords: List[String],
                          authors: List[String],
                          license: String
                        )

case class KitTomlConfig(
                          `package`: PackageConfig,
                          dependencies: Map[String, String] = Map.empty,
                          `dev-dependencies`: Map[String, String] = Map.empty
                        )

object InitCommand {

  def execute(globalOptions: GlobalOptions): Unit = {
    val currentDir = Path(".")
    val kitTomlPath = currentDir / "kit.toml"

    // Check if kit.toml already exists
    if (kitTomlPath.exists()) {
      println(s"kit.toml already exists in ${currentDir.toAbsolutePath}")
      println("Remove it first or run 'kit init' in a different directory.")
      return
    }

    println("This utility will walk you through creating a kit.toml file.")
    println("It only covers the most common items, and tries to guess sensible defaults.")
    println()
    println("See `kit help init` for definitive documentation on these fields")
    println("and exactly what they do.")
    println()
    println("Press ^C at any time to quit.")
    println()

    try {
      val packageConfig = promptForPackageConfig(currentDir)
      val kitConfig = KitTomlConfig(packageConfig)

      // Show preview
      val tomlContent = generateTomlContent(kitConfig)
      println()
      println(s"About to write to ${kitTomlPath.toAbsolutePath}:")
      println()
      println(tomlContent)
      println()

      val confirmation = promptWithDefault("Is this OK?", "yes")
      if (confirmation.toLowerCase.startsWith("y")) {
        kitTomlPath.writeText(tomlContent)
        println(s"✅ Created kit.toml")
      } else {
        println("Aborted.")
      }

    } catch {
      case _: InterruptedException =>
        println("\n^C")
        println("Aborted.")
      case e: Exception =>
        println(s"Error: ${e.getMessage}")
    }
  }

  private def promptForPackageConfig(currentDir: Path): PackageConfig = {
    val defaultName = inferProjectName(currentDir)
    val defaultAuthor = inferAuthor()

    val name = promptWithDefault("package name", defaultName)
    val version = promptWithDefault("version", "0.1.0")
    val description = promptWithDefault("description", "")
    val repository = promptWithDefault("git repository", "")
    val keywordsInput = promptWithDefault("keywords", "")
    val keywords = if (keywordsInput.trim.isEmpty) List.empty
    else keywordsInput.split(',').map(_.trim).toList
    val author = promptWithDefault("author", defaultAuthor)
    val authors = if (author.trim.isEmpty) List.empty else List(author)
    val license = promptWithDefault("license", "MIT")

    PackageConfig(
      name = name,
      version = version,
      description = description,
      repository = repository,
      keywords = keywords,
      authors = authors,
      license = license
    )
  }

  private def promptWithDefault(prompt: String, default: String): String = {
    val displayDefault = if (default.nonEmpty) s" ($default)" else ""
    print(s"$prompt:$displayDefault ")

    val input = StdIn.readLine()
    if (input == null) {
      // Handle Ctrl+C
      throw new InterruptedException()
    }

    if (input.trim.isEmpty) default else input.trim
  }

  private def inferProjectName(currentDir: Path): String = {
    val dirName = currentDir.toAbsolutePath.getFileName.toString
    // Convert to lowercase and replace invalid characters
    dirName.toLowerCase.replaceAll("[^a-z0-9-_]", "-")
  }

  private def inferAuthor(): String = {
    // Try to get author from git config
    try {
      val nameResult = scala.sys.process.Process("git config user.name").!!.trim
      val emailResult = scala.sys.process.Process("git config user.email").!!.trim

      if (nameResult.nonEmpty && emailResult.nonEmpty) {
        s"$nameResult <$emailResult>"
      } else if (nameResult.nonEmpty) {
        nameResult
      } else {
        ""
      }
    } catch {
      case _: Exception => ""
    }
  }

  private def generateTomlContent(config: KitTomlConfig): String = {
    // Manually format TOML to match expected layout
    val sb = new StringBuilder()

    sb.append("[package]\n")
    sb.append(s"""name = "${config.`package`.name}"""").append("\n")
    sb.append(s"""version = "${config.`package`.version}"""").append("\n")
    sb.append(s"""description = "${config.`package`.description}"""").append("\n")
    sb.append(s"""repository = "${config.`package`.repository}"""").append("\n")

    // Keywords array
    sb.append("keywords = [")
    if (config.`package`.keywords.nonEmpty) {
      sb.append(config.`package`.keywords.map(k => s""""$k"""").mkString(", "))
    }
    sb.append("]\n")

    // Authors array
    sb.append("authors = [")
    if (config.`package`.authors.nonEmpty) {
      sb.append(config.`package`.authors.map(a => s""""$a"""").mkString(", "))
    }
    sb.append("]\n")

    sb.append(s"""license = "${config.`package`.license}"""").append("\n")
    sb.append("\n")

    sb.append("[dependencies]\n")
    config.dependencies.foreach { case (name, version) =>
      sb.append(s"""$name = "$version"""").append("\n")
    }
    sb.append("\n")

    sb.append("[dev-dependencies]\n")
    config.`dev-dependencies`.foreach { case (name, version) =>
      sb.append(s"""$name = "$version"""").append("\n")
    }

    sb.toString
  }
}