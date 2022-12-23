package com.github.hypfvieh.dbus.examples.nested.data;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("com.acme.MyInterfacePart")
public interface MyInterfacePart extends DBusInterface {

    String getVal1();

    String getVal2();
}
