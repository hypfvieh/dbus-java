package com.github.hypfvieh.dbus.examples.nested.data;

public class MyObjectPart implements MyInterfacePart {
    
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