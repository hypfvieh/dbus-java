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

public class DBusPath implements Comparable<DBusPath> {
    private String path;

    public DBusPath(String _path) {
        this.setPath(_path);
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return getPath();
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof DBusPath) && getPath().equals(((DBusPath) other).getPath());
    }

    @Override
    public int hashCode() {
        return getPath().hashCode();
    }

    @Override
    public int compareTo(DBusPath that) {
        return getPath().compareTo(that.getPath());
    }

    public void setPath(String _path) {
        path = _path;
    }
}
