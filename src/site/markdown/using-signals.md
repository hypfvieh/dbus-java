# Using Signals

## What is Signal?

A signal in DBus terms is something like a callback which is invoked when something specific happens on the bus.

The best example for this is the `PropertiesChangedSignal`. This signal is emitted when a property of an object was changed using a DBus call.

For example, you have 2 applications both listening to NetworkManager interfaces. 
The first application asks NetworkManager to disconnect a network device. The NetworkManager will disconnect the device
and emit a signal to all recipients listening for signals (Application 2 in this case).
This allow application 2 to do whatever needed when knowing that a device disconnects.

## Receiving signals

To receive signals, you first have to create a public interface class which extends `DBusInterface`.
Inside this interface you create a inner class which extends `DBusSignal` class.

This inner class must have a public constructor which takes the object path as first parameter and all
additional required parameters after that.
This constructor must call the super constructor providing all arguments which are part of this signal.

Example:
```java
public interface MySignal extends DBusInterface {

    class MySignalClass extends DBusSignal {
        private final String someValue;
        private final int someIntValue;
        
        public MySignalClass(String _objectPath, String _someValue, int _someInt) {
            super(_objectPath, _someValue, _someInt);
            someValue = _someValue;
            someIntValue = _someInt;
        }
        
        public String getSomeValue() {
            return someValue;
        }
        
        public int getSomeIntValue() {
            return someIntValue;
        }
    }
}
```

To get notified when the signal arrives you have to register a signal handler on you connection to DBus.
A signal handler must implements `DBusSigHandler<?>` interface and can be registered the current connection.

There are different `addSigHandler` methods depending on the use case.
If you only want to listen for specific signals of a specific remote object, you should use something like:

```java
MySignal remoteSignal connection.getRemoteObject("some.bus.name", "/some/object/path", MySignal.class);
connection.addSigHandler(MySignalClass.class, remoteSignal, new MySignalClassHandler());
```

If you want to listen to all object paths of an exported object you can use:
`connection.addSigHandler(MySignalClass.class, new MySignalClassHandler())`.

It is also possible to express a signal handler as Lambda e.g.:
`connection.addSigHandler(MySignalClass.class, signal -> System.out.println("Got signal: " + signal))`

To remove a handler you can either call `close()` on the object returned by the `addSigHandler` calls or use the appropriate `removeSigHandler` call.
You don't have to remove your signal handler when you want to close the connection anyway. 
All registered signal handlers will automatically be unregistered when connection is closed.

### Using DBusMatchRule

In addition to the `addSigHandler` methods described above you can also use a feature called MatchRule.

MatchRules are simple filters which will be registered on server side. The associated callback will be called when the server selects a message matching the filter criteria and sending it to the connection the filter has been registered for.

To allow you to use custom filter criteria, you can use DBusMatchRuleBuilder to create any rule matching your needs.

Example:

```java
connection.addSigHandler(DBusMatchRuleBuilder.create()
    .withPath("/org/test/Introduction")
    .withSender("org.my.Sender")
    .build(), 
    new MyCustomSignalHandler());
```

The code above will create a custom rule using `path` and `sender`. 
The handler will be called when both conditions are true.  

For more information on MatchRules see [DBus-Specification](https://dbus.freedesktop.org/doc/dbus-specification.html#message-bus-routing-match-rules).

Please note: dbus-java does not support the eavesdrop option. 
Eavesdrop is deprecated according to specification, therefore there are no plans to add it.

## Signals and constructors

A signal class can have multiple constructors. 
The actual constructor used for signal creation depends on the signature of the signal received on the bus.

Every object and also every signal has as signature which defines what kind of data has to be expected
in the received DBus message.

When dbus-java receives a signal, it uses DBus interface name as package name and the signal name as class name.
It checks for presents of a proper signal class using these parameters.
If a signal class could be found, all public constructors are read and put in a cache (so no need to re-scan every time).

Then the signature of the received signal is used to find a matching constructor previously fetched from the signal class.
If no suitable constructor is found, a warning is logged and the signal is ignored.

Starting from dbus-java 5.1.1, you can add a `unknownSignalHandler` using the `DBusConnectionBuilder`. This
callback will than always be called when dbus-java is unable to de-serialize a signal.

If a suitable constructor is found, a instance of the signal class is created and all registered handlers will be notified.

### Problems with constructors

When having multiple constructors it is good practice the use constructor chaining to ensure all constructors will
end up in one constructor calling the super constructor.
Ignoring this advice may lead to confusion, because the used constructor is picked by DBus signature and may not be the
constructor you expect.

#### Collection / array in constructor

Due to the nature of DBus only supporting arrays, using collections or arrays in constructors was a bit confusing.
Before dbus-java 5.1.0, it was always assumed that the signal will using arrays and never uses collections in constructor.
Furthermore, it was expected that this array was a primitive array when a primitive version of the type existed in Java.

For example `MySignal(String _objPath, Integer[] _some)` would not work with older dbus-java versions.
Same for  `MySignal(String _objPath, List<Integer> _some)`. 
The signature of the signal on DBus would always be `ai` and so the library always converted that to `int[]`.

In dbus-java 5.1.0, there was a bug which caused all constructors which previously worked were broken because
this version of dbus-java always created `List<Integer>` and never created `int[]`.
In other words, a constructor of `MySignal(String _objPath, int[] _some)` was suddenly broken.

With dbus-java 5.1.1 this problem was fixed and also the misbehavior of dbus-java < 5.1.0 was also fixed.

Now dbus-java will do some further investigation of the received signal signature and will not only look for
constructors using primitive array, but also for `Collection`.

This flexibility comes with a small price: If you have multiple constructors with the same argument length and order, but one uses array and one collection, the first constructor listed in your class will be used.

Example:
```java
class MySignal extends DBusSignal {
    public MySignal(String _objPath, int[] _arr, String _text) {
       super(_objectPath, _arr, _text);
    }
    
    public MySignal(String _objPath, List<Integer> _arr, String _text) {
       super(_objectPath, _arr, _text);
    }
}
```

In the example above, the constructor using `int[]` will be used, because it is listed first in the class.
If the order would be inverted, the constructor with `List` would always be called.

If you use proper constructor chaining like suggested above, this will not bother you.
The example would then look like this:

```java
class MySignal extends DBusSignal {
    public MySignal(String _objPath, int[] _arr, String _text) {
       this(_objectPath, Arrays.asList(_arr), _text);
    }
    
    public MySignal(String _objPath, List<Integer> _arr, String _text) {
       super(_objectPath, _arr, _text);
    }
}
```
