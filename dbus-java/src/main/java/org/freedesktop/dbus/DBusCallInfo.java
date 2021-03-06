package org.freedesktop.dbus;

import org.freedesktop.dbus.messages.Message;

/**
 * Holds information on a method call
 */
public class DBusCallInfo {
    /**
    * Indicates the caller won't wait for a reply (and we won't send one).
    */
    public static final int NO_REPLY = Message.Flags.NO_REPLY_EXPECTED;
    public static final int ASYNC    = 0x100;
    private String          source;
    private String          destination;
    private String          objectpath;
    private String          iface;
    private String          method;
    private int             flags;

    public DBusCallInfo(Message m) {
        this.source = m.getSource();
        this.destination = m.getDestination();
        this.objectpath = m.getPath();
        this.iface = m.getInterface();
        this.method = m.getName();
        this.flags = m.getFlags();
    }

    /** Returns the BusID which called the method.
     * @return source
     */
    public String getSource() {
        return source;
    }

    /** Returns the name with which we were addressed on the Bus.
     * @return destination
     */
    public String getDestination() {
        return destination;
    }

    /** Returns the object path used to call this method.
     * @return objectpath
     */
    public String getObjectPath() {
        return objectpath;
    }

    /** Returns the interface this method was called with.
     * @return interface
     */
    public String getInterface() {
        return iface;
    }

    /** Returns the method name used to call this method.
     * @return method
     */
    public String getMethod() {
        return method;
    }

    /** Returns any flags set on this method call.
     * @return flags
     */
    public int getFlags() {
        return flags;
    }
}
