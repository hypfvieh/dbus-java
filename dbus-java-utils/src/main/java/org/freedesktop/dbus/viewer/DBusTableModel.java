/*
   D-Bus Java Viewer
   Copyright (c) 2006 Peter Cox

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.viewer;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
class DBusTableModel extends AbstractTableModel {
    private static final String INTROSPECTABLE = "introspectable?";

    private static final String OWNER          = "owner";

    private static final String USER           = "user";

    private static final String NAME           = "name";

    private static final String PATH           = "path";

    private final String[]       columns        = {
            NAME, PATH, USER, OWNER, INTROSPECTABLE
    };

    private final List<DBusEntry> entries        = new ArrayList<>();

    /** {@inheritDoc} */
    @Override
    public int getRowCount() {
        return entries.size();
    }

    /** Add a row to the table model
     *
     * @param _entry The dbus entry to add
     */
    public void add(DBusEntry _entry) {
        entries.add(_entry);
    }

    /** {@inheritDoc} */
    @Override
    public int getColumnCount() {
        return columns.length;
    }

    /** {@inheritDoc} */
    @Override
    public String getColumnName(int _column) {
        return columns[_column];
    }

    /** Get a row of the table
     * @param _row The row index
     * @return The table row
     */
    public DBusEntry getEntry(int _row) {
        return entries.get(_row);
    }

    /** {@inheritDoc} */
    @Override
    public Class<?> getColumnClass(int _columnIndex) {
        String columnName = getColumnName(_columnIndex);
        if (columnName.equals(NAME)) {
            return String.class;
        }
        return switch (columnName) {
            case PATH  -> String.class;
            case USER  -> Object.class;
            case OWNER -> String.class;
            case INTROSPECTABLE -> Boolean.class;
            default -> super.getColumnClass(_columnIndex);
        };
    }

    /** {@inheritDoc} */
    @Override
    public Object getValueAt(int _rowIndex, int _columnIndex) {
        DBusEntry entry = getEntry(_rowIndex);
        String columnName = getColumnName(_columnIndex);
        if (columnName.equals(NAME)) {
            return entry.getName();
        }
        return switch (columnName) {
            case PATH  -> entry.getPath();
            case USER  -> entry.getUser();
            case OWNER -> entry.getOwner();
            case INTROSPECTABLE -> entry.getIntrospectable() != null;
            default -> null;
        };
    }

}
