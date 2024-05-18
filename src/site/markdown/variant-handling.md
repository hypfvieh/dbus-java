# Using the `Variant` class

## What is `Variant`?

The `Variant<?>` class is a wrapper type used on DBus when an arbitrary type can be returned.
Like the `Object` type in Java `Variant<?>` can be any kind of data, starting from primitive types like
`int`, `boolean`, `double` etc. to more complex types like `String`.

One example usage of the `Variant<?>` type is the DBus `Properties` interface. In that case a property key
is mapped to a `Variant<?>` which allows mapping the key to any value.

## Constraints using `Variant`

Using `Variant<?>` allows to wrap an arbitrary type. As fancy as this sounds it introduces some problems
regarding the usage of `Variant<?>` in Java.

When wrapping simple types, `Variant<?>` works like expected.
A `Variant<String>` will contain a `String`, a `Variant<Integer>` will contain an `Integer` etc.

The problem appears when wrapping Collections like `List` or `Set` or when using arrays.
In DBus protocol there are no Collections. The protocol only supports array.

When converting a `Variant<List<Integer>>` to the DBus protocol it will be translated to a compatible form
therefore the fact that a `List` was used in Java will get lost and the `List` will become an array.   

When getting data from the bus to convert back to `Variant<List<Integer>>` the information that the Variant
should contain a `List` and not an array is already gone. From DBus standpoint the data is organized as array therefore
the Variant will contain an array of int (`int[]`) and not a `List<Integer>`.

The same result will be produced when using a `Variant<int[]>` - but in this case the de-serialized value
of `int[]` is the expected value.

## Changes introduced with dbus-java 5.1.0

Starting with dbus-java 5.1.0 the behavior of `Variant<?>` has been changed regarding the support of Collections and arrays.
Because the correct type cannot be known from a message on de-serialization, it is now always assumed that a `List` was
requested.

This means if a method or property is specified to return a Variant of int array it will be converted to a `Variant<List<Integer>>`.

This change may conflict with older code but allows a more consistent way to deal with `Variant<?>` containing Collections.