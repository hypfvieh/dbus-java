package org.freedesktop.dbus.connections.config;

import java.util.OptionalLong;

import org.freedesktop.dbus.connections.SASL.SaslMode;

/**
 * Bean contains configuration for SASL authentication.
 *
 * @author hypfvieh
 *
 * @since 4.1.1 - 2022-07-22
 */
public class SaslConfig {
    private SaslMode     mode;
    private int          authMode;
    private String       guid;
    private OptionalLong saslUid;

    public SaslMode getMode() {
        return mode;
    }

    public void setMode(SaslMode _mode) {
        mode = _mode;
    }

    public int getAuthMode() {
        return authMode;
    }

    public void setAuthMode(int _types) {
        authMode = _types;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String _guid) {
        guid = _guid;
    }

    public OptionalLong getSaslUid() {
        return saslUid;
    }

    public void setSaslUid(OptionalLong _saslUid) {
        saslUid = _saslUid;
    }
}
