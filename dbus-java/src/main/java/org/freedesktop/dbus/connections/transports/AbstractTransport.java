package org.freedesktop.dbus.connections.transports;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.freedesktop.dbus.MessageReader;
import org.freedesktop.dbus.MessageWriter;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.SASL;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTransport implements Closeable {

    private final Logger     logger;
    private final BusAddress address;
    private final int        timeout;

    private SASL.SaslMode    saslMode;

    private int              saslAuthMode;
    private MessageReader    inputReader;
    private MessageWriter    outputWriter;

    AbstractTransport(BusAddress _address, int _timeout) {
        address = _address;
        timeout = _timeout;
        saslMode = SASL.SaslMode.CLIENT;
        saslAuthMode = SASL.AUTH_NONE;
        logger = LoggerFactory.getLogger(getClass());
    }

    public void writeMessage(Message _msg) throws IOException {
        if (!outputWriter.isClosed()) {
            outputWriter.writeMessage(_msg);
        }
    }
    
    public Message readMessage() throws IOException, DBusException {
        if (!inputReader.isClosed()) {
            return inputReader.readMessage();
        }
        return null;
    }
    
    public void start() throws IOException {
        connect();
    }
    
    abstract void connect() throws IOException;
    
    protected void authenticate(OutputStream _out, InputStream _in, Socket _sock) throws IOException {
        if (!(new SASL()).auth(saslMode, saslAuthMode, address.getGuid(), _out, _in, _sock)) {
            _out.close();
            throw new IOException("Failed to auth");
        }
    }


    protected void setOutputWriter(OutputStream _outputStream) {
        outputWriter = new MessageWriter(_outputStream);        
    }

    protected void setInputReader(InputStream _inputStream) {
        inputReader = new MessageReader(_inputStream);
    }
    
    protected int getSaslAuthMode() {
        return saslAuthMode;
    }

    protected void setSaslAuthMode(int _saslAuthMode) {
        saslAuthMode = _saslAuthMode;
    }

    protected SASL.SaslMode getSaslMode() {
        return saslMode;
    }

    protected void setSaslMode(SASL.SaslMode _saslMode) {
        saslMode = _saslMode;
    }

    protected BusAddress getAddress() {
        return address;
    }

    protected int getTimeout() {
        return timeout;
    }

    protected Logger getLogger() {
        return logger;
    }

    @Override
    public void close() throws IOException {
        inputReader.close();
        outputWriter.close();
    }
    
}
