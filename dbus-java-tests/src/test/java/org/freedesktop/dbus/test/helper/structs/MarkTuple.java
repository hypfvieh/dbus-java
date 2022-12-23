package org.freedesktop.dbus.test.helper.structs;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

public class MarkTuple extends Tuple {
    @Position(0)
    private String slotName;
    @Position(1)
    private String message;

    public MarkTuple(String _slotName, String _message) {
        this.slotName = _slotName;
        this.message = _message;
    }

    public void setSlotName(String _arg) {
        slotName = _arg;
    }

    public String getSlotName() {
        return slotName;
    }
    public void setMessage(String _arg) {
        message = _arg;
    }

    public String getMessage() {
        return message;
    }

}
