module org.freedesktop.dbus.transport.jnr {
    requires transitive org.jnrproject.unixsocket;
    requires transitive org.jnrproject.posix;
    requires transitive org.jnrproject.constants;
    requires transitive org.jnrproject.ffi;
    requires transitive org.jnrproject.enxio;

    requires org.freedesktop.dbus;
}