package com.github.hypfvieh.dbus.examples.nested.data;

import java.util.List;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("com.acme.MyInterface")
public interface MyInterface extends DBusInterface {
    
    String sayHello();
    
    List<MyInterfacePart> getParts();
    
    List<String> getPartNames();
}