module org.freedesktop.dbus {
    exports org.freedesktop.dbus;
    exports org.freedesktop.dbus.bin;
    exports org.freedesktop.dbus.annotations;
    exports org.freedesktop.dbus.config;
    exports org.freedesktop.dbus.connections;
    exports org.freedesktop.dbus.connections.config;
    exports org.freedesktop.dbus.connections.impl;
    exports org.freedesktop.dbus.connections.base;
    exports org.freedesktop.dbus.connections.transports;
    exports org.freedesktop.dbus.connections.shared;
    exports org.freedesktop.dbus.errors;
    exports org.freedesktop.dbus.exceptions;
    exports org.freedesktop.dbus.handlers;
    exports org.freedesktop.dbus.interfaces;
    exports org.freedesktop.dbus.messages;
    exports org.freedesktop.dbus.spi.transport;
    exports org.freedesktop.dbus.spi.message;
    exports org.freedesktop.dbus.types;
    exports org.freedesktop.dbus.utils;
    exports org.freedesktop.dbus.propertyref;

    requires jdk.security.auth;

    requires transitive java.xml;

    requires transitive org.slf4j;

    uses org.freedesktop.dbus.spi.message.ISocketProvider;
    uses org.freedesktop.dbus.spi.transport.ITransportProvider;
}