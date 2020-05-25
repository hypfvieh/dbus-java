package org.freedesktop.dbus.connections;

import java.io.IOException;
import java.net.Socket;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import jnr.ffi.StructLayout;

import jnr.posix.POSIXFactory;

import jnr.constants.platform.*;
import jnr.posix.CmsgHdr;
import jnr.posix.MsgHdr;
import jnr.posix.NativePOSIX;
import jnr.posix.POSIXFactory;
import jnr.posix.util.Platform;

import jnr.unixsocket.UnixSocket;
import jnr.unixsocket.UnixSocketChannel;

/**
 * Helpers to support FreeBSD. Ideally some of these will be abstracted
 * and moved to jnr-unixsocket at some point.
 *
 * @author grembo
 */
public class FreeBSDHelper {

    public static boolean isFreeBSD() {
        return Platform.IS_FREEBSD;
    }

    public static class CmsgCredLayout extends StructLayout {
        protected CmsgCredLayout(jnr.ffi.Runtime runtime) {
            super(runtime);
        }

	public final pid_t cmcred_pid = new pid_t();
	public final uid_t cmcred_uid = new uid_t();
	public final uid_t cmcred_euid = new uid_t();
	public final gid_t cmcred_gid = new gid_t();
	public final Signed16 cmcred_ngroups = new Signed16();
	public final gid_t[] cmcred_groups = array(new gid_t[16]);
    }
    public static final CmsgCredLayout cmsgCredLayout = new CmsgCredLayout(jnr.ffi.Runtime.getSystemRuntime());

    public static void send_cred(Socket us) throws java.io.IOException {
        NativePOSIX posix = (NativePOSIX) POSIXFactory.getNativePOSIX();
        String data = "\0";
        byte[] dataBytes = data.getBytes();

        MsgHdr outMessage = posix.allocateMsgHdr();

        ByteBuffer[] outIov = new ByteBuffer[1];
        outIov[0] = ByteBuffer.allocateDirect(dataBytes.length);
        outIov[0].put(dataBytes);
        outIov[0].flip();

        outMessage.setIov(outIov);

        CmsgHdr outControl = outMessage.allocateControl(cmsgCredLayout.size());
        outControl.setLevel(SocketLevel.SOL_SOCKET.intValue());
        outControl.setType(0x03); // 0x03 == SCM_CREDS

        ByteBuffer fdBuf = ByteBuffer.allocateDirect(cmsgCredLayout.size());
        fdBuf.order(ByteOrder.nativeOrder());
        //fdBuf.putInt(i, 0);
        outControl.setData(fdBuf);

        int fd = ((UnixSocketChannel)((UnixSocket) us).getChannel()).getFD();
        int sentBytes = -1;
        do
        {
            sentBytes = posix.sendmsg(fd, outMessage, 0);
        }
        while (sentBytes < 0 && Errno.EINTR.equals(posix.errno()));

        if (sentBytes < 0)
        {
            long err = posix.errno();
            /* This might've fail with EINVAL if the socket isn't AF_UNIX */
            if (Errno.EINVAL.equals(err))
            {
                us.getOutputStream().write(dataBytes);
            }
            else
            {
                throw new IOException("Failed to write credentials byte: " +
                    LastError.valueOf(err).toString());
            }
        }
        else if (sentBytes == 0)
        {
            throw new IOException("wrote zero bytes writing credentials byte");
        }
    }

    public static long recv_cred(Socket us) {
        NativePOSIX posix = (NativePOSIX) POSIXFactory.getNativePOSIX();
        MsgHdr inMessage = posix.allocateMsgHdr();
        ByteBuffer[] inIov = new ByteBuffer[1];
        inIov[0] = ByteBuffer.allocateDirect(1);
        inMessage.setIov(inIov);
        CmsgHdr inControl = inMessage.allocateControl(cmsgCredLayout.size());

        int fd = ((UnixSocketChannel)((UnixSocket) us).getChannel()).getFD();
        int recvBytes = -1;
        do
        {
            recvBytes = posix.recvmsg(fd, inMessage, 0);
        }
        while (recvBytes < 0 && Errno.EINTR.equals(posix.errno()));

        if (recvBytes > 0 && inIov[0].get(0) == 0) {
            for (CmsgHdr cmsg : inMessage.getControls()) {
                if (cmsg.getType() == 0x03 && // 0x03 == SCM_CREDS
                    cmsg.getLevel() == SocketLevel.SOL_SOCKET.intValue() &&
                    cmsg.getLen() >= posix.socketMacros().CMSG_LEN(84))
                {
                    ByteBuffer data = cmsg.getData();
                    final jnr.ffi.Pointer memory = jnr.ffi.Runtime.getSystemRuntime().getMemoryManager().allocateTemporary(cmsgCredLayout.size(), true);
                    for (int i=0; i<cmsgCredLayout.size(); ++i) {
                        memory.putByte(i, data.get(i));
                    }
                    return cmsgCredLayout.cmcred_euid.get(memory);
                }
            }
        }
        return -1;
    }
}
