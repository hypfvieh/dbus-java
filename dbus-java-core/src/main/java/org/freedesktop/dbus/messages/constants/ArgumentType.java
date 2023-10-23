package org.freedesktop.dbus.messages.constants;

/**
 * Defines constants for each argument type. There are two constants for each argument type, as a byte or as a
 * String (the _STRING version)
 *
 * @since 5.0.0 - 2023-10-23
 */
public interface ArgumentType {
    String BYTE_STRING           = "y";
    String BOOLEAN_STRING        = "b";
    String INT16_STRING          = "n";
    String UINT16_STRING         = "q";
    String INT32_STRING          = "i";
    String UINT32_STRING         = "u";
    String INT64_STRING          = "x";
    String UINT64_STRING         = "t";
    String DOUBLE_STRING         = "d";
    String FLOAT_STRING          = "f";
    String STRING_STRING         = "s";
    String OBJECT_PATH_STRING    = "o";
    String SIGNATURE_STRING      = "g";
    String FILEDESCRIPTOR_STRING = "h";
    String ARRAY_STRING          = "a";
    String VARIANT_STRING        = "v";
    String STRUCT_STRING         = "r";
    String STRUCT1_STRING        = "(";
    String STRUCT2_STRING        = ")";
    String DICT_ENTRY_STRING     = "e";
    String DICT_ENTRY1_STRING    = "{";
    String DICT_ENTRY2_STRING    = "}";

    byte   BYTE                  = 'y';
    byte   BOOLEAN               = 'b';
    byte   INT16                 = 'n';
    byte   UINT16                = 'q';
    byte   INT32                 = 'i';
    byte   UINT32                = 'u';
    byte   INT64                 = 'x';
    byte   UINT64                = 't';
    byte   DOUBLE                = 'd';
    byte   FLOAT                 = 'f';
    byte   STRING                = 's';
    byte   OBJECT_PATH           = 'o';
    byte   SIGNATURE             = 'g';
    byte   FILEDESCRIPTOR        = 'h';
    byte   ARRAY                 = 'a';
    byte   VARIANT               = 'v';
    byte   STRUCT                = 'r';
    byte   STRUCT1               = '(';
    byte   STRUCT2               = ')';
    byte   DICT_ENTRY            = 'e';
    byte   DICT_ENTRY1           = '{';
    byte   DICT_ENTRY2           = '}';
}
