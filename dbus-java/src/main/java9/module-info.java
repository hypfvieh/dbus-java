module org.freedesktop.dbus {
    exports org.freedesktop.dbus;
    exports org.freedesktop.dbus.bin;
    exports org.freedesktop.dbus.annotations;
    exports org.freedesktop.dbus.connections;
    exports org.freedesktop.dbus.connections.impl;
    exports org.freedesktop.dbus.connections.transports;
    exports org.freedesktop.dbus.errors;
    exports org.freedesktop.dbus.exceptions;
    exports org.freedesktop.dbus.handlers;
    exports org.freedesktop.dbus.interfaces;
    exports org.freedesktop.dbus.messages;
    exports org.freedesktop.dbus.spi;
    exports org.freedesktop.dbus.types;
    exports org.freedesktop.dbus.utils;

    requires java.datatransfer;
    requires java.desktop;
    requires transitive java.xml;
    
    requires transitive org.slf4j;
    requires transitive org.jnrproject.unixsocket;
    requires transitive org.jnrproject.posix;
    requires transitive org.jnrproject.constants;
    requires transitive org.jnrproject.ffi;
    requires transitive org.jnrproject.enxio;

    uses org.freedesktop.dbus.spi.ISocketProvider;
}