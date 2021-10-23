package org.freedesktop.dbus.test.helper.twopart;

import org.freedesktop.dbus.test.helper.interfaces.TwoPartObject;

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
            System.out.println("client name");
            return toString();
        }
    }
}
