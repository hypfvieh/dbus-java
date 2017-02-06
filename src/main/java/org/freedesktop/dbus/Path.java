/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus;

public class Path implements Comparable<Path> {
    // CHECKSTYLE:OFF
    protected String path;
    // CHECKSTYLE:ON

    public Path(String _path) {
        this.path = _path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof Path) && path.equals(((Path) other).path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public int compareTo(Path that) {
        return path.compareTo(that.path);
    }
}
