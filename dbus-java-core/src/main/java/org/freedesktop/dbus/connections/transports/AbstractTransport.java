package org.freedesktop.dbus.connections.transports;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.SASL;
import org.freedesktop.dbus.exceptions.AuthenticationException;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.spi.message.IMessageReader;
import org.freedesktop.dbus.spi.message.IMessageWriter;
import org.freedesktop.dbus.spi.message.ISocketProvider;
import org.freedesktop.dbus.spi.message.InputStreamMessageReader;
import org.freedesktop.dbus.spi.message.OutputStreamMessageWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all transport types.
 *
 * @author hypfvieh
 * @since v3.2.0 - 2019-02-08
 */
public abstract class AbstractTransport implements Closeable {

    private final ServiceLoader<ISocketProvider> spiLoader = ServiceLoader.load(ISocketProvider.class);

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final BusAddress address;

    private SASL.SaslMode    saslMode;

    private int              saslAuthMode;
    private IMessageReader    inputReader;
    private IMessageWriter    outputWriter;

    private boolean fileDescriptorSupported;

    protected AbstractTransport(BusAddress _address) {
        address = _address;

        if (!isAbstractAllowed() && _address.isAbstract()) {
            throw new UnsupportedOperationException("Abstract sockets are not supported by transport " + getClass().getName());
        }

        if (_address.isListeningSocket()) {
            saslMode = SASL.SaslMode.SERVER;
        } else {
            saslMode = SASL.SaslMode.CLIENT;
        }

        saslAuthMode = SASL.AUTH_NONE;
    }

    /**
     * Write a message to the underlying socket.
     *
     * @param _msg message to write
     * @throws IOException on write error or if output was already closed or null
     */
    public void writeMessage(Message _msg) throws IOException {
        if (!fileDescriptorSupported && Message.ArgumentType.FILEDESCRIPTOR == _msg.getType()) {
            throw new IllegalArgumentException("File descriptors are not supported!");
        }
        if (outputWriter != null && !outputWriter.isClosed()) {
            outputWriter.writeMessage(_msg);
        } else {
            throw new IOException("OutputWriter already closed or null");
        }
    }

    /**
     * Read a message from the underlying socket.
     *
     * @return read message, maybe null
     * @throws IOException when input already close or null
     * @throws DBusException when message could not be converted to a DBus message
     */
    public Message readMessage() throws IOException, DBusException {
        if (inputReader != null && !inputReader.isClosed()) {
            return inputReader.readMessage();
        }
        throw new IOException("InputReader already closed or null");
    }

    /**
     * Method to indicate if passing of file descriptors is allowed.
     *
     * @return true to allow FD passing, false otherwise
     */
    protected abstract boolean hasFileDescriptorSupport();

    /**
     * Return true if the transport supports 'abstract' sockets.
     * @return true if abstract sockets supported, false otherwise
     */
    protected abstract boolean isAbstractAllowed();

    /**
     * Abstract method implemented by concrete sub classes to establish a connection
     * using whatever transport type (e.g. TCP/Unix socket).
     * @throws IOException when connection fails
     */
    protected abstract SocketChannel connectImpl() throws IOException;

    /**
     * Establish connection on created transport.
     *
     * @return {@link SocketChannel}
     * @throws IOException if connection fails
     */
    public final SocketChannel connect() throws IOException {
        SocketChannel channel = connectImpl();
        authenticate(channel);
        setInputOutput(channel);
        return channel;
    }

    /**
     * Helper method to authenticate to DBus using SASL.
     *
     * @param _sock socketchannel
     * @throws IOException on any error
     */
    private void authenticate(SocketChannel _sock) throws IOException {
        SASL sasl = new SASL(hasFileDescriptorSupport());
        if (!sasl.auth(saslMode, saslAuthMode, address.getGuid(), _sock, this)) {
            _sock.close();
            throw new AuthenticationException("Failed to authenticate");
        }
        fileDescriptorSupported = sasl.isFileDescriptorSupported();
    }

    /**
     * Setup message reader/writer.
     * Will look for SPI provider first, if none is found default implementation is used.
     * The default implementation does not support file descriptor passing!
     *
     * @param _socket socket to use
     */
    private void setInputOutput(SocketChannel _socket) {
        try {
            for (ISocketProvider provider : spiLoader) {
                logger.debug("Found ISocketProvider {}", provider);

                provider.setFileDescriptorSupport(hasFileDescriptorSupport() && fileDescriptorSupported);
                inputReader = provider.createReader(_socket);
                outputWriter = provider.createWriter(_socket);
                if (inputReader != null && outputWriter != null) {
                    logger.debug("Using ISocketProvider {}", provider);
                    break;
                }
            }
        } catch (ServiceConfigurationError _ex) {
            logger.error("Could not initialize service provider", _ex);
        } catch (IOException _ex) {
            logger.error("Could not initialize alternative message reader/writer", _ex);
        }

        if (inputReader == null || outputWriter == null) {
            logger.debug("No alternative ISocketProvider found, using built-in implementation");
            inputReader = new InputStreamMessageReader(_socket);
            outputWriter = new OutputStreamMessageWriter(_socket);
        }

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

    protected Logger getLogger() {
        return logger;
    }

    @Override
    public void close() throws IOException {
        if (inputReader != null) {
            inputReader.close();
            inputReader = null;
        }

        if (outputWriter != null) {
            outputWriter.close();
            outputWriter = null;
        }
    }

}
