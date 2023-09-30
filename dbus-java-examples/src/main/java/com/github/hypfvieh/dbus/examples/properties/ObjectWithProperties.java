package com.github.hypfvieh.dbus.examples.properties;

public class ObjectWithProperties implements InterfaceWithProperties {

    private String myProperty = "Initial value";
    private boolean myOtherProperty;
    private long myAltProperty = 123;
    private Color color = Color.RED;

    @Override
    public String getObjectPath() {
        return "/com/acme/ObjectWithProperties";
    }

    @Override
    public String sayHello() {
        return "Hello!";
    }

    @Override
    public long getMyAltProperty() {
        return myAltProperty;
    }

    @Override
    public void setMyAltProperty(long _myAltProperty) {
        this.myAltProperty = _myAltProperty;
    }

    @Override
    public int getJustAnInteger() {
        return 99;
    }

    @Override
    public String getMyProperty() {
        return myProperty;
    }

    @Override
    public void setMyProperty(String _property) {
        myProperty = _property;
    }

    @Override
    public boolean isMyOtherProperty() {
        return myOtherProperty;
    }

    @Override
    public void setMyOtherProperty(boolean _property) {
        myOtherProperty = _property;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color _color) {
        this.color = _color;
    }
}
