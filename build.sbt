// SPDX-License-Identifier: Apache-2.0

enablePlugins(SiteScaladocPlugin)

val scala3Version = "3.4.1" // "3.3.3"

Compile / compile / logLevel := Level.Error

lazy val commonSettings = Seq(
  organization := "edu.berkeley.cs",
  scalaVersion := scala3Version,
  crossScalaVersions := Seq("2.13.10", "2.12.17", scala3Version)
)

lazy val isAtLeastScala213 = Def.setting {
  import Ordering.Implicits._
  CrossVersion.partialVersion(scalaVersion.value).exists(_ >= (2, 13))
}

lazy val firrtlSettings = Seq(
  name := "firrtl",
  version := "1.6-SNAPSHOT",
  // addCompilerPlugin(scalafixSemanticdb),
  scalacOptions := Seq(
    "-deprecation",
    "-unchecked",
    "-language:reflectiveCalls",
    "-language:existentials",
    "-language:implicitConversions",
    // "-rewrite", "-source:3.4-migration"
  ),
  // Always target Java8 for maximum compatibility
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  libraryDependencies ++= Seq(
    "org.scala-lang" %% "toolkit" % "0.1.7",
  //   "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  //   "org.scalatest" %% "scalatest" % "3.2.14" % "test",
  //   "org.scalatestplus" %% "scalacheck-1-15" % "3.2.11.0" % "test",
    "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
    "com.github.scopt" %% "scopt" % "4.1.0",
  //   "net.jcazevedo" %% "moultingyaml" % "0.4.2",
    "org.json4s" %% "json4s-native" % "4.1.0-M5",
    "org.apache.commons" % "commons-text" % "1.12.0",
  //   "io.github.alexarchambault" %% "data-class" % "0.2.5",
    "com.lihaoyi" %% "os-lib" % "0.9.1"
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    Resolver.sonatypeRepo("releases")
  )
)

lazy val mimaSettings = Seq(
  mimaPreviousArtifacts := Set("edu.berkeley.cs" %% "firrtl" % "1.6.0-RC2")
)

lazy val protobufSettings = Seq(
  // The parentheses around the version help avoid version ambiguity in release scripts
  ProtobufConfig / version := ("3.18.3"), // CVE-2021-22569
  ProtobufConfig / sourceDirectory := baseDirectory.value / "src" / "main" / "proto",
  ProtobufConfig / protobufRunProtoc := (args => com.github.os72.protocjar.Protoc.runProtoc("-v351" +: args.toArray))
)

lazy val assemblySettings = Seq(
  assembly / assemblyJarName := "firrtl.jar",
  assembly / test := {},
  assembly / assemblyOutputPath := file("./utils/bin/firrtl.jar")
)

lazy val testAssemblySettings = Seq(
  Test / assembly / test := {}, // Ditto above
  Test / assembly / assemblyMergeStrategy := {
    case PathList("firrtlTests", xs @ _*) => MergeStrategy.discard
    case x =>
      val oldStrategy = (Test / assembly / assemblyMergeStrategy).value
      oldStrategy(x)
  },
  Test / assembly / assemblyJarName := s"firrtl-test.jar",
  Test / assembly / assemblyOutputPath := file("./utils/bin/" + (Test / assembly / assemblyJarName).value)
)

lazy val antlrSettings = Seq(
  Antlr4 / antlr4GenVisitor := true,
  Antlr4 / antlr4GenListener := true,
  Antlr4 / antlr4PackageName := Option("firrtl.antlr"),
  Antlr4 / antlr4Version := "4.9.3",
  Antlr4 / javaSource := (Compile / sourceManaged).value
)

lazy val firrtl = (project in file("."))
  .enablePlugins(ProtobufPlugin)
  .enablePlugins(ScalaUnidocPlugin)
  .enablePlugins(Antlr4Plugin)
  .settings(
    fork := true,
    Test / testForkedParallel := true
  )
  .settings(commonSettings)
  .settings(firrtlSettings)
  .settings(protobufSettings)
  .settings(antlrSettings)
  .settings(assemblySettings)
  .settings(inConfig(Test)(baseAssemblySettings))
  .settings(testAssemblySettings)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoPackage := name.value,
    buildInfoUsePackageAsPath := true,
    buildInfoKeys := Seq[BuildInfoKey](buildInfoPackage, version, scalaVersion, sbtVersion)
  )
  .settings(mimaSettings)

lazy val benchmark = (project in file("benchmark"))
  .dependsOn(firrtl)
  .settings(commonSettings)
  .settings(
    assembly / assemblyJarName := "firrtl-benchmark.jar",
    assembly / test := {},
    assembly / assemblyOutputPath := file("./utils/bin/firrtl-benchmark.jar")
  )

val JQF_VERSION = "1.5"

lazy val jqf = (project in file("jqf"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "edu.berkeley.cs.jqf" % "jqf-fuzz" % JQF_VERSION,
      "edu.berkeley.cs.jqf" % "jqf-instrument" % JQF_VERSION,
      "com.github.scopt" %% "scopt" % "3.7.1"
    )
  )

lazy val jqfFuzz = sbt.inputKey[Unit]("input task that runs the firrtl.jqf.JQFFuzz main method")
lazy val jqfRepro = sbt.inputKey[Unit]("input task that runs the firrtl.jqf.JQFRepro main method")

lazy val testClassAndMethodParser = {
  import sbt.complete.DefaultParsers._
  val spaces = SpaceClass.+.string
  val testClassName =
    token(Space) ~> token(charClass(c => isScalaIDChar(c) || (c == '.')).+.string, "<test class name>")
  val testMethod = spaces ~> token(charClass(isScalaIDChar).+.string, "<test method name>")
  val rest = spaces.? ~> token(any.*.string, "<other args>")
  (testClassName ~ testMethod ~ rest).map {
    case ((a, b), c) => (a, b, c)
  }
}

lazy val fuzzer = (project in file("fuzzer"))
  .dependsOn(firrtl)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.pholser" % "junit-quickcheck-core" % "0.8",
      "com.pholser" % "junit-quickcheck-generators" % "0.8",
      "edu.berkeley.cs.jqf" % "jqf-fuzz" % JQF_VERSION,
      "org.scalacheck" %% "scalacheck" % "1.14.3" % Test
    ),
    jqfFuzz := (Def.inputTaskDyn {
      val (testClassName, testMethod, otherArgs) = testClassAndMethodParser.parsed
      val outputDir = (Compile / target).value / "JQF" / testClassName / testMethod
      val classpath = (Compile / fullClasspathAsJars).toTask.value.files.mkString(":")
      (Compile / (jqf / runMain)).toTask(
        s" firrtl.jqf.JQFFuzz " +
          s"--testClassName $testClassName " +
          s"--testMethod $testMethod " +
          s"--classpath $classpath " +
          s"--outputDirectory $outputDir " +
          otherArgs
      )
    }).evaluated,
    jqfRepro := (Def.inputTaskDyn {
      val (testClassName, testMethod, otherArgs) = testClassAndMethodParser.parsed
      val classpath = (Compile / fullClasspathAsJars).toTask.value.files.mkString(":")
      (Compile / (jqf / runMain)).toTask(
        s" firrtl.jqf.JQFRepro " +
          s"--testClassName $testClassName " +
          s"--testMethod $testMethod " +
          s"--classpath $classpath " +
          otherArgs
      )
    }).evaluated
  )
