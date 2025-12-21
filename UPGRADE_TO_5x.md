### Information when updating from dbus-java 4.x.x

Requires code changes and at least **Java 17**.

When migrating from 4.x to 5.x you have to fix/replace all usages of deprecated method calls and class usages.
Everything deprecated in a previous major version (4.x/3.x) and marked "forRemoval" is gone in 5.x.

When migrating from 3.x, the main difference is the separation of dbus-java into multi module project.
The base artifact is dbus-java-core and requires at least one additional transport artifact.
A transport provides the code to connect to DBus daemon on various ways (e.g. unix socket or TCP).

When updating to 4.x you have to add at least one transport to your project.
If you add a unix socket transport, you have to choose between jnr-unixsocket and native-unixsocket.
The jnr implementation will pull in jnr-unixsocket, jnr-posix etc. to your project.
It will also provide support for abstract unixsockets and is required if you want to use file descriptor passing.
If you need file descriptors as well you also have to add a proper implementation for that (see below).

If you don't know what abstract unixsockets are and you don't need file descriptors you'll probably you can use native-unixsockets.

### Upgrade notes for 5.1.0
Upgrading to 5.1.0 should be no problem in most cases.

Beginning with 5.1.0 the behavior of `Variant<?>` has been altered to get a more consistent behavior.
More about that can be found [here](https://hypfvieh.github.io/dbus-java/variant-handling.html) .

If you used `DBusMap` before, you should use `LinkedHashMap` instead.

All methods previously returned instances of `DBusMap` will now return `LinkedHashMap`.
This is transparent if you didn't use `DBusMap` directly but using `Map` interface instead.

In this version the broken `hashCode()` implementation has been fixed in `DBusPath` allowing proper usage
of `DBusPath` as `Map` key. Before equal objects did not create the same `hashCode()` breaking the contract of `hashCode()` and `equals()`.

### Note to SPI providers
If you have used the SPI to extend the MessageReader/Writer of dbus-java before dbus-java 4.x, you have to update your code.
Old providers will not work with dbus-java 4.x/5.x because of changed SPI interfaces (sorry!).

The changes were required due to the support of native-unixsocket which is using java.nio, while the old dbus-java code
uses the old java.io socket API.

With dbus-java 4.x (and 5.x as well), java.nio is used for all transports and therefore required changes on the SPI.
```ISocketProvider``` will now use ```SocketChannel``` instead of ```Socket``` in the exported methods.

### Note for custom transports
If you previously used a custom transport you have to update your code when switching to dbus-java 5.x.
The `AbstractTransport` base class has been changed and now provides different methods to separate client and listening
(server) connections.

Additionally there is a new method called `closeTransport()` which must be implemented to ensure all used resources
(e.g. `SocketChannel` or `ServerSocketChannel` instance) are properly closed.
Previously a transport should have overridden `close()` and call `super.close()` after closing the transport.
This is now longer needed (`close()` is now final and cannot be overriden) and is replaced by `closeTransport()`.

The `connectImpl()` was previously existing and will now only be called for client side connections.
The new methods `bindImpl()`, `acceptImpl()` and `isBound()` are used for server connections and has been added in dbus-java 5.x.

The reason to provide separate methods was to allow bootstrapping server connections before accepting connections.
In the old implementation `accept()` was usually called in `connectImpl()` and therefore blocked the method until
a client was connected. This blocked setting up the server side before the first client was connecting.
This forced the user to use some random sleep times to wait for the server setup after first client connects.
With the separation no more sleep waits are required.

With the new methods, everything related to setup the server socket should be done in `bindImpl()` including the binding
of the listening socket (calling `bind()` on the server socket). Everything done in this method should not block.
You mustn't call `accept()` on your server socket in `bindImpl()`!

In `acceptImpl()` it is expected that the transport calls `accept()` on its server socket and therefore this method will block.
It must return the `SocketChannel` for each client connected (like `connectImpl()` does).

The `isBound()` method must return the bind status of the server socket. This means for example
server socket is not `null` and server socket is opened.
This method is used by `AbstractTransport` to determine if `bindImpl()` was called before and if the server socket is ready to accept
connections.
