/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.types;

/**
 * Class to represent 16-bit unsigned integers.
 */
@SuppressWarnings("serial")
public class UInt16 extends Number implements Comparable<UInt16> {
    /** Maximum possible value. */
    public static final int MAX_VALUE = 65535;
    /** Minimum possible value. */
    public static final int MIN_VALUE = 0;
    private int             value;

    /** Create a UInt16 from an int.
    * @param _value Must be within MIN_VALUE&ndash;MAX_VALUE
    * @throws NumberFormatException if value is not between MIN_VALUE and MAX_VALUE
    */
    public UInt16(int _value) {
        if (_value < MIN_VALUE || _value > MAX_VALUE) {
            throw new NumberFormatException(String.format("%s is not between %s and %s.", _value, MIN_VALUE, MAX_VALUE));
        }
        this.value = _value;
    }

    /** Create a UInt16 from a String.
    * @param _value Must parse to a valid integer within MIN_VALUE&ndash;MAX_VALUE
    * @throws NumberFormatException if value is not an integer between MIN_VALUE and MAX_VALUE
    */
    public UInt16(String _value) {
        this(Integer.parseInt(_value));
    }

    /** The value of this as a byte. */
    @Override
    public byte byteValue() {
        return (byte) value;
    }

    /** The value of this as a double. */
    @Override
    public double doubleValue() {
        return value;
    }

    /** The value of this as a float. */
    @Override
    public float floatValue() {
        return value;
    }

    /** The value of this as a int. */
    @Override
    public int intValue() {
        return /*(int)*/ value;
    }

    /** The value of this as a long. */
    @Override
    public long longValue() {
        return value;
    }

    /** The value of this as a short. */
    @Override
    public short shortValue() {
        return (short) value;
    }

    /** Test two UInt16s for equality. */
    @Override
    public boolean equals(Object o) {
        return o instanceof UInt16 && ((UInt16) o).value == this.value;
    }

    @Override
    public int hashCode() {
        return /*(int)*/ value;
    }

    /** Compare two UInt16s.
    * @return 0 if equal, -ve or +ve if they are different.
    */
    @Override
    public int compareTo(UInt16 other) {
        return /*(int)*/ (this.value - other.value);
    }

    /** The value of this as a string. */
    @Override
    public String toString() {
        return "" + value;
    }
}
