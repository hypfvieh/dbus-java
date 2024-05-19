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
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public boolean equals(Object _obj) {
        if (this == _obj) {
            return true;
        } else if (_obj == null || getClass() != _obj.getClass()) {
            return false;
        }
        return Objects.equals(path, ((DBusPath) _obj).path);
    }

    @Override
    public int compareTo(DBusPath _that) {
        if (getPath() == null || _that == null) {
            return 0;
        }
        return getPath().compareTo(_that.getPath());
    }

    public void setPath(String _path) {
        path = _path;
    }
}
