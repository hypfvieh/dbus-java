package com.github.hypfvieh.dbus.examples.nested;

import java.util.List;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;

import com.github.hypfvieh.dbus.examples.nested.data.MyInterface;
import com.github.hypfvieh.dbus.examples.nested.data.MyInterfacePart;
import com.github.hypfvieh.dbus.examples.nested.data.MyObject;
import com.github.hypfvieh.dbus.examples.nested.data.MyObjectPart;

/**
 * Sample which demonstrates on how to export nested objects.
 * <p>
 * Nested means that you export objects inside of another object.
 * <p>
 * In this sample, an object is created which contains a two other objects which
 * are also exported to the bus but can also be retrieved by calling {@link MyObject#getParts()}.
 *  
 * @author brettsmith / hypfvieh
 * 
 * @since 4.1.0 - 2022-03-21
 */
public class ExportNested {
        public static void main(String[] args) throws Exception {
            try (DBusConnection conn = DBusConnectionBuilder.forSessionBus().build()) {
                // create first object
                MyObjectPart part1 = new MyObjectPart();
                part1.setVal1("ABC");
                part1.setVal2("123");

                // create second object
                MyObjectPart part2 = new MyObjectPart();
                part2.setVal1("DEF");
                part2.setVal2("456");
                
                // create parent which contains both created objects
                MyObject myIface = new MyObject();
                myIface.getParts().addAll(List.of(part1, part2));

                conn.requestBusName("com.acme");
                // export everything to bus
                conn.exportObject(part1);
                conn.exportObject(part2);
                conn.exportObject(myIface);
                
                try (DBusConnection innerConn = DBusConnectionBuilder.forSessionBus().build()) {
                    // get the exported parent
                    MyInterface myObject = innerConn.getRemoteObject("com.acme", "/com/acme/MyObject", MyInterface.class);
                    
                    // Print hello from 'parent' object
                    System.out.println("> " + myObject.sayHello());
                    
                    // print all names used in child objects
                    for(String part : myObject.getPartNames()) {
                        System.out.println("  " + part);
                    }
                    
                    // print the child objects
                    for(MyInterfacePart part : myObject.getParts()) {
                        System.out.println("  " + part.getObjectPath() + " = " +  part.getVal1() + " / " + part.getVal2());
                    }
                }
            }
        }
}
