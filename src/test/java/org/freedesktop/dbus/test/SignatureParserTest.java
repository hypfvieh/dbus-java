package org.freedesktop.dbus.test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.DBusListType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the Marshaller.getJavaType() method.
 *
 * @author mdo
 */
public final class SignatureParserTest
{
  @Test
  public void parse_complex_message_returns_correct_types() throws DBusException {
    List<Type> temp = new ArrayList<>();
    Marshalling.getJavaType("a(oa{sv})ao", temp, -1);

    Assertions.assertEquals(2, temp.size(), "result must contain two types");
    Assertions.assertTrue(temp.get(0) instanceof DBusListType);
    Assertions.assertTrue(temp.get(1) instanceof DBusListType);
  }

  @Test
  public void parse_struct_returns_correct_number_of_chars_parsed() throws Exception {
    List<Type> temp = new ArrayList<>();
    int parsedCharsCount = Marshalling.getJavaType("(oa{sv})ao", temp, 1);

    Assertions.assertEquals(8, parsedCharsCount);
  }
}
