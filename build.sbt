name := "colorectal_cancer"
 
version := "1.0" 
      
lazy val `colorectal_cancer` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"
      
scalaVersion := "2.13.2"

libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice )

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

libraryDependencies += "commons-io" % "commons-io" % "2.5"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.6"

// https://mvnrepository.com/artifact/com.typesafe.play/play-slick
libraryDependencies += "com.typesafe.play" %% "play-slick" % "5.0.0"

// https://mvnrepository.com/artifact/com.typesafe.slick/slick-codegen
libraryDependencies += "com.typesafe.slick" %% "slick-codegen" % "3.3.2"

// https://mvnrepository.com/artifact/com.github.tototoshi/slick-joda-mapper
libraryDependencies += "com.github.tototoshi" %% "slick-joda-mapper" % "2.4.2"

libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.47"

libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.3"
libraryDependencies += "org.typelevel" %% "cats-core" % "2.0.0"

// https://mvnrepository.com/artifact/cn.edu.hfut.dmic.webcollector/WebCollector
libraryDependencies += "cn.edu.hfut.dmic.webcollector" % "WebCollector" % "2.71"

libraryDependencies += "org.apache.poi" % "poi-ooxml" % "3.15"

// https://mvnrepository.com/artifact/org.apache.pdfbox/pdfbox
libraryDependencies += "org.apache.pdfbox" % "pdfbox" % "2.0.22"



