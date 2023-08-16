package com.github.hypfvieh.dbus.examples.systemd;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.StructHelper;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Properties;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
 * To deal with this you have two options:<br><br>
 * 1) Stick to Object[] and use the values of it directly.<br>
 *    As the return type is defined as dbus type '{so}', we know that the struct is of type<br>
 *    String and DBusPath. This means: Object[0] = String, Object[1] = DBusPath.<br><br>
 * 2) Use {@link StructHelper#convertToStructList(List, Class)} to create a proper List of structs.<br>
 * </p>
 *
 * @author hypfvieh
 * 
 * @since 2023-02-27
 */
public final class PrintUserSessions {
    private PrintUserSessions() {}

    public static void main(String[] _args) throws IOException, DBusException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        try (DBusConnection sessionConnection = DBusConnectionBuilder.forSystemBus().build()) {

            // fetch properties
            Properties properties = sessionConnection.getRemoteObject("org.freedesktop.login1", "/org/freedesktop/login1/user/_1000", Properties.class);

            // get the 'Sessions', which returns a complex type
            List<Object[]> sessions = properties.Get("org.freedesktop.login1.User", "Sessions");

            // Option 1: Use object array directly:
            for (Object[] us : sessions) {
                System.out.println(us[0] + " --> " + us[1]);
            }
            
            // Option 2: Use StructHelper and appropriate struct class
            List<SessionStruct> convertToStruct = StructHelper.convertToStructList(sessions, SessionStruct.class);
            for (SessionStruct us : convertToStruct) {
                System.out.println(us.getUser() + " --> " + us.getDbusPath());
            }
        }
    }

    public static class SessionStruct extends Struct {
        @Position(0)
        private final String user;
        @Position(1)
        private final DBusPath dbusPath;
        
        public SessionStruct(String _user, DBusPath _dbusPath) {
            user = _user;
            dbusPath = _dbusPath;
        }

        public String getUser() {
            return user;
        }

        public DBusPath getDbusPath() {
            return dbusPath;
        }
        
    }
}
