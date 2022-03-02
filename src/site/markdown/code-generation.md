# Code Generation


Since DBus provides a mechanism for introspecting objects via the
`org.freedesktop.DBus.Introspectable` interface, it is possible to query an
already running application for information about interfaces that it has.  This
will then return an XML file, which you can then run the code generator against
in order to produce a Java interface that you can then use to get a remote
object, or use as an interface for your program.

The instructions below will assume you use the dbus-java source to run the code generator.
It is also possible to run the code generator using the dbus-java-utils artificat, but this
requires the classpath with all dependencies and transitive dependencies.
Therefore it is recommended to run the code generator using Maven or your development IDE 
(e.g. Eclipse) to execute the code generator.

## Requirements
 * dbus-java sources (https://github.com/hypfvieh/dbus-java/archive/master.zip)
 * Maven

## Prerequisites
 * Install Maven
 * Download and extract the dbus-java sources
 * Open a terminal and change to the directory where you extracted the dbus-java sources
 * Build the sources: `mvn clean install -DskipTests=true`
 * Change to the sub directory `dbus-java-utils`
 * Now you can continue with the steps below 

## New code generation
 * You can directly obtain the required interface information by reading the introspection data directly from DBus:
 
        mvn exec:java \
          -Dexec.mainClass="org.freedesktop.dbus.utils.generator.InterfaceCodeGenerator" \
          -Dexec.executable="java" \
          -Dexec.args="%classpath --system --outputDir /tmp/classes org.bluez /org/bluez"	
          
 * You may replace  `org.bluez` and `/org/bluez` with the busname and object path of the DBus service 
 you want to generate java classes for.
          
 * You can also use introspection data which is stored in an xml file:
 
        mvn exec:java \
          -Dexec.mainClass="org.freedesktop.dbus.utils.generator.InterfaceCodeGenerator" \
          -Dexec.executable="java" \
          -Dexec.args="%classpath --inputFile /tmp/org.freedesktop.UDisks2.xml --outputDir /tmp/classes ' '"

In both cases the generated classes/interfaces will be written to the provided output directory.
