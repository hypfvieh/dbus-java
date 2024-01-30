package com.github.hypfvieh.dbus.examples.nested.data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MyObject implements MyInterface {

    private final List<MyInterfacePart> parts = new ArrayList<>();

    @Override
    public String sayHello() {
        return "Hello!";
    }

    @Override
    public List<MyInterfacePart> getParts() {
        return parts;
    }

    @Override
    public String getObjectPath() {
        return "/com/acme/MyObject";
    }

    @Override
    public List<String> getPartNames() {
        return parts.stream().map(MyInterfacePart::getVal1).collect(Collectors.toList());
    }
}
