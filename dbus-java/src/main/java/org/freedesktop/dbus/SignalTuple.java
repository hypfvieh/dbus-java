/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus;

import java.util.HashSet;
import java.util.Set;

public class SignalTuple {
    private String type;
    private String name;
    private String object;
    private String source;

    public SignalTuple(String _type, String _name, String _object, String _source) {
        this.type = _type;
        this.name = _name;
        this.object = _object;
        this.source = _source;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SignalTuple)) {
            return false;
        }
        SignalTuple other = (SignalTuple) o;
        if (null == this.type && null != other.type) {
            return false;
        }
        if (null != this.type && !this.type.equals(other.type)) {
            return false;
        }
        if (null == this.name && null != other.name) {
            return false;
        }
        if (null != this.name && !this.name.equals(other.name)) {
            return false;
        }
        if (null == this.object && null != other.object) {
            return false;
        }
        if (null != this.object && !this.object.equals(other.object)) {
            return false;
        }
        if (null == this.source && null != other.source) {
            return false;
        }
        if (null != this.source && !this.source.equals(other.source)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return (null == type ? 0 : type.hashCode()) + (null == name ? 0 : name.hashCode()) + (null == source ? 0 : source.hashCode()) + (null == object ? 0 : object.hashCode());
    }

    @Override
    public String toString() {
        return "SignalTuple(" + type + "," + name + "," + object + "," + source + ")";
    }

    /**
     * Get a {@link Set} of all possible SignalTuples that we can have, given the 4 parameters.
     * @param _type interface type
     * @param _name name
     * @param _object object
     * @param _source source
     * @return {@link Set} of {@link SignalTuple}, never null
     */
    public static Set<SignalTuple> getAllPossibleTuples(String _type, String _name, String _object, String _source){
        Set<SignalTuple> allTuples = new HashSet<>();

        // Tuple with no null
        allTuples.add(new SignalTuple(_type, _name, _object, _source));

        // Tuples with one null
        allTuples.add(new SignalTuple(null, _name, _object, _source));
        allTuples.add(new SignalTuple(_type, null, _object, _source));
        allTuples.add(new SignalTuple(_type, _name, null, _source));
        allTuples.add(new SignalTuple(_type, _name, _object, null));

        // Tuples where type is null, and one other null
        allTuples.add(new SignalTuple(null, null, _object, _source));
        allTuples.add(new SignalTuple(null, _name, null, _source));
        allTuples.add(new SignalTuple(null, _name, _object, null));

        // Tuples where name is null, and one other null
        allTuples.add(new SignalTuple(_type, null, null, _source));
        allTuples.add(new SignalTuple(_type, null, _object, null));

        // Tuples where object is null, and one other null
        allTuples.add(new SignalTuple(null, _name, null, _source));
        allTuples.add(new SignalTuple(_type, _name, null, null));

        // Tuples where source is null, and one other null
        allTuples.add(new SignalTuple(null, _name, _object, null));
        allTuples.add(new SignalTuple(_type, _name, null, null));

        // Tuples with three nulls
        allTuples.add(new SignalTuple(_type, null, null, null));
        allTuples.add(new SignalTuple(null, _name, null, null));
        allTuples.add(new SignalTuple(null, null, _object, null));
        allTuples.add(new SignalTuple(null, null, null, _source));

        return allTuples;
    }
}
