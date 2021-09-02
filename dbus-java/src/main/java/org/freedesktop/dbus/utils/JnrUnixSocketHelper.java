package org.freedesktop.dbus.utils;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import jnr.unixsocket.Credentials;
import jnr.unixsocket.UnixSocketOptions;

public class JnrUnixSocketHelper {


    /**
     * Get the UID of peer credentials.
     *
     * @param _sock socket to read from
     * @return UID, -1 if given {@link SocketChannel} was null
     *
     * @throws IOException when socket channel fails to read SO_PEERCRED option
     */
    public static int getUid(SocketChannel _sock) throws IOException {
        if (_sock == null) {
            return -1;
        }
        Credentials credentials = _sock.getOption(UnixSocketOptions.SO_PEERCRED);
        return credentials.getUid();
    }

}
