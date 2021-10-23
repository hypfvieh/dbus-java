package org.freedesktop.dbus.utils;

import java.io.PrintStream;

public final class Hexdump {
    public static final char[] HEX_CHARS = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private Hexdump() {

    }

    public static String toHex(byte[] buf) {
        return toHex(buf, 0, buf.length);
    }

    public static String toHex(byte[] buf, int ofs, int len) {
        StringBuilder sb = new StringBuilder();
        int j = ofs + len;
        for (int i = ofs; i < j; i++) {
            if (i < buf.length) {
                sb.append(HEX_CHARS[(buf[i] & 0xF0) >> 4]);
                sb.append(HEX_CHARS[buf[i] & 0x0F]);
                sb.append(' ');
            } else {
                sb.append(' ');
                sb.append(' ');
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    public static String toAscii(byte[] buf) {
        return toAscii(buf, 0, buf.length);
    }

    public static String toAscii(byte[] buf, int ofs, int len) {
        StringBuilder sb = new StringBuilder();
        int j = ofs + len;
        for (int i = ofs; i < j; i++) {
            if (i < buf.length) {
                if (20 <= buf[i] && 126 >= buf[i]) {
                    sb.append((char) buf[i]);
                } else {
                    sb.append('.');
                }
            } else {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    public static String format(byte[] buf) {
        return format(buf, 80);
    }

    public static String format(byte[] buf, int width) {
        int bs = (width - 8) / 4;
        int i = 0;
        StringBuilder sb = new StringBuilder();
        do {
            for (int j = 0; j < 6; j++) {
                sb.append(HEX_CHARS[(i << (j * 4) & 0xF00000) >> 20]);
            }
            sb.append('\t');
            sb.append(toHex(buf, i, bs));
            sb.append(' ');
            sb.append(toAscii(buf, i, bs));
            sb.append('\n');
            i += bs;
        } while (i < buf.length);
        sb.deleteCharAt(sb.length() - 1); // remove the last \n
        return sb.toString();
    }

    public static void print(byte[] buf) {
        print(buf, System.err);
    }

    public static void print(byte[] buf, int width) {
        print(buf, width, System.err);
    }

    public static void print(byte[] buf, int width, PrintStream out) {
        out.print(format(buf, width));
    }

    public static void print(byte[] buf, PrintStream out) {
        out.print(format(buf));
    }

    /**
     * Returns a string which can be written to a Java source file as part
     * of a static initializer for a byte array.
     * Returns data in the format 0xAB, 0xCD, ....
     * use like:
     * javafile.print("byte[] data = {")
     * javafile.print(Hexdump.toByteArray(data));
     * javafile.println("};");     * @param buf
     * @param buf buffer
     * @return string
     */
    public static String toByteArray(byte[] buf) {
        return toByteArray(buf, 0, buf.length);
    }

    /**
     * Returns a string which can be written to a Java source file as part
     * of a static initializer for a byte array.
     * Returns data in the format 0xAB, 0xCD, ....
     * use like:
     * javafile.print("byte[] data = {")
     * javafile.print(Hexdump.toByteArray(data));
     * javafile.println("};");
     *
     * @param buf buffer
     * @param ofs offset
     * @param len length
     * @return string
     */
    public static String toByteArray(byte[] buf, int ofs, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = ofs; i < len && i < buf.length; i++) {
            sb.append('0');
            sb.append('x');
            sb.append(HEX_CHARS[(buf[i] & 0xF0) >> 4]);
            sb.append(HEX_CHARS[buf[i] & 0x0F]);
            if ((i + 1) < len && (i + 1) < buf.length) {
                sb.append(',');
            }
        }
        return sb.toString();
    }
}
