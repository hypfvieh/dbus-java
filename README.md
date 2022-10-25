[![Maven Build/Test JDK 17](https://github.com/hypfvieh/dbus-java/actions/workflows/maven_jdk17.yml/badge.svg)](https://github.com/hypfvieh/dbus-java/actions/workflows/maven_jdk17.yml) [![Maven Build/Test JDK 11](https://github.com/hypfvieh/dbus-java/actions/workflows/maven_jdk11.yml/badge.svg)](https://github.com/hypfvieh/dbus-java/actions/workflows/maven_jdk11.yml)
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

#### Who uses dbus-java?
See the list in our [Wiki](https://github.com/hypfvieh/dbus-java/wiki)

### Sponsorship
[![Logonbox](.github/lb-logo.png "LogonBox")](https://www.logonbox.com)  

This project receives code contributions and donations from [LogonBox](https://www.logonbox.com).     
However [LogonBox](https://www.logonbox.com) is not responsible for this project and does not take influence in the development.  
The library will remain open source and MIT licensed and can still be used, forked or modified for free.

#### Changes

##### Changes in 4.2.2 (not released yet):
   - Fixed thread priority settings were never passed to thread factory, thanks to [DaveJarvis](https://github.com/DaveJarvis) ([#190](https://github.com/hypfvieh/dbus-java/issues/190))
   - Fixed possible NullPointer dereference in ReceivingService, thanks to [DaveJarvis](https://github.com/DaveJarvis) ([#191](https://github.com/hypfvieh/dbus-java/issues/191))
   - Make ReceivingServiceConfig final in builder, thanks to [DaveJarvis](https://github.com/DaveJarvis) ([#192](https://github.com/hypfvieh/dbus-java/issues/192))
   
##### Changes in 4.2.1 (2022-09-08):
   - Updated dependencies 
   - Compare bus type names using US locale ([#185](https://github.com/hypfvieh/dbus-java/issues/185))
   - Smaller cleanup/redesign in DBusDaemon
   - Fixed regression: Signals could not be created when using `@DBusInterfaceName("XX")` on signal interface classes ([#186](https://github.com/hypfvieh/dbus-java/issues/186))

##### Changes in 4.2.0 (2022-09-05):
   - Deprecated `TransportBuilder.isListening(boolean)` as method name signals that a `boolean` is returned but `TransportBuilder` is returned. Please use `TransportBuilder.listening(boolean)` instead. Old method will be removed in 4.3.0
   - Applied more PMD/CPD suggestions
   - Deprecated `DBusConnectionBuilder.getSystemEndianness()` and `DirectConnectionBuilder.getSystemEndianness()`, use `BaseConnectionBuilder.getSystemEndianness()` instead
   - Refactoring of `DBusConnectionBuilder` and `DirectConnectionBuilder` to use same base class `BaseConnectionBuilder` to reduce duplicated code
   - Moved receiving thread configuration stuff from `BaseConnectionBuilder` to `ReceivingServiceConfigBuilder`
     to configure receiving thread-pools e.g. use `DBusConnectionBuilder.forSessionBus().receivingThreadConfig().withXXX` and continue either with `.buildConnection()` 
     to get the connection object or `.connectionConfig()` to get back to the chosen connection builder
   - Added methods `withXXXThreadPriority` methods to `ReceivingServiceConfigBuilder` to allow changing the thread priority set for `ReceivingService` thread pool threads  ([#173](https://github.com/hypfvieh/dbus-java/issues/173))
   - Improved handling with different transports and address (e.g. fail early if no transport is provided for given address)
   - Added possibility to add custom retry-handler to `ReceivingService` using the builder
   - `ReceivingService` will now throw `IllegalThreadPoolStateException` (subclass of `IllegalStateException`) instead of `IllegalStateException` directly
   - Use `BusAddress` internally instead of Strings
   - Use subclasses of `BusAddress` in Tcp/UnixTransport
   - Added method `isBusType(String)` to `BusAddress` class which allows checking which kind of transport is used case-insensitive and null-safe
   - Support a custom callback on transports right before connecting (preConnectCallback) ([#174](https://github.com/hypfvieh/dbus-java/issues/174))
   - Reorganized `TransportBuilder`, this will also deprecate a lot of methods (`withXXX`) which were moved to `configure()` (which returns a `TransportConfigBuilder`)
   - Transport configuration is now accessible using the `DBusConnectionBuilder` or `DirectConnectionBuilder` (by using e.g. `DBusConnectionBuilder.forSession().transportConfig()`)
   - New `TransportConfig` supports additional configuration by providing a `Map<String,Object>` which allows passing arbitrary values to the transport
   - Updated SPI `ITransportProvider` which now takes a `TransportConfig` object instead of the timeout int. For compatibility the old method is still present (and will be delegated), but should be considered deprecated and will be removed in the future
   - Improved handling of remaining messages to send when disconnection is happening due to `IOException`. 
   In case the disconnect is forced by an exception the remaining messages will be omitted. Otherwise connection may block because of waiting for a replies for `MethodCall`s.
   It is assumed that a disconnection caused by an exception might have closed the transport already so no further messages may be send or received.
   - Allow setting the SASL user ID manually, thanks to [brett-smith](https://github.com/brett-smith) ([PR#178](https://github.com/hypfvieh/dbus-java/issues/178))
   - Moved all SASL related configuration to SaslConfig bean, deprecated methods in AbstractTransport directly related to that change
   - Removed usage of `AbstractConnection.TCP_ADDRESS_PROPERTY` as this was a special behavior for using/testing `DBusDaemon`
   - `EmbeddedDBusDaemon` will no longer set `AbstractConnection.TCP_ADDRESS_PROPERTY`, instead you have to handle the address you used for construction of `EmbeddedDBusDaemon` yourself
   - `DBusConnectionBuilder.forSessionBus()` will use the same validation applied to system addresses
   - Replaced calls to expensive method calls when logging with call which only gets executed if log level is enabled
   - Changed signal handling to use a matching method instead of relying on having a suitable map-key for every possible signal (causes high memory usage / lots of temporary objects, see [#182](https://github.com/hypfvieh/dbus-java/issues/182))
   - Removed usage of `SignalTuple` class
   - Improved `InputMessageStreamReader` to use a final socket channel and some more final member variables for constant size buffers ([#183](https://github.com/hypfvieh/dbus-java/issues/183))
   - Improved `Message` class to not create superflous `Variant` objects to populate message header ([#184](https://github.com/hypfvieh/dbus-java/issues/184))
   
##### Changes in 4.1.0 (2022-05-23):
   - Fixed regression not allowing to use classes directly implementing `DBusInterface` to be exported on the bus ([#157](https://github.com/hypfvieh/dbus-java/issues/157))
   - Throw AuthenticationException when SASL command was unreadable during logon handshake, thanks to [brett-smith](https://github.com/brett-smith) ([PR#158](https://github.com/hypfvieh/dbus-java/issues/158))
   - Fixed issue with handling signals in wrong order ([#159](https://github.com/hypfvieh/dbus-java/issues/159))
   - Applied changes found by PMD/IntelliJ static code analyzers
   - Deprecated `DBusConnection.newConnection`/`DBusConnection.getConnection`, please use DBusConnectionBuilder
   - Deprecated public constructors of `DirectConnection`, please use DirectConnectionBuilder
   - Added dbus-java-example module which contains samples and demonstration code on how to use dbus-java
   - Fixed issue when exporting nested objects ([#163](https://github.com/hypfvieh/dbus-java/issues/163))
   - Added bom ("Bill of Material") project module, thanks to [mk868](https://github.com/mk868) ([PR#167](https://github.com/hypfvieh/dbus-java/issues/167))
   - Fixed missing value in deprecated annotation; Removed duplicated code, thanks to [mk868](https://github.com/mk868) ([PR#168](https://github.com/hypfvieh/dbus-java/issues/168))
   
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

##### Older Changes: [See Wiki ChangeLog](https://github.com/hypfvieh/dbus-java/wiki/Changelog)
