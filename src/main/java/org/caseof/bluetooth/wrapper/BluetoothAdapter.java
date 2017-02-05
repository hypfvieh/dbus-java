package org.caseof.bluetooth.wrapper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bluez.Adapter1;
import org.bluez.Device1;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezInvalidArgumentException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotReadyException;
import org.bluez.exceptions.BluezNotSupportedException;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.UInt16;
import org.freedesktop.dbus.UInt32;

/**
 * Wrapper class which represents an bluetooth adapter.
 *
 * @author maniac
 *
 */
public class BluetoothAdapter extends AbstractBluetoothObject {

    private final Map<String, Class<?>> supportedFilterOptions = new HashMap<>();
    private final String[] supportedTransportValues = new String[] {"auto", "bredr", "le"};

    private final Adapter1 adapter;

    /** Used to toggle discovery-mode because {@link #isDiscovering()} not always working as expected. */
    private boolean internalDiscover;

    public BluetoothAdapter(Adapter1 _adapter, String _dbusPath, DBusConnection _dbusConnection) {
        super(BluetoothType.ADAPTER, _dbusConnection, _dbusPath);
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
     * @return
     */
    public String getDeviceName() {
        return getDbusPath().substring(getDbusPath().lastIndexOf("/") + 1);
    }

    /**
     * The changeable bluetooth friendly name.
     * @return
     */
    public String getAlias() {
        return getTyped("Alias", String.class);
    }

    /**
     * Change the current bluetooth friendly name.
     * @param _alias
     */
    public void setAlias(String _alias) {
        setTyped("Alias", _alias);
    }

    /**
     * Returns if the adapter is powered on (true) or powered off (false).
     * @return
     */
    public boolean isPowered() {
        return getTyped("Powered", Boolean.class);
    }

    /**
     * Turn adapter on (true) or off (false).
     * @param _powered
     */
    public void setPowered(boolean _powered) {
        setTyped("Powered", _powered);
    }

    /**
     * True if device is discoverable (= visible for others), false otherwise.
     * @return
     */
    public boolean isDiscoverable() {
        return getTyped("Discoverable", Boolean.class);
    }

    /**
     * Set to true to enable device visibility for other bluetooth devices nearby.
     * @param _discoverable
     */
    public void setDiscoverable(boolean _discoverable) {
        setTyped("Discoverable", _discoverable);
    }

    /**
     * The current timeout (in seconds) configured before disabling discoverbility.<br>
     * If 0 discoverbility will never be disabled automatically.
     * @return
     */
    public Integer getDiscoverableTimeout() {
        return getTyped("DiscoverableTimeout", UInt32.class).intValue();
    }

    /**
     * Timeout (in seconds) to set before the device visibility for others will be disabled.<br>
     * If set to 0, device will stay visible forever.
     */
    public void setDiscoverableTimeout(Integer _discoverableTimeout) {
        setTyped("DiscoverableTimeout", new UInt32(_discoverableTimeout));
    }

    /**
     * True if pairing with this adapter is allowed for others.
     * @return
     */
    public boolean isPairable() {
        return getTyped("Pairable", Boolean.class);
    }

    /**
     * Set to true to allow pairing of other devices with this adapter.<br>
     * This is a global setting and effects all applications using this adapter.
     * @param _pairable
     */
    public void setPairable(boolean _pairable) {
        setTyped("Pairable", _pairable);
    }

    /**
     * Current configured timeout (in seconds) before the pairable mode is disabled.<br>
     * If 0, timeout is disabled and pairability will never be disabled.
     * @return
     */
    public Integer getPairableTimeout() {
        return getTyped("PairableTimeout", UInt32.class).intValue();
    }

    /**
     * Set the timeout (in seconds) before the pairable mode is disabled.<br>
     * Setting this to 0 disables the timeout.
     * @param _pairableTimeout
     */
    public void setPairableTimeout(Integer _pairableTimeout) {
        setTyped("PairableTimeout", new UInt32(_pairableTimeout));
    }

    /**
     * Returns the bluetooth device (MAC) address.
     * @return
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
     * @return
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
     * @return
     */
    public Integer getDeviceClass() {
        return getTyped("Class", UInt32.class).intValue();
    }

    /**
     * True if discovery procedure is active, false otherwise.
     * @return
     */
    public boolean isDiscovering() {
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
        return getTyped("UUIDs", String[].class);
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
     * @param _device
     * @throws BluezFailedException
     * @throws BluezInvalidArgumentException
     */
    public void removeDevice(Device1 _device) throws BluezFailedException, BluezInvalidArgumentException {
        adapter.RemoveDevice(_device);
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
     * @param _filter
     * @throws BluezInvalidArgumentException thrown if any arguments in the map are not supported
     * @throws BluezNotReadyException
     * @throws BluezNotSupportedException
     * @throws BluezFailedException
     */
    public void setDiscoveryFilter(Map<String, Object> _filter) throws BluezInvalidArgumentException, BluezNotReadyException, BluezNotSupportedException, BluezFailedException {

        for (Entry<String, Object> entry : _filter.entrySet()) {
            if (!supportedFilterOptions.containsKey(entry.getKey())) {
                throw new BluezInvalidArgumentException("Key " + entry.getKey() + " is not supported by Bluez library");
            }
            Class<?> typeClass = supportedFilterOptions.get(entry.getKey());
            if (!typeClass.isAssignableFrom(entry.getValue().getClass())) {
                throw new BluezInvalidArgumentException("Key " + entry.getKey() + " uses unsupported data type "
                        + entry.getValue().getClass() + ", only "+ typeClass.getName() + " is supported.");
            }
        }
        if (_filter.containsKey("Transport")) {
            String transportType = (String) _filter.get("Transport");
            if (!Arrays.asList(supportedTransportValues).contains(transportType)) {
                throw new BluezInvalidArgumentException("Transport option " + transportType + " is unsupported.");
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
