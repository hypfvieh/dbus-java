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

import javax.swing.JTabbedPane;

@SuppressWarnings("serial")
final class SaveAllAction extends TabbedSaveAction {

    protected SaveAllAction(JTabbedPane _tabbedPane) {
        super(_tabbedPane, "Save All...");
    }

    @Override
    public Iterator<TextFile> iterator() {
        return new TabIterator();
    }

    private final class TabIterator implements Iterator<TextFile> {
        private int i = 0;

        @Override
        public boolean hasNext() {
            return i < tabbedPane.getTabCount();
        }

        @Override
        public TextFile next() {
            if (hasNext()) {
                int currentIndex = i;
                i++;
                return getTextFile(currentIndex);
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

}
