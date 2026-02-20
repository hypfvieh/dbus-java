package org.freedesktop.dbus;

public record DBusPath(String source, String path) implements Comparable<DBusPath> {
    public DBusPath(String _path) {
        this(null, _path);
    }

    @Override
    public String toString() {
        return path();
    }

    @Override
    public int compareTo(DBusPath _that) {
        if (path() == null || _that == null) {
            return 0;
        }
        return path().compareTo(_that.path());
    }

    /**
     * Create a DBusPath object using one or multiple string parts.
     * Leading slash will automatically appended if missing.
     *
     * @param _parts parts to build DBusPath
     * @return DBusPath
     * @throws IllegalArgumentException when no parts are given
     */
    public static DBusPath of(String... _parts) {
        if (_parts == null || _parts.length == 0) {
            throw new IllegalArgumentException("No Strings given to build DBusPath");
        }

        String pathStr = _parts[0].indexOf('/') == 0 ? "" : "/" + String.join("/", _parts);

        return new DBusPath(pathStr);
    }
}
