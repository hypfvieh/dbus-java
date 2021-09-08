package org.freedesktop.dbus.test.helper.structs;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

public class MarkTuple extends Tuple {
    @Position(0)
    private String slotName;
    @Position(1)
    private String message;

    public MarkTuple(String slotName, String message) {
        this.slotName = slotName;
        this.message = message;
    }

    public void setSlotName(String arg) {
        slotName = arg;
    }

    public String getSlotName() {
        return slotName;
    }
    public void setMessage(String arg) {
        message = arg;
    }

    public String getMessage() {
        return message;
    }


}
