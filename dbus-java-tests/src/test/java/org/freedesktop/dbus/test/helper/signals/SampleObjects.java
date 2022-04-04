package org.freedesktop.dbus.test.helper.signals;

import org.freedesktop.dbus.interfaces.DBusInterface;

public abstract class SampleObjects {
	public interface SampleInterface extends DBusInterface {
		String getValue();
	}
	
	public static class SampleObject implements SampleInterface {
		
		private String value;
		
		public SampleObject(String value) {
			this.value = value;
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public String getObjectPath() {
			return null;
		}

	}
}
