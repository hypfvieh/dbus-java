package org.freedesktop.dbus.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.junit.jupiter.api.Test;

public class ExportNestedTest extends AbstractDBusBaseTest {

    @Test
    public void testExportNested() throws IOException, DBusException {
        try (DBusConnection conn = DBusConnectionBuilder.forSessionBus().build()) {
            var part1 = new MyObjectPart();
            part1.setVal1("ABC");
            part1.setVal2("123");

            var part2 = new MyObjectPart();
            part2.setVal1("DEF");
            part2.setVal2("456");
            
            var myIface = new MyObject();
            myIface.getParts().addAll(Arrays.asList(part1, part2));
            
            conn.requestBusName("com.acme");
            conn.exportObject(part1);
            conn.exportObject(part2);
            conn.exportObject(myIface);
            
            try (DBusConnection innerConn = DBusConnectionBuilder.forSessionBus().build()) {
                var myObject = innerConn.getRemoteObject("com.acme", "/com/acme/MyObject", MyInterface.class);
                
                // hello from 'parent' object
                assertEquals("Hello!", myObject.sayHello());
                
                List<String> partNames = myObject.getPartNames();
                assertEquals(2, partNames.size());
                
                // all names used in child objects
                assertEquals(partNames.get(0), "ABC");
                assertEquals(partNames.get(1), "DEF");
                
                List<MyInterfacePart> parts = myObject.getParts();
                assertEquals(2, parts.size());
                // check the child objects
                assertEquals("123", parts.get(0).getVal2());
                assertEquals("456", parts.get(1).getVal2());
            }
        }
    }
    
    @DBusInterfaceName("com.acme.MyInterface")
    public interface MyInterface extends DBusInterface {
        
        String sayHello();
        
        List<MyInterfacePart> getParts();
        
        List<String> getPartNames();
    }
    
    public static class MyObject implements MyInterface {
        
        private List<MyInterfacePart> parts = new ArrayList<>();
        
        public String sayHello() {
            return "Hello!";
        }
        
        public List<MyInterfacePart> getParts() {
            return parts;
        }

        public String getObjectPath() {
            return "/com/acme/MyObject";
        }

        @Override
        public List<String> getPartNames() {
            return parts.stream().map(i -> i.getVal1()).collect(Collectors.toList());
        }
    }

    @DBusInterfaceName("com.acme.MyInterfacePart")
    public interface MyInterfacePart extends DBusInterface {
        
        String getVal1();

        String getVal2();
    }

    public static class MyObjectPart implements MyInterfacePart {
        
        private String val1;
        private String val2;
        
        public String getVal1() {
            return val1;
        }

        public void setVal1(String val1) {
            this.val1 = val1;
        }

        public String getVal2() {
            return val2;
        }

        public void setVal2(String val2) {
            this.val2 = val2;
        }

        public String getObjectPath() {
            return "/com/acme/MyPart" + val1;
        }
    }
}
