/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop;

import java.util.Map;

import org.freedesktop.dbus.errors.MatchRuleInvalid;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
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
    * Initial message to register ourselves on the Bus.
    * @return The unique name of this connection to the Bus.
    */
    String Hello();

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
    * List the connections currently queued for a name.
    * @param name The name to query
    * @return A list of unique connection IDs.
    */
    String[] ListQueuedOwners(String name);

    /**
    * Lists all connected names on the Bus.
    * @return An array of all connected names.
    */
    String[] ListNames();

    /**
     * Returns a list of all names that can be activated on the bus. 
     * @return Array of strings where each string is a bus name
     */
    String[] ListActivatableNames();
    
    /**
    * Determine if a name has an owner.
    * @param name The name to query.
    * @return true if the name has an owner.
    */
    boolean NameHasOwner(String name);

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
    * Start a service. If the given service is not provided
    * by any application, it will be started according to the .service file
    * for that service.
    * @param name The service name to start.
    * @param flags Unused.
    * @return DBUS_START_REPLY constants.
    */
    UInt32 StartServiceByName(String name, UInt32 flags);

    /**
     * <b><a href="https://dbus.freedesktop.org/doc/dbus-specification.html">DBUS Specification</a>:</b><br>
     * Normally, session bus activated services inherit the environment of the bus daemon. This method adds to or modifies that environment when activating services.
     * Some bus instances, such as the standard system bus, may disable access to this method for some or all callers.
     * Note, both the environment variable names and values must be valid UTF-8. There's no way to update the activation environment with data that is invalid UTF-8. 
     *
     * @param environment Environment to add or update
     */
    void UpdateActivationEnvironment(Map<String,String>[] environment);
    
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
    * Returns the proccess ID associated with a connection.
    * @param connection_name The name of the connection
    * @return The PID of the connection.
    */
    UInt32 GetConnectionUnixProcessID(String connection_name);

    /**
     * <b><a href="https://dbus.freedesktop.org/doc/dbus-specification.html">DBUS Specification</a>:</b><br>
     * Returns as many credentials as possible for the process connected to
     * the server. If unable to determine certain credentials (for instance,
     * because the process is not on the same machine as the bus daemon,
     * or because this version of the bus daemon does not support a
     * particular security framework), or if the values of those credentials
     * cannot be represented as documented here, then those credentials
     * are omitted.
     * </p><p>
     * Keys in the returned dictionary not containing "." are defined
     * by this specification. Bus daemon implementors supporting
     * credentials frameworks not mentioned in this document should either
     * contribute patches to this specification, or use keys containing
     * "." and starting with a reversed domain name.
     * </p><div class="informaltable"><table class="informaltable" border="1"><colgroup><col><col><col></colgroup><thead><tr><th>Key</th><th>Value type</th><th>Value</th></tr></thead><tbody><tr><td>UnixUserID</td><td>UINT32</td><td>The numeric Unix user ID, as defined by POSIX</td></tr><tr><td>ProcessID</td><td>UINT32</td><td>The numeric process ID, on platforms that have
     * this concept. On Unix, this is the process ID defined by
     * POSIX.</td></tr><tr><td>WindowsSID</td><td>STRING</td><td>The Windows security identifier in its string form,
     * e.g. "S-1-5-21-3623811015-3361044348-30300820-1013" for
     * a domain or local computer user or "S-1-5-18" for the
     * LOCAL_SYSTEM user</td></tr><tr><td>LinuxSecurityLabel</td><td>ARRAY of BYTE</td><td>
     * <p>On Linux systems, the security label that would result
     * from the SO_PEERSEC getsockopt call. The array contains
     * the non-zero bytes of the security label in an unspecified
     * ASCII-compatible encoding<a href="#ftn.idm2993" class="footnote" name="idm2993"><sup class="footnote">[a]</sup></a>, followed by a single zero byte.</p>
     * <p>
     * For example, the SELinux context
     * <code class="literal">system_u:system_r:init_t:s0</code>
     * (a string of length 27) would be encoded as 28 bytes
     * ending with ':', 's', '0', '\x00'.<a href="#ftn.idm2997" class="footnote" name="idm2997"><sup class="footnote">[b]</sup></a>
     * </p>
     * <p>
     * On SELinux systems this is the SELinux context, as output
     * by <code class="literal">ps -Z</code> or <code class="literal">ls -Z</code>.
     * Typical values might include
     * <code class="literal">system_u:system_r:init_t:s0</code>,
     * <code class="literal">unconfined_u:unconfined_r:unconfined_t:s0-s0:c0.c1023</code>,
     * or
     * <code class="literal">unconfined_u:unconfined_r:chrome_sandbox_t:s0-s0:c0.c1023</code>.
     * </p>
     * <p>
     * On Smack systems, this is the Smack label.
     * Typical values might include
     * <code class="literal">_</code>, <code class="literal">*</code>,
     * <code class="literal">User</code>, <code class="literal">System</code>
     * or <code class="literal">System::Shared</code>.
     * </p>
     * <p>
     * On AppArmor systems, this is the AppArmor context,
     * a composite string encoding the AppArmor label (one or more
     * profiles) and the enforcement mode.
     * Typical values might include <code class="literal">unconfined</code>,
     * <code class="literal">/usr/bin/firefox (enforce)</code> or
     * <code class="literal">user1 (complain)</code>.
     * </p>
     * </td></tr></tbody><tbody class="footnotes"><tr><td colspan="3"><div id="ftn.idm2993" class="footnote"><p><a href="#idm2993" class="para"><sup class="para">[a] </sup></a>It could be ASCII or UTF-8, but could also be
     * ISO Latin-1 or any other encoding.</p></div><div id="ftn.idm2997" class="footnote"><p><a href="#idm2997" class="para"><sup class="para">[b] </sup></a>Note that this is not the same as the older
     * GetConnectionSELinuxContext method, which does
     * not append the zero byte. Always appending the
     * zero byte allows callers to read the string
     * from the message payload without copying.</p></div></td></tr></tbody></table></div><p>
     * </p><p>
     * This method was added in D-Bus 1.7 to reduce the round-trips
     * required to list a process's credentials. In older versions, calling
     * this method will fail: applications should recover by using the
     * separate methods such as
     * <a  href="https://dbus.freedesktop.org/doc/dbus-specification.html#bus-messages-get-connection-unix-user" title="org.freedesktop.DBus.GetConnectionUnixUser">the section called “<code class="literal">org.freedesktop.DBus.GetConnectionUnixUser</code>”</a>
     * instead.
     * 
     * 
     * @param busName Unique or well-known bus name of the connection to query, such as :12.34 or com.example.tea
     * @return Credentials
     */
    Map<String, Variant<?>> GetConnectionCredentials(String busName);
    
    
    /**
     * <b><a href="https://dbus.freedesktop.org/doc/dbus-specification.html">DBUS Specification</a>:</b><br>
     * 
     * Returns auditing data used by Solaris ADT, in an unspecified<br>
     * binary format. If you know what this means, please contribute<br>
     * documentation via the D-Bus bug tracking system.<br>
     * This method is on the core DBus interface for historical reasons;<br>
     * the same information should be made available via<br>
     * <a  href="https://dbus.freedesktop.org/doc/dbus-specification.html#bus-messages-get-connection-credentials" title="org.freedesktop.DBus.GetConnectionCredentials">the section called “<code class="literal">org.freedesktop.DBus.GetConnectionCredentials</code>”</a><br>
     * in future.<br>
     * 
     * @param busName Unique or well-known bus name of the connection to query, such as :12.34 or com.example.tea
     * @return auditing data as returned by adt_export_session_data()
     */
    Byte[] GetAdtAuditSessionData(String busName);
    
    /**
    * <b><a href="https://dbus.freedesktop.org/doc/dbus-specification.html">DBUS Specification</a>:</b><br>
    * Returns the security context used by SELinux, in an unspecified<br>
    * format. If you know what this means, please contribute<br>
    * documentation via the D-Bus bug tracking system.<br>
    * This method is on the core DBus interface for historical reasons;<br>
    * the same information should be made available via<br>
    * <a  href="https://dbus.freedesktop.org/doc/dbus-specification.html#bus-messages-get-connection-credentials" title="org.freedesktop.DBus.GetConnectionCredentials">the section called “<code class="literal">org.freedesktop.DBus.GetConnectionCredentials</code>”</a><br>
    * in future.
    * 
    * @param busName Unique or well-known bus name of the connection to query, such as :12.34 or com.example.tea
    *
    * @return some sort of string of bytes, not necessarily UTF-8, not including '\0'
    */
    Byte[] GetConnectionSELinuxSecurityContext(String busName);

    /**
    * Add a match rule.
    * Will cause you to receive messages that aren't directed to you which
    * match this rule.
    * @param matchrule The Match rule as a string. Format Undocumented.
    */
    void AddMatch(String matchrule) throws MatchRuleInvalid;

    /**
    * Remove a match rule.
    * Will cause you to stop receiving messages that aren't directed to you which
    * match this rule.
    * @param matchrule The Match rule as a string. Format Undocumented.
    */
    void RemoveMatch(String matchrule) throws MatchRuleInvalid;

    /**
     * <b><a href="https://dbus.freedesktop.org/doc/dbus-specification.html">DBUS Specification</a>:</b><br>
     * Gets the unique ID of the bus. The unique ID here is shared among all addresses the<br>
     * bus daemon is listening on (TCP, UNIX domain socket, etc.) and its format is described in<br>
     * <a  href="#uuids" title="UUIDs">the section called “UUIDs”</a>. <br> 
     * Each address the bus is listening on also has its own unique<br>
     * ID, as described in <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#addresses" title="Server Addresses">the section called “Server Addresses”</a>. The per-bus and per-address IDs are not related.<br>
     * There is also a per-machine ID, described in <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#standard-interfaces-peer">the section called “<code class="literal">org.freedesktop.DBus.Peer</code>”</a> and returned
     * by org.freedesktop.DBus.Peer.GetMachineId().<br>
     * For a desktop session bus, the bus ID can be used as a way to uniquely identify a user's session.
     *    
     * @return id Unique ID identifying the bus daemon
     */
    String GetId();

    
}
//CHECKSTYLE:ON