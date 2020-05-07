package org.freedesktop.dbus.spi;

import java.io.Closeable;
import java.io.IOException;
import org.freedesktop.dbus.messages.Message;

/**
 * Interface that lets you write a message out.
 */
public interface IMessageWriter extends Closeable {
    
    /**
     * Write a message out to the bus.
     * 
     * @param m The message to write
     * @throws IOException If an IO error occurs.
     */
    public void writeMessage(Message m) throws IOException;
    
    public boolean isClosed();
}
