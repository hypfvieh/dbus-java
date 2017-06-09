package org.freedesktop.dbus;

import com.github.hypfvieh.bluetooth.DeviceManager;

/**
 * Subclass this abstract class for creating a callback for changed properties.
 *
 * As soon as your callback is registered by calling {@link DeviceManager#registerPropertyHandler(AbstractPropertiesHandler)},
 * all property changes by Dbus will be visible in the handle(DBusSigHandler) method.
 * method of your callback class.
 *
 * @author hypfvieh
 *
 */
public abstract class AbstractPropertiesHandler implements DBusSigHandler<org.freedesktop.dbus.SignalAwareProperties.PropertiesChanged> {

}
