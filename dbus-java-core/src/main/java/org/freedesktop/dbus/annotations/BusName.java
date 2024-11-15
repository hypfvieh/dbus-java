package org.freedesktop.dbus.annotations;


import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface BusName {
    String value();
}
