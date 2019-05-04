import ReleaseTransformations._
import sbtcrossproject.{crossProject, CrossType}

enablePlugins(ScalaJSPlugin)

val defaultSettings = Seq(
  scalaVersion := "2.11.12",
  crossScalaVersions := List("2.11.12", "2.12.8", "2.13.0-RC1"),
  resolvers += Resolver.sonatypeRepo("releases"),
  homepage := Some(url("http://monovore.com/decline")),
  organization := "com.monovore",
  scalacOptions ++= Seq("-Xfatal-warnings", "-deprecation", "-feature", "-language:higherKinds"),
  licenses += ("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/bkirwi/decline"),
      "git@github.com:bkirwi/decline.git"
    )
  ),
  pomExtra := (
    <developers>
      <developer>
        <id>bkirwi</id>
        <name>Ben Kirwin</name>
        <url>http://ben.kirw.in/</url>
      </developer>
    </developers>
  ),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := Some(
    if (isSnapshot.value) Opts.resolver.sonatypeSnapshots
    else Opts.resolver.sonatypeStaging
  ),
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    releaseStepCommand("sonatypeReleaseAll"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val root =
  project.in(file("."))
    .aggregate(declineJS, declineJVM, refinedJS, refinedJVM, doc)
    .settings(defaultSettings)
    .settings(noPublishSettings)

lazy val decline =
  crossProject(JSPlatform, JVMPlatform).in(file("core"))
    .settings(defaultSettings)
    .settings(addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.0" cross CrossVersion.binary))
    .settings(
      name := "decline",
      description := "Composable command-line parsing for Scala",
      libraryDependencies ++= {
        val catsVersion = "2.0.0-M1"

        Seq(
          "org.typelevel"  %%% "cats-core"    % catsVersion,
          "org.typelevel"  %%% "cats-laws"    % catsVersion % "test",
          "org.typelevel"  %%% "cats-testkit" % catsVersion % "test",
          "org.scalatest"  %%% "scalatest"    % "3.0.8-RC2" % "test",
          "org.scalacheck" %%% "scalacheck"   % "1.14.0" % "test"
        )
      }
    )
    .jsSettings(
      libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.0.0-RC1"
    )

lazy val declineJVM = decline.jvm
lazy val declineJS = decline.js

lazy val bench =
  project.in(file("bench"))
    .enablePlugins(JmhPlugin)
    .dependsOn(declineJVM, refinedJVM)
    .settings(defaultSettings)
    .settings(noPublishSettings)
    .settings(fork := true)

lazy val refined =
  crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure).in(file("refined"))
    .settings(defaultSettings)
    .settings(
      name := "refined",
      moduleName := "decline-refined",
      libraryDependencies ++= {
        val refinedVersion = "0.9.5"

        Seq(
          "eu.timepit" %%% "refined"            % refinedVersion,
          "eu.timepit" %%% "refined-scalacheck" % refinedVersion % "test"
        )
      }
    )
    .dependsOn(decline % "compile->compile;test->test")

lazy val refinedJVM = refined.jvm
lazy val refinedJS = refined.js

lazy val doc =
  project.in(file("doc"))
    .dependsOn(declineJVM, refinedJVM)
    .enablePlugins(MicrositesPlugin)
    .settings(defaultSettings)
    .settings(noPublishSettings)
    .settings(
      micrositeName := "decline",
      micrositeDescription := "Composable command-line parsing for Scala",
      micrositeConfigYaml := microsites.ConfigYml(
        yamlInline = """kramdown: { input: GFM, hard_wrap: false }"""
      ),
      micrositeBaseUrl := "/decline",
      micrositeGithubOwner := "bkirwi",
      micrositeGithubRepo := "decline",
      micrositeHighlightTheme := "solarized-light",
      micrositeDocumentationUrl := "usage.html"
    )
