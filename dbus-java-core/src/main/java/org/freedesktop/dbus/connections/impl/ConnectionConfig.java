package org.freedesktop.dbus.connections.impl;

import org.freedesktop.dbus.connections.IDisconnectCallback;

public class ConnectionConfig {
    private boolean exportWeakReferences;
    private boolean importWeakReferences;
    private IDisconnectCallback disconnectCallback;

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

}
