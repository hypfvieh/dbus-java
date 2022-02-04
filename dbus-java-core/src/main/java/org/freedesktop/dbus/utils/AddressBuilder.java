package org.freedesktop.dbus.utils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.freedesktop.dbus.exceptions.AddressResolvingException;

public class AddressBuilder {
    public static final String DEFAULT_SYSTEM_BUS_ADDRESS = "unix:path=/var/run/dbus/system_bus_socket";
    private static final String DBUS_MACHINE_ID_SYS_VAR = "DBUS_MACHINE_ID_LOCATION";

    /**
     * Determine the address of the DBus system connection.
     *
     * @return String
     */
    public static String getSystemConnection() {
        String bus = System.getenv("DBUS_SYSTEM_BUS_ADDRESS");
        if (bus == null) {
            bus = DEFAULT_SYSTEM_BUS_ADDRESS;
        }
        return bus;
    }

    /**
     * Retrieves the connection address to connect to the DBus session-bus.
     * @param _dbusMachineIdFile alternative location of dbus machine id file, use null if not needed
     * @return address
     */
    public static String getSessionConnection(String _dbusMachineIdFile) {
        String s = null;

        // MacOS support: e.g DBUS_LAUNCHD_SESSION_BUS_SOCKET=/private/tmp/com.apple.launchd.4ojrKe6laI/unix_domain_listener
        if (Util.isMacOs()) {
            s = "unix:path=" + System.getenv("DBUS_LAUNCHD_SESSION_BUS_SOCKET");
        } else { // all others (linux)
            s = System.getenv("DBUS_SESSION_BUS_ADDRESS");
        }

        if (s == null) {
            // address gets stashed in $HOME/.dbus/session-bus/`dbus-uuidgen --get`-`sed 's/:\(.\)\..*/\1/' <<<
            // $DISPLAY`
            String display = System.getenv("DISPLAY");
            if (null == display) {
                throw new AddressResolvingException("Cannot Resolve Session Bus Address");
            }
            if (!display.startsWith(":") && display.contains(":")) { // display seems to be a remote display
                                                                     // (e.g. X forward through SSH)
                display = display.substring(display.indexOf(':'));
            }

            String uuid = getDbusMachineId(_dbusMachineIdFile);
            String homedir = System.getProperty("user.home");
            File addressfile = new File(homedir + "/.dbus/session-bus",
                    uuid + "-" + display.replaceAll(":([0-9]*)\\..*", "$1"));
            
            if (!addressfile.exists()) {
                throw new AddressResolvingException("Cannot Resolve Session Bus Address");
            }
            
            Properties readProperties = Util.readProperties(addressfile);
            String sessionAddress = readProperties.getProperty("DBUS_SESSION_BUS_ADDRESS");

            if (Util.isEmpty(sessionAddress)) {
                throw new AddressResolvingException("Cannot Resolve Session Bus Address");
            }

            // sometimes (e.g. Ubuntu 18.04) the returned address is wrapped in single quotes ('), we have to remove them
            if (sessionAddress.matches("^'[^']+'$")) {
                sessionAddress = sessionAddress.replaceFirst("^'([^']+)'$", "$1");
            }

            return sessionAddress;
        }

        return s;
    }

    /**
     * Extracts the machine-id usually found on Linux in various system directories, or
     * generate a fake id for non-Linux platforms. 
     *
     * @param _dbusMachineIdFile alternative location of dbus machine id file, null if not needed
     * @return machine-id string, never null
     */
    public static String getDbusMachineId(String _dbusMachineIdFile) {
        File uuidfile = determineMachineIdFile(_dbusMachineIdFile);
        if(uuidfile != null) {
            String uuid = Util.readFileToString(uuidfile);
            if (uuid.length() > 0) {
                return uuid;
            } else {
                throw new AddressResolvingException("Cannot Resolve Session Bus Address: MachineId file is empty.");
            }
        }
        if (Util.isWindows() || Util.isMacOs()) {
            /* Linux *should* have a machine-id */
            return getFakeDbusMachineId();
        }
        throw new AddressResolvingException("Cannot Resolve Session Bus Address: MachineId file can not be found");
    }

    /**
     * Tries to find the DBus machine-id file in different locations.
     *
     * @param _dbusMachineIdFile alternative location of dbus machine id file
     * 
     * @return File with machine-id
     */
    private static File determineMachineIdFile(String _dbusMachineIdFile) {
        List<String> locationPriorityList = Arrays.asList(System.getenv(DBUS_MACHINE_ID_SYS_VAR), _dbusMachineIdFile,
                "/var/lib/dbus/machine-id", "/usr/local/var/lib/dbus/machine-id", "/etc/machine-id");
        return locationPriorityList.stream()
                .filter(s -> s != null)
                .map(s -> new File(s))
                .filter(f -> f.exists() && f.length() > 0)
                .findFirst()
                .orElse(null);
    }

    /**
     * Generates a fake machine-id when DBus is running on Windows.
     * @return String
     */
    private static String getFakeDbusMachineId() {
        // we create a fake id on windows
        return String.format("%s@%s", Util.getCurrentUser(), Util.getHostName());
    }

}
