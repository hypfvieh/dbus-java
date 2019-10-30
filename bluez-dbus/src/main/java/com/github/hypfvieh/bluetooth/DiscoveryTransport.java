package com.github.hypfvieh.bluetooth;

/**
 * Supported DiscoveryTransport values.
 */
public enum DiscoveryTransport {
    /** "auto"    - interleaved scan, default value */
    AUTO,
    /** "bredr"   - BR/EDR inquiry */
    BREDR,
    /** "le"      - LE scan only */
    LE;
    
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
