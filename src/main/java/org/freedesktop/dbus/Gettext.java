package org.freedesktop.dbus;

/**
 * Class overridden to disable the crappy Gettext which always want display localized messages.
 * @author maniac
 *
 */
public class Gettext
{
   public static String _(String s) {
      return s;
   }
}