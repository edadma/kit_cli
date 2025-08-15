package io.github.edadma.kit_cli

// Main entry point - delegates to CLI parser
@main def run(args: String*): Unit = {
  CliParser.parse(args.toArray) match {
    case Some(config) =>
      CommandExecutor.execute(config)
    case None =>
      // scopt already printed error message and help
      System.exit(1)
  }
}
