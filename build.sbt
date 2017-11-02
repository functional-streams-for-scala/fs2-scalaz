lazy val contributors = Seq(
  "pchiusano" -> "Paul Chiusano",
  "pchlupacek" -> "Pavel Chlupáček",
  "alissapajer" -> "Alissa Pajer",
  "djspiewak" -> "Daniel Spiewak",
  "fthomas" -> "Frank Thomas",
  "runarorama" -> "Rúnar Ó. Bjarnason",
  "jedws" -> "Jed Wesley-Smith",
  "wookietreiber" -> "Christian Krause",
  "mpilquist" -> "Michael Pilquist"
)

organization := "co.fs2"
name := "fs2-scalaz"

scalaVersion := "2.11.11"

crossScalaVersions := Seq("2.11.11", "2.12.4")

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps",
  "-Xfatal-warnings",
  "-Yno-adapted-args",
  "-Ywarn-value-discard",
  "-Ywarn-unused-import"
)
scalacOptions in (Compile, console) ~= {_.filterNot("-Ywarn-unused-import" == _)}
scalacOptions in (Test, console) <<= (scalacOptions in (Compile, console))

resolvers += "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/"
val scalazVersion = "7.2.16"
libraryDependencies ++= Seq(
  "co.fs2"     %% "fs2-core"          % "0.9.7",
  "org.scalaz" %% "scalaz-core"       % scalazVersion,
  "org.scalaz" %% "scalaz-concurrent" % scalazVersion
)

scmInfo := Some(ScmInfo(url("https://github.com/functional-streams-for-scala/fs2-scalaz"), "git@github.com:functional-streams-for-scala/fs2-scalaz.git"))
homepage := Some(url("https://github.com/functional-streams-for-scala/fs2"))
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

initialCommands := s"""
  import fs2._
  import fs2.interop.scalaz._
  import scalaz._
  import Scalaz._
"""

parallelExecution in Test := false
logBuffered in Test := false
testOptions in Test += Tests.Argument("-verbosity", "2")
testOptions in Test += Tests.Argument("-minSuccessfulTests", "500")
publishArtifact in Test := true

scalacOptions in (Compile, doc) ++= Seq(
  "-doc-source-url", scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
  "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath,
  "-implicits",
  "-implicits-show-all"
)
scalacOptions in (Compile, doc) ~= (_.filterNot(_ == "-Xfatal-warnings"))
autoAPIMappings := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
credentials ++= (for {
  username <- Option(System.getenv().get("SONATYPE_USERNAME"))
  password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
} yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq
publishMavenStyle := true
pomIncludeRepository := { _ => false }
pomExtra := {
  <developers>
    {for ((username, name) <- contributors) yield
    <developer>
      <id>{username}</id>
      <name>{name}</name>
      <url>http://github.com/{username}</url>
    </developer>
    }
  </developers>
}
pomPostProcess := { node =>
  import scala.xml._
  import scala.xml.transform._
  def stripIf(f: Node => Boolean) = new RewriteRule {
    override def transform(n: Node) =
      if (f(n)) NodeSeq.Empty else n
  }
  val stripTestScope = stripIf { n => n.label == "dependency" && (n \ "scope").text == "test" }
  new RuleTransformer(stripTestScope).transform(node)(0)
}

releaseCrossBuild := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value

