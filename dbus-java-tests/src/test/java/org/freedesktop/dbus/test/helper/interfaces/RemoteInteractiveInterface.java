package org.freedesktop.dbus.test.helper.interfaces;

import org.freedesktop.dbus.annotations.MethodAllowInteractiveAutorization;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.constants.Flags;

/**
 * Interface for testing {@link Flags#ALLOW_INTERACTIVE_AUTHORIZATION} flag.
 */
public interface RemoteInteractiveInterface extends DBusInterface {
    @MethodAllowInteractiveAutorization
    void interactiveMethod();
}
