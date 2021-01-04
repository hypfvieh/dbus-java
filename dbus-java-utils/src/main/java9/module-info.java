module org.freedesktop.dbus.utils {
    exports org.freedesktop.dbus.utils.bin;
    exports org.freedesktop.dbus.utils.generator;
    exports org.freedesktop.dbus.viewer;
    requires transitive org.freedesktop.dbus;
    requires java.xml;
    requires java.desktop;
}