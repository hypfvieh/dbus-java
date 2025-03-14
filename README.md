[![Maven Build/Test JDK 17](https://github.com/hypfvieh/dbus-java/actions/workflows/maven_jdk17.yml/badge.svg)](https://github.com/hypfvieh/dbus-java/actions/workflows/maven_jdk17.yml)
[![Maven Build/Test JDK 21](https://github.com/hypfvieh/dbus-java/actions/workflows/maven_jdk21.yml/badge.svg)](https://github.com/hypfvieh/dbus-java/actions/workflows/maven_jdk21.yml)

# dbus-java
 - Legacy 4.x: [![Maven Central](https://img.shields.io/maven-central/v/com.github.hypfvieh/dbus-java-core?versionPrefix=4&color=%2300AA00)](https://search.maven.org/search?q=g:com.github.hypfvieh%20AND%20a:dbus-java-core%20AND%20v:4*)
 - Javadoc 4.x: [![Javadoc](https://img.shields.io/maven-central/v/com.github.hypfvieh/dbus-java-core?versionPrefix=4&label=javadoc)](https://javadoc.io/doc/com.github.hypfvieh/dbus-java-core)
 - Current 5.x: [![Maven Central](https://img.shields.io/maven-central/v/com.github.hypfvieh/dbus-java-core?versionPrefix=5&color=%2300AA00)](https://search.maven.org/search?q=g:com.github.hypfvieh%20AND%20a:dbus-java-core%20AND%20v:5*)
 - Javadoc 5.x: [![Javadoc](https://img.shields.io/maven-central/v/com.github.hypfvieh/dbus-java-core?versionPrefix=5&label=javadoc)](https://javadoc.io/doc/com.github.hypfvieh/dbus-java-core)
 - Site: [Maven Site](https://hypfvieh.github.io/dbus-java/)

Improved version of [Java-DBus library provided by freedesktop.org](https://dbus.freedesktop.org/doc/dbus-java/) with support for Java 17+. 

### Important information when updating from dbus-java 4.x.x and earlier

The new major is no drop-in replacement for earlier versions!
It requires code changes and at least **Java 17**.

When migrating from 4.x to 5.x you have to fix/replace all usages of deprecated method calls and class usages.
Everything deprecated in a previous major version (4.x/3.x) and marked "forRemoval" is gone in 5.x.

When migrating from 3.x, the main difference is the separation of dbus-java into multi module project.
The base artifact is dbus-java-core and requires at least one additional transport artifact.
A transport provides the code to connect to DBus daemon on various ways (e.g. unix socket or TCP).

When updating to 4.x you have to add at least one transport to your project.
If you add a unix socket transport, you have to choose between jnr-unixsocket and native-unixsocket.
The jnr implementation will pull in jnr-unixsocket, jnr-posix etc. to your project.
It will also provide support for abstract unixsockets and is required if you want to use file descriptor passing.
If you need file descriptors as well you also have to add a proper implementation for that (see below).  

If you don't know what abstract unixsockets are and you don't need file descriptors you'll probably you can use native-unixsockets.

### Upgrade notes for 5.1.0
Upgrading to 5.1.0 should be no problem in most cases.

Beginning with 5.1.0 the behavior of `Variant<?>` has been altered to get a more consistent behavior.
More about that can be found [here](https://hypfvieh.github.io/dbus-java/variant-handling.html) .

If you used `DBusMap` before, you should use `LinkedHashMap` instead.

All methods previously returned instances of `DBusMap` will now return `LinkedHashMap`.
This is transparent if you didn't use `DBusMap` directly but using `Map` interface instead.

In this version the broken `hashCode()` implementation has been fixed in `DBusPath` allowing proper usage
of `DBusPath` as `Map` key. Before equal objects did not create the same `hashCode()` breaking the contract of `hashCode()` and `equals()`.

### Note to SPI providers
If you have used the SPI to extend the MessageReader/Writer of dbus-java before dbus-java 4.x, you have to update your code.
Old providers will not work with dbus-java 4.x/5.x because of changed SPI interfaces (sorry!).

The changes were required due to the support of native-unixsocket which is using java.nio, while the old dbus-java code
uses the old java.io socket API.

With dbus-java 4.x (and 5.x as well), java.nio is used for all transports and therefore required changes on the SPI.
```ISocketProvider``` will now use ```SocketChannel``` instead of ```Socket``` in the exported methods.

### Note for custom transports
If you previously used a custom transport you have to update your code when switching to dbus-java 5.x.
The `AbstractTransport` base class has been changed and now provides different methods to separate client and listening
(server) connections.

Additionally there is a new method called `closeTransport()` which must be implemented to ensure all used resources
(e.g. `SocketChannel` or `ServerSocketChannel` instance) are properly closed.
Previously a transport should have overridden `close()` and call `super.close()` after closing the transport.
This is now longer needed (`close()` is now final and cannot be overriden) and is replaced by `closeTransport()`.

The `connectImpl()` was previously existing and will now only be called for client side connections.
The new methods `bindImpl()`, `acceptImpl()` and `isBound()` are used for server connections and has been added in dbus-java 5.x.

The reason to provide separate methods was to allow bootstrapping server connections before accepting connections.
In the old implementation `accept()` was usually called in `connectImpl()` and therefore blocked the method until
a client was connected. This blocked setting up the server side before the first client was connecting.
This forced the user to use some random sleep times to wait for the server setup after first client connects.
With the separation no more sleep waits are required.

With the new methods, everything related to setup the server socket should be done in `bindImpl()` including the binding
of the listening socket (calling `bind()` on the server socket). Everything done in this method should not block.
You mustn't call `accept()` on your server socket in `bindImpl()`!

In `acceptImpl()` it is expected that the transport calls `accept()` on its server socket and therefore this method will block.
It must return the `SocketChannel` for each client connected (like `connectImpl()` does).

The `isBound()` method must return the bind status of the server socket. This means for example
server socket is not `null` and server socket is opened.
This method is used by `AbstractTransport` to determine if `bindImpl()` was called before and if the server socket is ready to accept
connections.


### How to use file descriptors?
In DBus-Java version below < 4.3.1 file descriptor usage was not supported out of the box and required a third party libary (see below).
Starting with version 4.3.1 file descriptors are supported when using junixsocket-transport.

When trying to use file descriptors in dbus-java 3.x and not providing a implementation for this feature, you may see weird NullPointerExceptions thrown in Message class.
In dbus-java < 4.3.1 you should see error messages indicating that file descriptors are not supported.

To use file descriptors with dbus-java version 4.x before 4.3.1 you have to do the following:
 - Add dbus-java-transport-jnr-unixsocket dependency to your project
 - Remove dbus-java-transport-native-unixsocket if you have used it before
 - Add dependency [com.rm5248:dbus-java-nativefd](https://github.com/rm5248/dbus-java-nativefd) to your classpath
 
When using dbus-java-nativefd, you have to use version 2.x when using dbus-java 4.x/5.x and 1.x if you use dbus-java 3.x.
DBus-java will automatically detect dbus-java-nativefd and will then provide access to file descriptors.

If you are using version 4.3.1 or higher, you may simple switch to `dbus-java-transport-junixsocket` (instead of `dbus-java-transport-jnr-unixsocket` or `dbus-java-transport-native-unixsocket`).
You do this by adding `dbus-java-transport-junixsocket` to your classpath.
Remember to remove the other unixsocket implementations because you are not allowed to have multiple implementations of the same protocol at once.

#### Please note: 
When adding `dbus-java-transport-junixsocket` to your classpath, you will also pull-in some artifacts of junixsocket project.
It is also possible that junixsocket will not work on your platform (depends on which platform and architecture you are using).
They provide a lot of ready-to-use artifacts for different platforms and architectures, but certainly not for all possible combinations out there.
In case your platform is not supported, you may try `dbus-java-transport-jnr-unixsocket` with [com.rm5248:dbus-java-nativefd](https://github.com/rm5248/dbus-java-nativefd), compile
junixsocket yourself or open a ticket at [junixsocket](https://github.com/kohlschutter/junixsocket) asking for help.

### Who uses dbus-java?
See the list in our [Wiki](https://github.com/hypfvieh/dbus-java/wiki)

### Sponsorship
[![Logonbox](.github/lb-logo.png "LogonBox")](https://www.logonbox.com)  

This project receives code contributions and donations from [LogonBox](https://www.logonbox.com).     
However [LogonBox](https://www.logonbox.com) is not responsible for this project and does not take influence in the development.  
The library will remain open source and MIT licensed and can still be used, forked or modified for free.

#### Changes

##### Changes in 5.1.2 (not yet released):
   - Nothing yet

##### Changes in 5.1.1 (2025-03-14):
   - Added new Helper class `VariantBuilder` to allow creating Variants which contain Maps or Collections without messing with the required DBus type arguments
   - Fixed wrong/missing increment when resolving nested structs or deeply nested objects in `Marshalling.getDBusType` ([#265](https://github.com/hypfvieh/dbus-java/issues/265))
   - Fixed wrong import when generating Tuple containing Struct ([#264](https://github.com/hypfvieh/dbus-java/issues/264))
   - Added support for argument prefix for methods and constructors in `InterfaceCodeGenerator` (to e. g. allow generating code using similar code style like dbus-java with prefixing every argument with `_`)
   - Fixed printed version information in `InterfaceCodeGenerator` was always `null`
   - Smaller code cleanup in `InterfaceCodeGenerator` to prevent creating multiple empty lines
   - Dependency updates
   - Added support for `EmitsChangedSignal` ([PR#267](https://github.com/hypfvieh/dbus-java/issues/267)), thanks to [GeVa2072](https://github.com/GeVa2072)
   - Tighten PMD rules to disallow usage of `var` keyword
   - Updated Maven plugins
   - Improved documentation
   - Fixed issue with arrays, primitive arrays and `Collection` when used in signal constructors ([#268](https://github.com/hypfvieh/dbus-java/issues/268))
   - Improvements when using library in Kotlin projects ([PR#270](https://github.com/hypfvieh/dbus-java/issues/270)), thanks to [vicr123](https://github.com/vicr123)
   - Fixed exporting of methods which used a `Tuple` return type caused `ClassCastException` ([#271](https://github.com/hypfvieh/dbus-java/issues/271))
   - Deprecated `ObjectPath`, use `DBusPath` instead
   - Added `of(String...)` factory method to `DBusPath`
   - Smaller refactorings to reduce duplicated code
   - Added additional `getRemoteObject` methods which uses `DBusPath` as argument
   - Smaller improvements in empty array creation and other minor Improvements ([PR#276](https://github.com/hypfvieh/dbus-java/issues/276)), thanks to [joerg1985](https://github.com/joerg1985)

##### Changes in 5.1.0 (2024-08-01):
   - Use Junit BOM thanks to [spannm](https://github.com/spannm) ([PR#248](https://github.com/hypfvieh/dbus-java/issues/248))
   - More Java 17 syntactic sugar, thanks to [spannm](https://github.com/spannm) ([PR#249](https://github.com/hypfvieh/dbus-java/issues/249))
   - Added support for custom ClassLoader/ModuleLayer when configuring Transport (allows usage of third party transports when e.g. using JPMS) ([#251](https://github.com/hypfvieh/dbus-java/issues/251))
   - Improved handling of `@DBusBoundProperty` annotation ([#253](https://github.com/hypfvieh/dbus-java/issues/253)), ([PR#252](https://github.com/hypfvieh/dbus-java/issues/252))
   - Improved InterfaceCodeGenerator to handle generated struct class names properly ([#254](https://github.com/hypfvieh/dbus-java/issues/254))
   - Improved InterfaceCodeGenerator to add parameter/argument name to created struct class name (e.g. MyMethod(something) => MyMethodSomethingStruct)
   - Added dbus-java-transport-junixsocket to BOM ([#255](https://github.com/hypfvieh/dbus-java/issues/255))
   - Fixed issues in InterfaceCodeGenerator regarding missing imports or wrong annotation content ([#257](https://github.com/hypfvieh/dbus-java/issues/257))
   - Fixed issues with `GetAll` on Properites using Annotations ([#258](https://github.com/hypfvieh/dbus-java/issues/258))
   - Changed behavior of de-serialization on Variants containing Collections (Lists). Collections which contained a object which also has a primitive representation the collection was always converted to an array of primitives (e.g. Variant<List<Integer>> got Variant<int[]> on de-serialization). This is usually not expected. When defining a Variant<List<Integer>> it is expected to return that same type when de-serialized. The wrong behavior also caused issues when using `GetAll` method in `Properties` interface. [More information](https://hypfvieh.github.io/dbus-java/variant-handling.html) 
   - Deprecated `DBusMap` - all methods previously used or returned `DBusMap` will now return a `LinkedHashMap`
   - Fixed `hashCode()` and `equals()` method in `DBusPath` (`hashCode()` was completely wrong and violating the `hashCode()` contract when e.g. used as key in maps)
   - Added possibility to use `WeakHashMap` for imported objects (configurable by `DBusConnectionBuilder`), default behavior of using a `ConcurrentHashMap` is not changed (yet) ([#261](https://github.com/hypfvieh/dbus-java/issues/261))
 
##### Changes in 5.0.0 (2024-01-25):
   - **Updated minimum required Java version to 17**
   - Removed all classes and methods marked as deprecated in 4.x
   - Updated dependencies and maven plugins
   - Improved handling of listening connections to allow proper bootstrapping the connection before actually starting accepting new connections (thanks to [brett-smith](https://github.com/brett-smith) ([#213](https://github.com/hypfvieh/dbus-java/issues/213)))
   - Updated export-object documentation ([#236](https://github.com/hypfvieh/dbus-java/issues/236))
   - Fixed issues with autoConnect option, added method to register to bus by 'Hello' message manually, thanks to [brett-smith](https://github.com/brett-smith) ([#238](https://github.com/hypfvieh/dbus-java/issues/238))
   - Added feature which allows to annotate setter or getter methods in exported objects to use the DBus Properties interface behind the scenes, thanks to [brett-smith](https://github.com/brett-smith) ([PR#235](https://github.com/hypfvieh/dbus-java/issues/235))
   - `ExportedObject.isExcluded(Method)` now returns true for bridge, default and synthetic methods, reported by [brett-smith](https://github.com/brett-smith) ([#240](https://github.com/hypfvieh/dbus-java/issues/240))
   - DBusViewer: Remove DOCTYPE definition in introspection data using a regex which handles line breaks properly
   - Applied changes found by Sonarcloud static code analysis
   - Fixed issue with shared connections did not work when underlying transport was disconnected due to end-point (daemon) was stopped/restarted ([#244](https://github.com/hypfvieh/dbus-java/issues/244))
   - Fixed class cast exception in LoggingHelper ([#247](https://github.com/hypfvieh/dbus-java/issues/247)), reported by [AsamK](https://github.com/AsamK)

##### Changes in 4.3.1 (2023-10-03):
   - Provide classloader to ServiceLoader in TransportBuilder (for loading actual transports) and AbstractTransport (for loading IMessageReader/Writer implementations), thanks to [cthbleachbit](https://github.com/cthbleachbit) ([#210](https://github.com/hypfvieh/dbus-java/issues/210), [PR#211](https://github.com/hypfvieh/dbus-java/issues/211))
   - Added missing `connect()` method to `AbstractTransport` to allow connecting the underlying transport manually, thanks to [brett-smith](https://github.com/brett-smith) ([#212](https://github.com/hypfvieh/dbus-java/issues/212))
   - Fixed issue with SASL DATA command when no actual data is sent, thanks to [Prototik](https://github.com/Prototik) ([#214](https://github.com/hypfvieh/dbus-java/issues/214))
   - Fixed SASL AUTH did not read enough data when COOKIE was used, thanks to [Prototik](https://github.com/Prototik) ([#215](https://github.com/hypfvieh/dbus-java/issues/215))
   - Fixed several other issues when using SASL anonymous authentication, thanks to [Prototik](https://github.com/Prototik) ([PR#216](https://github.com/hypfvieh/dbus-java/pull/216))
   - Updated documentation ([#218](https://github.com/hypfvieh/dbus-java/issues/218))
   - Fixed possible issue with used serial numbers in messages because signals may manually incremented the serial without updating global serial ([#220](https://github.com/hypfvieh/dbus-java/issues/220))
   - Updated module-info exports, thanks to [brett-smith](https://github.com/brett-smith) ([#221](https://github.com/hypfvieh/dbus-java/issues/221))
   - Ensure that DBusDaemonThread is terminated when close() is called, thanks to [brett-smith](https://github.com/brett-smith) ([#222](https://github.com/hypfvieh/dbus-java/issues/222))
   - Fixed configured authentication mechanism was always ignored when connecting, thanks to [brett-smith](https://github.com/brett-smith) ([#223](https://github.com/hypfvieh/dbus-java/issues/223))
   - Improved logging and handling of disconnected transports in `DBusDaemon`, thanks to [brett-smith](https://github.com/brett-smith) ([#225](https://github.com/hypfvieh/dbus-java/issues/225))
   - Added additional transport (dbus-java-junixsocket), thanks to [Prototik](https://github.com/Prototik) ([#227](https://github.com/hypfvieh/dbus-java/issues/227)) for providing the implementation
   - Smaller refactorings to avoid code duplication for new transport
   - Use daemon threads in `ExecutorService` for DBus-Sender-Threads ([#234](https://github.com/hypfvieh/dbus-java/issues/234))

##### Changes in 4.3.0 (2023-03-10):

   - Fixed thread priority settings were never passed to thread factory, thanks to [DaveJarvis](https://github.com/DaveJarvis) ([#190](https://github.com/hypfvieh/dbus-java/issues/190))
   - Fixed possible NullPointer dereference in ReceivingService, thanks to [DaveJarvis](https://github.com/DaveJarvis) ([#191](https://github.com/hypfvieh/dbus-java/issues/191))
   - Make ReceivingServiceConfig final in builder, thanks to [DaveJarvis](https://github.com/DaveJarvis) ([#192](https://github.com/hypfvieh/dbus-java/issues/192))
   - Fixed issues with code generator, creating tuple classes without proper imports and having issues creating nested structs (struct in struct) ([#195](https://github.com/hypfvieh/dbus-java/issues/195))
   - Updated dependencies
   - Applied checkstyle suggestions
   - Fixed 'type' was not passed when creating dynamic proxy, thanks to [drivera73](https://github.com/drivera73) ([PR#198](https://github.com/hypfvieh/dbus-java/pull/198))
   - Fixed some possible NullPointerExceptions ([#201](https://github.com/hypfvieh/dbus-java/issues/201), [#202](https://github.com/hypfvieh/dbus-java/issues/202), [#204](https://github.com/hypfvieh/dbus-java/issues/204))
   - Improved encapsulation in BusAddress (and subclasses), deprecated `getParameter(String)` method ([#202](https://github.com/hypfvieh/dbus-java/issues/202))
   - Fixed potentially leaking streams in `SASL` `addCookie()`/`findCookie()` methods ([#205](https://github.com/hypfvieh/dbus-java/issues/205))
   - Replaced old school `PrintStream` file writing with `Files.write` ([#205](https://github.com/hypfvieh/dbus-java/issues/205))
   - Replaced usages of calls to `System.currentMillis()` for locking and waiting due to possible issues when NTP changes time during lock/wait ([#206](https://github.com/hypfvieh/dbus-java/issues/206))
   - Reduced duplicated code ([#206](https://github.com/hypfvieh/dbus-java/issues/206))
   - Updated PMD rules / enabled build failing when PMD/Checkstyle rules are violated
   - Replaced most usages of `System.out.print` and friends in unit tests (use a proper logger instead)
   - Improved logging (changed usage of LoggingHelper, added a better deepToString)
   - Support DBUS_TEST_HOME_DIR system property
   - Fixed issue with broken MethodReturn messages when running as server ([#207](https://github.com/hypfvieh/dbus-java/issues/207))
   - Changed SASL to read responses bytewise to prevent reading to much (and break the following message)
   - Added option to change authentication mode used in DBusDaemon (--auth-mode/-m)
   - Improved handling of broken connections in DBusDaemon
   - Fixed leaking threads in DBusDaemon
   - Fixed `EmbeddedDBusDaemon.startInBackgroundAndWait(long)` did not properly wait for the sender thread to be started ([#208](https://github.com/hypfvieh/dbus-java/issues/208))
   - Fixed DBusDaemon never used `MessageWriter`/`MessageReader` provided on classpath (always used the default implementation)
   - Some refactorings in DBusDaemon/EmbeddedDBusDaemon
   - Improved `AbstractTransport` to support listener connections properly (you have to use `listen()` method now, this allows proper usage of detected `MessageReader`/`MessageWriter` implementation)
   - New option in TransportBuilder to enforce Dbus-Keyring directory permissions (like adviced in DBus Spec). The default is not to check permissions, so the behavior is the same as before (dbus-java didn't care about permissions before)
   - Fixed OSGi packaging

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

##### Older Changes: [See Wiki ChangeLog 3x](https://github.com/hypfvieh/dbus-java/wiki/Changelog-3.x)
