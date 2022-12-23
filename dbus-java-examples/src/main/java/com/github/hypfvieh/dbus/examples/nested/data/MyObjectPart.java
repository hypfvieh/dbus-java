package com.github.hypfvieh.dbus.examples.nested.data;

public class MyObjectPart implements MyInterfacePart {

    private String val1;
    private String val2;

    @Override
    public String getVal1() {
        return val1;
    }

    public void setVal1(String _val1) {
        this.val1 = _val1;
    }

    @Override
    public String getVal2() {
        return val2;
    }

    public void setVal2(String _val2) {
        this.val2 = _val2;
    }

    @Override
    public String getObjectPath() {
        return "/com/acme/MyPart" + val1;
    }
}
