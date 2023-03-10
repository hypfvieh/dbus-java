package com.github.hypfvieh.dbus.examples.systemd;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Properties;

import java.io.IOException;
import java.util.List;

/**
 * Demonstrates the usage of {@link Properties} to get a complex type.<br>
 * <p>
 * In this case, the example tries to fetch the Sessions of the user with UID 1000.
 * Systemd will provide the sessions from the user as part of the properties of
 * the org.freedesktop.login1.User interface.
 * </p>
 * <p>
 * The returned properties are defined as array of struct.
 * Sadly, Java is not capable of transforming the array of struct to proper classes
 * due to type erasure.
 * </p>
 * <p>
 * The {@link Properties} interface will return a Variant&lt;?&gt; when {@link Properties#Get(String, String)}
 * is used. The concrete Variant returned for 'Sessions' is Variant&lt;List&lt;Object[]&gt;&gt;.
 * The Variant-part will be resolved by dbus-java, so a List&lt;Object[]&gt; will remain.
 * </p>
 * <p>
 * Due to type erasure, there is no information about which type the List should contain.
 * Therefore dbus-java cannot convert the Object[] in the List to a Struct/Tuple or any other
 * suitable type.
 * </p>
 * <p>
 * The only way to deal with that is to stick to Object[] and use the values of it directly.
 * As the return type is defined as dbus type '{so}', we know that the struct is of type
 * String and DBusPath. This means: Object[0] = String, Object[1] = DBusPath.
 * </p>
 *
 * @author hypfvieh
 * @since 2023-02-27
 */
public final class PrintUserSessions {
    private PrintUserSessions() {}

    public static void main(String[] _args) throws IOException, DBusException {
        try (DBusConnection sessionConnection = DBusConnectionBuilder.forSystemBus().build()) {

            // fetch properties
            Properties properties = sessionConnection.getRemoteObject("org.freedesktop.login1", "/org/freedesktop/login1/user/_1000", Properties.class);

            // get the 'Sessions', which returns a complex type
            List<Object[]> sessions = properties.Get("org.freedesktop.login1.User", "Sessions");

            // print all sessions
            for (Object[] us : sessions) {
                System.out.println(us[0] + " --> " + us[1]);
            }
        }
    }

}
