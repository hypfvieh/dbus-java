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
|o             |org.freedesktop.dbus.ObjectPath|
|g             |???      |
|a             |java.util.List|
|() struct     |org.freedesktop.dbus.Struct|
|v             |org.freedesktop.types.Variant|
|{} dictionary |java.util.Map|
|h             |org.freedesktop.dbus.FileDescriptor*|

*File Descriptor passing is not enabled by default - you need to add the
3rd-party component [dbus-java-nativefd](https://github.com/rm5248/dbus-java-nativefd)

## Examples

If we have the DBus signature for a method of `iid`, that means the method
looks like the following:

```
void methodname( int a, int b, double c );
```

A DBus signature of `ai` would be a list of integers:

```
void methodname( List<int> a );
```

A Dbus signature of `asid` would be the following:

```
void methodname( List<String> a, int b, double c );
```