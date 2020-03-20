# Code Generation

Since DBus provides a mechanism for introspecting objects via the
`org.freedesktop.DBus.Introspectable` interface, it is possible to query an
already running application for information about interfaces that it has.  This
will then return an XML file, which you can then run the code generator against
in order to produce a Java interface that you can then use to get a remote
object, or use as an interface for your program.

1. Obtain the introspection XML file from the service that you wish to use or
  implement.  These introspection XML files are often in the source code of
  the reference application.  If the application is running on DBus, you can
  obtain the introspection XML using a dbus-send query such as the following
  (change the 'dest' and path as appropriate):

        dbus-send --print-reply=literal --type=method_call --dest=org.freedesktop.Notifications /org/freedesktop/Notifications org.freedesktop.DBus.Introspectable.Introspect

2. Run the code generator on the given XML file.

        org.freedesktop.dbus.bin.CreateInterface /path/to/introspection.xml

3. The code will be printed to stdout; copy and paste it into a file.
