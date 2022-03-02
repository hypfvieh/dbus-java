package com.github.hypfvieh.dbus.examples.struct;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

/**
 * Interface for sample struct server.
 *
 * @author hypfvieh
 */
@DBusInterfaceName("hypfvieh.sample.StructServer")
public interface IStructServer extends DBusInterface {
    void setStructFromVariant(Variant<?> _variant);
    void setStructDirectly(SampleStruct _struct);
}
