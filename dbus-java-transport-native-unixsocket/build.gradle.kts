/*
 * This file was generated by the Gradle 'init' task.
 *
 * This project uses @Incubating APIs which are subject to change.
 */

plugins {
    id("com.github.hypfvieh.java-conventions")
}

dependencies {
    api(project(":dbus-java-core"))
    testImplementation(project(":dbus-java-core"))
}

description = "dbus-java-transport-native-unixsocket"

java {
    withJavadocJar()
}
