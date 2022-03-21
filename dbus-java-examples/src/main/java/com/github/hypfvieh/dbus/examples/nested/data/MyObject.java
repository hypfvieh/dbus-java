package com.github.hypfvieh.dbus.examples.nested.data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MyObject implements MyInterface {
    
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