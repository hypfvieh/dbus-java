/*
 * Java Unix Sockets Library
 *
 * Copyright (c) Matthew Johnson 2004
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * To Contact the author, please email src@matthew.ath.cx
 *
 */
package cx.ath.matthew.unix;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hypfvieh.common.SearchOrder;
import com.github.hypfvieh.system.NativeLibraryLoader;
import com.github.hypfvieh.util.SystemUtil;
import com.github.hypfvieh.util.TypeUtil;

/**
 * Represents a UnixSocket.
 */
public class UnixSocket extends Socket implements Closeable {

    static {
        if (SystemUtil.isMacOs()) {
            String macOsMajorVersion = SystemUtil.getMacOsMajorVersion();
            String[] split = macOsMajorVersion.split("\\.");
            if (split.length == 2 && TypeUtil.isInteger(split[1])) {
                if (Integer.parseInt(split[1]) >= 6) {
                    NativeLibraryLoader.loadLibrary(true, "libunix-java.so", "macos/" + macOsMajorVersion + "/");    
                }
            } else { // cannot determine version, try to load the one hopefully provided by the OS
                NativeLibraryLoader.loadLibrary("libunix-java.so", new SearchOrder[] {SearchOrder.SYSTEM_PATH});
            }
        } else {
            NativeLibraryLoader.loadLibrary(true, "libunix-java.so", "lib/");
        }
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private UnixSocketAddress address   = null;
    private USOutputStream    os        = null;
    private USInputStream     is        = null;
    private boolean           closed    = false;
    private boolean           connected = false;
    private boolean           passcred  = false;
    private int               sock      = 0;
    private boolean           blocking  = true;
    private int               uid       = -1;
    private int               pid       = -1;
    private int               gid       = -1;

    UnixSocket(int _sock, UnixSocketAddress _address) {
        this.sock = _sock;
        this.address = _address;
        this.connected = true;
        this.os = new USOutputStream(_sock, this);
        this.is = new USInputStream(_sock, this);
    }

    /**
    * Create an unconnected socket.
    */
    public UnixSocket() {
    }

    /**
    * Create a socket connected to the given address.
    * @param _address The Unix Socket address to connect to
    * @throws IOException on error
    */
    public UnixSocket(UnixSocketAddress _address) throws IOException {
        connect(_address);
    }

    /**
    * Create a socket connected to the given address.
    * @param _address The Unix Socket address to connect to
    * @throws IOException on error
    */
    public UnixSocket(String _address) throws IOException {
        this(new UnixSocketAddress(_address));
    }

    /**
    * Connect the socket to this address.
    * @param _address The Unix Socket address to connect to
    * @throws IOException on error
    */
    public void connect(UnixSocketAddress _address) throws IOException {
        if (connected)
            close();
        this.sock = native_connect(_address.getPath(), _address.isAbs());
        this.os = new USOutputStream(this.sock, this);
        this.is = new USInputStream(this.sock, this);
        this.address = _address;
        this.connected = true;
        this.closed = false;
        this.is.setBlocking(blocking);
    }

    /**
    * Connect the socket to this address.
    * @param _address The Unix Socket address to connect to
    * @throws IOException on error
    */
    public void connect(String _address) throws IOException {
        connect(new UnixSocketAddress(_address));
    }

    /**
    * Closes the connection.
    * @throws IOException on error
    */
    @Override
    public synchronized void close() throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Closing socket");
        }
        native_close(sock);
        sock = 0;
        this.closed = true;
        this.connected = false;
        os = null;
        is = null;
    }

    /**
    * Returns an InputStream for reading from the socket.
    * @return An InputStream connected to this socket.
    */
    @Override
    public InputStream getInputStream() {
        return is;
    }

    /**
    * Returns an OutputStream for writing to the socket.
    * @return An OutputStream connected to this socket.
    */
    @Override
    public OutputStream getOutputStream() {
        return os;
    }

    /**
    * Returns the address this socket is connected to.
    * Returns null if the socket is unconnected.
    * @return The UnixSocketAddress the socket is connected to
    */
    public UnixSocketAddress getAddress() {
        return address;
    }

    /**
    * Send a single byte of data with credentials.
    * (Works on BSDs)
    * @param data The byte of data to send.
    * @throws IOException on error
    */
    public void sendCredentialByte(byte data) throws IOException {
        if (!connected)
            throw new NotConnectedException();
        native_send_creds(sock, data);
    }

    /**
    * Receive a single byte of data, with credentials.
    * (Works on BSDs)
    * @see getPeerUID
    * @see getPeerPID
    * @see getPeerGID
    * @return read byte
    * @throws IOException on error
    */
    public byte recvCredentialByte() throws IOException {
        if (!connected)
            throw new NotConnectedException();
        int[] creds = new int[] {
                -1, -1, -1
        };
        byte data = native_recv_creds(sock, creds);
        pid = creds[0];
        uid = creds[1];
        gid = creds[2];
        return data;
    }

    /**
    * Get the credential passing status.
    * (only effective on linux)
    * @return The current status of credential passing.
    * @see setPassCred
    */
    public boolean getPassCred() {
        return passcred;
    }

    /**
    * Return the uid of the remote process.
    * Some data must have been received on the socket to do this.
    * Either setPassCred must be called on Linux first, or recvCredentialByte
    * on BSD.
    * @return the UID or -1 if it is not available
    */
    public int getPeerUID() {
        if (-1 == uid)
            uid = native_getUID(sock);
        return uid;
    }

    /**
    * Return the gid of the remote process.
    * Some data must have been received on the socket to do this.
    * Either setPassCred must be called on Linux first, or recvCredentialByte
    * on BSD.
    * @return the GID or -1 if it is not available
    */
    public int getPeerGID() {
        if (-1 == gid)
            gid = native_getGID(sock);
        return gid;
    }

    /**
    * Return the pid of the remote process.
    * Some data must have been received on the socket to do this.
    * Either setPassCred must be called on Linux first, or recvCredentialByte
    * on BSD.
    * @return the PID or -1 if it is not available
    */
    public int getPeerPID() {
        if (-1 == pid)
            pid = native_getPID(sock);
        return pid;
    }

    /**
    * Set the credential passing status.
    * (Only does anything on linux, for other OS, you need
    * to use send/recv credentials)
    * @param enable Set to true for credentials to be passed.
    * @throws IOException on error
    */
    public void setPassCred(boolean enable) throws IOException {
        native_set_pass_cred(sock, enable);
        passcred = enable;
    }

    /**
    * Get the blocking mode.
    * @return true if reads are blocking.
    * @see setBlocking
    */
    public boolean getBlocking() {
        return blocking;
    }

    /**
    * Set the blocking mode.
    * @param enable Set to false for non-blocking reads.
    */
    public void setBlocking(boolean enable) {
        blocking = enable;
        if (null != is)
            is.setBlocking(enable);
    }

    /**
    * Check the socket status.
    * @return true if closed.
    */
    @Override
    public boolean isClosed() {
        return closed;
    }

    /**
    * Check the socket status.
    * @return true if connected.
    */
    @Override
    public boolean isConnected() {
        return connected;
    }

    /**
    * Check the socket status.
    * @return true if the input stream has been shutdown
    */
    @Override
    public boolean isInputShutdown() {
        return is.isClosed();
    }

    /**
    * Check the socket status.
    * @return true if the output stream has been shutdown
    */
    @Override
    public boolean isOutputShutdown() {
        return os.isClosed();
    }

    /**
    * Shuts down the input stream.
    * Subsequent reads on the associated InputStream will fail.
    */
    @Override
    public void shutdownInput() {
        is.setClosed(true);
    }

    /**
    * Shuts down the output stream.
    * Subsequent writes to the associated OutputStream will fail.
    */
    @Override
    public void shutdownOutput() {
        os.setClosed(true);
    }

    /**
    * Set timeout of read requests.
    * @param timeout to set
    */
    @Override
    public void setSoTimeout(int timeout) {
        is.setSoTimeout(timeout);
    }

    // CHECKSTYLE:OFF
    private native void native_set_pass_cred(int sock, boolean passcred) throws IOException;

    private native int native_connect(String address, boolean abs) throws IOException;

    private native void native_close(int sock) throws IOException;

    private native int native_getPID(int sock);

    private native int native_getUID(int sock);

    private native int native_getGID(int sock);

    private native void native_send_creds(int sock, byte data) throws IOException;

    private native byte native_recv_creds(int sock, int[] creds) throws IOException;
    // CHECKSTYLE:ON
}
