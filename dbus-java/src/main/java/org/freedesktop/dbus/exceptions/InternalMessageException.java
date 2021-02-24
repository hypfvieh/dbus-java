package org.freedesktop.dbus.exceptions;

import org.freedesktop.dbus.interfaces.NonFatalException;

@SuppressWarnings("serial")
public class InternalMessageException extends DBusExecutionException implements NonFatalException
{
   public InternalMessageException(String message)
   {
      super (message);
   }
}
