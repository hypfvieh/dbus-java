# Remote Objects

If you want to call a method on another application, it's very simple to create
the interface and call the method.  If we use the same interface as we used
in the [exporting objects](./exporting-objects.html) example, we come up with
the following code:

```
package com.foo;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;


public class RemoteExample {

    private DBusConnection m_conn;

    RemoteExample() throws DBusException {
        /* Get a connection to the session bus so we can get data */
        m_conn = DBusConnection.getConnection( DBusConnection.DBusBusType.SESSION );

        /* Get the remote object */
        IntInterface i = m_conn.getRemoteObject( "test.dbusjava.export", "/", IntInterface.class );
        System.out.println( i.add( 5, 7 ) );
    }

    public static void main(String[] args ) throws DBusException {
        new RemoteExample();
    }

}
```

Once we have a reference to the remote object, we can call methods on this
object exactly as if it were an object that we had created on our process.  The
actual calling of the method, as well as the marshalling/unmarshalling of the
data is handled completely transparently.

*Note:* The one issue that this does have is that the method calls are all
blocking.  If the remote application has an issue, your application will stall
until the method call times out.