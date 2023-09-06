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
    api("com.kohlschutter.junixsocket:junixsocket-core:2.7.0")
    api("com.kohlschutter:compiler-annotations:1.5.6")
}

description = "dbus-java-transport-junixsocket"

java {
    withJavadocJar()
}
