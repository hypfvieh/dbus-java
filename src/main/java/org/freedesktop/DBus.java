/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.Variant;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
//CHECKSTYLE:OFF
public interface DBus extends DBusInterface {
    int DBUS_NAME_FLAG_ALLOW_REPLACEMENT      = 0x01;
    int DBUS_NAME_FLAG_REPLACE_EXISTING       = 0x02;
    int DBUS_NAME_FLAG_DO_NOT_QUEUE           = 0x04;
    int DBUS_REQUEST_NAME_REPLY_PRIMARY_OWNER = 1;
    int DBUS_REQUEST_NAME_REPLY_IN_QUEUE      = 2;
    int DBUS_REQUEST_NAME_REPLY_EXISTS        = 3;
    int DBUS_REQUEST_NAME_REPLY_ALREADY_OWNER = 4;
    int DBUS_RELEASE_NAME_REPLY_RELEASED      = 1;
    int DBUS_RELEASE_NAME_REPLY_NON_EXISTANT  = 2;
    int DBUS_RELEASE_NAME_REPLY_NOT_OWNER     = 3;
    int DBUS_START_REPLY_SUCCESS              = 1;
    int DBUS_START_REPLY_ALREADY_RUNNING      = 2;

    /**
    * All DBus Applications should respond to the Ping method on this interface
    */
    public interface Peer extends DBusInterface {
        void Ping();
    }

    /**
    * Objects can provide introspection data via this interface and method.
    * See the <a href="http://dbus.freedesktop.org/doc/dbus-specification.html#introspection-format">Introspection Format</a>.
    */
    public interface Introspectable extends DBusInterface {
        /**
         * @return The XML introspection data for this object
         */
        String Introspect();
    }

    /**
    * A standard properties interface.
    */
    public interface Properties extends DBusInterface {
        /**
         * Get the value for the given property.
         * @param <A> whatever
         * @param interface_name The interface this property is associated with.
         * @param property_name The name of the property.
         * @return The value of the property (may be any valid DBus type).
         */
        <A> A Get(String interface_name, String property_name);

        /**
         * Set the value for the given property.
         * @param <A> whatever
         * @param interface_name The interface this property is associated with.
         * @param property_name The name of the property.
         * @param value The new value of the property (may be any valid DBus type).
         */
        <A> void Set(String interface_name, String property_name, A value);

        /**
         * Get all properties and values.
         * @param interface_name The interface the properties is associated with.
         * @return The properties mapped to their values.
         */
        Map<String, Variant<?>> GetAll(String interface_name);

        /**
         * Signal generated when a property changes.
         */
        public class PropertiesChanged extends DBusSignal {
            public final String interfaceName;
            public final Map<String, Variant<?>> changedProperties;
            public final List<String> invalidatedProperties;

            public PropertiesChanged(final String path, final String _interfaceName,
                    final Map<String, Variant<?>> _changedProperties, final List<String> _invalidatedProperties)
                    throws DBusException {
                super(path, _interfaceName, _changedProperties, _invalidatedProperties);
                this.interfaceName = _interfaceName;
                this.changedProperties = _changedProperties;
                this.invalidatedProperties = _invalidatedProperties;
            }
        }
    }

    public interface ObjectManager extends DBusInterface {
        /**
         * Get a sub-tree of objects. The root of the sub-tree is this object.
         * @return A Map from object path (DBusInterface) to a Map from interface name to a properties Map (as returned by Properties.GetAll())
         */
        Map<DBusInterface, Map<String, Map<String, Variant<?>>>> GetManagedObjects();

        /**
         * Signal generated when a new interface is added
         */
        class InterfacesAdded extends DBusSignal {
            public final DBusInterface object;
            public final Map<String, Map<String, Variant<?>>> interfaces;

            public InterfacesAdded(String path, DBusInterface object, Map<String, Map<String, Variant<?>>> interfaces) throws DBusException {
                super(path, object, interfaces);
                this.object = object;
                this.interfaces = interfaces;
            }
        }

        /**
         * Signal generated when an interface is removed
         */
        class InterfacesRemoved extends DBusSignal {
            public final DBusInterface object;
            public final List<String> interfaces;

            public InterfacesRemoved(String path, DBusInterface object, List<String> interfaces) throws DBusException {
                super(path, object, interfaces);
                this.object = object;
                this.interfaces = interfaces;
            }
        }

    }

    /**
    * Messages generated locally in the application.
    */
    public interface Local extends DBusInterface {
        class Disconnected extends DBusSignal {
            public Disconnected(String path) throws DBusException {
                super(path);
            }
        }
    }

    /**
    * Initial message to register ourselves on the Bus.
    * @return The unique name of this connection to the Bus.
    */
    String Hello();

    /**
    * Lists all connected names on the Bus.
    * @return An array of all connected names.
    */
    String[] ListNames();

    /**
    * Determine if a name has an owner.
    * @param name The name to query.
    * @return true if the name has an owner.
    */
    boolean NameHasOwner(String name);

    /**
    * Get the connection unique name that owns the given name.
    * @param name The name to query.
    * @return The connection which owns the name.
    */
    String GetNameOwner(String name);

    /**
    * Get the Unix UID that owns a connection name.
    * @param connection_name The connection name.
    * @return The Unix UID that owns it.
    */
    UInt32 GetConnectionUnixUser(String connection_name);

    /**
    * Start a service. If the given service is not provided
    * by any application, it will be started according to the .service file
    * for that service.
    * @param name The service name to start.
    * @param flags Unused.
    * @return DBUS_START_REPLY constants.
    */
    UInt32 StartServiceByName(String name, UInt32 flags);

    /**
    * Request a name on the bus.
    * @param name The name to request.
    * @param flags DBUS_NAME flags.
    * @return DBUS_REQUEST_NAME_REPLY constants.
    */
    UInt32 RequestName(String name, UInt32 flags);

    /**
    * Release a name on the bus.
    * @param name The name to release.
    * @return DBUS_RELEASE_NAME_REPLY constants.
    */
    UInt32 ReleaseName(String name);

    /**
    * Add a match rule.
    * Will cause you to receive messages that aren't directed to you which
    * match this rule.
    * @param matchrule The Match rule as a string. Format Undocumented.
    */
    void AddMatch(String matchrule) throws Error.MatchRuleInvalid;

    /**
    * Remove a match rule.
    * Will cause you to stop receiving messages that aren't directed to you which
    * match this rule.
    * @param matchrule The Match rule as a string. Format Undocumented.
    */
    void RemoveMatch(String matchrule) throws Error.MatchRuleInvalid;

    /**
    * List the connections currently queued for a name.
    * @param name The name to query
    * @return A list of unique connection IDs.
    */
    String[] ListQueuedOwners(String name);

    /**
    * Returns the proccess ID associated with a connection.
    * @param connection_name The name of the connection
    * @return The PID of the connection.
    */
    UInt32 GetConnectionUnixProcessID(String connection_name);

    /**
    * Does something undocumented.
    * @param a string
    *
    * @return byte array
    */
    Byte[] GetConnectionSELinuxSecurityContext(String a);

    /**
    * Does something undocumented.
    */
    void ReloadConfig();

    /**
    * Signal sent when the owner of a name changes
    */
    class NameOwnerChanged extends DBusSignal {
        public final String name;
        public final String oldOwner;
        public final String newOwner;

        public NameOwnerChanged(String path, String _name, String _oldOwner, String _newOwner) throws DBusException {
            super(path, new Object[] {
                    _name, _oldOwner, _newOwner
            });
            this.name = _name;
            this.oldOwner = _oldOwner;
            this.newOwner = _newOwner;
        }
    }

    /**
    * Signal sent to a connection when it loses a name
    */
    class NameLost extends DBusSignal {
        public final String name;

        public NameLost(String path, String _name) throws DBusException {
            super(path, _name);
            this.name = _name;
        }
    }

    /**
    * Signal sent to a connection when it aquires a name
    */
    class NameAcquired extends DBusSignal {
        public final String name;

        public NameAcquired(String _path, String _name) throws DBusException {
            super(_path, _name);
            this.name = _name;
        }
    }

    /**
    * Contains standard errors that can be thrown from methods.
    */
    public interface Error {
        /**
         * Thrown if the method called was unknown on the remote object
         */
        @SuppressWarnings("serial")
        class UnknownMethod extends DBusExecutionException {
            public UnknownMethod(String message) {
                super(message);
            }
        }

        /**
         * Thrown if the object was unknown on a remote connection
         */
        @SuppressWarnings("serial")
        class UnknownObject extends DBusExecutionException {
            public UnknownObject(String message) {
                super(message);
            }
        }

        /**
         * Thrown if the requested service was not available
         */
        @SuppressWarnings("serial")
        class ServiceUnknown extends DBusExecutionException {
            public ServiceUnknown(String message) {
                super(message);
            }
        }

        /**
         * Thrown if the match rule is invalid
         */
        @SuppressWarnings("serial")
        class MatchRuleInvalid extends DBusExecutionException {
            public MatchRuleInvalid(String message) {
                super(message);
            }
        }

        /**
         * Thrown if there is no reply to a method call
         */
        @SuppressWarnings("serial")
        class NoReply extends DBusExecutionException {
            public NoReply(String message) {
                super(message);
            }
        }

        /**
         * Thrown if a message is denied due to a security policy
         */
        @SuppressWarnings("serial")
        class AccessDenied extends DBusExecutionException {
            public AccessDenied(String message) {
                super(message);
            }
        }
    }

    /**
    * Description of the interface or method, returned in the introspection data
    */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Description {
        String value();
    }

    /**
    * Indicates that a DBus interface or method is deprecated
    */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Deprecated {
    }

    /**
    * Contains method-specific annotations
    */
    public interface Method {
        /**
         * Methods annotated with this do not send a reply
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        public @interface NoReply {
        }

        /**
         * Give an error that the method can return
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        public @interface Error {
            String value();
        }
    }

    /**
    * Contains GLib-specific annotations
    */
    public interface GLib {
        /**
         * Define a C symbol to map to this method. Used by GLib only
         */
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        public @interface CSymbol {
            String value();
        }
    }
}
//CHECKSTYLE:ON