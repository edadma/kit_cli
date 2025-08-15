package io.github.edadma.kit_cli

import scopt.OParser
import io.github.edadma.path.Path
import scala.io.StdIn
import scala.util.{Try, Success, Failure}
import toml.derivation.auto._
import toml.Toml

// Global options available to all commands
case class GlobalOptions(
    verbose: Boolean = false,
    configPath: Option[String] = None,
    registry: Option[String] = None,
)

// Command definitions
sealed trait KitCommand

// Core Package Management
case class NewCommand(projectName: String)                    extends KitCommand
case class InitCommand()                                      extends KitCommand
case class InstallCommand(packageSpec: Option[String] = None) extends KitCommand // None = install all from kit.toml
case class UninstallCommand(packageName: String)              extends KitCommand
case class UpdateCommand()                                    extends KitCommand

// Information
case class ListCommand()                    extends KitCommand
case class ShowCommand(packageName: String) extends KitCommand
case class SearchCommand(query: String)     extends KitCommand

// Publishing
case class LoginCommand()                                                    extends KitCommand
case class LogoutCommand()                                                   extends KitCommand
case class PublishCommand()                                                  extends KitCommand
case class UnpublishCommand(version: String)                                 extends KitCommand
case class DeprecateCommand(version: String, message: Option[String] = None) extends KitCommand

// User Management
case class WhoamiCommand()   extends KitCommand
case class ProfileCommand()  extends KitCommand
case class PackagesCommand() extends KitCommand

// Validation
case class CheckCommand() extends KitCommand

// Main config combining global options and command
case class KitConfig(
    global: GlobalOptions = GlobalOptions(),
    command: Option[KitCommand] = None,
)

object CliParser {

  val builder = OParser.builder[KitConfig]

  def parser: OParser[Unit, KitConfig] = {
    import builder._

    OParser.sequence(
      programName("kit"),
      head("kit", "0.0.1"),
      help("help").text("Show this help message"),
      version("version").text("Show version information"),

      // Global options
      opt[Unit]('v', "verbose")
        .action((_, c) => c.copy(global = c.global.copy(verbose = true)))
        .text("Enable verbose output"),
      opt[String]("config")
        .action((x, c) => c.copy(global = c.global.copy(configPath = Some(x))))
        .text("Path to config file"),
      opt[String]("registry")
        .action((x, c) => c.copy(global = c.global.copy(registry = Some(x))))
        .text("Registry URL to use"),
      note(""),
      note("Commands:"),
      note(""),

      // Core Package Management Commands
      cmd("new")
        .action((_, c) => c.copy(command = Some(NewCommand(""))))
        .text("Create a new project")
        .children(
          arg[String]("<project-name>")
            .action((x, c) => c.copy(command = Some(NewCommand(x))))
            .text("Name of the new project"),
        ),
      cmd("init")
        .action((_, c) => c.copy(command = Some(InitCommand())))
        .text("Initialize kit.toml in current directory"),
      cmd("install")
        .action((_, c) => c.copy(command = Some(InstallCommand())))
        .text("Install package or all dependencies")
        .children(
          arg[String]("<package>")
            .optional()
            .action((x, c) => c.copy(command = Some(InstallCommand(Some(x)))))
            .text("Package to install (optional)"),
        ),

      // Aliases for install
      cmd("add")
        .action((_, c) => c.copy(command = Some(InstallCommand())))
        .text("Alias for install")
        .children(
          arg[String]("<package>")
            .optional()
            .action((x, c) => c.copy(command = Some(InstallCommand(Some(x)))))
            .text("Package to install"),
        ),
      cmd("i")
        .action((_, c) => c.copy(command = Some(InstallCommand())))
        .text("Short alias for install")
        .children(
          arg[String]("<package>")
            .optional()
            .action((x, c) => c.copy(command = Some(InstallCommand(Some(x)))))
            .text("Package to install"),
        ),
      cmd("uninstall")
        .action((_, c) => c.copy(command = Some(UninstallCommand(""))))
        .text("Uninstall a package")
        .children(
          arg[String]("<package>")
            .action((x, c) => c.copy(command = Some(UninstallCommand(x))))
            .text("Package to uninstall"),
        ),

      // Aliases for uninstall
      cmd("remove")
        .action((_, c) => c.copy(command = Some(UninstallCommand(""))))
        .text("Alias for uninstall")
        .children(
          arg[String]("<package>")
            .action((x, c) => c.copy(command = Some(UninstallCommand(x))))
            .text("Package to uninstall"),
        ),
      cmd("rm")
        .action((_, c) => c.copy(command = Some(UninstallCommand(""))))
        .text("Short alias for uninstall")
        .children(
          arg[String]("<package>")
            .action((x, c) => c.copy(command = Some(UninstallCommand(x))))
            .text("Package to uninstall"),
        ),
      cmd("r")
        .action((_, c) => c.copy(command = Some(UninstallCommand(""))))
        .text("Shortest alias for uninstall")
        .children(
          arg[String]("<package>")
            .action((x, c) => c.copy(command = Some(UninstallCommand(x))))
            .text("Package to uninstall"),
        ),
      cmd("update")
        .action((_, c) => c.copy(command = Some(UpdateCommand())))
        .text("Update all dependencies"),

      // Information Commands
      cmd("list")
        .action((_, c) => c.copy(command = Some(ListCommand())))
        .text("List installed packages"),
      cmd("show")
        .action((_, c) => c.copy(command = Some(ShowCommand(""))))
        .text("Show package information")
        .children(
          arg[String]("<package>")
            .action((x, c) => c.copy(command = Some(ShowCommand(x))))
            .text("Package to show"),
        ),
      cmd("search")
        .action((_, c) => c.copy(command = Some(SearchCommand(""))))
        .text("Search for packages")
        .children(
          arg[String]("<query>")
            .action((x, c) => c.copy(command = Some(SearchCommand(x))))
            .text("Search query"),
        ),

      // Publishing Commands
      cmd("login")
        .action((_, c) => c.copy(command = Some(LoginCommand())))
        .text("Login to registry"),
      cmd("logout")
        .action((_, c) => c.copy(command = Some(LogoutCommand())))
        .text("Logout from registry"),
      cmd("publish")
        .action((_, c) => c.copy(command = Some(PublishCommand())))
        .text("Publish current package"),
      cmd("unpublish")
        .action((_, c) => c.copy(command = Some(UnpublishCommand(""))))
        .text("Unpublish a version")
        .children(
          arg[String]("<version>")
            .action((x, c) => c.copy(command = Some(UnpublishCommand(x))))
            .text("Version to unpublish"),
        ),
      cmd("deprecate")
        .action((_, c) => c.copy(command = Some(DeprecateCommand("", None))))
        .text("Deprecate a version")
        .children(
          arg[String]("<version>")
            .action((x, c) => c.copy(command = Some(DeprecateCommand(x, None))))
            .text("Version to deprecate"),
          opt[String]('m', "message")
            .action((x, c) =>
              c.command match {
                case Some(DeprecateCommand(v, _)) => c.copy(command = Some(DeprecateCommand(v, Some(x))))
                case _                            => c
              },
            )
            .text("Deprecation message"),
        ),

      // User Management Commands
      cmd("whoami")
        .action((_, c) => c.copy(command = Some(WhoamiCommand())))
        .text("Show current user"),
      cmd("profile")
        .action((_, c) => c.copy(command = Some(ProfileCommand())))
        .text("Show user profile"),
      cmd("packages")
        .action((_, c) => c.copy(command = Some(PackagesCommand())))
        .text("List your published packages"),

      // Validation Commands
      cmd("check")
        .action((_, c) => c.copy(command = Some(CheckCommand())))
        .text("Validate kit.toml and dependencies"),
      checkConfig(c =>
        if (c.command.isEmpty) failure("No command specified")
        else success,
      ),
    )
  }

  def parse(args: Array[String]): Option[KitConfig] = {
    OParser.parse(parser, args, KitConfig())
  }
}

// TOML structure for kit.toml
case class PackageConfig(
    name: String,
    version: String,
    description: String,
    repository: String,
    keywords: List[String],
    authors: List[String],
    license: String,
)

case class KitTomlConfig(
    `package`: PackageConfig,
    dependencies: Map[String, String] = Map.empty,
    `dev-dependencies`: Map[String, String] = Map.empty,
)

object InitCommandImpl {

  def execute(globalOptions: GlobalOptions): Unit = {
    val currentDir  = Path(".")
    val kitTomlPath = currentDir / "kit.toml"

    // Check if kit.toml already exists
    if (kitTomlPath.exists) {
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
      val kitConfig     = KitTomlConfig(packageConfig)

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
    val defaultName   = inferProjectName(currentDir)
    val defaultAuthor = inferAuthor()

    val name          = promptWithDefault("package name", defaultName)
    val version       = promptWithDefault("version", "0.1.0")
    val description   = promptWithDefault("description", "")
    val repository    = promptWithDefault("git repository", "")
    val keywordsInput = promptWithDefault("keywords", "")
    val keywords      = if (keywordsInput.trim.isEmpty) List.empty
    else keywordsInput.split(',').map(_.trim).toList
    val author  = promptWithDefault("author", defaultAuthor)
    val authors = if (author.trim.isEmpty) List.empty else List(author)
    val license = promptWithDefault("license", "MIT")

    PackageConfig(
      name = name,
      version = version,
      description = description,
      repository = repository,
      keywords = keywords,
      authors = authors,
      license = license,
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
    val dirName = currentDir.toAbsolutePath.filename.toString
    // Convert to lowercase and replace invalid characters
    dirName.toLowerCase.replaceAll("[^a-z0-9-_]", "-")
  }

  private def inferAuthor(): String = {
    // Only try git detection on JVM and Native platforms
    if (platform == "js") {
      ""
    } else {
      try {
        import scala.sys.process._
        val nameResult  = Process("git config user.name").!!.trim
        val emailResult = Process("git config user.email").!!.trim

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

// Test runner with print statements
object CommandExecutor {

  def execute(config: KitConfig): Unit = {
    // Print global options
    if (config.global.verbose) {
      println("=== VERBOSE MODE ENABLED ===")
      println(s"Global options: ${config.global}")
    }

    config.global.configPath.foreach(path =>
      println(s"Using config file: $path"),
    )
    config.global.registry.foreach(url =>
      println(s"Using registry: $url"),
    )

    // Execute command
    config.command match {
      case None =>
        println("No command specified")

      case Some(command) => command match {
          // Core Package Management
          case NewCommand(projectName) =>
            println(s"📦 Creating new project: $projectName")

          case InitCommand() =>
            InitCommandImpl.execute(config.global)

          case InstallCommand(None) =>
            println("⬇️  Installing all dependencies from kit.toml")

          case InstallCommand(Some(packageSpec)) =>
            println(s"⬇️  Installing package: $packageSpec")

          case UninstallCommand(packageName) =>
            println(s"🗑️  Uninstalling package: $packageName")

          case UpdateCommand() =>
            println("🔄 Updating all dependencies")

          // Information
          case ListCommand() =>
            println("📋 Listing installed packages")

          case ShowCommand(packageName) =>
            println(s"🔍 Showing package info: $packageName")

          case SearchCommand(query) =>
            println(s"🔎 Searching packages: $query")

          // Publishing
          case LoginCommand() =>
            println("🔐 Logging into registry")

          case LogoutCommand() =>
            println("👋 Logging out of registry")

          case PublishCommand() =>
            println("📤 Publishing current package")

          case UnpublishCommand(version) =>
            println(s"🚫 Unpublishing version: $version")

          case DeprecateCommand(version, message) =>
            println(s"⚠️  Deprecating version: $version")
            message.foreach(msg => println(s"   Message: $msg"))

          // User Management
          case WhoamiCommand() =>
            println("👤 Current user info")

          case ProfileCommand() =>
            println("📋 User profile")

          case PackagesCommand() =>
            println("📦 Your published packages")

          // Validation
          case CheckCommand() =>
            println("✅ Checking kit.toml and dependencies")
        }
    }
  }
}
