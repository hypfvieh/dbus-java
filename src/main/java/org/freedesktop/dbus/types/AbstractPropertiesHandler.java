package org.freedesktop.dbus.types;

import org.freedesktop.dbus.interfaces.DBusSigHandler;


/**
 * Subclass this abstract class for creating a callback for changed properties.
 *
 * As soon as your callback is registered by calling {@link DeviceManager#registerPropertyHandler(AbstractPropertiesHandler)},
 * all property changes by Dbus will be visible in the handle(DBusSigHandler) method.
 * method of your callback class.
 */
public abstract class AbstractPropertiesHandler implements DBusSigHandler<org.freedesktop.dbus.interfaces.SignalAwareProperties.PropertiesChanged> {

}
