package sample.issue;
import java.io.IOException;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;

public class ExportClass implements DBusInterface {

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public String getObjectPath() {
        return "/";
    }

    public static void main(String[] args) throws DBusException, InterruptedException, IOException{
        try (DBusConnection conn = DBusConnectionBuilder.forSessionBus().build()) {
            conn.requestBusName( "sample.issue" );

            ExportClass ex = new ExportClass();
            conn.exportObject( "/path", ex);

            System.out.println( "Exported object, waiting" );
            Thread.sleep( 5000 );

            conn.unExportObject( "/path" );
            System.out.println( "Unexported object, waiting" );

            Thread.sleep( 5000 );
        }
    }

}
