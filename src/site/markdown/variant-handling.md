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

When getting data from the bus, the information whether the `Variant` originally held a `List` or an array is not present -
from the DBus standpoint the data is always organized as an array. Since dbus-java 5.1.0 such data is therefore **always**
deserialized as a `List` (see the section "Changes introduced with dbus-java 5.1.0" below).
So a `Variant<int[]>` that was sent will be received as a `Variant<List<Integer>>` - a `Variant<?>` will never contain an
array after deserialization.

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

### Using `VariantBuilder`

Since dbus-java 5.1.1 there is a more convenient way to build a `Variant<?>` around a Collection or Map without
having to compute the signature yourself: `org.freedesktop.dbus.types.VariantBuilder`.
You declare the container type and its generic types, and the builder derives the correct signature for you.

```java
Variant<List<String>>          v1 = VariantBuilder.of(List.class).withGenericTypes(String.class).create(List.of("foo", "bar"));
Variant<Set<Integer>>          v2 = VariantBuilder.of(Set.class).withGenericTypes(Integer.class).create(Set.of(1, 2, 3));
Variant<Map<String, Boolean>>  v3 = VariantBuilder.of(Map.class).withGenericTypes(String.class, Boolean.class).create(Map.of("foo", true));
```

## Changes introduced with dbus-java 5.1.0

Starting with dbus-java 5.1.0 the behavior of `Variant<?>` has been changed regarding the support of Collections and arrays.
Because the correct type cannot be known from a message on de-serialization, it is now always assumed that a `List` was
requested.

This means if a method or property is specified to return a Variant of int array it will be converted to a `Variant<List<Integer>>`.

This change may conflict with older code but allows a more consistent way to deal with `Variant<?>` containing Collections.

Therefore de-serialzation will never create a `Variant<?>` containing any type of array. **Arrays will always be**
**represented as** `List`.