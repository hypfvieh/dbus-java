package org.freedesktop.dbus.test.helper.twopart;

import org.freedesktop.dbus.test.helper.interfaces.TwoPartObject;
import org.slf4j.LoggerFactory;

public final class TwoPartTestClient {

    private TwoPartTestClient() {

    }

    public static class TwoPartTestObject implements TwoPartObject {
        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public String getObjectPath() {
            return null;
        }

        @Override
        public String getName() {
            LoggerFactory.getLogger(getClass()).debug("client name");
            return toString();
        }
    }
}
