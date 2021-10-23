package org.freedesktop.dbus.exceptions;

import java.io.IOException;

import org.freedesktop.dbus.interfaces.FatalException;

@SuppressWarnings("serial")
public class MessageProtocolVersionException extends IOException implements FatalException
{
   public MessageProtocolVersionException(String message)
   {
      super(message);
   }
}
