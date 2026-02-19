package org.freedesktop.dbus.transport.jre;

import jdk.net.ExtendedSocketOptions;
import jdk.net.UnixDomainPrincipal;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.file.attribute.UserPrincipal;

public final class NativeUnixSocketHelper {

    private NativeUnixSocketHelper() {}

    /**
     * Get the UID of peer credentials.
     * <p>
     * Gathering the UID of SO_PEERCRED directly is not obvious when it comes to JDK native unix sockets.<br>
     * based on the implementation in {@code sun.nio.fs.UnixUserPrincipals.User},<br>
     * calling {@code hashCode()} on the {@link UserPrincipal} will give you either the UID or the hashCode of the name.
     * </p><p>
     * This method ensures that a proper UID is returned and not the hashCode of the name.
     * If there is no UID, -1 is returned.
     * </p>
     *
     * @param _sock socket to read from
     * @return UID, -1 if given {@link SocketChannel} was {@code null} or UID could not be determined
     *
     * @throws IOException when socket channel fails to read SO_PEERCRED option
     */
    public static int getUid(SocketChannel _sock) throws IOException {
        if (_sock == null) {
            return -1;
        }

        UnixDomainPrincipal creds = _sock.getOption(ExtendedSocketOptions.SO_PEERCRED);
        UserPrincipal user = creds.user();

        int uid = -1;
        if (user != null && user.hashCode() != user.getName().hashCode()) {
            uid = user.hashCode();
        }

        return uid;
    }

}
