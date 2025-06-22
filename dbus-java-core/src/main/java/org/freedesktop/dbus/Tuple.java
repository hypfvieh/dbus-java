package org.freedesktop.dbus;

/**
 * This class should be extended to create Tuples.
 * <p>
 * Any such class may be used as the return type for a method
 * which returns multiple values.
 * </p><p>
 * All fields in the Tuple which you wish to be serialized and sent to the
 * remote method should be annotated with the {@link org.freedesktop.dbus.Position Position}
 * annotation, in the order they should appear to DBus.
 * </p><p>
 * A Tuple should have generic type parameters, otherwise deserialization may fail.
 * </p>
 * Example:
 * <pre>
 * public class MyTuple&lt;String, Integer&gt; extends Tuple {
 *      &#64;Position(0)
 *      private final String firstValue;
 *      &#64;Position(1)
 *      private final int secondValue;
 *
 *      // constructor/getter/setter omitted for brevity
 * }
 * </pre>
 */
public abstract class Tuple extends Container {
}
