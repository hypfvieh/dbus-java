package org.freedesktop.dbus.exceptions;

import org.freedesktop.dbus.interfaces.FatalException;

@SuppressWarnings("serial")
public class FatalDBusException extends DBusException implements FatalException
{
   public FatalDBusException(String message)
   {
      super(message);
   }
}
