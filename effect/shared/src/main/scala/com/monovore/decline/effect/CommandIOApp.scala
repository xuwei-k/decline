package com.monovore.decline.effect

import cats.effect.{IO, IOApp, ExitCode}
import cats.implicits._

import com.monovore.decline._

abstract class CommandIOApp(name: String, header: String, helpFlag: Boolean = true, version: String = "") extends IOApp {

  def main: Opts[IO[ExitCode]]

  private[this] def command: Command[IO[ExitCode]] = {
    val showVersion = {
      if (version.isEmpty) Opts.never
      else {
        val flag = Opts.flag(
          long = "version",
          short = "v",
          help = "Print the version number and exit.",
          visibility = Visibility.Partial
        )
        flag.as(IO(System.out.println(version)).as(ExitCode.Success))
      }
    }

    Command(name, header, helpFlag)(showVersion orElse main)
  }

  override final def run(args: List[String]): IO[ExitCode] = {
    def printHelp(help: Help): IO[ExitCode] =
      IO(System.err.println(help)).as {
        if (help.errors.size > 0) ExitCode.Error
        else ExitCode.Success
      }

    for {
      parseResult <- IO(command.parse(PlatformApp.ambientArgs getOrElse args, sys.env))
      exitCode    <- parseResult.fold(printHelp, identity)
    } yield exitCode
  }

}
