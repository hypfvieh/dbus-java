# dbus-java

dbus-java is a pure-Java implementation of the [D-Bus](https://www.freedesktop.org/wiki/Software/dbus/)
protocol. D-Bus is a message bus system used on most Linux systems for inter-process communication,
for example to talk to system services such as NetworkManager, systemd, BlueZ or UPower, or to
exchange messages between applications on the user's session bus.

With dbus-java you can:

 * connect to the SESSION or SYSTEM bus (or a custom address),
 * call methods on remote objects exported by other applications,
 * export your own objects so other applications can call them,
 * send and receive signals,
 * read and write properties,
 * run an embedded D-Bus daemon for testing.

If you are new here, start with the **[Quickstart](./quick-start.html)**.

## Module overview

dbus-java is split into a small core library plus interchangeable transport modules. Pick the
core plus exactly one transport that fits your platform; the other modules are optional.

|Module|Purpose|
|------|-------|
|`dbus-java-core`|The main library (connections, marshalling, message handling). Always required.|
|`dbus-java-transport-native-unixsocket`|Unix socket transport using the JDK's native unix domain socket support (Java 16+). No extra native dependency.|
|`dbus-java-transport-junixsocket`|Unix socket transport using [junixsocket](https://github.com/kohlschutter/junixsocket). Supports file descriptor passing out of the box.|
|`dbus-java-transport-jnr-unixsocket`|Unix socket transport using [JNR](https://github.com/jnr/jnr-unixsocket).|
|`dbus-java-transport-tcp`|TCP transport (mostly for testing or remote buses).|
|`dbus-java-utils`|Tools, most notably the `InterfaceCodeGenerator` (see [Code Generation](./code-generation.html)).|
|`dbus-java-bom`|A Maven Bill-of-Materials to keep the module versions aligned.|

A typical dependency set is `dbus-java-core` + one transport. See the
[Quickstart](./quick-start.html) for a ready-to-copy Maven snippet, and the
[README](https://github.com/hypfvieh/dbus-java#how-to-use-file-descriptors) for guidance on
choosing a transport (for example when you need file descriptor support).

### Transports that are not provided

The `unixexec:` transport is intentionally not provided. Its model (spawning a helper and
speaking D-Bus over that process' stdin/stdout) does not fit dbus-java's `SocketChannel`-based
transport architecture, and the practical use cases are too rare to justify the effort. For the
most common scenario - tunnelling D-Bus over SSH - a separate, third-party transport based on
SSHj already exists.

The `launchd:` transport (macOS-only) is not provided either. It merely looks up the actual
session bus socket via macOS' `launchd`; dbus-java already supports this indirectly by honouring
the `DBUS_LAUNCHD_SESSION_BUS_SOCKET` environment variable, so a dedicated transport would add
little.

### Session bus discovery and `autolaunch`

When connecting to the session bus, dbus-java resolves the address from (in order) the
`DBUS_SESSION_BUS_ADDRESS` system property, the `DBUS_SESSION_BUS_ADDRESS` environment variable
(on macOS also `DBUS_LAUNCHD_SESSION_BUS_SOCKET`), and finally the classic
`$HOME/.dbus/session-bus/<machine-id>-<display>` session file. If none of these yield an address,
the connection fails.

The reference implementations additionally support `autolaunch:`, which auto-starts a session bus
daemon on demand (via the external `dbus-launch` helper and an X11 root-window property on Linux,
or a platform-native mechanism on Windows). dbus-java intentionally does not do this: it would
require external helpers / platform-native code outside the scope of a pure-Java library, and
silently spawning a bus daemon is undesirable for a client library. If you need an in-process bus,
start an [`EmbeddedDBusDaemon`](https://github.com/hypfvieh/dbus-java/tree/master/dbus-java-examples)
explicitly.

## Where to go next

 * [Quickstart](./quick-start.html) - add the dependencies and open a connection
 * [DBus Types](./dbus-types.html) - how D-Bus types map to Java types
 * [Exporting Objects](./exporting-objects.html) - make your objects callable on the bus
 * [Calling Remote Objects](./remote-objects.html) - call methods on other applications
 * [Properties](./properties.html) - expose and consume D-Bus properties
 * [Variant](./variant-handling.html) - work with the `Variant<?>` wrapper type
 * [Signals](./using-signals.html) - send and receive signals
 * [Code Generation](./code-generation.html) - generate interfaces from introspection data
 * [Howto...](./howto.html) - task-oriented links into the example code

## Further resources

 * **Javadoc** - the API reference, linked from the project [README](https://github.com/hypfvieh/dbus-java)
   and available on [javadoc.io](https://javadoc.io/doc/com.github.hypfvieh/dbus-java-core)
 * **Examples** - runnable code in the
   [dbus-java-examples](https://github.com/hypfvieh/dbus-java/tree/master/dbus-java-examples) module
 * **Wiki** - additional articles in the [GitHub Wiki](https://github.com/hypfvieh/dbus-java/wiki)
