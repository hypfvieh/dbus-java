/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.test;

import java.util.List;
import java.util.Map;

import org.freedesktop.DBus.Description;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Path;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * A sample signal with two parameters
 */
//CHECKSTYLE:OFF
@Description("Test interface containing signals")
public interface TestSignalInterface extends DBusInterface {
    @Description("Test basic signal")
    class TestSignal extends DBusSignal {
        public final String value;
        public final UInt32 number;

        /**
         * Create a signal.
         */
        public TestSignal(String _path, String _value, UInt32 _number) throws DBusException {
            super(_path, _value, _number);
            this.value = _value;
            this.number = _number;
        }
    }

    class StringSignal extends DBusSignal {
        public final String aoeu;

        public StringSignal(String _path, String _aoeu) throws DBusException {
            super(_path, _aoeu);
            this.aoeu = _aoeu;
        }
    }

    class EmptySignal extends DBusSignal {
        public EmptySignal(String path) throws DBusException {
            super(path);
        }
    }

    @Description("Test signal with arrays")
    class TestArraySignal extends DBusSignal {
        public final List<TestStruct2>        v;
        public final Map<UInt32, TestStruct2> m;

        public TestArraySignal(String _path, List<TestStruct2> _v, Map<UInt32, TestStruct2> _m) throws DBusException {
            super(_path, _v, _m);
            this.v = _v;
            this.m = _m;
        }
    }

    @Description("Test signal sending an object path")
    @DBusMemberName("TestSignalObject")
    class TestObjectSignal extends DBusSignal {
        public final DBusInterface otherpath;

        public TestObjectSignal(String _path, DBusInterface _otherpath) throws DBusException {
            super(_path, _otherpath);
            this.otherpath = _otherpath;
        }
    }

    class TestPathSignal extends DBusSignal {
        public final Path            otherpath;
        public final List<Path>      pathlist;
        public final Map<Path, Path> pathmap;

        public TestPathSignal(String _path, Path _otherpath, List<Path> _pathlist, Map<Path, Path> _pathmap) throws DBusException {
            super(_path, _otherpath, _pathlist, _pathmap);
            this.otherpath = _otherpath;
            this.pathlist = _pathlist;
            this.pathmap = _pathmap;
        }
    }
    //CHECKSTYLE:ON
}
