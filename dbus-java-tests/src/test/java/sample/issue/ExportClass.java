package sample.issue;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.slf4j.LoggerFactory;

public class ExportClass implements DBusInterface {

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public String getObjectPath() {
        return "/";
    }

    public static void main(String[] _args) throws DBusException, InterruptedException {
        try (DBusConnection conn = DBusConnectionBuilder.forSessionBus().build()) {
            conn.requestBusName("sample.issue");

            ExportClass ex = new ExportClass();
            conn.exportObject("/path", ex);

            LoggerFactory.getLogger(ExportClass.class).debug("Exported object, waiting");
            Thread.sleep(5000);

            conn.unExportObject("/path");
            LoggerFactory.getLogger(ExportClass.class).debug("Unexported object, waiting");

            Thread.sleep(5000);
        }
    }

}
