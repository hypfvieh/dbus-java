package com.github.hypfvieh.dbus.examples.properties;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("com.acme.InterfaceWithProperties")
public interface InterfaceWithProperties extends DBusInterface {

    enum Color {
        RED, GREEN, BLUE
    }

    String sayHello();

    int getJustAnInteger();

    @DBusProperty(access = Access.READ, name = "MyProperty")
    String getMyProperty();

    @DBusProperty(access = Access.WRITE, name = "MyProperty")
    void setMyProperty(String _property);

    @DBusProperty(access = Access.READ, name = "ZZZZZZZ")
    long getMyAltProperty();

    @DBusProperty(access = Access.WRITE, name = "ZZZZZZZ")
    void setMyAltProperty(long _property);

    @DBusProperty
    boolean isMyOtherProperty();

    @DBusProperty
    void setMyOtherProperty(boolean  _property);

    @DBusProperty
    Color getColor();

    @DBusProperty
    void setColor(Color _color);
}
