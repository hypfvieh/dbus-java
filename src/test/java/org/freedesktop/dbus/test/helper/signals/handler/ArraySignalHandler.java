package org.freedesktop.dbus.test.helper.signals.handler;

import static org.junit.jupiter.api.Assertions.fail;

import org.freedesktop.dbus.test.helper.signals.SampleSignals;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;

/**
 * Untyped signal handler
 */
public class ArraySignalHandler extends AbstractSignalHandler<SampleSignals.TestArraySignal> {

    public ArraySignalHandler(int _expectedRuns) {
        super(_expectedRuns);
    }

    /** Handling a signal */
    @Override
    public void handleImpl(SampleSignals.TestArraySignal t) {
        try {
            if (t.getListOfStruct().size() != 1) {
                fail("Incorrect TestArraySignal array length: should be 1, actually " + t.getListOfStruct().size());
            }
            System.out.println("Got a test array signal with Parameters: ");
            for (String str : t.getListOfStruct().get(0).getValueList()) {
                System.out.println("--" + str);
            }
            System.out.println(t.getListOfStruct().get(0).getVariantValue().getType());
            System.out.println(t.getListOfStruct().get(0).getVariantValue().getValue());
            if (!(t.getListOfStruct().get(0).getVariantValue().getValue() instanceof UInt64) || 567L != ((UInt64) t.getListOfStruct().get(0).getVariantValue().getValue()).longValue()
                    || t.getListOfStruct().get(0).getValueList().size() != 5 || !"hi".equals(t.getListOfStruct().get(0).getValueList().get(0))
                    || !"hello".equals(t.getListOfStruct().get(0).getValueList().get(1)) || !"hej".equals(t.getListOfStruct().get(0).getValueList().get(2))
                    || !"hey".equals(t.getListOfStruct().get(0).getValueList().get(3)) || !"aloha".equals(t.getListOfStruct().get(0).getValueList().get(4))) {
                fail("Incorrect TestArraySignal parameters");
            }

            if (t.getMapOfIntStruct().keySet().size() != 2) {
                fail("Incorrect TestArraySignal map size: should be 2, actually " + t.getMapOfIntStruct().keySet().size());
            }
            if (!(t.getMapOfIntStruct().get(new UInt32(1)).getVariantValue().getValue() instanceof UInt64)
                    || 678L != ((UInt64) t.getMapOfIntStruct().get(new UInt32(1)).getVariantValue().getValue()).longValue()
                    || !(t.getMapOfIntStruct().get(new UInt32(42)).getVariantValue().getValue() instanceof UInt64)
                    || 789L != ((UInt64) t.getMapOfIntStruct().get(new UInt32(42)).getVariantValue().getValue()).longValue()) {
                fail("Incorrect TestArraySignal parameters");
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail("SignalHandler 2 threw an exception: " + e);
        }
    }
}