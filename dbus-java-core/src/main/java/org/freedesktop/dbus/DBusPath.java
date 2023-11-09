package org.freedesktop.dbus;

import java.util.Objects;

public class DBusPath implements Comparable<DBusPath> {
    private String path;

    public DBusPath(String _path) {
        setPath(_path);
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return getPath();
    }

    @Override
    public boolean equals(Object _other) {
        return _other instanceof DBusPath dp && getPath() != null && getPath().equals(dp.getPath());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(path);
        return result;
    }

    @Override
    public int compareTo(DBusPath _that) {
        if (getPath() == null) {
            return 0;
        } else if (_that == null) {
            return 0;
        }
        return getPath().compareTo(_that.getPath());
    }

    public void setPath(String _path) {
        path = _path;
    }
}
