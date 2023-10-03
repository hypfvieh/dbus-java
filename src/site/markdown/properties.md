# Properties

[DBus Properties](https://dbus.freedesktop.org/doc/dbus-specification.html#standard-interfaces-properties)  can be accessed 
and exported using one of two methods.

 * [Directly Bound To Setters And Getters](#using-the-exported-object).
 * [Implementing the Properties Interface](#implementing-the-properties-interface).

## Directly Bound To Setters And Getters

Since version 5 of `dbus-java`, it has been possible to directly map DBus properties to Java
methods. By convention this is done using a JavaBeans like pattern.

This method is likely preferable in most cases, as it has a number of advantages over the
legacy method described below.

 * Simpler code when exporting services, no need to implement `Get()`, `Set()` and `GetAll()` from `org.freedesktop.DBus.Properties`.  In fact, you do not need to implement this interface at all.
 * Simpler code when accessing services, no casting or unwrapping `Variant`.  
 * More natural. Java programmers are familiar with this pattern.
 * Makes it much easier to share interfaces between client and server code.

This feature is activated by the `@DBusBoundProperty` annotation.

### The Interface 

Consider the following interface.

```
package com.acme;

import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.annotations.DBusBoundProperty;

public interface MyInterface extends DBusInterface {

    String sayHello();

    @DBusBoundProperty(access = Access.READ, name = "MyProperty")
    String getMyProperty();

    @DBusBoundProperty(access = Access.WRITE, name = "MyProperty")
    void setMyProperty(String _property);

    @DBusBoundProperty(access = Access.READ, name = "ZZZZZZZ")
    long getMyAltProperty();

    @DBusBoundProperty(access = Access.WRITE, name = "ZZZZZZZ")
    void setMyAltProperty(long _property);

    @DBusBoundProperty
    boolean isMyOtherProperty();

    @DBusBoundProperty
    void setMyOtherProperty(boolean  _property);
}
```

When instances of the interface are exported, just a single method call will be visible.
All other attributes of the interface will be exported as properties instead. 

Every DBus property has 3 optional attributes. It's `name`, it's `type` and it's `access` mode.
All 3 of these attributes may optionally be supplied to the annotation, but for most cases it
is sufficient to omit all attributes and for dbus-java to infer these from the method
signature.
 
  * The name of the property will be derived from the method name, with the leading `get`,
    `set` or `is` removed (case insensitively).
  * The access mode of the property will be determined by whether the method is a `set` or
    and `get` or `is`.
  * The type of the property will be derived from either the return value or the first (and only) parameter.
  

*All getters should have zero arguments and a return value, and all setters should have
a single argument, and no return value*

If you are just interested in using a 3rd party service that exports your target object, you then
can just skip the next section and go straight to [Using The Exported Object](#using-the-exported-object) below.

However if you wish to export your own service that uses properties, then read on.

### The Implementation

Expanding on the above example, we will now provide the implementation of this interface
and export it for clients to use.

```java
package com.acme;

public class MyObject implements MyInterface {

    private String myProperty = "Initial value";
    private boolean myOtherProperty;
    private long myAltProperty = 123;

    @Override
    public String getObjectPath() {
        return "/com/acme/MyObject";
    }

    @Override
    public String sayHello() {
        return "Hello!";
    }

    @Override
    public long getMyAltProperty() {
        return myAltProperty;
    }

    @Override
    public void setMyAltProperty(long _myAltProperty) {
        this.myAltProperty = _myAltProperty;
    }

    @Override
    public String getMyProperty() {
        return myProperty;
    }

    @Override
    public void setMyProperty(String _property) {
        myProperty = _property;
    }

    @Override
    public boolean isMyOtherProperty() {
        return myOtherProperty;
    }

    @Override
    public void setMyOtherProperty(boolean _property) {
        myOtherProperty = _property;
    }

    @Override
    public boolean isRemote() {
        /* Whenever you are implementing an object, always return false */
        return false;
    }
}

```

### Exporting The Object

You would export the object the same as normal.

```java
package com.acme;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;

public class ExportMyObject {

    ExportMyObject() throws DBusException {
        DBusConnection conn = DBusConnectionBuilder.forSessionBus().build();
        conn.requestBusName( "com.acme" );
        
        MyObject obj = new MyObject();
        
        conn.exportObject( obj.getObjectPath(), obj );
    }


    public static void main(String[] args) throws DBusException {
        new ExportMyObject();
    }
}
```

### Using The Exported Object

And finally making use of the exported interface in client code.

```java
package com.acme;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;

public class ImportMyObject {

    ImportMyObject() throws DBusException {
        DBusConnection conn = DBusConnectionBuilder.forSessionBus().build();

        MyInterface  obj = conn.getRemoteObject("com.acme", "/com/acme/MyObject", MyInterface.class);

        System.out.println("My property: " + obj.getMyProperty());
        System.out.println("My Alt property: " + obj.getMyAltProperty());
        System.out.println("My Other property: " + obj.isMyOtherProperty());
    }

    public static void main(String[] args) throws DBusException {
        new ImportMyObject();
    }
}
```

## Implementing the Properties Interface

As an alternative to the above, you can use DBus's `org.freedesktop.dbus.Properties` interface.

If you are exporting your own service, this means that you `extends Properties` in your interface,
and provide the required implementations of the `Get()`, `GetAll()` and `Set()` methods in your 
concrete class.

If you are using an exported interface in client code, then you access your remote object as normal,
and invoke the `Get()`, `GetAll()` or `Set()` methods on that object.  

The above example, re-written to use `@DBusProperty`, would look like this.

```
package com.acme;

import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;

@DBusProperty(access = Access.READ_WRITE, name = "MyProperty", type = String.class)
@DBusProperty(access = Access.READ_WRITE, name = "ZZZZZZZ", type = String.class)
@DBusProperty(access = Access.READ_WRITE, name = "MyOtherProperty", type = Boolean.class)
public interface MyInterface extends DBusInterface, Properties {

    String sayHello();
}
``` 

While this interface is smaller than the above, there is a bit more to do in the implementation,
or when you need to access these properties.

```java
package com.acme;

public class MyObject implements MyInterface {

    private String myProperty = "Initial value";
    private boolean myOtherProperty;
    private long myAltProperty = 123;

    @Override
    public String getObjectPath() {
        return "/com/acme/MyObject";
    }

    @Override
    public String sayHello() {
        return "Hello!";
    }
    
    @Override
    public <A> A Get(String _interfaceName, String _propertyName) {
        if(_propertyName.equals("MyProperty")) {
            return (A)myProperty;
        }
        else if(_propertyName.equals("ZZZZZZZ")) {
            return (A)myAltProperty;
        }
        else if(_propertyName.equals("MyOtherProperty")) {
            return (A)myOtherProperty;
        }
        else
            throw new IllegalArgumentException();
    }

    @Override
    public <A> void Set(String _interfaceName, String _propertyName, A _value) {
         if(_propertyName.equals("MyProperty")) {
            myProperty = (String)_value;
        }
        else if(_propertyName.equals("ZZZZZZZ")) {
            myAltProperty = (String)_value;
        }
        else if(_propertyName.equals("MyOtherProperty")) {
            myOtherProperty = (Boolean)_value;
        }
        else
            throw new IllegalArgumentException();
    }

    @Override
    public Map<String, Variant<?>> GetAll(String _interfaceName) {
        Map<String, Variant<?>> all = new HashMap<>();
        all.put("MyProperty", new Variant<>(myProperty));
        all.put("ZZZZZZZ", new Variant<>(myAltProperty));
        all.put("MyOtherProperty", new Variant<>(myOtherProperty));
        return all;
    }

    @Override
    public boolean isRemote() {
        /* Whenever you are implementing an object, always return false */
        return false;
    }
}
```

And finally making use of these properties in client code.

```java
package com.acme;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.Variant;

public class ImportMyObject {

    ImportMyObject() throws DBusException {
        DBusConnection conn = DBusConnectionBuilder.forSessionBus().build();
        
        MyInterface  obj = conn.getRemoteObject(MyInterface.class);
        
        System.out.println("My property: " + 
                        ((Variant<String>)obj.Get("MyProperty")).getValue());
        System.out.println("My Alt property: " + 
                        ((Variant<String>)obj.Get("ZZZZZZZ")).getValue());
        System.out.println("My Other property: " + 
                        ((Variant<Boolean>)obj.Get("MyOtherProperty")).getValue());
    }

    public static void main(String[] args) throws DBusException {
        new ImportMyObject();
    }
}
```