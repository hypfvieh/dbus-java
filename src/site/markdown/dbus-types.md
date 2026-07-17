# DBus Types

The following table contains a mapping of DBus types to DBus-Java types

|DBus type code|Java type|
|--------------|---------|
|y             |byte     |
|b             |boolean  |
|n             |short    |
|q             |org.freedesktop.dbus.types.UInt16|
|i             |int      |
|u             |org.freedesktop.dbus.types.UInt32|
|x             |long     |
|t             |org.freedesktop.dbus.types.UInt64|
|d             |double   |
|s             |String   |
|o             |org.freedesktop.dbus.DBusPath|
|g             |String (a DBus type signature)|
|a             |java.util.List|
|() struct     |org.freedesktop.dbus.Struct|
|v             |org.freedesktop.dbus.types.Variant|
|{} dictionary |java.util.Map|
|h             |org.freedesktop.dbus.FileDescriptor*|

*File descriptor passing (`h`) depends on the transport you use:

  * `dbus-java-transport-junixsocket` - **recommended**; file descriptors work out of the box
    (since dbus-java 4.3.1), no additional dependency required.
  * `dbus-java-transport-native-unixsocket` - does **not** support file descriptor passing.
  * `dbus-java-transport-jnr-unixsocket` - supports file descriptors only in combination with an
    additional, third-party native library
    ([com.rm5248:dbus-java-nativefd](https://github.com/rm5248/dbus-java-nativefd)). That library
    relies on platform-specific native (JNI/C) code, is therefore **not** architecture-portable,
    and is **not** shipped or maintained by the dbus-java project. If you need file descriptor
    passing, prefer the `junixsocket` transport instead.
  * `dbus-java-transport-tcp` - file descriptor passing is not possible over TCP.

See the [README](https://github.com/hypfvieh/dbus-java#how-to-use-file-descriptors) for setup details.

## Examples

If we have the DBus signature for a method of `iid`, that means the method
looks like the following:

```java
void methodname(int a, int b, double c);
```

A DBus signature of `ai` would be a list of integers:

```java
void methodname(List<Integer> a);
```

A DBus signature of `asid` would be the following:

```java
void methodname(List<String> a, int b, double c);
```
