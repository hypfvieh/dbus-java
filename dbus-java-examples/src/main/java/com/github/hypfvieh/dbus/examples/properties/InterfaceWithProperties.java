package com.github.hypfvieh.dbus.examples.properties;

import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("com.acme.InterfaceWithProperties")
public interface InterfaceWithProperties extends DBusInterface {

    enum Color {
        RED, GREEN, BLUE
    }

    String sayHello();

    int getJustAnInteger();

    @DBusBoundProperty(access = Access.READ, name = "MyProperty")
    String getMyProperty();

    @DBusBoundProperty(access = Access.WRITE, name = "MyProperty")
    void setMyProperty(String _property);

    @DBusBoundProperty(access = Access.READ, name = "ZZZZZZZ")
    long getMyAltProperty();

    @DBusBoundProperty(access = Access.WRITE, name = "ZZZZZZZ")
    void setMyAltProperty(long _property);

    @DBusBoundProperty
    boolean isMyOtherProperty();

    @DBusBoundProperty
    void setMyOtherProperty(boolean  _property);

    @DBusBoundProperty
    Color getColor();

    @DBusBoundProperty
    void setColor(Color _color);
}
