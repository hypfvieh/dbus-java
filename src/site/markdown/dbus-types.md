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

*File descriptor passing (`h`) requires a transport that supports it. When using the
`dbus-java-transport-junixsocket` transport (recommended, available since dbus-java 4.3.1),
file descriptors work out of the box - no additional dependency is required.
See the [README](https://github.com/hypfvieh/dbus-java#how-to-use-file-descriptors) for
details and for the legacy setup using the `dbus-java-transport-jnr-unixsocket` transport.

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
