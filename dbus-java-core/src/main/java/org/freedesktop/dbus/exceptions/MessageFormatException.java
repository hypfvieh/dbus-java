package org.freedesktop.dbus.exceptions;

import org.freedesktop.dbus.interfaces.NonFatalException;

/**
 * Thrown if a message is formatted incorrectly.
 */
@SuppressWarnings("serial")
public class MessageFormatException extends DBusException implements NonFatalException
{
   public MessageFormatException(String _message)
   {
      super (_message);
   }
}
