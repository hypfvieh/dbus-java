package org.freedesktop.dbus.transport.jnr;

import jnr.constants.platform.Errno;
import jnr.constants.platform.LastError;
import jnr.constants.platform.SocketLevel;
import jnr.ffi.StructLayout;
import jnr.posix.CmsgHdr;
import jnr.posix.MsgHdr;
import jnr.posix.NativePOSIX;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import jnr.posix.util.Platform;
import jnr.unixsocket.UnixSocket;
import jnr.unixsocket.UnixSocketChannel;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Helpers to support FreeBSD. Ideally some of these will be abstracted
 * and moved to jnr-unixsocket at some point.
 *
 * @author grembo
 * @deprecated no longer used
 */
@Deprecated(forRemoval = true, since = "4.2.2 - 2022-12-23")
public final class FreeBSDHelper {
    private static final CmsgCredLayout CMSG_CRED_LAYOUT = new CmsgCredLayout(jnr.ffi.Runtime.getSystemRuntime());

    private FreeBSDHelper() {}

    public static boolean isFreeBSD() {
        return Platform.IS_FREEBSD;
    }

    @SuppressWarnings({"checkstyle:methodname"})
    public static void send_cred(Socket _us) throws IOException {
        POSIX posix = POSIXFactory.getNativePOSIX();
        String data = "\0";
        byte[] dataBytes = data.getBytes();

        MsgHdr outMessage = posix.allocateMsgHdr();

        ByteBuffer[] outIov = new ByteBuffer[1];
        outIov[0] = ByteBuffer.allocateDirect(dataBytes.length);
        outIov[0].put(dataBytes);
        outIov[0].flip();

        outMessage.setIov(outIov);

        CmsgHdr outControl = outMessage.allocateControl(CMSG_CRED_LAYOUT.size());
        outControl.setLevel(SocketLevel.SOL_SOCKET.intValue());
        outControl.setType(0x03); // 0x03 == SCM_CREDS

        ByteBuffer fdBuf = ByteBuffer.allocateDirect(CMSG_CRED_LAYOUT.size());
        fdBuf.order(ByteOrder.nativeOrder());
        // fdBuf.putInt(i, 0);
        outControl.setData(fdBuf);

        int fd = ((UnixSocketChannel) ((UnixSocket) _us).getChannel()).getFD();
        int sentBytes = -1;
        do {
            sentBytes = posix.sendmsg(fd, outMessage, 0);
        } while (sentBytes < 0 && Errno.valueOf(posix.errno()) == Errno.EINTR);

        if (sentBytes < 0) {
            long err = posix.errno();
            /* This might fail with EINVAL if the socket isn't AF_UNIX */
            if (Errno.valueOf(err) == Errno.EINVAL) {
                _us.getOutputStream().write(dataBytes);
            } else {
                throw new IOException("Failed to write credentials byte: "
                        + LastError.valueOf(err).toString());
            }
        } else if (sentBytes == 0) {
            throw new IOException("wrote zero bytes writing credentials byte");
        }
    }

    @SuppressWarnings({"checkstyle:methodname"})
    public static long recv_cred(Socket _us) {
        NativePOSIX posix = (NativePOSIX) POSIXFactory.getNativePOSIX();
        MsgHdr inMessage = posix.allocateMsgHdr();
        ByteBuffer[] inIov = new ByteBuffer[1];
        inIov[0] = ByteBuffer.allocateDirect(1);
        inMessage.setIov(inIov);
        // CmsgHdr inControl = inMessage.allocateControl(cmsgCredLayout.size());

        int fd = ((UnixSocketChannel) ((UnixSocket) _us).getChannel()).getFD();
        int recvBytes = -1;
        do {
            recvBytes = posix.recvmsg(fd, inMessage, 0);
        }

        while (recvBytes < 0 && Errno.valueOf(posix.errno()) == Errno.EINTR);

        if (recvBytes > 0 && inIov[0].get(0) == 0) {
            for (CmsgHdr cmsg : inMessage.getControls()) {
                if (cmsg.getType() == 0x03 // 0x03 == SCM_CREDS
                        && cmsg.getLevel() == SocketLevel.SOL_SOCKET.intValue()
                        && cmsg.getLen() >= posix.socketMacros().CMSG_LEN(84)) {
                    ByteBuffer data = cmsg.getData();
                    final jnr.ffi.Pointer memory = jnr.ffi.Runtime.getSystemRuntime().getMemoryManager()
                            .allocateTemporary(CMSG_CRED_LAYOUT.size(), true);
                    for (int i = 0; i < CMSG_CRED_LAYOUT.size(); ++i) {
                        memory.putByte(i, data.get(i));
                    }
                    return CMSG_CRED_LAYOUT.cmcred_euid.get(memory);
                }
            }
        }
        return -1;
    }

    @SuppressWarnings({"checkstyle:membername", "checkstyle:visibilitymodifier"})
    static class CmsgCredLayout extends StructLayout {

        final pid_t    cmcred_pid     = new pid_t();
        final uid_t    cmcred_uid     = new uid_t();
        final uid_t    cmcred_euid    = new uid_t();
        final gid_t    cmcred_gid     = new gid_t();
        final Signed16 cmcred_ngroups = new Signed16();
        final gid_t[]  cmcred_groups  = array(new gid_t[16]);

        CmsgCredLayout(jnr.ffi.Runtime _runtime) {
            super(_runtime);
        }

    }
}
