plugins {
    id("scala")
    kotlin("jvm") version "1.8.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.scala-lang:scala-library:2.13.10")
    implementation("com.twitter:util-core_2.13:21.9.0")

    testImplementation("org.scalatest:scalatest_3:3.2.9")
    testImplementation("junit:junit:4.13.1")

    testRuntimeOnly("org.scala-lang.modules:scala-xml_2.13:1.2.0")
}

kotlin {
    jvmToolchain(15)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}