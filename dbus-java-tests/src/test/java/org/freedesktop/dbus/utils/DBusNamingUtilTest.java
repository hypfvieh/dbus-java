package org.freedesktop.dbus.utils;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.annotations.DeprecatedOnDBus;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DBusNamingUtilTest {

    @Test
    void getInterfaceNameTest() {
        assertEquals("org.freedesktop.dbus.utils.DBusNamingUtilTest.Foo", DBusNamingUtil.getInterfaceName(Foo.class));
        assertEquals("com.example.Bar", DBusNamingUtil.getInterfaceName(Bar.class));
    }

    @Test
    void getMethodNameTest() throws NoSuchMethodException {
        Method method1 = InterfaceWithMethodsAndSignals.class.getMethod("method1");
        assertEquals("method1", DBusNamingUtil.getMethodName(method1));
        Method method2 = InterfaceWithMethodsAndSignals.class.getMethod("method2");
        assertEquals("methodAnnotationName", DBusNamingUtil.getMethodName(method2));
    }

    @Test
    void getSignalNameTest() {
        Class<? extends DBusSignal> signal1 = InterfaceWithMethodsAndSignals.Signal1.class;
        assertEquals("Signal1", DBusNamingUtil.getSignalName(signal1));
        Class<? extends DBusSignal> signal2 = InterfaceWithMethodsAndSignals.Signal2.class;
        assertEquals("SignalAnnotationName", DBusNamingUtil.getSignalName(signal2));
    }

    @Test
    void getAnnotationNameTest() {
        assertEquals("org.freedesktop.DBus.Deprecated", DBusNamingUtil.getAnnotationName(DeprecatedOnDBus.class));
    }

    interface Foo extends DBusInterface {
    }

    @DBusInterfaceName("com.example.Bar")
    interface Bar extends DBusInterface {
    }

    public interface InterfaceWithMethodsAndSignals extends DBusInterface {

        void method1();

        @DBusMemberName("methodAnnotationName")
        void method2();

        class Signal1 extends DBusSignal {
            public Signal1(String objectPath, Object... args) throws DBusException {
                super(objectPath, args);
            }
        }

        @DBusMemberName("SignalAnnotationName")
        class Signal2 extends DBusSignal {
            public Signal2(String objectPath, Object... args) throws DBusException {
                super(objectPath, args);
            }
        }
    }
}
