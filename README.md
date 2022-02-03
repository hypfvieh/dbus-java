# dbus-java
 - Legacy 3.x: [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.hypfvieh/dbus-java/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.hypfvieh/dbus-java)
 - Javadoc 3.x: [![Javadoc](https://javadoc.io/badge2/com.github.hypfvieh/dbus-java/javadoc.svg)](https://javadoc.io/doc/com.github.hypfvieh/dbus-java)
 - Current 4.x: [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.hypfvieh/dbus-java-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.hypfvieh/dbus-java-core)
 - Javadoc 4.x: [![Javadoc](https://javadoc.io/badge2/com.github.hypfvieh/dbus-java-core/javadoc.svg)](https://javadoc.io/doc/com.github.hypfvieh/dbus-java-core)

Improved version of [Java-DBus library provided by freedesktop.org](https://dbus.freedesktop.org/doc/dbus-java/) with support for Java 11+. 

### Important information when updating from dbus-java 3.x.x and earlier

The new major is no drop-in replacement for 2.7.x or 3.x.x version!
It requires code changes and at least **Java 11**.

Main difference is the separation of dbus-java functions (now called dbus-java-core) and the transports.
A transport provides the code to connect to DBus daemon on various ways (e.g. unix socket or TCP).

When updating to 4.x you have to add at least one transport to your project.
If you add a unix socket transport, you have to choose between jnr-unixsocket and native-unixsocket.
The later will require **Java 16+**, while jnr-unixsockets will work with Java 11 but will pull-in jnr-posix and friends to your project.

The native-unixsockets will work almost like the jnr-unixsockets except it does not support abstract unixsockets.
If you don't know what abstract unixsockets are, you'll probably don't need it and you can use native-unixsockets when using proper Java version.

If you use ```TransportFactory``` directly, you have to replace it with ```TransportBuilder```.

### Note to SPI providers
If you have used the SPI to extend the MessageReader/Writer of dbus-java, you have to update your code.
Old providers will not work with dbus-java 4.x because of changed SPI interfaces (sorry!).

The changes were required due to the support of native-unixsocket which is using java.nio, while the old dbus-java code
uses the old java.io socket API.

With dbus-java 4.x, java.nio is used for all transports and therefore required changes on the SPI.
```ISocketProvider``` will now use ```SocketChannel``` instead of ```Socket``` in the exported methods.

### Who uses dbus-java?
See the list in our [Wiki](https://github.com/hypfvieh/dbus-java/wiki)

### Sponsorship
![Logonbox](.github/logonbox-new-logo-black.png#gh-light-mode-only)![Logonbox](.github/logonbox-new-logo-white.png#gh-dark-mode-only)

This project receives code contributions and donations from [LogonBox](https://www.logonbox.com).     
However [LogonBox](https://www.logonbox.com) is not responsible for this project and does not take influence in the development.  
The library will remain open source and MIT licensed and can still be used, forked or modified for free.

### Changes
##### Changes in 4.0.1 (not yet released):
   - Fixed regression not allowing to use classes directly implementing `DBusInterface` to be exported on the bus ([#157](https://github.com/hypfvieh/dbus-java/issues/157))
   - Throw AuthenticationException when SASL command was unreadable during logon handshake, thanks to [brett-smith](https://github.com/brett-smith) ([PR#158](https://github.com/hypfvieh/dbus-java/issues/158))
   
##### Changes in 4.0.0 (2021-12-30):
   - Requires at least **Java 11**
   - Added transport which uses native unix sockets provided by Java 16+ (see: [#145](https://github.com/hypfvieh/dbus-java/issues/145))
   - Splitted dbus-java to multiple modules:
      - dbus-java-core: Core dbus-java functions (always required)
      - dbus-java-transport-jnr-unixsocket: Unix socket support based on jnr-unixsocket
      - dbus-java-transport-native-unixsocket: Unix socket support based on native unix sockets (Java 16+ required)
      - dbus-java-transport-tcp: TCP based DBus messaging
      - dbus-java-utils: utilities which may help during development
   - Updated dependencies

##### Changes in 3.3.1 (Released: 2021-10-23):
   - Fixed some issues in InterfaceCodeGenerator related to signal constructors, thanks to [poeschel](https://github.com/poeschel) ([PR#146](https://github.com/hypfvieh/dbus-java/pull/146))
   - Fixed some issues in InterfaceCodeGenerator related to usage of Tuples, thanks to [poeschel](https://github.com/poeschel) ([PR#147](https://github.com/hypfvieh/dbus-java/pull/147))
   - Improved SASL authentication with domain sockets on OS X, thanks to [brett-smith](https://github.com/brett-smith) ([PR#148](https://github.com/hypfvieh/dbus-java/pull/148))
   - Fixed some issues related to marshalling/unmarshalling of Tuples, thanks to [poeschel](https://github.com/poeschel) ([PR#149](https://github.com/hypfvieh/dbus-java/pull/149))
   - Fixed various issues in Introspection generation introduced with [PR#143](https://github.com/hypfvieh/dbus-java/pull/143) and in marshalling caused by  [PR#149](https://github.com/hypfvieh/dbus-java/pull/149)
   - Fixed issue with interupted status of thread got lost after a synchronous method call was interrupted [#150](https://github.com/hypfvieh/dbus-java/issues/150)
   
##### Changes in 3.3.0 (Released: 2021-03-17):
  *CAUTION* - This version may contain breaking changes!!
  - Removed usage of java-utils
  - Moved Hexdump class from org.freedesktop to org.freedesktop.dbus.utils
  - Moved DBus interface from org.freedesktop to org.freedesktop.dbus.interfaces
  - Modulize dbus-java ([PR#125](https://github.com/hypfvieh/dbus-java/pull/125), thanks to [brett-smith](https://github.com/brett-smith))
  - DBusConnection.getConnection(DBusBusType _bustype) will no longer throw RuntimeException but DBusConnectionException if something went wrong establishing DBus connection [#128](https://github.com/hypfvieh/dbus-java/issues/128)
  - Support annotation to specify properties provided by an exported object (DBusProperty annotation) ([PR#131](https://github.com/hypfvieh/dbus-java/pull/131)/[#130](https://github.com/hypfvieh/dbus-java/issues/130)), thanks to [mk868](https://github.com/mk868)
  - Added support for the new DBusProperty annotations in InterfaceCodeGenerator Util ([PR#132](https://github.com/hypfvieh/dbus-java/pull/132)), thanks to [mk868](https://github.com/mk868)
  - License changed from LGPLv2 to MIT

##### Changes in 3.2.4:
  - Improved logging usage of Arrays.deepToString(Object[]), so this message will not be called in log statements if the loglevel is not enabled
  - Improved usage of regex and length checks [#119](https://github.com/hypfvieh/dbus-java/issues/119) & [PR#120](https://github.com/hypfvieh/dbus-java/pull/120) (thanks to [OlegAndreych](https://github.com/OlegAndreych))
  - Improvements in InterfaceCodeGenerator Util regarding creation of Struct classes ([#121](https://github.com/hypfvieh/dbus-java/issues/121))
  - Fixed possible race-conditions/dead-locks when disconnecting from DBus ([#123](https://github.com/hypfvieh/dbus-java/pull/123))
  
##### Changes in 3.2.3:
  - Fixed regression introduced with [#110](https://github.com/hypfvieh/dbus-java/pull/110) ([#114](https://github.com/hypfvieh/dbus-java/pull/114))

##### Changes in 3.2.2:
  - Fixed issue with introspection caused by changes in [#80](https://github.com/hypfvieh/dbus-java/issues/80) ([#103](https://github.com/hypfvieh/dbus-java/issues/103)), thanks to [AsamK](https://github.com/AsamK)
  - Added support for FreeBSD, ([#105](https://github.com/hypfvieh/dbus-java/pull/105)) thanks to [grembo](https://github.com/grembo)
  - Fixed SASL authentication may get stuck when using TCP ([#106](https://github.com/hypfvieh/dbus-java/pull/106)) thanks to [brett-smith](https://github.com/brett-smith)
 - Fixed issues when dealing with multiple signals of the same name but different signatures ([#110](https://github.com/hypfvieh/dbus-java/pull/110))
 - Dependency updates

##### Changes in 3.2.1
  - Fixed leaking signal handlers when using addSigHandler/removeSigHandler a lot ([#76](https://github.com/hypfvieh/dbus-java/issues/76))
  - Fixed unexported objects shown in introspection output ([#80](https://github.com/hypfvieh/dbus-java/issues/80))
  - Added support for providing alternative implementations of IMessageReader/IMessageWriter ([#81](https://github.com/hypfvieh/dbus-java/issues/81))
  - Removed support for file descriptor passing, as it is not working with jnr-unixsocket ([#81](https://github.com/hypfvieh/dbus-java/issues/81))
  - Fixed issue dealing with handled signals ([#97](https://github.com/hypfvieh/dbus-java/issues/97))
  - Fixes issue with InterfaceCodeGenerator util ([#95](https://github.com/hypfvieh/dbus-java/issues/95))
  - Fixes issue with InterfaceCodeGenerator util using incorrect type ([#83](https://github.com/hypfvieh/dbus-java/issues/83))

##### Changes in 3.2.0 
  - Replaced libmatthew with jnr-unixsocket
  - Removed all native library files and sources 
  - Added new transport system to use jnr-unixsocket (replaces old Transport class)
  - Added support for empty collections/arrays (thanks to [chris-melman](https://github.com/chris-melman))
  - Added support of DBUS_MACHINE_ID_LOCATION environment property to allow usage of dbus-java on Windows (thanks to [chris-melman](https://github.com/chris-melman))
  - Allow change of the default socket timeout (thanks to [chris-melman](https://github.com/chris-melman))
  - Detect system endianness for DBus message instead of using BIG endian all the time (#54)
  - Allow changing the default endianness (DBusConnection.setEndianness())
  - Providing OSGi ready artifact ([#33](https://github.com/hypfvieh/dbus-java/issues/32)) (thanks to [stack-head](https://github.com/stack-head))

##### Older Changes: [See Wiki ChangeLog](https://github.com/hypfvieh/dbus-java/wiki/Changelog)
