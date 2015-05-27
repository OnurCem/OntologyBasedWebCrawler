name := """OntMngApp"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

resolvers ++= Seq(
  "anormcypher" at "http://repo.anormcypher.org/",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "org.anormcypher" %% "anormcypher" % "0.6.0",
  "com.github.ansell.pellet" % "pellet-owlapiv3" % "2.3.6-ansell",
  "net.sourceforge.owlapi" % "owlapi-distribution" % "3.4.9",
  "com.sksamuel.elastic4s" % "elastic4s_2.11" % "1.5.7",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.4.2",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.2",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.4.2"
)
