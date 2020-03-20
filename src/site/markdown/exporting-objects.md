# Exporting Objects

In order for other programs to call a method, you must first export the object
onto the bus in order for other programs to see it.  The object that is exported
must implement an interface which extends `DBusInterface`.

Here's a sample interface:

```
package com.foo;

import org.freedesktop.dbus.interfaces.DBusInterface;

public interface IntInterface extends DBusInterface {

    public int add( int a, int b );
}
```

In order for other applications to call this interface, we need to create an
object that implements this interface and export it onto the bus.  Here's the
full code that does that:

```
package com.foo;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;

public class ExportExample implements IntInterface {

    private DBusConnection m_conn;

    ExportExample() throws DBusException {
        /* Get a connection to the session bus so we can request a bus name */
        m_conn = DBusConnection.getConnection( DBusConnection.DBusBusType.SESSION );
        /* Request a unique bus name */
        m_conn.requestBusName( "test.dbusjava.export" );
        /* Export this object onto the bus using the path '/' */
        m_conn.exportObject( getObjectPath(), this );
    }

    @Override
    public int add(int a, int b) {
        return a + b;
    }

    @Override
    public boolean isRemote() {
        /* Whenever you are implementing an object, always return false */
        return false;
    }

    @Override
    public String getObjectPath() {
        /* This is not strictly needed; it is a convenience method for housekeeping
         * on your application side if you will be exporting and unexporting
         * many times
         */
        return "/";
    }


    public static void main(String[] args) throws DBusException {
        new ExportExample();
    }
}
```

If you compile and run this program, you will be able to do two things at this
point: view the introspection data, and get the result of an addition.

If we introspect our application like the following, we can see the
_automatically_ generated introspection XML document.  This means that we can
define an entire interface without touching XML at all.

```
$ dbus-send --print-reply=literal --type=method_call --dest=test.dbusjava.export / org.freedesktop.DBus.Introspectable.Introspect
   <!DOCTYPE node PUBLIC "-//freedesktop//DTD D-BUS Object Introspection 1.0//EN" "http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd">
<node name="/">
 <interface name="com.foo.IntInterface">
  <method name="add" >
   <arg type="i" direction="in"/>
   <arg type="i" direction="in"/>
   <arg type="i" direction="out"/>
  </method>
 </interface>
 <interface name="org.freedesktop.DBus.Introspectable">
  <method name="Introspect">
   <arg type="s" direction="out"/>
  </method>
 </interface>
 <interface name="org.freedesktop.DBus.Peer">
  <method name="Ping">
  </method>
 </interface>
</node>
```

Next, we can go call the remote method using dbus-send and get the result of
the addition back:

```
$ dbus-send --print-reply=literal --type=method_call --dest=test.dbusjava.export / com.foo.IntInterface.add int32:5 int32:7
   int32 12
```