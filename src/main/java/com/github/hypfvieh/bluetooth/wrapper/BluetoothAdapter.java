package com.github.hypfvieh.bluetooth.wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bluez.Adapter1;
import org.bluez.Device1;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInvalidArgumentsException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotReadyException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/**
 * Wrapper class which represents an bluetooth adapter.
 *
 * @author hypfvieh
 *
 */
public class BluetoothAdapter extends AbstractBluetoothObject {

    private final Map<String, Class<?>> supportedFilterOptions = new HashMap<>();
    private final String[] supportedTransportValues = new String[] {"auto", "bredr", "le"};

    private final Adapter1 adapter;

    /** Used to toggle discovery-mode because {@link #isDiscovering()} not always working as expected. */
    private boolean internalDiscover;

    public BluetoothAdapter(Adapter1 _adapter, String _dbusPath, DBusConnection _dbusConnection) {
        super(BluetoothDeviceType.ADAPTER, _dbusConnection, _dbusPath);
        adapter = _adapter;

        supportedFilterOptions.put("UUIDs", String[].class);
        supportedFilterOptions.put("RSSI", short.class);
        supportedFilterOptions.put("Pathloss", UInt16.class);
        supportedFilterOptions.put("Transport", String.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<? extends DBusInterface> getInterfaceClass() {
        return Adapter1.class;
    }

    /**
     * Get the deviceName used in DBus (e.g. hci0).
     *
     * @return the device name
     */
    public String getDeviceName() {
        return getDbusPath().substring(getDbusPath().lastIndexOf("/") + 1);
    }

    /**
     * The changeable bluetooth friendly name.
     *
     * @return the alias name
     */
    public String getAlias() {
        return getTyped("Alias", String.class);
    }

    /**
     * Change the current bluetooth friendly name.
     *
     * @param _alias new alias to set
     */
    public void setAlias(String _alias) {
        setTyped("Alias", _alias);
    }

    /**
     * Returns if the adapter is powered on (true) or powered off (false).
     *
     * @return power status
     */
    public boolean isPowered() {
        return getTyped("Powered", Boolean.class);
    }

    /**
     * Turn adapter on (true) or off (false).
     *
     * @param _powered set power status
     */
    public void setPowered(boolean _powered) {
        setTyped("Powered", _powered);
    }

    /**
     * True if device is discoverable (= visible for others), false otherwise.
     *
     * @return maybe null if feature is not supported
     */
    public Boolean isDiscoverable() {
        return getTyped("Discoverable", Boolean.class);
    }

    /**
     * Set to true to enable device visibility for other bluetooth devices nearby.
     * @param _discoverable set device visible for others
     */
    public void setDiscoverable(boolean _discoverable) {
        setTyped("Discoverable", _discoverable);
    }

    /**
     * The current timeout (in seconds) configured before disabling discoverbility.<br>
     * If 0 discoverbility will never be disabled automatically.
     *
     * @return integer maybe null
     */
    public Integer getDiscoverableTimeout() {
        UInt32 typed = getTyped("DiscoverableTimeout", UInt32.class);
        return typed != null ? typed.intValue() : null;
    }

    /**
     * Timeout (in seconds) to set before the device visibility for others will be disabled.<br>
     * If set to 0, device will stay visible forever.
     *
     * @param _discoverableTimeout timeout to set
     */
    public void setDiscoverableTimeout(Integer _discoverableTimeout) {
        setTyped("DiscoverableTimeout", new UInt32(_discoverableTimeout));
    }

    /**
     * True if pairing with this adapter is allowed for others.
     *
     * @return maybe null if feature is not supported
     */
    public Boolean isPairable() {
        return getTyped("Pairable", Boolean.class);
    }

    /**
     * Set to true to allow pairing of other devices with this adapter.<br>
     * This is a global setting and effects all applications using this adapter.
     *
     * @param _pairable set pairing
     */
    public void setPairable(boolean _pairable) {
        setTyped("Pairable", _pairable);
    }

    /**
     * Current configured timeout (in seconds) before the pairable mode is disabled.<br>
     * If 0, timeout is disabled and pairability will never be disabled.
     *
     * @return integer maybe null
     */
    public Integer getPairableTimeout() {
        UInt32 typed = getTyped("PairableTimeout", UInt32.class);
        return typed != null ? typed.intValue() : null;
    }

    /**
     * Set the timeout (in seconds) before the pairable mode is disabled.<br>
     * Setting this to 0 disables the timeout.
     *
     * @param _pairableTimeout set pairable timeout
     */
    public void setPairableTimeout(Integer _pairableTimeout) {
        setTyped("PairableTimeout", new UInt32(_pairableTimeout));
    }

    /**
     * Returns the bluetooth device (MAC) address.
     *
     * @return mac address, maybe null
     */
    public String getAddress() {
        return getTyped("Address", String.class);
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * The Bluetooth system name (pretty hostname).
     * This property is either a static system default
     * or controlled by an external daemon providing
     * access to the pretty hostname configuration.
     * </p>
     * @return name, maybe null
     */
    public String getName() {
        return getTyped("Name", String.class);
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * The Bluetooth class of device.
     * This property represents the value that is either
     * automatically configured by DMI/ACPI information
     * or provided as static configuration.
     * </p>
     *
     * @return integer, maybe null
     */
    public Integer getDeviceClass() {
        UInt32 typed = getTyped("Class", UInt32.class);
        return typed != null ? typed.intValue() : null;
    }

    /**
     * True if discovery procedure is active, false otherwise.
     *
     * @return discovering status
     */
    public Boolean isDiscovering() {
        return internalDiscover || getTyped("Discovering", Boolean.class);
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * List of 128-bit UUIDs that represents the available
     * local services.
     * </p>
     * @return String[], maybe null
     */
    public String[] getUuids() {
        List<?> typed = getTyped("UUIDs", ArrayList.class);
        if (typed != null) {
            return typed.toArray(new String[]{});
        }
        return null;
    }

    /**
     * <b>From bluez Documentation:</b>
     * <p>
     * Local Device ID information in modalias format
     * used by the kernel and udev.
     * </p>
     * @return String, maybe null
     */
    public String getModAlias() {
        return getTyped("Modalias", String.class);
    }

    /**
     * Start a new discovery operation to find any devices nearby.
     * @return true if discovery is running, false otherwise
     */
    public boolean startDiscovery() {
        if (!isDiscovering()) {
            try {
                adapter.StartDiscovery();
                internalDiscover = true;
            } catch (Exception _ex) {
                return false;
            }
        }
        return isDiscovering();
    }

    /**
     * Stops the current discovery operation.
     *
     * @return true if discovery was stopped, false otherwise
     */
    public boolean stopDiscovery() {
        if (isDiscovering()) {
            try {
                adapter.StopDiscovery();
                internalDiscover = false;
                return true;
            } catch (BluezNotReadyException | BluezFailedException | BluezNotAuthorizedException _ex) {
                return false;
            }
        }
        return true;
    }

    /**
     * Remove a device and it's pairing information.
     *
     * @param _device device to remove
     * @throws BluezFailedException when something went wrong
     * @throws BluezInvalidArgumentsException when device was invalid
     */
    public void removeDevice(Device1 _device) throws BluezFailedException, BluezInvalidArgumentsException {
        adapter.RemoveDevice(new DBusPath(_device.getObjectPath()));
    }

    /**
     * Use this to set a discovery filter.<br>
     * This will cause the bluez library to only add device objects which are matching the given criterias.<br>
     * <br>
     * Support values for the Map:<br>
     * <pre>
     * Type     KeyName   : Purpose
     * ============================================
     * String[] UUIDs     : filtered service UUIDs
     * Short    RSSI      : RSSI threshold value
     * UInt16   Pathloss  : Pathloss threshold value
     * String   Transport : type of scan to run
     * </pre>
     * <br>
     * The 'Transport' Key supports the following options:
     * <pre>
     * Value     - Description
     * ============================================
     * "auto"    - interleaved scan, default value
     * "bredr"   - BR/EDR inquiry
     * "le"      - LE scan only
     * </pre>
     *
     * If a transport mode is used which is not supported by the device, a {@link BluezNotSupportedException} is thrown.
     *
     * @param _filter filter to use
     * @throws BluezInvalidArgumentsException thrown if any arguments in the map are not supported
     * @throws BluezNotReadyException if adapter not ready
     * @throws BluezNotSupportedException if operation not supported
     * @throws BluezFailedException any other error
     */
    public void setDiscoveryFilter(Map<String, Variant<?>> _filter) throws BluezInvalidArgumentsException, BluezNotReadyException, BluezNotSupportedException, BluezFailedException {

        for (Entry<String, Variant<?>> entry : _filter.entrySet()) {
            if (!supportedFilterOptions.containsKey(entry.getKey())) {
                throw new BluezInvalidArgumentsException("Key " + entry.getKey() + " is not supported by Bluez library");
            }
            Class<?> typeClass = supportedFilterOptions.get(entry.getKey());
            if (!typeClass.isAssignableFrom(entry.getValue().getClass())) {
                throw new BluezInvalidArgumentsException("Key " + entry.getKey() + " uses unsupported data type "
                        + entry.getValue().getClass() + ", only "+ typeClass.getName() + " is supported.");
            }
        }
        if (_filter.containsKey("Transport")) {
            String transportType = (String) _filter.get("Transport").getValue();
            if (!Arrays.asList(supportedTransportValues).contains(transportType)) {
                throw new BluezInvalidArgumentsException("Transport option " + transportType + " is unsupported.");
            }
        }

        adapter.SetDiscoveryFilter(_filter);
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + " [adapter=" + adapter + ", getBluetoothType()="
                + getBluetoothType().name() + ", getDbusPath()=" + getDbusPath() + "]";
    }

}
