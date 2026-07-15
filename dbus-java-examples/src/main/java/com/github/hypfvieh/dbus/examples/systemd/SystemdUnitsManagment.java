package com.github.hypfvieh.dbus.examples.systemd;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.annotations.MethodAllowInteractiveAutorization;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * Demonstrates how to use {@link MethodAllowInteractiveAutorization} to call
 * systemd methods that may require Polkit authentication.<br>
 * <p>
 * In this example, we connect to the systemd Manager interface and call
 * {@code StartUnit} or {@code StopUnit}. These methods typically require
 * administrative privileges and may trigger an interactive Polkit dialog
 * when called by an unprivileged user.
 * </p>
 * <p>
 * The {@link MethodAllowInteractiveAutorization} annotation tells the D-Bus
 * daemon that the caller is prepared to wait for interactive authorization
 * (e.g., password prompt). Without this annotation, such calls may fail
 * with a {@code DBusExecutionException: Interactive authentication required}.
 * </p>
 * <p>
 * The interface {@link SimpleSystemdManagerInterface} uses the
 * {@link DBusInterfaceName} annotation to specify the D-Bus interface name,
 * and {@link DBusMemberName} to map Java method names to D-Bus method names.
 * </p>
 *
 * @author unfamiliarS
 *
 * @see MethodAllowInteractiveAutorization
 * @see DBusInterfaceName
 * @see DBusMemberName
 *
 * @since 2026-07-15
 */
public final class SystemdUnitsManagment {
    private SystemdUnitsManagment() {}

    public static void main(String[] _args) throws DBusException {

        try (DBusConnection sessionConnection = DBusConnectionBuilder.forSystemBus().build()) {

            SimpleSystemdManagerInterface sysdManager = sessionConnection.getRemoteObject(
                "org.freedesktop.systemd1",
                "/org/freedesktop/systemd1",
                SimpleSystemdManagerInterface.class
            );

            System.out.println(sysdManager.stopUnit("httpd.service", "replace"));

        }
    }

    @DBusInterfaceName("org.freedesktop.systemd1.Manager")
    interface SimpleSystemdManagerInterface extends DBusInterface {

        @DBusMemberName(value = "StartUnit")
        @MethodAllowInteractiveAutorization
        DBusPath startUnit(String _name, String _mode);

        @DBusMemberName(value = "StopUnit")
        @MethodAllowInteractiveAutorization
        DBusPath stopUnit(String _name, String _mode);
    }
}
