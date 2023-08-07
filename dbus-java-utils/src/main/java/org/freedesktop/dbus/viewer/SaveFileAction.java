/*
   D-Bus Java Viewer
   Copyright (c) 2006 Peter Cox

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.viewer;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.swing.Action;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")
final class SaveFileAction extends TabbedSaveAction implements ChangeListener {
    SaveFileAction(JTabbedPane _tabbedPane) {
        super(_tabbedPane);

        enableAndSetName();

        _tabbedPane.addChangeListener(this);
    }

    @Override
    public void stateChanged(ChangeEvent _event) {
        enableAndSetName();
    }

    /**
     * Enable and set the name of the action based on the shown tab
     */
    void enableAndSetName() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        boolean enabled = selectedIndex > -1;
        putValue(Action.NAME, "Save " + getFileName(selectedIndex) + "...");
        setEnabled(enabled);
    }

    @Override
    public Iterator<TextFile> iterator() {
        return new SelectedTabIterator();
    }

    private final class SelectedTabIterator implements Iterator<TextFile> {
        // CHECKSTYLE:OFF
        boolean iterated = false;
        // CHECKSTYLE:ON

        @Override
        public boolean hasNext() {
            return !iterated;
        }

        @Override
        public TextFile next() {
            if (iterated) {
                throw new NoSuchElementException("Already iterated");
            }
            iterated = true;
            return getTextFile(tabbedPane.getSelectedIndex());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }
}
