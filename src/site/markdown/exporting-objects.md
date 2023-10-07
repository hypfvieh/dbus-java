# Exporting Objects

_[Full Source](https://github.com/hypfvieh/dbus-java/tree/master/dbus-java-examples/src/main/java/com/github/hypfvieh/dbus/examples/export)_

In order for other programs to call a method, you must first export the object
onto the bus in order for other programs to see it.  The object that is exported
must implement an interface which extends `DBusInterface`.

Here's a sample interface:

```java
package com.github.hypfvieh.dbus.examples.export;

import org.freedesktop.dbus.interfaces.DBusInterface;

public interface ISampleExport extends DBusInterface {
    int add(int _a, int _b);

    void terminateApp();
}
```

In order for other applications to call this interface, we need to create an
object that implements this interface and export it onto the bus.  Here's the
full code that does that:

```java
package com.github.hypfvieh.dbus.examples.export;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;

import java.util.concurrent.CountDownLatch;

public class ExportExample implements ISampleExport {

    private DBusConnection dbusConn;
    private CountDownLatch waitClose;

    ExportExample() throws DBusException, InterruptedException {
        waitClose = new CountDownLatch(1);
        // Get a connection to the session bus so we can request a bus name
        dbusConn = DBusConnectionBuilder.forSessionBus().build();
        // Request a unique bus name
        dbusConn.requestBusName("test.dbusjava.export");
        // Export this object onto the bus using the path '/'
        dbusConn.exportObject(getObjectPath(), this);
        // this will cause the countdown latch to wait until terminateApp() was called
        // you probably want to do something more useful
        waitClose.await();
        System.out.println("bye bye");
    }

    @Override
    public int add(int _a, int _b) {
        return _a + _b;
    }

    @Override
    public void terminateApp() {
        waitClose.countDown();
    }

    @Override
    public boolean isRemote() {
        /* Whenever you are implementing an object, always return false */
        return false;
    }

    @Override
    public String getObjectPath() {
        /*
         * This is not strictly needed; 
         * it is a convenience method for housekeeping on your application side if you will
         * be exporting and unexporting many times
         */
        return "/";
    }

    public static void main(String[] _args) throws Exception {
        new ExportExample();
    }

}
```

If you compile and run this program, you will be able to do three things at this
point: view the introspection data get the result of an addition and terminate our application by
calling `terminateApp()`.

If we introspect our application like the following, we can see the
_automatically_ generated introspection XML document.  This means that we can
define an entire interface without touching XML at all.

```
$ dbus-send --print-reply=literal --type=method_call --dest=test.dbusjava.export / org.freedesktop.DBus.Introspectable.Introspect

<!DOCTYPE node PUBLIC "-//freedesktop//DTD D-BUS Object Introspection 
 1.0//EN" "http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd">
 <node name="/">
  <interface name="com.github.hypfvieh.dbus.examples.export.ISampleExport">
   <method name="add" >
    <arg type="i" direction="in"/>
    <arg type="i" direction="in"/>
    <arg type="i" direction="out"/>
   </method>
   <method name="terminateApp" >
   </method>
  </interface>
  <interface name="org.freedesktop.DBus.Introspectable">
   <method name="Introspect" >
    <arg type="s" direction="out"/>
   </method>
  </interface>
  <interface name="org.freedesktop.DBus.Peer">
   <method name="Ping" >
   </method>
   <method name="GetMachineId" >
    <arg type="s" direction="out"/>
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

Or we can use the `terminateApp()` method to stop our application:

```
$ dbus-send --print-reply=literal --type=method_call --dest=test.dbusjava.export / com.foo.IntInterface.terminateApp
```