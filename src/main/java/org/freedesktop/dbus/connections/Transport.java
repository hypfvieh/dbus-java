/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.connections;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import org.freedesktop.dbus.MessageReader;
import org.freedesktop.dbus.MessageWriter;
import org.freedesktop.dbus.connections.BusAddress.AddressBusTypes;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cx.ath.matthew.unix.UnixServerSocket;
import cx.ath.matthew.unix.UnixSocket;
import cx.ath.matthew.unix.UnixSocketAddress;
import cx.ath.matthew.utils.Hexdump;

public class Transport implements Closeable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private MessageReader min;
    private MessageWriter mout;

    private UnixServerSocket unixServerSocket;

    public Transport() {
    }

    public static String genGUID() {
        Random r = new Random();
        byte[] buf = new byte[16];
        r.nextBytes(buf);
        String guid = Hexdump.toHex(buf);
        return guid.replaceAll(" ", "");
    }

    public Transport(BusAddress address) throws IOException {
        connect(address);
    }

    public Transport(String address) throws IOException, DBusException {
        connect(new BusAddress(address));
    }

    public Transport(String address, int timeout) throws IOException, DBusException {
        connect(new BusAddress(address), timeout);
    }

    public Transport(BusAddress address, int timeout) throws IOException, DBusException {
        connect(address, timeout);
    }
    
    public void writeMessage(Message message) throws IOException {
        if (mout != null) {
            mout.writeMessage(message);
        }
    }
    
    public Message readMessage() throws IOException, DBusException {
        if (min != null) {
            try {
                return min.readMessage();
            } catch (Exception _ex) {
                if (_ex instanceof EOFException) { return null; }
                _ex.printStackTrace();
                System.exit(1);
            }
        }
        return null;
    }
   
    private void connect(BusAddress address) throws IOException {
        connect(address, 0);
    }

    private void connect(BusAddress address, int timeout) throws IOException {
        logger.debug("Connecting to " + address);
        OutputStream out = null;
        InputStream in = null;
        UnixSocket us = null;
        Socket s = null;
        int mode = 0;
        int types = 0;
        
        if (address.getBusType() == AddressBusTypes.UNIX) {
            types = SASL.AUTH_EXTERNAL;
            if (null != address.getParameter("listen")) {
                mode = SASL.MODE_SERVER;
                unixServerSocket = new UnixServerSocket();
                if (null != address.getParameter("abstract")) {
                    unixServerSocket.bind(new UnixSocketAddress(address.getParameter("abstract"), true));
                } else if (null != address.getParameter("path")) {
                    unixServerSocket.bind(new UnixSocketAddress(address.getParameter("path"), false));
                }
                us = unixServerSocket.accept();
            } else {
                mode = SASL.MODE_CLIENT;
                us = new UnixSocket();
                if (null != address.getParameter("abstract")) {
                    us.connect(new UnixSocketAddress(address.getParameter("abstract"), true));
                } else if (null != address.getParameter("path")) {
                    us.connect(new UnixSocketAddress(address.getParameter("path"), false));
                }
            }
            us.setPassCred(true);
            in = us.getInputStream();
            out = us.getOutputStream();
        } else if (address.getBusType() == AddressBusTypes.TCP) {
            types = SASL.AUTH_SHA;
            if (null != address.getParameter("listen")) {
                mode = SASL.MODE_SERVER;
                try (ServerSocket ss = new ServerSocket()) {
                    ss.bind(new InetSocketAddress(address.getParameter("host"), Integer.parseInt(address.getParameter("port"))));
                    s = ss.accept();
                }
            } else {
                mode = SASL.MODE_CLIENT;
                s = new Socket();
                s.connect(new InetSocketAddress(address.getParameter("host"), Integer.parseInt(address.getParameter("port"))));
            }
            in = s.getInputStream();
            out = s.getOutputStream();
        } else {
            throw new IOException("unknown address type " + address.getType());
        }

        if (!(new SASL()).auth(mode, types, address.getParameter("guid"), out, in, us)) {
            out.close();
            throw new IOException("Failed to auth");
        }
        if (null != us) {
            logger.trace("Setting timeout to " + timeout + " on Socket");
            if (timeout == 1) {
                us.setBlocking(false);
            } else {
                us.setSoTimeout(timeout);
            }
        }
        if (null != s) {
            logger.trace("Setting timeout to " + timeout + " on Socket");
            s.setSoTimeout(timeout);
        }
        mout = new MessageWriter(out);
        min = new MessageReader(in);
    }

    public synchronized void disconnect() throws IOException {
        logger.debug("Disconnecting Transport");
        min.close();
        mout.close();
        if (unixServerSocket != null && !unixServerSocket.isClosed()) {
            unixServerSocket.close();
        }
    }

    public boolean isConnected() {
        return min.isClosed() || mout.isClosed();
    }
    
    @Override
    public void close() throws IOException {
        disconnect();
    }

}
