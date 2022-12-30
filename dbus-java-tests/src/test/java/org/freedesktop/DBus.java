package org.freedesktop;

import org.freedesktop.dbus.interfaces.DBusInterface;

public interface DBus extends DBusInterface {
	public interface Peer extends DBusInterface {

		public String GetMachineId();

		public void Ping();

	}
}