import AssemblyKeys._ // put this at the top of the file

assemblySettings

name := "UDPChat"

version := "0.1"

scalaVersion := "2.10.1"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"   % "2.2-M3",
  "com.typesafe.akka" %% "akka-slf4j"   % "2.2-M3",
  "com.typesafe.akka" %% "akka-remote"  % "2.2-M3",
  "com.typesafe.akka" %% "akka-agent"   % "2.2-M3",
  "com.typesafe.akka" %% "akka-testkit" % "2.2-M3" % "test"
)

