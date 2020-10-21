name := "bitfinex-bot"

version := "1.0"

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-ahc-ws-standalone" % "2.1.2",
  "com.typesafe.play" %% "play-ws-standalone-json" % "2.1.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "commons-codec" % "commons-codec" % "1.14",
  "org.scalatest" %% "scalatest" % "3.2.1" % Test
)

test in assembly := {}

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs@_*) => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".html" => MergeStrategy.first
  case "application.conf" => MergeStrategy.concat
  case "unwanted.txt" => MergeStrategy.discard
  case "module-info.class" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

fork := true