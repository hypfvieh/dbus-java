# Remote Objects

If you want to call a method on another application, it's very simple to create
the interface and call the method.  If we use the same interface as we used
in the [exporting objects](./exporting-objects.html) example, we come up with
the following code:

```java
package com.foo;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;


public class RemoteExample {

    private DBusConnection m_conn;

    RemoteExample() throws DBusException {
        /* Get a connection to the session bus so we can get data */
        m_conn = DBusConnectionBuilder.forSessionBus().build();

        /* Get the remote object */
        ISampleExport i = m_conn.getRemoteObject("test.dbusjava.export", "/", ISampleExport.class );
        System.out.println( i.add( 5, 7 ) );
    }

    public static void main(String[] _args) throws DBusException {
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

## Controlling Remote Method Call Behavior

When calling remote D-Bus methods, you can influence how the call is handled by using annotations on the methods declared in your interface. These annotations allow you to set specific flags in the D-Bus message header.

### Available Annotations

*   **`@MethodNoReply`**
    Method does not return replies or error replies, even if it is of a type that can have a reply.
    ```java
    @MethodNoReply
    int add(int _a, int _b);
    ```
    The call returns immediately without waiting for a reply, so the return value carries no
    result: for object return types it is `null`, for primitive types it is the default value
    (e.g. `0` for `int`). Use this annotation only for methods whose result you do not need.

*   **`@MethodAllowInteractiveAutorization`**
    This annotation signals the D-Bus daemon that the caller is ready to wait for interactive authorization (e.g., Polkit password prompts). It is useful when unprivileged code calls a privileged method, and an authorization framework that supports user interaction is in place.

    As an example, let's take a call to a remote Systemd manager object method that requires elevated privileges to execute. Use [the code from the example](https://github.com/hypfvieh/dbus-java/tree/master/dbus-java-examples/src/main/java/com/github/hypfvieh/dbus/examples/systemd/SystemdUnitsManagment.java):
    ```java
    @DBusMemberName(value = "StartUnit")
    @MethodAllowInteractiveAutorization
    DBusPath startUnit(String name, String mode);

    @DBusMemberName(value = "StopUnit")
    @MethodAllowInteractiveAutorization
    DBusPath stopUnit(String name, String mode);
    ```
    After calling the method, the user authorization window is guaranteed to be displayed if it needed.

*Note:* More info about this you can find [here](https://dbus.freedesktop.org/doc/dbus-specification.html).
