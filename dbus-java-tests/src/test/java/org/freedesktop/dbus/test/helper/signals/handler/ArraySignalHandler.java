package org.freedesktop.dbus.test.helper.signals.handler;

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
    public void handleImpl(SampleSignals.TestArraySignal _t) {
        try {
            setFailed(_t.getListOfStruct().size() != 1, "Incorrect TestArraySignal array length: should be 1, actually " + _t.getListOfStruct().size());

            logger.debug("Got a test array signal with Parameters: ");
            for (String str : _t.getListOfStruct().get(0).getValueList()) {
                logger.debug("--{}", str);
            }
            logger.debug("{}", _t.getListOfStruct().get(0).getVariantValue().getType());
            logger.debug("{}", _t.getListOfStruct().get(0).getVariantValue().getValue());

            boolean b = !(_t.getListOfStruct().get(0).getVariantValue().getValue() instanceof UInt64) || 567L != ((UInt64) _t.getListOfStruct().get(0).getVariantValue().getValue()).longValue()
                    || _t.getListOfStruct().get(0).getValueList().size() != 5 || !"hi".equals(_t.getListOfStruct().get(0).getValueList().get(0))
                    || !"hello".equals(_t.getListOfStruct().get(0).getValueList().get(1)) || !"hej".equals(_t.getListOfStruct().get(0).getValueList().get(2))
                    || !"hey".equals(_t.getListOfStruct().get(0).getValueList().get(3)) || !"aloha".equals(_t.getListOfStruct().get(0).getValueList().get(4));

            setFailed(b, "Incorrect TestArraySignal parameters");

            setFailed(_t.getMapOfIntStruct().keySet().size() != 2, "Incorrect TestArraySignal map size: should be 2, actually " + _t.getMapOfIntStruct().keySet().size());

            boolean c = !(_t.getMapOfIntStruct().get(new UInt32(1)).getVariantValue().getValue() instanceof UInt64)
                    || 678L != ((UInt64) _t.getMapOfIntStruct().get(new UInt32(1)).getVariantValue().getValue()).longValue()
                    || !(_t.getMapOfIntStruct().get(new UInt32(42)).getVariantValue().getValue() instanceof UInt64)
                    || 789L != ((UInt64) _t.getMapOfIntStruct().get(new UInt32(42)).getVariantValue().getValue()).longValue();

            setFailed(c, "Incorrect TestArraySignal parameters");

        } catch (Exception _ex) {
            _ex.printStackTrace();
            setFailed(false, "SignalHandler 2 threw an exception: ", _ex);
        }
    }
}
