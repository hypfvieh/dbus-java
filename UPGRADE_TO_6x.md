### Information when updating from dbus-java 5.x.x

The dbus-java version 6 requires Java 21 or greater.

Again, all classes and/or methods marked as deprecated in 5.x were removed.
Deprecation warnings regarding usages of deprecated JRE methods were fixed.

The new version supports returning of `Struct` based classes in methods calls in addition to `Tuple` based classes.
This allows you to use shorter definitions when creating or generating code. Instead of e.g., `GetCurrentStateTuple<UInt32, List<GetCurrentStateMonitorsStruct>, List<GetCurrentStateLogicalMonitorsStruct>, Map<String, Variant<?>>> myVariable` you can use `GetCurrentStateStruct` ([#285](https://github.com/hypfvieh/dbus-java/issues/285)).

To instruct the InterfaceCodeGenerator to create `Struct` based return values instead of `Tuple`s, use the new `--disable-tuples` option.
Please be aware, that the created code will only work with dbus-java 6.x and will fail during runtime when used with older versions!
