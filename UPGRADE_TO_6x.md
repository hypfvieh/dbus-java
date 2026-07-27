### Information when updating from dbus-java 5.x.x

The dbus-java version 6 requires Java 21 or greater.

Again, all classes and/or methods marked as deprecated in 5.x were removed.
Deprecation warnings regarding usages of deprecated JRE methods were fixed.

The new version supports returning of `Struct` based classes in methods calls in addition to `Tuple` based classes.
This allows you to use shorter definitions when creating or generating code. Instead of e.g., `GetCurrentStateTuple<UInt32, List<GetCurrentStateMonitorsStruct>, List<GetCurrentStateLogicalMonitorsStruct>, Map<String, Variant<?>>> myVariable` you can use `GetCurrentStateStruct` ([#285](https://github.com/hypfvieh/dbus-java/issues/285)).

To instruct the InterfaceCodeGenerator to create `Struct` based return values instead of `Tuple`s, use the new `--disable-tuples` option.
Please be aware, that the created code will only work with dbus-java 6.x and will fail during runtime when used with older versions!

#### Behaviour change: automatic `ObjectManager` handling

Starting with dbus-java 6.x, exported objects implementing `org.freedesktop.DBus.ObjectManager` are handled
automatically by default: dbus-java answers `GetManagedObjects` itself (by enumerating the exported sub-tree
and collecting each object's properties) and automatically emits `InterfacesAdded`/`InterfacesRemoved` when
objects below an `ObjectManager` are exported/unexported.

If you already provide your own server-side `ObjectManager` implementation and want to keep full manual
control (your own `GetManagedObjects` and your own signal emission), build the connection with
`withManualObjectManager(true)`:

```java
DBusConnection conn = DBusConnectionBuilder.forSessionBus()
    .withManualObjectManager(true)
    .build();
```

If you do not implement `ObjectManager` yourself, you can now export a ready-to-use one via
`connection.exportObjectManager("/your/path")` without writing any class.
