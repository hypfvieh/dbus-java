package com.github.hypfvieh.dbus.examples.nested.data;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

import java.util.List;

@DBusInterfaceName("com.acme.MyInterface")
public interface MyInterface extends DBusInterface {

    String sayHello();

    List<MyInterfacePart> getParts();

    List<String> getPartNames();
}
