# Using the `Variant` class

## What is `Variant`?

The `Variant<?>` class is a wrapper type used on DBus when an arbitrary type can be returned.
Like the `Object` type in Java `Variant<?>` can be any kind of data, like
`Integer`, `Boolean`, `Double` etc. to more complex types like `String` or subclasses of `Struct`.
To achieve this the `Variant` class is parameterized and will store the actual data type used inside of the `Variant<?>`.  

One example usage of the `Variant<?>` type is the DBus `Properties` interface. In that case a property key
is mapped to a `Variant<?>` which allows mapping the key to any value.

## Limitations using `Variant`

Using `Variant<?>` allows to wrap an arbitrary type. As good as this sounds it introduces some problems
regarding the usage of `Variant<?>` in Java.

When wrapping "simple" types, `Variant<?>` works like expected.
A `Variant<String>` will contain a `String`, a `Variant<Integer>` will contain an `Integer` etc.

You cannot wrap primitive types like `int` or `byte`, you have to use the object wrapper types in that case (e.g. `Integer` or `Byte`). This works like any other parameterized class (e.g. `Collection`).

Special care must be taken when using `Collection` types like `List` or `Set` or when you use arrays.

While it is possible to wrap an array of any type including primitives (e.g. `int[]` or `Integer[]`) in a `Variant<?>`,
it will cause issues when trying to deserialize them using dbus-java.

In DBus protocol there are no Collections. The protocol only supports array.

When converting a `Variant<List<Integer>>` to the DBus protocol it will be translated to a compatible form
therefore a `List` that was used in Java will become an array in DBus terms.

That means, serializing `Variant<List<Integer>>` will create the same DBus signature as serializing `Variant<int[]>` or `Variant<Integer[]>`.  

When getting data from the bus to convert back to `Variant<List<Integer>>` the information that the Variant
should contain a `List` and not an array is not present. 
From DBus standpoint the data is organized as array therefore the Variant will contain an array of int (`int[]`) and not a `List<Integer>`.

## How to put a Collection / Map into a `Variant<?>`

Putting `List<?>`, `Set<?>` or `Map<?, ?>` into a `Variant<?>` does not work without a little help because of type erasure of Java.
While you know the actual data type of Collections or Maps while writing the code it is not available during runtime. 
Therefore determining the data type used inside of the Collection/Map passed to the `Variant<?>` constructor is impossible.

To get around this limitation, you have to use another `Variant<?>` constructor which expects the "signature" as second
argument. The signature is the signature string as defined by the DBus protocol.

Some examples:
`List<String>` -> "as"
`Set<Integer>` -> "ai"
`Map<String, Boolean>` -> "a{sb}"

As nobody can remember all of those protocol details, there is a utility `org.freedesktop.dbus.Marshalling.convertJavaClassesToSignature(Class<?>...)` method which will convert the given classes to the appropriate DBus signature value. 

Sample usage:
`Marshalling.convertJavaClassesToSignature(List.class, String.class)` -> "as"
`Marshalling.convertJavaClassesToSignature(Set.class, Integer.class)` -> "ai"
`Marshalling.convertJavaClassesToSignature(Map.class, String.class, Boolean.class)` -> "a{sb}"

Usage with `Variant<?>` constructor:

`new Variant<>(List.of("foo", "bar"), Marshalling.convertJavaClassesToSignature(List.class, String.class))`;
`new Variant<>(Set.of(1, 2, 3), Marshalling.convertJavaClassesToSignature(Set.class, Integer.class))`;
`new Variant<>(Map.of("foo", true, "bar", false), Marshalling.convertJavaClassesToSignature(Map.class, String.class, Boolean.class));`

## Changes introduced with dbus-java 5.1.0

Starting with dbus-java 5.1.0 the behavior of `Variant<?>` has been changed regarding the support of Collections and arrays.
Because the correct type cannot be known from a message on de-serialization, it is now always assumed that a `List` was
requested.

This means if a method or property is specified to return a Variant of int array it will be converted to a `Variant<List<Integer>>`.

This change may conflict with older code but allows a more consistent way to deal with `Variant<?>` containing Collections.

Therefore de-serialzation will never create a `Variant<?>` containing any type of array. **Arrays will always be**
**represented as** `List`.