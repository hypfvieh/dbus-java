[![Maven Build/Test JDK 21](https://github.com/hypfvieh/dbus-java/actions/workflows/maven_jdk21.yml/badge.svg)](https://github.com/hypfvieh/dbus-java/actions/workflows/maven_jdk21.yml)
[![Maven Build/Test JDK 25](https://github.com/hypfvieh/dbus-java/actions/workflows/maven_jdk25.yml/badge.svg)](https://github.com/hypfvieh/dbus-java/actions/workflows/maven_jdk25.yml)

# dbus-java
 - Legacy 4.x: [![Maven Central](https://img.shields.io/maven-central/v/com.github.hypfvieh/dbus-java-core?versionPrefix=4&color=%2300AA00)](https://search.maven.org/search?q=g:com.github.hypfvieh%20AND%20a:dbus-java-core%20AND%20v:4*)
 - Javadoc 4.x: [![Javadoc](https://img.shields.io/maven-central/v/com.github.hypfvieh/dbus-java-core?versionPrefix=4&label=javadoc)](https://javadoc.io/doc/com.github.hypfvieh/dbus-java-core)
 - Current 5.x: [![Maven Central](https://img.shields.io/maven-central/v/com.github.hypfvieh/dbus-java-core?versionPrefix=5&color=%2300AA00)](https://search.maven.org/search?q=g:com.github.hypfvieh%20AND%20a:dbus-java-core%20AND%20v:5*)
 - Javadoc 5.x: [![Javadoc](https://img.shields.io/maven-central/v/com.github.hypfvieh/dbus-java-core?versionPrefix=5&label=javadoc)](https://javadoc.io/doc/com.github.hypfvieh/dbus-java-core)
 - Site: [Maven Site](https://hypfvieh.github.io/dbus-java/)

Improved version of [Java-DBus library provided by freedesktop.org](https://dbus.freedesktop.org/doc/dbus-java/) with support for Java 21+.

### Important information when updating from older dbus-java versions

Updating dbus-java to a newer major version may always introduce incompatibilities due to breaking changes in classes, methods or API.

If you plan to update to a newer version, please check the update guides below.

- [Update from dbus-java 4.x or older to 5.x (or from 5.0.0 >= 5.1.0)](UPGRADE_TO_5x.md)
- [Update from dbus-java 5.x to 6.x](UPGRADE_TO_6x.md)

### Which Java version for which dbus-java version

|dbus-java version|Required JDK/JRE| Supported | Remarks                                    |
|-----------------|----------------|-----------|--------------------------------------------|
| 2.7.x           | JDK 8          | EOL | First forked version with some minor fixes  |
| 3.x.x           | JDK 8          | EOL | General overhaul version                    |
| 4.x.x           | JDK 11         | EOL | First version using newer JDK and features (java modules), introduced new artifact names and split between core library and transports|
| 5.x.x           | JDK 17         | SSP | Lots of cleanups, helper methods and better testing, usage of new JDK features (sealed classes) |
| 6.x.x           | JDK 21         | Fully supported | Current version |

*EOL* -> End-Of-Life, no longer supported - no bugfixes, no security updates! <br>
*SSP* -> Sunset-Period, smaller bugfixes/security fixes only - support will end approx. Winter 2026

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

##### Changes in 6.0.0 (not released yet):
   - **Minimum Java version: 21**
   - **Removed** all methods, members and classes marked as deprecated
   - Update JUnit to Version 6
   - Remove `throws IOException` from `AbstractConnectionBase.close()` ([#287](https://github.com/hypfvieh/dbus-java/issues/287))
   - Support usage of `Struct`s as return value (as alternative to `Tuple` with generics) (based on discussion in #285)
   - Updated dependencies and plugins
   - Added support to use `Struct` datatypes as return values instead of `Tuple`#
      - Before this change, only `Tuple` based classes could be returned when multiple values were need as result of a method call
      - this leads to very long definitions of classes in diamond operators (e.g., `GetCurrentStateTuple<UInt32, List<GetCurrentStateMonitorsStruct>, List<GetCurrentStateLogicalMonitorsStruct>, Map<String, Variant<?>>>`)
      - with this change, you can also create Structs and use them as return value having something like `GetCurrentStateStruct` instead of using the long Tuple-name
      - Further details on this in [#285](https://github.com/hypfvieh/dbus-java/issues/285)
   - Added new commandline option `--disable-tuples` to `InterfaceCodeGenerator` to create `Struct` classes instead of `Tuple`s for multi value return (**Caution** the generated code will only work with dbus-java 6.0.0+)
   - Added support for Virtual-Threads in `ReceivingService`
     - This can be enabled using the `DBusConnectionBuilder`, example: `DBusConnection sessionConnection = DBusConnectionBuilder.forSystemBus().receivingThreadConfig().withAllVirtualThreads(true).connectionConfig().build()`
     - Virtual-Threads can be enabled/disabled for each of the different executor services used in `ReceivingService`: `SIGNAL`, `ERROR`, `METHODCALL`, `METHODRETURN`
     - default remains native threads on all executors
   - Fixed possible NullPointerException in SASL auth ([#294](https://github.com/hypfvieh/dbus-java/issues/294))
   - Fixed SASL authentication issue when running in server mode in combination with unix sockets ([#298](https://github.com/hypfvieh/dbus-java/issues/298)) 

##### Changes in 5.2.0 (2025-12-21):
   - removed properties from dbus-java.version which causes issues with reproducable builds ([PR#279](https://github.com/hypfvieh/dbus-java/issues/279)) 
   - Re-Implemented `DBusMatchRule`
     - The new implementation can be found in `org.freedesktop.dbus.matchrules.DBusMatchRule`, the old `DBusMatchRule` class still exists and is now a subclass
     of the new implementation but is deprecated.
     - use `DBusMatchRuleBuilder` to create instances of the new `DBusMatchRule`
     - the `AbstractConnection.addSigHandler(DBusMatchRule, SigHandler)` is now `public` and can be used to register arbitrary rules
     - the new implementation supports additional MatchRules as defined by DBus Specification (except eavesdrop)
     - Extended `EmbeddedDBusDaemon` to properly support MatchRules
   - Improved `InterfaceCodeGenerator` to properly create Tuple classes and create empty signal classes as well
   - Dependency updates
   - Plugin updates
   - Increased minimum required Maven version from 3.6.3 to 3.9.3
   - Update site descriptors
   - Addressed a few PMD findings
   - Fixed issue when using `DBusPath("/")` in `getRemoteObject` method ([#290](https://github.com/hypfvieh/dbus-java/issues/290))

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

##### Older Changes: [See Wiki ChangeLog 4x](https://github.com/hypfvieh/dbus-java/wiki/Changelog-4.x)
##### Older Changes: [See Wiki ChangeLog 3x](https://github.com/hypfvieh/dbus-java/wiki/Changelog-3.x)
