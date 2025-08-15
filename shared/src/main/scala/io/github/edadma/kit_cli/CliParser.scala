package io.github.edadma.kit_cli

import scopt.OParser

// Global options available to all commands
case class GlobalOptions(
    verbose: Boolean = false,
    configPath: Option[String] = None,
    registry: Option[String] = None,
)

// Command definitions
sealed trait KitCommand

// Core Package Management
case class New(projectName: String)                    extends KitCommand
case class Init()                                      extends KitCommand
case class Install(packageSpec: Option[String] = None) extends KitCommand // None = install all from kit.toml
case class Uninstall(packageName: String)              extends KitCommand
case class Update()                                    extends KitCommand

// Information
case class List()                    extends KitCommand
case class Show(packageName: String) extends KitCommand
case class Search(query: String)     extends KitCommand

// Publishing
case class Login()                                                    extends KitCommand
case class Logout()                                                   extends KitCommand
case class Publish()                                                  extends KitCommand
case class Unpublish(version: String)                                 extends KitCommand
case class Deprecate(version: String, message: Option[String] = None) extends KitCommand

// User Management
case class Whoami()   extends KitCommand
case class Profile()  extends KitCommand
case class Packages() extends KitCommand

// Validation
case class Check() extends KitCommand

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
        .action((_, c) => c.copy(command = Some(New(""))))
        .text("Create a new project")
        .children(
          arg[String]("<project-name>")
            .action((x, c) => c.copy(command = Some(New(x))))
            .text("Name of the new project"),
        ),
      cmd("init")
        .action((_, c) => c.copy(command = Some(Init())))
        .text("Initialize kit.toml in current directory"),
      cmd("install")
        .action((_, c) => c.copy(command = Some(Install())))
        .text("Install package or all dependencies")
        .children(
          arg[String]("<package>")
            .optional()
            .action((x, c) => c.copy(command = Some(Install(Some(x)))))
            .text("Package to install (optional)"),
        ),

      // Aliases for install
      cmd("add")
        .action((_, c) => c.copy(command = Some(Install())))
        .text("Alias for install")
        .children(
          arg[String]("<package>")
            .optional()
            .action((x, c) => c.copy(command = Some(Install(Some(x)))))
            .text("Package to install"),
        ),
      cmd("i")
        .action((_, c) => c.copy(command = Some(Install())))
        .text("Short alias for install")
        .children(
          arg[String]("<package>")
            .optional()
            .action((x, c) => c.copy(command = Some(Install(Some(x)))))
            .text("Package to install"),
        ),
      cmd("uninstall")
        .action((_, c) => c.copy(command = Some(Uninstall(""))))
        .text("Uninstall a package")
        .children(
          arg[String]("<package>")
            .action((x, c) => c.copy(command = Some(Uninstall(x))))
            .text("Package to uninstall"),
        ),

      // Aliases for uninstall
      cmd("remove")
        .action((_, c) => c.copy(command = Some(Uninstall(""))))
        .text("Alias for uninstall")
        .children(
          arg[String]("<package>")
            .action((x, c) => c.copy(command = Some(Uninstall(x))))
            .text("Package to uninstall"),
        ),
      cmd("rm")
        .action((_, c) => c.copy(command = Some(Uninstall(""))))
        .text("Short alias for uninstall")
        .children(
          arg[String]("<package>")
            .action((x, c) => c.copy(command = Some(Uninstall(x))))
            .text("Package to uninstall"),
        ),
      cmd("r")
        .action((_, c) => c.copy(command = Some(Uninstall(""))))
        .text("Shortest alias for uninstall")
        .children(
          arg[String]("<package>")
            .action((x, c) => c.copy(command = Some(Uninstall(x))))
            .text("Package to uninstall"),
        ),
      cmd("update")
        .action((_, c) => c.copy(command = Some(Update())))
        .text("Update all dependencies"),

      // Information Commands
      cmd("list")
        .action((_, c) => c.copy(command = Some(List())))
        .text("List installed packages"),
      cmd("show")
        .action((_, c) => c.copy(command = Some(Show(""))))
        .text("Show package information")
        .children(
          arg[String]("<package>")
            .action((x, c) => c.copy(command = Some(Show(x))))
            .text("Package to show"),
        ),
      cmd("search")
        .action((_, c) => c.copy(command = Some(Search(""))))
        .text("Search for packages")
        .children(
          arg[String]("<query>")
            .action((x, c) => c.copy(command = Some(Search(x))))
            .text("Search query"),
        ),

      // Publishing Commands
      cmd("login")
        .action((_, c) => c.copy(command = Some(Login())))
        .text("Login to registry"),
      cmd("logout")
        .action((_, c) => c.copy(command = Some(Logout())))
        .text("Logout from registry"),
      cmd("publish")
        .action((_, c) => c.copy(command = Some(Publish())))
        .text("Publish current package"),
      cmd("unpublish")
        .action((_, c) => c.copy(command = Some(Unpublish(""))))
        .text("Unpublish a version")
        .children(
          arg[String]("<version>")
            .action((x, c) => c.copy(command = Some(Unpublish(x))))
            .text("Version to unpublish"),
        ),
      cmd("deprecate")
        .action((_, c) => c.copy(command = Some(Deprecate("", None))))
        .text("Deprecate a version")
        .children(
          arg[String]("<version>")
            .action((x, c) => c.copy(command = Some(Deprecate(x, None))))
            .text("Version to deprecate"),
          opt[String]('m', "message")
            .action((x, c) =>
              c.command match {
                case Some(Deprecate(v, _)) => c.copy(command = Some(Deprecate(v, Some(x))))
                case _                     => c
              },
            )
            .text("Deprecation message"),
        ),

      // User Management Commands
      cmd("whoami")
        .action((_, c) => c.copy(command = Some(Whoami())))
        .text("Show current user"),
      cmd("profile")
        .action((_, c) => c.copy(command = Some(Profile())))
        .text("Show user profile"),
      cmd("packages")
        .action((_, c) => c.copy(command = Some(Packages())))
        .text("List your published packages"),

      // Validation Commands
      cmd("check")
        .action((_, c) => c.copy(command = Some(Check())))
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
          case New(projectName) =>
            println(s"📦 Creating new project: $projectName")

          case Init() =>
            println("📝 Initializing kit.toml in current directory")

          case Install(None) =>
            println("⬇️  Installing all dependencies from kit.toml")

          case Install(Some(packageSpec)) =>
            println(s"⬇️  Installing package: $packageSpec")

          case Uninstall(packageName) =>
            println(s"🗑️  Uninstalling package: $packageName")

          case Update() =>
            println("🔄 Updating all dependencies")

          // Information
          case List() =>
            println("📋 Listing installed packages")

          case Show(packageName) =>
            println(s"🔍 Showing package info: $packageName")

          case Search(query) =>
            println(s"🔎 Searching packages: $query")

          // Publishing
          case Login() =>
            println("🔐 Logging into registry")

          case Logout() =>
            println("👋 Logging out of registry")

          case Publish() =>
            println("📤 Publishing current package")

          case Unpublish(version) =>
            println(s"🚫 Unpublishing version: $version")

          case Deprecate(version, message) =>
            println(s"⚠️  Deprecating version: $version")
            message.foreach(msg => println(s"   Message: $msg"))

          // User Management
          case Whoami() =>
            println("👤 Current user info")

          case Profile() =>
            println("📋 User profile")

          case Packages() =>
            println("📦 Your published packages")

          // Validation
          case Check() =>
            println("✅ Checking kit.toml and dependencies")
        }
    }
  }
}
