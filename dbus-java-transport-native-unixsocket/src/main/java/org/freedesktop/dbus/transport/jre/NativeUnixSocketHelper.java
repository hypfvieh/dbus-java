package org.freedesktop.dbus.transport.jre;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public final class NativeUnixSocketHelper {

    private NativeUnixSocketHelper() {}

    /**
     * Get the UID of peer credentials.
     *
     * @param _sock socket to read from
     * @return UID, -1 if given {@link SocketChannel} was null
     *
     * @throws IOException when socket channel fails to read SO_PEERCRED option
     */
    public static int getUid(SocketChannel _sock) throws IOException {
        // gathering the UID of SO_PEERCRED is currently not possible using pure Java.
        // The code below will only provide the username, not the UID.
        // This does not comply with the DBus-Spec which wants UID.
        return -1;
//        if (_sock == null) {
//            return -1;
//        }
//
//        UnixDomainPrincipal creds = _sock.getOption(ExtendedSocketOptions.SO_PEERCRED);
//        UserPrincipal user = creds.user();
//
//        return Integer.parseInt(user.getName());
    }

}
