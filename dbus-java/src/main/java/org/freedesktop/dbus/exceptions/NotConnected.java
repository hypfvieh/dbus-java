package org.freedesktop.dbus.exceptions;

import org.freedesktop.dbus.interfaces.FatalException;

/**
 * Thrown if a DBus action is called when not connected to the Bus.
 */
@SuppressWarnings("serial")
public class NotConnected extends DBusExecutionException implements FatalException
{
   public NotConnected(String message)
   {
      super (message);
   }
}
