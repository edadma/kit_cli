package io.github.edadma.kit_cli

import io.github.edadma.path.Path
import toml.derivation.auto._
import toml.Toml
import scala.util.{Try, Success, Failure}
import java.util.regex.Pattern

object CheckCommandImpl {

  def execute(globalOptions: GlobalOptions): Unit = {
    val currentDir  = Path(".")
    val kitTomlPath = currentDir / "kit.toml"

    if (!kitTomlPath.exists) {
      println("❌ No kit.toml found in current directory")
      println("Run 'kit init' to create one.")
      return
    }

    if (globalOptions.verbose) {
      println(s"📋 Checking ${kitTomlPath.toAbsolutePath}")
    }

    try {
      val tomlContent = kitTomlPath.readText()
      validateKitToml(tomlContent, globalOptions.verbose)
    } catch {
      case e: Exception =>
        println(s"❌ Error reading kit.toml: ${e.getMessage}")
    }
  }

  private def validateKitToml(tomlContent: String, verbose: Boolean): Unit = {
    // Parse TOML
    val parseResult = Toml.parseAs[KitTomlConfig](tomlContent)

    parseResult match {
      case Left(error) =>
        println("❌ TOML parsing failed:")
        println(s"   ${error}")
        return

      case Right(config) =>
        if (verbose) {
          println("✅ TOML parsing successful")
        }

        // Validate structure and content
        val issues = validateConfig(config)

        if (issues.isEmpty) {
          println("✅ kit.toml is valid")
          if (verbose) {
            printConfigSummary(config)
          }
        } else {
          println("❌ Validation issues found:")
          issues.foreach(issue => println(s"   • $issue"))
        }
    }
  }

  private def validateConfig(config: KitTomlConfig): List[String] = {
    val issues = scala.collection.mutable.ListBuffer[String]()

    // Validate package section
    validatePackage(config.`package`, issues)

    // Validate dependencies
    validateDependencies("dependencies", config.dependencies, issues)
    validateDependencies("dev-dependencies", config.`dev-dependencies`, issues)

    issues.toList
  }

  private def validatePackage(pkg: PackageConfig, issues: scala.collection.mutable.ListBuffer[String]): Unit = {
    // Required fields
    if (pkg.name.trim.isEmpty) {
      issues += "Package name is required"
    } else if (!isValidPackageName(pkg.name)) {
      issues += s"Invalid package name '${pkg.name}' (must be lowercase alphanumeric with hyphens/underscores)"
    }

    if (pkg.version.trim.isEmpty) {
      issues += "Package version is required"
    } else if (!isValidSemanticVersion(pkg.version)) {
      issues += s"Invalid semantic version '${pkg.version}' (expected format: X.Y.Z)"
    }

    if (pkg.description.trim.isEmpty) {
      issues += "Package description is required"
    }

    if (pkg.repository.trim.isEmpty) {
      issues += "Repository URL is required"
    } else if (!isValidGitUrl(pkg.repository)) {
      issues += s"Invalid repository URL '${pkg.repository}'"
    }

    // Validate license
    if (pkg.license.trim.isEmpty) {
      issues += "License is required"
    }

    // Validate authors
    if (pkg.authors.isEmpty) {
      issues += "At least one author is required"
    } else {
      pkg.authors.zipWithIndex.foreach { case (author, idx) =>
        if (author.trim.isEmpty) {
          issues += s"Author ${idx + 1} cannot be empty"
        }
      }
    }

    // Validate keywords
    if (pkg.keywords.length > 10) {
      issues += s"Too many keywords (${pkg.keywords.length}), maximum is 10"
    }

    pkg.keywords.zipWithIndex.foreach { case (keyword, idx) =>
      if (keyword.trim.isEmpty) {
        issues += s"Keyword ${idx + 1} cannot be empty"
      } else if (keyword.length > 50) {
        issues += s"Keyword '${keyword}' is too long (maximum 50 characters)"
      }
    }
  }

  private def validateDependencies(
      sectionName: String,
      deps: Map[String, String],
      issues: scala.collection.mutable.ListBuffer[String],
  ): Unit = {
    deps.foreach { case (name, version) =>
      if (!isValidPackageName(name)) {
        issues += s"Invalid dependency name '${name}' in $sectionName"
      }

      if (!isValidVersionConstraint(version)) {
        issues += s"Invalid version constraint '${version}' for '$name' in $sectionName"
      }
    }
  }

  private def isValidPackageName(name: String): Boolean = {
    // Package names: lowercase alphanumeric, hyphens, underscores, 2-50 chars
    val pattern = Pattern.compile("^[a-z0-9][a-z0-9_-]{1,49}$")
    pattern.matcher(name).matches()
  }

  private def isValidSemanticVersion(version: String): Boolean = {
    // Basic semantic versioning: X.Y.Z with optional pre-release/build metadata
    val pattern = Pattern.compile(
      "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$",
    )
    pattern.matcher(version).matches()
  }

  private def isValidVersionConstraint(constraint: String): Boolean = {
    // Support common version constraint formats:
    // "1.2.3", "^1.2.3", "~1.2.3", ">=1.2.3", "1.2.*", "1.*"
    if (constraint.trim.isEmpty) return false

    val patterns = List(
      "^\\^[0-9]+\\.[0-9]+\\.[0-9]+$", // ^1.2.3
      "^~[0-9]+\\.[0-9]+\\.[0-9]+$",   // ~1.2.3
      "^>=[0-9]+\\.[0-9]+\\.[0-9]+$",  // >=1.2.3
      "^[0-9]+\\.[0-9]+\\.[0-9]+$",    // 1.2.3
      "^[0-9]+\\.[0-9]+\\.\\*$",       // 1.2.*
      "^[0-9]+\\.\\*$",                // 1.*
    )

    patterns.exists(pattern => Pattern.compile(pattern).matcher(constraint).matches())
  }

  private def isValidGitUrl(url: String): Boolean = {
    // Basic URL validation - accepts HTTP(S) and SSH Git URLs
    val patterns = List(
      "^https://github\\.com/[^/]+/[^/]+(?:\\.git)?/?$",
      "^https://gitlab\\.com/[^/]+/[^/]+(?:\\.git)?/?$",
      "^git@github\\.com:[^/]+/[^/]+\\.git$",
      "^git@gitlab\\.com:[^/]+/[^/]+\\.git$",
    )

    patterns.exists(pattern => Pattern.compile(pattern).matcher(url).matches()) ||
    url.startsWith("https://") || url.startsWith("git@")
  }

  private def printConfigSummary(config: KitTomlConfig): Unit = {
    val pkg = config.`package`
    println()
    println("📦 Package Information:")
    println(s"   Name: ${pkg.name}")
    println(s"   Version: ${pkg.version}")
    println(s"   Description: ${pkg.description}")
    println(s"   Repository: ${pkg.repository}")
    println(s"   License: ${pkg.license}")
    println(s"   Authors: ${pkg.authors.mkString(", ")}")

    if (pkg.keywords.nonEmpty) {
      println(s"   Keywords: ${pkg.keywords.mkString(", ")}")
    }

    if (config.dependencies.nonEmpty) {
      println()
      println("📋 Dependencies:")
      config.dependencies.foreach { case (name, version) =>
        println(s"   $name: $version")
      }
    }

    if (config.`dev-dependencies`.nonEmpty) {
      println()
      println("🔧 Dev Dependencies:")
      config.`dev-dependencies`.foreach { case (name, version) =>
        println(s"   $name: $version")
      }
    }
  }
}
