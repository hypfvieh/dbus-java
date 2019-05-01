package org.freedesktop.dbus.test.helper.interfaces;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.IntrospectionDescription;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;

/**
* Contains Binding-test interfaces
*/
public interface Binding {
    public interface SingleSample extends DBusInterface {
        @IntrospectionDescription("Returns the sum of the values in the input list")
        UInt32 Sum(byte[] a);
    }

    public interface SampleClient extends DBusInterface {
        @IntrospectionDescription("when the trigger signal is received, this method should be called on the sending process/object.")
        void Response(UInt16 a, double b);

        @IntrospectionDescription("Causes a callback")
        class Trigger extends DBusSignal {
            private final UInt16 sampleUint16;
            private final double sampleDouble;

            public Trigger(String path, UInt16 _a, double _b) throws DBusException {
                super(path, _a, _b);
                this.sampleUint16 = _a;
                this.sampleDouble = _b;
            }

            public UInt16 getSampleUint16() {
                return sampleUint16;
            }

            public double getSampleDouble() {
                return sampleDouble;
            }
        }

    }

    public interface SampleSignals extends DBusInterface {
        @IntrospectionDescription("Sent in response to a method call")
        class Triggered extends DBusSignal {
            private final UInt64 sampleUint64;

            public Triggered(String _path, UInt64 _a) throws DBusException {
                super(_path, _a);
                this.sampleUint64 = _a;
            }

            public UInt64 getSampleUint64() {
                return sampleUint64;
            }
        }
    }

    final class Triplet<A, B, C> extends Tuple {
        @Position(0)
        private final A first;
        @Position(1)
        private final B second;
        @Position(2)
        private final C third;

        public Triplet(A _a, B _b, C _c) {
            this.first = _a;
            this.second = _b;
            this.third = _c;
        }

        public A getFirst() {
            return first;
        }

        public B getSecond() {
            return second;
        }

        public C getThird() {
            return third;
        }
    }

    final class CrossSampleStruct extends Struct {
        @Position(0)
        private final String sampleString;
        @Position(1)
        private final UInt32 sampleUint32;
        @Position(2)
        private final Short  sampleShort;

        public CrossSampleStruct(String _a, UInt32 _b, Short _c) {
            this.sampleString = _a;
            this.sampleUint32 = _b;
            this.sampleShort = _c;
        }

        public String getSampleString() {
            return sampleString;
        }

        public UInt32 getSampleUint32() {
            return sampleUint32;
        }

        public Short getSampleShort() {
            return sampleShort;
        }
    }
}