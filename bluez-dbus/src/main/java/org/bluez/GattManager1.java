package org.bluez;

import java.util.Map;

import org.bluez.exceptions.BluezAlreadyExistsException;
import org.bluez.exceptions.BluezDoesNotExistException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

/**
 * File generated - 2018-07-25.<br>
 * Based on bluez Documentation: gatt-api.txt.<br>
 * <br>
 * <b>Service:</b> org.bluez<br>
 * <b>Interface:</b> org.bluez.GattManager1<br>
 * <br>
 * <b>Object path:</b><br>
 *             [variable prefix]/{hci0,hci1,...}<br>
 * <br>
 * GATT Manager hierarchy<br>
 * ======================<br>
 <br>
 * GATT Manager allows external applications to register GATT services and<br>
 * profiles.<br>
 <br>
 * Registering a profile allows applications to subscribe to *remote* services.<br>
 * These must implement the GattProfile1 interface defined above.<br>
 <br>
 * Registering a service allows applications to publish a *local* GATT service,<br>
 * which then becomes available to remote devices. A GATT service is represented by<br>
 * a D-Bus object hierarchy where the root node corresponds to a service and the<br>
 * child nodes represent characteristics and descriptors that belong to that<br>
 * service. Each node must implement one of GattService1, GattCharacteristic1,<br>
 * or GattDescriptor1 interfaces described above, based on the attribute it<br>
 * represents. Each node must also implement the standard D-Bus Properties<br>
 * interface to expose their properties. These objects collectively represent a<br>
 * GATT service definition.<br>
 <br>
 * To make service registration simple, BlueZ requires that all objects that belong<br>
 * to a GATT service be grouped under a D-Bus Object Manager that solely manages<br>
 * the objects of that service. Hence, the standard DBus.ObjectManager interface<br>
 * must be available on the root service path. An example application hierarchy<br>
 * containing two separate GATT services may look like this:<br>
 <br>
 * -&gt; /com/example<br>
 *   |   - org.freedesktop.DBus.ObjectManager<br>
 *   |<br>
 *   -&gt; /com/example/service0<br>
 *   | |   - org.freedesktop.DBus.Properties<br>
 *   | |   - org.bluez.GattService1<br>
 *   | |<br>
 *   | -&gt; /com/example/service0/char0<br>
 *   | |     - org.freedesktop.DBus.Properties<br>
 *   | |     - org.bluez.GattCharacteristic1<br>
 *   | |<br>
 *   | -&gt; /com/example/service0/char1<br>
 *   |   |   - org.freedesktop.DBus.Properties<br>
 *   |   |   - org.bluez.GattCharacteristic1<br>
 *   |   |<br>
 *   |   -&gt; /com/example/service0/char1/desc0<br>
 *   |       - org.freedesktop.DBus.Properties<br>
 *   |       - org.bluez.GattDescriptor1<br>
 *   |<br>
 *   -&gt; /com/example/service1<br>
 *     |   - org.freedesktop.DBus.Properties<br>
 *     |   - org.bluez.GattService1<br>
 *     |<br>
 *     -&gt; /com/example/service1/char0<br>
 *         - org.freedesktop.DBus.Properties<br>
 *         - org.bluez.GattCharacteristic1<br>
 <br>
 * When a service is registered, BlueZ will automatically obtain information about<br>
 * all objects using the service's Object Manager. Once a service has been<br>
 * registered, the objects of a service should not be removed. If BlueZ receives an<br>
 * InterfacesRemoved signal from a service's Object Manager, it will immediately<br>
 * unregister the service. Similarly, if the application disconnects from the bus,<br>
 * all of its registered services will be automatically unregistered.<br>
 * InterfacesAdded signals will be ignored.<br>
 <br>
 * Examples:<br>
 *  - Client<br>
 *      test/example-gatt-client<br>
 *      client/bluetoothctl<br>
 *  - Server<br>
 *      test/example-gatt-server<br>
 *      tools/gatt-service<br>
 */
public interface GattManager1 extends DBusInterface {

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * Registers a local GATT services hierarchy as described<br>
     * above (GATT Server) and/or GATT profiles (GATT Client).<br>
     * <br>
     * The application object path together with the D-Bus<br>
     * system bus connection ID define the identification of<br>
     * the application registering a GATT based<br>
     * service or profile.<br>
     * <br>
     * 
     * @param _application
     * @param _options
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezAlreadyExistsException when item already exists
     */
    void RegisterApplication(DBusPath _application, Map<String, Variant<?>> _options) throws BluezInvalidArgumentsException, BluezAlreadyExistsException;

    /**
     * <b>From bluez documentation:</b><br>
     * <br>
     * This unregisters the services that has been<br>
     * previously registered. The object path parameter<br>
     * must match the same value that has been used<br>
     * on registration.<br>
     * <br>
     * 
     * @param _application
     * 
     * @throws BluezInvalidArgumentsException when argument is invalid
     * @throws BluezDoesNotExistException when item does not exist
     */
    void UnregisterApplication(DBusPath _application) throws BluezInvalidArgumentsException, BluezDoesNotExistException;

}
