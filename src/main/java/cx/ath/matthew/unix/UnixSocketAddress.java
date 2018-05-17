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

/**
 * Represents an address for a Unix Socket
 */
public class UnixSocketAddress {
    private String  path;
    private boolean abs;

    /**
    * Create the address.
    * @param _path The path to the Unix Socket.
    * @param _abstract True if this should be an abstract socket.
    */
    public UnixSocketAddress(String _path, boolean _abstract) {
        this.path = _path;
        this.abs = _abstract;
    }

    public boolean isAbs() {
        return abs;
    }

    public void setAbs(boolean _abs) {
        abs = _abs;
    }

    public void setPath(String _path) {
        path = _path;
    }

    /**
    * Create the address.
    * @param _path The path to the Unix Socket.
    */
    public UnixSocketAddress(String _path) {
        this.path = _path;
        this.abs = false;
    }

    /**
    * Return the path.
    * @return the path as String
    */
    public String getPath() {
        return path;
    }

    /**
    * Returns true if this an address for an abstract socket.
    * @return true if address is abstract socket
    */
    public boolean isAbstract() {
        return abs;
    }

    /**
    * Return the Address as a String.
    */
    public String toString() {
        return "unix" + (abs ? ":abstract" : "") + ":path=" + path;
    }

    public boolean equals(Object o) {
        if (!(o instanceof UnixSocketAddress))
            return false;
        return ((UnixSocketAddress) o).path.equals(this.path);
    }

    public int hashCode() {
        return path.hashCode();
    }
}
