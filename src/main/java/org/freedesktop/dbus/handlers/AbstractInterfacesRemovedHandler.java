package org.freedesktop.dbus.handlers;

import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.interfaces.DBusSigHandler;


/**
* Subclass this abstract class for creating a callback for InterfaceRemoved signal provided by DBus ObjectManager.
*
* As soon as your callback is registered by calling {@link AbstractConnection#addSigHandler(Class, DBusSigHandler)},
* all property changes by Dbus will be visible in the handle(DBusSigHandler) method of your callback class.
*/
public abstract class AbstractInterfacesRemovedHandler implements DBusSigHandler<org.freedesktop.dbus.interfaces.ObjectManager.InterfacesRemoved> {

}
