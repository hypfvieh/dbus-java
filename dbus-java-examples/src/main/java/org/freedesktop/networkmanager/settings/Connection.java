package org.freedesktop.networkmanager.settings;

import java.util.Map;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

@DBusInterfaceName("org.freedesktop.NetworkManager.Settings.Connection")
public interface Connection extends DBusInterface {

    void Update(Map<String, Map<String, Variant<?>>> _properties);

    void UpdateUnsaved(Map<String, Map<String, Variant<?>>> _properties);

    void Delete();

    Map<String, Map<String, Variant<?>>> GetSettings();

    Map<String, Map<String, Variant<?>>> GetSecrets(String _settingName);

    void ClearSecrets();

    void Save();

    Map<String, Variant<?>> Update2(Map<String, Map<String, Variant<?>>> _settings, UInt32 _flags,
            Map<String, Variant<?>> _args);

    class Updated extends DBusSignal {

        public Updated(String _path) throws DBusException {
            super(_path);
        }
    }

    class Removed extends DBusSignal {

        public Removed(String _path) throws DBusException {
            super(_path);
        }
    }

    class PropertyNames {

        public static final String Unsaved = "Unsaved";
        public static final String Flags = "Flags";
        public static final String Filename = "Filename";
    }
}