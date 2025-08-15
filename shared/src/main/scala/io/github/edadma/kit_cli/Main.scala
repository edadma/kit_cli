package io.github.edadma.kit_cli

import io.github.edadma.cross_platform.processArgs

// Main entry point - delegates to CLI parser
@main def run(args: String*): Unit = {
  CliParser.parse(processArgs(args).toArray) match {
    case Some(config) =>
      CommandExecutor.execute(config)
    case None =>
    // scopt already printed error message and help
//      sys.exit(1)
  }
}
