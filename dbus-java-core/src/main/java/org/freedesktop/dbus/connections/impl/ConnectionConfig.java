package org.freedesktop.dbus.connections.impl;

import org.freedesktop.dbus.connections.IDisconnectCallback;
import org.freedesktop.dbus.messages.DBusSignal;

import java.util.function.Consumer;

public class ConnectionConfig {
    private boolean exportWeakReferences;
    private boolean importWeakReferences;
    private IDisconnectCallback disconnectCallback;
    private Consumer<DBusSignal> unknownSignalHandler;
    private boolean autoEmitPropertiesChanged;
    private boolean manualObjectManager;

    public boolean isExportWeakReferences() {
        return exportWeakReferences;
    }

    public void setExportWeakReferences(boolean _exportWeakReferences) {
        exportWeakReferences = _exportWeakReferences;
    }

    public boolean isImportWeakReferences() {
        return importWeakReferences;
    }

    public void setImportWeakReferences(boolean _importWeakReferences) {
        importWeakReferences = _importWeakReferences;
    }

    public IDisconnectCallback getDisconnectCallback() {
        return disconnectCallback;
    }

    public void setDisconnectCallback(IDisconnectCallback _disconnectCallback) {
        disconnectCallback = _disconnectCallback;
    }

    public Consumer<DBusSignal> getUnknownSignalHandler() {
        return unknownSignalHandler;
    }

    public void setUnknownSignalHandler(Consumer<DBusSignal> _unknownSignalHandler) {
        unknownSignalHandler = _unknownSignalHandler;
    }

    public boolean isAutoEmitPropertiesChanged() {
        return autoEmitPropertiesChanged;
    }

    public void setAutoEmitPropertiesChanged(boolean _autoEmitPropertiesChanged) {
        autoEmitPropertiesChanged = _autoEmitPropertiesChanged;
    }

    public boolean isManualObjectManager() {
        return manualObjectManager;
    }

    public void setManualObjectManager(boolean _manualObjectManager) {
        manualObjectManager = _manualObjectManager;
    }

}
