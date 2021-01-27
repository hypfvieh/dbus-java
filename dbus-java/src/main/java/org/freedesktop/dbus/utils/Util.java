package org.freedesktop.dbus.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class providing helper methods for handling strings, files and so on.
 *
 * @author hypfvieh
 * @since v3.2.5 - 2020-12-28
 */
public final class Util {
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    /** Characters used for random strings */
    private static final char[] SYMBOLS;

    static {
        StringBuilder tmp = new StringBuilder();
        for (char ch = '0'; ch <= '9'; ++ch)
          tmp.append(ch);
        for (char ch = 'a'; ch <= 'z'; ++ch)
          tmp.append(ch);
        for (char ch = 'A'; ch <= 'Z'; ++ch)
            tmp.append(ch);
        SYMBOLS = tmp.toString().toCharArray();
    }

    private Util() {}


    /**
     * Trys to read a properties file.
     * Returns null if properties file could not be loaded
     * @param _file
     * @return Properties Object or null
     */
    public static Properties readProperties(File _file) {
        if (_file.exists()) {
            try {
                return readProperties(new FileInputStream(_file));
            } catch (FileNotFoundException _ex) {
                LOGGER.info("Could not load properties file: " + _file, _ex);
            }
        }
        return null;
    }

    /**
     * Tries to read a properties file from an inputstream.
     * @param _stream
     * @return properties object/null
     */
    public static Properties readProperties(InputStream _stream) {
        Properties props = new Properties();
        if (_stream == null) {
            return null;
        }

        try {
            props.load(_stream);
            return props;
        } catch (IOException | NumberFormatException _ex) {
            LOGGER.warn("Could not properties: ", _ex);
        }
        return null;
    }

    /**
     * Checks if the given String is either null or blank.
     * Blank means:<br>
     * <pre>
     * " " - true
     * "" - true
     * null - true
     * " xx" - false
     * </pre>
     * @param _str string to test
     * @return true if string is blank or null, false otherwise
     */
    public static boolean isBlank(String _str) {
        if (_str == null) {
            return true;
        }

        return _str.trim().isEmpty();
    }

    /**
     * Checks if the given String is either null or empty.
     * Blank means:<br>
     * <pre>
     * " " - false
     * "" - true
     * null - true
     * " xx" - false
     * </pre>
     * @param _str string to test
     * @return true if string is empty or null, false otherwise
     */
    public static boolean isEmpty(String _str) {
        if (_str == null) {
            return true;
        }

        return _str.isEmpty();
    }

    /**
     * Generate a simple (cryptographic insecure) random string.
     * @param _length length of random string
     * @return random string or empty string if _length &lt;= 0
     */
    public static String randomString(int _length) {
        if (_length <= 0) {
            return "";
        }
        Random random = new Random();
        char[] buf = new char[_length];
        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = SYMBOLS[random.nextInt(SYMBOLS.length)];
        return new String(buf);
    }

    /**
     * Upper case the first letter of the given string.
     *
     * @param _str string
     * @return uppercased string
     */
    public static String upperCaseFirstChar(String _str) {
        if (_str == null) {
            return null;
        }
        if (_str.isEmpty()) {
            return _str;
        }
        return _str.substring(0, 1).toUpperCase() + _str.substring(1);
    }

    /**
     * Converts a snake-case-string to camel case string.
     * <br>
     * Eg. this_is_snake_case &rarr; thisIsSnakeCase
     * @param _input string
     * @return camel case string or input if nothing todo. Returns null if input was null.
     */
    public static String snakeToCamelCase(String _input) {
        if (isBlank(_input)) {
            return _input;
        }

        Pattern compile = Pattern.compile("_[a-zA-Z]");
        Matcher matcher = compile.matcher(_input);

        String result = _input;

        while (matcher.find()) {
            String match = matcher.group();
            String replacement = match.replace("_", "");
            replacement = replacement.toUpperCase();

            result = result.replaceFirst(match, replacement);

        }

        return result;
    }

    /**
     * Abbreviates a String using ellipses.
     *
     * @param _str string to abbrivate
     * @param _length max length
     * @return abbreviated string, original string if string length is lower or equal then desired length or null if input was null
     */
    public static String abbreviate(String _str, int _length) {
        if (_str == null) {
            return null;
        }
        if (_str.length() <= _length) {
            return _str;
        }

        String abbr = _str.substring(0, _length -3) + "...";

        return abbr;
    }

    /**
     * Check if the given value is a valid network port (1 - 65535).
     * @param _port 'port' to check
     * @param _allowWellKnown allow ports below 1024 (aka reserved well known ports)
     * @return true if int is a valid network port, false otherwise
     */
    public static boolean isValidNetworkPort(int _port, boolean _allowWellKnown) {
        if (_allowWellKnown) {
            return _port > 0 && _port < 65536;
        }

        return _port > 1024 && _port < 65536;
    }

    /**
     * @see #isValidNetworkPort(int, boolean)
     * @param _str string to check
     * @param _allowWellKnown allow well known port
     * @return true if valid port, false otherwise
     */
    public static boolean isValidNetworkPort(String _str, boolean _allowWellKnown) {
        if (isInteger(_str, false)) {
            return isValidNetworkPort(Integer.parseInt(_str), _allowWellKnown);
        }
        return false;
    }

    /**
     * Check if string is an either positive or negative integer.
     *
     * @param _str string to validate
     * @param _allowNegative negative integer allowed
     * @return true if integer, false otherwise
     */
    public static boolean isInteger(String _str, boolean _allowNegative) {
        if (_str == null) {
            return false;
        }

        String regex = "[0-9]+$";
        if (_allowNegative) {
            regex = "^-?" + regex;
        } else {
            regex = "^" + regex;
        }
        return _str.matches(regex);
    }

    /**
     * Reads a file to a List&lt;String&gt; (each line is one entry in list).
     * Line endings (line feed/carriage return) are NOT removed!
     *
     * @param _fileName
     * @return
     */
    public static List<String> readFileToList(String _fileName) {
        List<String> localText = getTextfileFromUrl(_fileName, Charset.defaultCharset(), false);
        return localText;
    }

    public static String readFileToString(File _file) {
        return String.join(System.lineSeparator(), readFileToList(_file.getAbsolutePath()));
    }

    /**
     * @see #getTextfileFromUrl(String, Charset)
     * @param _url
     * @param _charset
     * @param _silent true to not log exceptions, false otherwise
     * @return list of string or null on error
     */
    public static List<String> getTextfileFromUrl(String _url, Charset _charset, boolean _silent) {
        if (_url == null) {
            return null;
        }
        String fileUrl = _url;
        if (!fileUrl.contains("://")) {
            fileUrl = "file://" + fileUrl;
        }

        try {
            URL dlUrl;
            if (fileUrl.startsWith("file:/")) {
                dlUrl = new URL("file", "", fileUrl.replaceFirst("file:\\/{1,2}", ""));
            } else {
                dlUrl = new URL(fileUrl);
            }
            URLConnection urlConn = dlUrl.openConnection();
            urlConn.setDoInput(true);
            urlConn.setUseCaches(false);

            return readTextFileFromStream(urlConn.getInputStream(), _charset, _silent);

        } catch (IOException _ex) {
            if (!_silent) {
                LOGGER.warn("Error while reading file:", _ex);
            }
        }

        return null;
    }

    /**
     * Reads a text file from given {@link InputStream} using the given {@link Charset}.
     * @param _input stream to read
     * @param _charset charset to use
     * @param _silent true to disable exception logging, false otherwise
     * @return List of string or null on error
     */
    public static List<String> readTextFileFromStream(InputStream _input, Charset _charset, boolean _silent) {
        if (_input == null) {
            return null;
        }
        try {
            List<String> fileContent;
            try (BufferedReader dis = new BufferedReader(new InputStreamReader(_input, _charset))) {
                String s;
                fileContent = new ArrayList<>();
                while ((s = dis.readLine()) != null) {
                    fileContent.add(s);
                }
            }

            return fileContent.size() > 0 ? fileContent : null;
        } catch (IOException _ex) {
            if (!_silent) {
                LOGGER.warn("Error while reading file:", _ex);
            }
        }

        return null;
    }

    /**
     * Write String to file with the given charset.
     * Optionally appends the data to the file.
     *
     * @param _fileName the file to write
     * @param _fileContent the content to write
     * @param _charset the charset to use
     * @param _append append content to file, if false file will be overwritten if existing
     *
     * @return true on successful write, false otherwise
     */
    public static boolean writeTextFile(String _fileName, String _fileContent, Charset _charset, boolean _append) {
        if (isBlank(_fileName)) {
            return false;
        }
        String allText = "";
        if (_append) {
            File file = new File(_fileName);
            if (file.exists()) {
                allText = readFileToString(file);
            }
        }
        allText += _fileContent;

        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(_fileName), _charset);
            writer.write(allText);
        } catch (IOException _ex) {
            LOGGER.error("Could not write file to '" + _fileName + "'", _ex);
            return false;
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException _ex) {
                LOGGER.error("Error while closing file '" + _fileName + "'", _ex);
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the host name of the local machine.
     * @return host name
     */
    public static String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (java.net.UnknownHostException _ex) {
            return null;
        }
    }

    /**
     * Determines the current logged on user.
     * @return logged on user
     */
    public static String getCurrentUser() {
        String[] sysPropParms = new String[] {"user.name", "USER", "USERNAME"};
        for (int i = 0; i < sysPropParms.length; i++) {
            String val = System.getProperty(sysPropParms[i]);
            if (!isEmpty(val)) {
                return val;
            }
        }
        return null;
    }

    /**
     * Checks if the running OS is a MacOS/MacOS X.
     * @return true if MacOS (or MacOS X), false otherwise
     */
    public static boolean isMacOs() {
        String osName = System.getProperty("os.name");
        return osName == null ? false : osName.toLowerCase().startsWith("mac");
    }

    /**
     * Checks if the running OS is a MS Windows OS.
     * @return true if Windows, false otherwise
     */
    public static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName == null ? false : osName.toLowerCase().startsWith("windows");
    }

}
