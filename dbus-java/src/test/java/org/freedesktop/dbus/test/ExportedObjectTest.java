package org.freedesktop.dbus.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.messages.ExportedObject;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;

public class ExportedObjectTest {

    /**
     * Test create introspection data for a class implementing
     * {@link DBusInterface} directly does not have any additional interfaces.
     * @throws DBusException should never happen
     */
    @Test
    void testNoInterface() throws DBusException {
        ExportedObject exportedObject = new ExportedObject(new NoInterface(), false);
        String introspectiondata = exportedObject.getIntrospectiondata();
        checkIntrospectionData(introspectiondata);
    }

    /**
     * Test creation of introspection data for a class implementing multiple
     * interfaces inheriting from {@link DBusInterface} and {@link Properties}.
     *
     * @throws DBusException should never happen
     */
    @Test
    void testMultiInterface() throws DBusException {
        ExportedObject exportedObject = new ExportedObject(new MultiInterface(), false);
        checkIntrospectionData(exportedObject.getIntrospectiondata());
    }

    /**
     * Test creation of introspection data for a class implementing
     * {@link DBusInterface} and {@link Properties}.
     *
     * @throws DBusException should never happen
     */
    @Test
    void testOneInterface() throws DBusException {
        ExportedObject exportedObject = new ExportedObject(new OneInterface(), false);
        checkIntrospectionData(exportedObject.getIntrospectiondata());
    }

    /**
     * Test creation of introspection data for a class implementing
     * {@link Properties} only.
     *
     * @throws DBusException should never happen
     */
    @Test
    void testIndirectInterface() throws DBusException {
        ExportedObject exportedObject = new ExportedObject(new IndirectInterface(), false);
        checkIntrospectionData(exportedObject.getIntrospectiondata());
    }

    /**
     * Checks the introspection string for duplicate interfaces or method names in interfaces.
     *
     * @param _introspectiondata data to investigate
     */
    private void checkIntrospectionData(String _introspectiondata) {
        if (_introspectiondata == null || _introspectiondata.isBlank()) {
            fail("Empty/null introspection data");
        }
        Pattern ifNamePattern = Pattern.compile("<interface.+name=\"([^\"]+)\".+");
        Pattern mthNamePattern = Pattern.compile("<method.+name=\"([^\"]+)\".+");

        Map<String, List<String>> interfacesAndMethods = new LinkedHashMap<>();
        String ifName = null;
        for (String line : _introspectiondata.split("\n")) {
            line = line.trim();

            if (line.contains("</interface>")) {
                ifName = null;
            }
            if (line.contains("<interface ")) {
                Matcher ifMatcher = ifNamePattern.matcher(line);
                ifMatcher.find();
                ifName = ifMatcher.group(1);
                if (!interfacesAndMethods.containsKey(ifName)) {
                    interfacesAndMethods.put(ifName, new ArrayList<>());
                } else {
                    fail("Duplicated exported interface: " + ifName);
                }
            }
            if (ifName != null && line.contains("<method ")) {
                Matcher mthMatcher = mthNamePattern.matcher(line);
                mthMatcher.find();
                String mthName = mthMatcher.group(1);

                if (interfacesAndMethods.get(ifName).contains(mthName)) {
                    fail("Duplicated method in interface: " + ifName);
                }
                interfacesAndMethods.get(ifName).add(mthName);
            }
        }
    }

    public static class MultiInterface implements BarFace, FooFace {

        @Override
        public String getObjectPath() {
            return "/foobar";
        }

        @Override
        public void foo() {
        }

        @Override
        public void bar() {
        }

        @Override
        public <A> A Get(String _interface_name, String _property_name) {
            return null;
        }

        @Override
        public <A> void Set(String _interface_name, String _property_name, A _value) {

        }

        @Override
        public Map<String, Variant<?>> GetAll(String _interface_name) {
            return null;
        }

    }

    public interface BarFace extends DBusInterface, Properties {
        void bar();
    }

    public interface FooFace extends DBusInterface, Properties {
        void foo();
    }

    public static class NoInterface implements DBusInterface {

        public void why() {

        }

        @Override
        public String getObjectPath() {
            return "/test";
        }

    }

    public static class OneInterface implements DBusInterface, Properties {
        @Override
        public String getObjectPath() {
            return "/test";
        }

        @Override
        public <A> A Get(String _interface_name, String _property_name) {
            return null;
        }

        @Override
        public <A> void Set(String _interface_name, String _property_name, A _value) {
        }

        @Override
        public Map<String, Variant<?>> GetAll(String _interface_name) {
            return null;
        }
    }

    public static class IndirectInterface implements Properties {
        @Override
        public String getObjectPath() {
            return null;
        }

        @Override
        public <A> A Get(String _interface_name, String _property_name) {
            return null;
        }

        @Override
        public <A> void Set(String _interface_name, String _property_name, A _value) {
        }

        @Override
        public Map<String, Variant<?>> GetAll(String _interface_name) {
            return null;
        }
    }
}
