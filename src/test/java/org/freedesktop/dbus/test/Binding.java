package org.freedesktop.dbus.test;

import org.freedesktop.DBus.Description;
import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.DBusSignal;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.UInt16;
import org.freedesktop.dbus.UInt32;
import org.freedesktop.dbus.UInt64;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.exceptions.DBusException;

/**
* Contains Binding-test interfaces
*/
public interface Binding {
    public interface SingleTests extends DBusInterface {
        @Description("Returns the sum of the values in the input list")
        UInt32 Sum(byte[] a);
    }

    public interface TestClient extends DBusInterface {
        @Description("when the trigger signal is received, this method should be called on the sending process/object.")
        void Response(UInt16 a, double b);

        @Description("Causes a callback")
        class Trigger extends DBusSignal {
            public final UInt16 a;
            public final double b;

            public Trigger(String path, UInt16 _a, double _b) throws DBusException {
                super(path, _a, _b);
                this.a = _a;
                this.b = _b;
            }
        }

    }

    public interface TestSignals extends DBusInterface {
        @Description("Sent in response to a method call")
        class Triggered extends DBusSignal {
            public final UInt64 a;

            public Triggered(String _path, UInt64 _a) throws DBusException {
                super(_path, _a);
                this.a = _a;
            }
        }
    }

    final class Triplet<A, B, C> extends Tuple {
        @Position(0)
        public final A a;
        @Position(1)
        public final B b;
        @Position(2)
        public final C c;

        public Triplet(A _a, B _b, C _c) {
            this.a = _a;
            this.b = _b;
            this.c = _c;
        }
    }

    final class TestStruct extends Struct {
        @Position(0)
        public final String a;
        @Position(1)
        public final UInt32 b;
        @Position(2)
        public final Short  c;

        public TestStruct(String _a, UInt32 _b, Short _c) {
            this.a = _a;
            this.b = _b;
            this.c = _c;
        }
    }
}