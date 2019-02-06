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

import com.github.hypfvieh.common.SearchOrder;
import com.github.hypfvieh.system.NativeLibraryLoader;
import com.github.hypfvieh.util.SystemUtil;
import com.github.hypfvieh.util.TypeUtil;

/**
 * Represents a listening UNIX Socket.
 */
public class UnixServerSocket implements Closeable {
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


    private UnixSocketAddress address = null;
    private boolean           bound   = false;
    private boolean           closed  = false;
    private int               sock;

    /**
    * Create an un-bound server socket.
    */
    public UnixServerSocket() {
    }

    /**
    * Create a server socket bound to the given address.
    * @param _address Path to the socket.
    * @throws IOException on error
    */
    public UnixServerSocket(UnixSocketAddress _address) throws IOException {
        bind(_address);
    }

    /**
    * Create a server socket bound to the given address.
    * @param _address Path to the socket.
    * @throws IOException on error
    */
    public UnixServerSocket(String _address) throws IOException {
        this(new UnixSocketAddress(_address));
    }

    /**
    * Accepts a connection on the ServerSocket.
    * @return A UnixSocket connected to the accepted connection.
    * @throws IOException on error
    */
    public UnixSocket accept() throws IOException {
        int clientSock = native_accept(sock);
        return new UnixSocket(clientSock, address);
    }

    /**
    * Closes the ServerSocket.
    * @throws IOException on error
    */
    public synchronized void close() throws IOException {
        native_close(sock);
        sock = 0;
        closed = true;
        bound = false;
    }

    /**
    * Binds a server socket to the given address.
    * @param _address Path to the socket.
    * @throws IOException on error
    */
    public void bind(UnixSocketAddress _address) throws IOException {
        if (bound)
            close();
        sock = native_bind(_address.getPath(), _address.isAbs());
        bound = true;
        closed = false;
        this.address = _address;
    }

    /**
    * Binds a server socket to the given address.
    * @param _address Path to the socket.
    * @throws IOException on error
    */
    public void bind(String _address) throws IOException {
        bind(new UnixSocketAddress(_address));
    }

    /**
    * Return the address this socket is bound to.
    * @return The UnixSocketAddress if bound or null if unbound.
    */
    public UnixSocketAddress getAddress() {
        return address;
    }

    /**
    * Check the status of the socket.
    * @return True if closed.
    */
    public boolean isClosed() {
        return closed;
    }

    /**
    * Check the status of the socket.
    * @return True if bound.
    */
    public boolean isBound() {
        return bound;
    }

    //CHECKSTYLE:OFF
    private native int native_bind(String address, boolean abs) throws IOException;

    private native void native_close(int sock) throws IOException;

    private native int native_accept(int sock) throws IOException;
    //CHECKSTYLE:ON
}
