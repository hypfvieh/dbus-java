package org.freedesktop.dbus.connections;

import static org.freedesktop.dbus.connections.SASL.SaslCommand.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.freedesktop.Hexdump;
import org.freedesktop.dbus.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jnr.posix.POSIXFactory;
import jnr.unixsocket.Credentials;
import jnr.unixsocket.UnixSocket;

public class SASL {

    public static final int    LOCK_TIMEOUT                = 1000;
    public static final int    NEW_KEY_TIMEOUT_SECONDS     = 60 * 5;
    public static final int    EXPIRE_KEYS_TIMEOUT_SECONDS = NEW_KEY_TIMEOUT_SECONDS + (60 * 2);
    public static final int    MAX_TIME_TRAVEL_SECONDS     = 60 * 5;
    public static final int    COOKIE_TIMEOUT              = 240;
    public static final String COOKIE_CONTEXT              = "org_freedesktop_java";
    
    private static Collator col = Collator.getInstance();
    static {
        col.setDecomposition(Collator.FULL_DECOMPOSITION);
        col.setStrength(Collator.PRIMARY);
    }
    
    private String challenge = "";
    private String cookie    = "";


    private final Logger logger = LoggerFactory.getLogger(getClass());
    private boolean fileDescriptorSupported;
    /** whether file descriptor passing is supported on the current connection. */
    private boolean hasFileDescriptorSupport;
    
    /**
     * Create a new SASL auth handler.
     * Defaults to disable file descriptor passing.
     */
    public SASL() {
        this(false);
    }
    
    /**
     * Create a new SASL auth handler.
     * 
     * @param _hasFileDescriptorSupport true to support file descriptor passing (usually only works with UNIX_SOCKET).
     */
    public SASL(boolean _hasFileDescriptorSupport) {
        hasFileDescriptorSupport = _hasFileDescriptorSupport;

    }

    private String findCookie(String context, String ID) throws IOException {
        String homedir = System.getProperty("user.home");
        File f = new File(homedir + "/.dbus-keyrings/" + context);
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        String s = null;
        String lCookie = null;
        long now = System.currentTimeMillis() / 1000;
        while (null != (s = r.readLine())) {
            String[] line = s.split(" ");
            long timestamp = Long.parseLong(line[1]);
            if (line[0].equals(ID) && (!(timestamp < 0 || (now + MAX_TIME_TRAVEL_SECONDS) < timestamp || (now - EXPIRE_KEYS_TIMEOUT_SECONDS) > timestamp))) {
                lCookie = line[2];
                break;
            }
        }
        r.close();
        return lCookie;
    }

    private void addCookie(String _context, String _id, long _timestamp, String _cookie) throws IOException {
        String homedir = System.getProperty("user.home");
        File keydir = new File(homedir + "/.dbus-keyrings/");
        File cookiefile = new File(homedir + "/.dbus-keyrings/" + _context);
        File lock = new File(homedir + "/.dbus-keyrings/" + _context + ".lock");
        File temp = new File(homedir + "/.dbus-keyrings/" + _context + ".temp");

        // ensure directory exists
        if (!keydir.exists()) {
            keydir.mkdirs();
        }

        // acquire lock
        long start = System.currentTimeMillis();
        while (!lock.createNewFile() && LOCK_TIMEOUT > (System.currentTimeMillis() - start)) {
            ;
        }

        // read old file
        List<String> lines = new ArrayList<>();
        if (cookiefile.exists()) {
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(cookiefile)));
            String s = null;
            while (null != (s = r.readLine())) {
                String[] line = s.split(" ");
                long time = Long.parseLong(line[1]);
                // expire stale cookies
                if ((_timestamp - time) < COOKIE_TIMEOUT) {
                    lines.add(s);
                }
            }
            r.close();
        }

        // add cookie
        lines.add(_id + " " + _timestamp + " " + _cookie);

        // write temp file
        PrintWriter w = new PrintWriter(new FileOutputStream(temp));
        for (String l : lines) {
            w.println(l);
        }
        w.close();

        // atomically move to old file
        if (!temp.renameTo(cookiefile)) {
            cookiefile.delete();
            temp.renameTo(cookiefile);
        }

        // remove lock
        lock.delete();
    }

    /**
     * Takes the string, encodes it as hex and then turns it into a string again.
     * No, I don't know why either.
     */
    private String stupidlyEncode(String data) {
        return Hexdump.toHex(data.getBytes()).replaceAll(" ", "");
    }

    private String stupidlyEncode(byte[] data) {
        return Hexdump.toHex(data).replaceAll(" ", "");
    }

    private byte getNibble(char c) {
        switch (c) {
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            return (byte) (c - '0');
        case 'A':
        case 'B':
        case 'C':
        case 'D':
        case 'E':
        case 'F':
            return (byte) (c - 'A' + 10);
        case 'a':
        case 'b':
        case 'c':
        case 'd':
        case 'e':
        case 'f':
            return (byte) (c - 'a' + 10);
        default:
            return 0;
        }
    }

    private String stupidlyDecode(String data) {
        char[] cs = new char[data.length()];
        char[] res = new char[cs.length / 2];
        data.getChars(0, data.length(), cs, 0);
        for (int i = 0, j = 0; j < res.length; i += 2, j++) {
            int b = 0;
            b |= getNibble(cs[i]) << 4;
            b |= getNibble(cs[i + 1]);
            res[j] = (char) b;
        }
        return new String(res);
    }

    public static final int AUTH_NONE        = 0;
    public static final int AUTH_EXTERNAL    = 1;
    public static final int AUTH_SHA         = 2;
    public static final int AUTH_ANON        = 4;

    public SASL.Command receive(InputStream s) throws IOException {
        StringBuffer sb = new StringBuffer();
        top: while (true) {
            int c = s.read();
            switch (c) {
            case -1:
                throw new IOException("Stream unexpectedly short (broken pipe)");
            case 0:
            case '\r':
                continue;
            case '\n':
                break top;
            default:
                sb.append((char) c);
            }
        }
        logger.trace("received: {}", sb);
        try {
            return new Command(sb.toString());
        } catch (Exception e) {
            logger.error("Cannot create command.", e);
            return new Command();
        }
    }

    public void send(OutputStream out, SaslCommand command, String... data) throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append(command.name());
        
        for (String s : data) {
            sb.append(' ');
            sb.append(s);
        }
        sb.append('\r');
        sb.append('\n');
        logger.trace("sending: {}", sb);
        out.write(sb.toString().getBytes());
    }
    
    public SaslResult doChallenge(int _auth, SASL.Command c) throws IOException {
        switch (_auth) {
        case AUTH_SHA:
            String[] reply = stupidlyDecode(c.getData()).split(" ");
            logger.trace(Arrays.toString(reply));
            if (3 != reply.length) {
                logger.debug("Reply is not length 3");
                return SaslResult.ERROR;
            }
            String context = reply[0];
            String id = reply[1];
            String serverchallenge = reply[2];
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException nsae) {
                logger.debug("", nsae);
                return SaslResult.ERROR;
            }
            byte[] buf = new byte[8];
            Message.marshallintBig(System.currentTimeMillis(), buf, 0, 8);
            String clientchallenge = stupidlyEncode(md.digest(buf));
            md.reset();
            long start = System.currentTimeMillis();
            String lCookie = null;
            while (null == lCookie && (System.currentTimeMillis() - start) < LOCK_TIMEOUT) {
                lCookie = findCookie(context, id);
            }
            if (null == lCookie) {
                logger.debug("Did not find a cookie in context {}  with ID {}",context, id);
                return SaslResult.ERROR;
            }
            String response = serverchallenge + ":" + clientchallenge + ":" + lCookie;
            buf = md.digest(response.getBytes());

            logger.trace("Response: {} hash: {}", response, Hexdump.format(buf));

            response = stupidlyEncode(buf);
            c.setResponse(stupidlyEncode(clientchallenge + " " + response));
            return SaslResult.OK;
        default:
            logger.debug("Not DBUS_COOKIE_SHA1 authtype.");
            return SaslResult.ERROR;
        }
    }

    public SaslResult doResponse(int _auth, String _uid, String _kernelUid, SASL.Command _c) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException nsae) {
            logger.error("", nsae);
            return SaslResult.ERROR;
        }
        switch (_auth) {
            case AUTH_NONE:
                switch (_c.getMechs()) {
                case AUTH_ANON:
                    return SaslResult.OK;
                case AUTH_EXTERNAL:
                    if (0 == col.compare(_uid, _c.getData()) && (null == _kernelUid || 0 == col.compare(_uid, _kernelUid))) {
                        return SaslResult.OK;
                    } else {
                        return SaslResult.REJECT;
                    }
                case AUTH_SHA:
                    String context = COOKIE_CONTEXT;
                    long id = System.currentTimeMillis();
                    byte[] buf = new byte[8];
                    Message.marshallintBig(id, buf, 0, 8);
                    challenge = stupidlyEncode(md.digest(buf));
                    Random r = new Random();
                    r.nextBytes(buf);
                    cookie = stupidlyEncode(md.digest(buf));
                    try {
                        addCookie(context, "" + id, id / 1000, cookie);
                    } catch (IOException ioe) {
                        logger.debug("", ioe);
                    }
    
                    logger.debug("Sending challenge: {} {} {}", context, id, challenge);
    
                    _c.setResponse(stupidlyEncode(context + ' ' + id + ' ' + challenge));
                    return SaslResult.CONTINUE;
                default:
                    return SaslResult.ERROR;
                }
            case AUTH_SHA:
                String[] response = stupidlyDecode(_c.getData()).split(" ");
                if (response.length < 2) {
                    return SaslResult.ERROR;
                }
                String cchal = response[0];
                String hash = response[1];
                String prehash = challenge + ":" + cchal + ":" + cookie;
                byte[] buf = md.digest(prehash.getBytes());
                String posthash = stupidlyEncode(buf);
                logger.debug("Authenticating Hash; data={} remote-hash={} local-hash={}",prehash, hash, posthash);
                if (0 == col.compare(posthash, hash)) {
                    return SaslResult.OK;
                } else {
                    return SaslResult.ERROR;
                }
            default:
                return SaslResult.ERROR;
            }
    }

    public String[] getTypes(int types) {
        switch (types) {
            case AUTH_EXTERNAL:
                return new String[] {
                        "EXTERNAL"
                };
            case AUTH_SHA:
                return new String[] {
                        "DBUS_COOKIE_SHA1"
                };
            case AUTH_ANON:
                return new String[] {
                        "ANONYMOUS"
                };
            case AUTH_SHA + AUTH_EXTERNAL:
                return new String[] {
                        "EXTERNAL", "DBUS_COOKIE_SHA1"
                };
            case AUTH_SHA + AUTH_ANON:
                return new String[] {
                        "ANONYMOUS", "DBUS_COOKIE_SHA1"
                };
            case AUTH_EXTERNAL + AUTH_ANON:
                return new String[] {
                        "ANONYMOUS", "EXTERNAL"
                };
            case AUTH_EXTERNAL + AUTH_ANON + AUTH_SHA:
                return new String[] {
                        "ANONYMOUS", "EXTERNAL", "DBUS_COOKIE_SHA1"
                };
            default:
                return new String[] {};
        }
    }

    /**
     * performs SASL auth on the given streams.
     * Mode selects whether to run as a SASL server or client.
     * Types is a bitmask of the available auth types.
     *
     * @param mode mode
     * @param types types
     * @param guid guid
     * @param out out
     * @param in in
     * @param us us
     * @return true if the auth was successful and false if it failed.
     * @throws IOException on failure
     */
    public boolean auth(SaslMode mode, int types, String guid, OutputStream out, InputStream in, Socket us) throws IOException {
        String luid = null;
        String kernelUid = null;

        long uid = POSIXFactory.getJavaPOSIX().getuid();
        luid = stupidlyEncode("" + uid);

        SASL.Command c;
        int failed = 0;
        int current = 0;
        SaslAuthState state = SaslAuthState.INITIAL_STATE;
        
        while (state != SaslAuthState.FINISHED && state != SaslAuthState.FAILED) {

            logger.trace("Mode: {} AUTH state: {}", mode, state);

            switch (mode) {
            case CLIENT:
                switch (state) {
                case INITIAL_STATE:
                    if (FreeBSDHelper.isFreeBSD()) {
                        FreeBSDHelper.send_cred(us);
                    } else {
                        out.write(new byte[] {
                                0
                        });
                    }
                    send(out, AUTH);
                    state = SaslAuthState.WAIT_DATA;
                    break;
                case WAIT_DATA:
                    c = receive(in);
                    switch (c.getCommand()) {
                        case DATA:
                            switch (doChallenge(current, c)) {
                                case CONTINUE:
                                    send(out, DATA, c.getResponse());
                                    break;
                                case OK:
                                    send(out, DATA, c.getResponse());
                                    state = SaslAuthState.WAIT_OK;
                                    break;
                                case ERROR:
                                default:
                                    send(out, ERROR, c.getResponse());
                                    break;
                            }
                        break;
                        case REJECTED:
                            failed |= current;
                            int available = c.getMechs() & (~failed);
                            if (0 != (available & AUTH_EXTERNAL)) {
                                send(out, AUTH, "EXTERNAL", luid);
                                current = AUTH_EXTERNAL;
                            } else if (0 != (available & AUTH_SHA)) {
                                send(out, AUTH, "DBUS_COOKIE_SHA1", luid);
                                current = AUTH_SHA;
                            } else if (0 != (available & AUTH_ANON)) {
                                send(out, AUTH, "ANONYMOUS");
                                current = AUTH_ANON;
                            } else {
                                state = SaslAuthState.FAILED;
                            }
                            break;
                        case ERROR:
                            // when asking for file descriptor support, ERROR means FD support is not supported
                            if (state == SaslAuthState.NEGOTIATE_UNIX_FD) { 
                                state = SaslAuthState.FINISHED;
                                logger.trace("File descriptors NOT supported by server");
                                fileDescriptorSupported = false;
                                send(out, BEGIN);                            
                            } else {
                                send(out, CANCEL);
                                state = SaslAuthState.WAIT_REJECT;
                            }
                            break;
                        case OK:
                            logger.trace("Authenticated");
                            state = SaslAuthState.AUTHENTICATED;

                            if (hasFileDescriptorSupport) {
                                state = SaslAuthState.WAIT_DATA;
                                logger.trace("Asking for file descriptor support");
                                // if authentication was successful, ask remote end for file descriptor support
                                send(out, SaslCommand.NEGOTIATE_UNIX_FD);
                            }else{
                                state = SaslAuthState.FINISHED;
                                send(out, BEGIN);                     
                            }
                            break;
                        case AGREE_UNIX_FD:
                            if (hasFileDescriptorSupport) {
                                state = SaslAuthState.FINISHED;
                                logger.trace("File descriptors supported by server");
                                fileDescriptorSupported = true;
                                send(out, BEGIN);
                            }
                            break;
                        default:
                            send(out, ERROR, "Got invalid command");
                            break;
                        }
                    break;
                case WAIT_OK:
                    c = receive(in);
                    switch (c.getCommand()) {
                    case OK:
                        send(out, BEGIN);
                        state = SaslAuthState.AUTHENTICATED;
                        break;
                    case ERROR:
                    case DATA:
                        send(out, CANCEL);
                        state = SaslAuthState.WAIT_REJECT;
                        break;
                    case REJECTED:
                        failed |= current;
                        int available = c.getMechs() & (~failed);
                        state = SaslAuthState.WAIT_DATA;
                        if (0 != (available & AUTH_EXTERNAL)) {
                            send(out, AUTH, "EXTERNAL", luid);
                            current = AUTH_EXTERNAL;
                        } else if (0 != (available & AUTH_SHA)) {
                            send(out, AUTH, "DBUS_COOKIE_SHA1", luid);
                            current = AUTH_SHA;
                        } else if (0 != (available & AUTH_ANON)) {
                            send(out, AUTH, "ANONYMOUS");
                            current = AUTH_ANON;
                        } else {
                            state = SaslAuthState.FAILED;
                        }
                        break;
                    default:
                        send(out, ERROR, "Got invalid command");
                        break;
                    }
                    break;
                case WAIT_REJECT:
                    c = receive(in);
                    switch (c.getCommand()) {
                        case REJECTED:
                            failed |= current;
                            int available = c.getMechs() & (~failed);
                            if (0 != (available & AUTH_EXTERNAL)) {
                                send(out, AUTH, "EXTERNAL", luid);
                                current = AUTH_EXTERNAL;
                            } else if (0 != (available & AUTH_SHA)) {
                                send(out, AUTH, "DBUS_COOKIE_SHA1", luid);
                                current = AUTH_SHA;
                            } else if (0 != (available & AUTH_ANON)) {
                                send(out, AUTH, "ANONYMOUS");
                                current = AUTH_ANON;
                            } else {
                                state = SaslAuthState.FAILED;
                            }
                        break;
                        default:
                            state = SaslAuthState.FAILED;
                            break;
                    }
                    break;
                default:
                    state = SaslAuthState.FAILED;
                }
                break;
            case SERVER:
                switch (state) {
                    case INITIAL_STATE:
                        byte[] buf = new byte[1];
                        if (null == us) {
                            in.read(buf); // 0
                            state = SaslAuthState.WAIT_AUTH;
                        } else {
                            Credentials credentials;
                            try {
                                if (FreeBSDHelper.isFreeBSD()) {
                                    long euid = FreeBSDHelper.recv_cred(us);
                                    if (euid >= 0) {
                                        kernelUid = stupidlyEncode("" + euid);
                                    }
                                } else {
                                    credentials = ((UnixSocket) us).getCredentials();
                                    int kuid = credentials.getUid();
                                    if (kuid >= 0) {
                                        kernelUid = stupidlyEncode("" + kuid);
                                    }
                                }
                                state = SaslAuthState.WAIT_AUTH;
    
                            } catch (SocketException _ex) {
                                state = SaslAuthState.FAILED;
                            }
                        }
                    break;
                    case WAIT_AUTH:
                        c = receive(in);
                        switch (c.getCommand()) {
                            case AUTH:
                                switch (doResponse(current, luid, kernelUid, c)) {
                                    case CONTINUE:
                                        send(out, DATA, c.getResponse());
                                        current = c.getMechs();
                                        state = SaslAuthState.WAIT_DATA;
                                        break;
                                    case OK:
                                        send(out, SaslCommand.OK, guid);
                                        state = SaslAuthState.WAIT_BEGIN;
                                        current = 0;
                                        break;
                                    case REJECT:
                                    default:
                                        send(out, REJECTED, getTypes(types));
                                        current = 0;
                                        break;
                                }
                                break;
                            case ERROR:
                                send(out, REJECTED, getTypes(types));
                                break;
                            case BEGIN:
                                state = SaslAuthState.FAILED;
                                break;
                            default:
                                send(out, ERROR, "Got invalid command");
                                break;
                            }
                    break;
                    case WAIT_DATA:
                        c = receive(in);
                    switch (c.getCommand()) {
                    case DATA:
                        switch (doResponse(current, luid, kernelUid, c)) {
                            case CONTINUE:
                                send(out, DATA, c.getResponse());
                                state = SaslAuthState.WAIT_DATA;
                                break;
                            case OK:
                                send(out, SaslCommand.OK, guid);
                                state = SaslAuthState.WAIT_BEGIN;
                                current = 0;
                                break;
                            case REJECT:
                            default:
                                send(out, REJECTED, getTypes(types));
                                current = 0;
                                break;
                            }
                        break;
                        case ERROR:
                        case CANCEL:
                            send(out, REJECTED, getTypes(types));
                            state = SaslAuthState.WAIT_AUTH;
                        break;
                        case BEGIN:
                            state = SaslAuthState.FAILED;
                        break;
                        default:
                            send(out, ERROR, "Got invalid command");
                        break;
                    }
                    break;
                    case WAIT_BEGIN:
                        c = receive(in);
                        switch (c.getCommand()) {
                            case ERROR:
                            case CANCEL:
                                send(out, REJECTED, getTypes(types));
                                state = SaslAuthState.WAIT_AUTH;
                            break;
                            case BEGIN:
                                    state = SaslAuthState.FINISHED;
                            break;
                            case NEGOTIATE_UNIX_FD:
                                logger.debug("File descriptor negotiation requested");
                                if (!hasFileDescriptorSupport) {
                                    send(out, ERROR);
                                } else {
                                    send(out, AGREE_UNIX_FD);
                                }
                                
                            break;
                            default:
                                send(out, ERROR, "Got invalid command");
                            break;
                        }
                    break;
                    default:
                        state = SaslAuthState.FAILED;
                    }
                break;
            default:
                return false;
            }
        }

        return state == SaslAuthState.FINISHED;
    }

    public static enum SaslMode {
        SERVER, CLIENT;
    }

    static enum SaslCommand {
        AUTH,     
        DATA,     
        REJECTED, 
        OK,       
        BEGIN,    
        CANCEL,   
        ERROR,
        NEGOTIATE_UNIX_FD,
        AGREE_UNIX_FD;  
    }

    static enum SaslAuthState {
        INITIAL_STATE,
        WAIT_DATA,
        WAIT_OK,
        WAIT_REJECT,
        WAIT_AUTH,
        WAIT_BEGIN,
        AUTHENTICATED,
        NEGOTIATE_UNIX_FD,
        FINISHED,
        FAILED;
    }

    static enum SaslResult {
        OK,      
        CONTINUE,
        ERROR,   
        REJECT;  
    }

    public static class Command {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        private SaslCommand    command;
        private int    mechs;
        private String data;
        private String response;
    
        public Command() {
        }
    
        public Command(String s) throws IOException {
            String[] ss = s.split(" ");
            logger.trace("Creating command from: {}", Arrays.toString(ss));
            if (0 == col.compare(ss[0], "OK")) {
                command = SaslCommand.OK;
                data = ss[1];
            } else if (0 == col.compare(ss[0], "AUTH")) {
                command = AUTH;
                if (ss.length > 1) {
                    if (0 == col.compare(ss[1], "EXTERNAL")) {
                        mechs = AUTH_EXTERNAL;
                    } else if (0 == col.compare(ss[1], "DBUS_COOKIE_SHA1")) {
                        mechs = AUTH_SHA;
                    } else if (0 == col.compare(ss[1], "ANONYMOUS")) {
                        mechs = AUTH_ANON;
                    }
                }
                if (ss.length > 2) {
                    data = ss[2];
                }
            } else if (0 == col.compare(ss[0], "DATA")) {
                command = DATA;
                data = ss[1];
            } else if (0 == col.compare(ss[0], "REJECTED")) {
                command = REJECTED;
                for (int i = 1; i < ss.length; i++) {
                    if (0 == col.compare(ss[i], "EXTERNAL")) {
                        mechs |= AUTH_EXTERNAL;
                    } else if (0 == col.compare(ss[i], "DBUS_COOKIE_SHA1")) {
                        mechs |= AUTH_SHA;
                    } else if (0 == col.compare(ss[i], "ANONYMOUS")) {
                        mechs |= AUTH_ANON;
                    }
                }
            } else if (0 == col.compare(ss[0], "BEGIN")) {
                command = BEGIN;
            } else if (0 == col.compare(ss[0], "CANCEL")) {
                command = CANCEL;
            } else if (0 == col.compare(ss[0], "ERROR")) {
                command = ERROR;
                data = ss[1];
            } else if (0 == col.compare(ss[0], "NEGOTIATE_UNIX_FD")) {
                command = NEGOTIATE_UNIX_FD;
            } else if (0 == col.compare(ss[0], "AGREE_UNIX_FD")) {
                command = AGREE_UNIX_FD;                
            } else {
                throw new IOException("Invalid Command " + ss[0]);
            }
            logger.trace("Created command: {}", this);
        }
    
        public SaslCommand getCommand() {
            return command;
        }
    
        public int getMechs() {
            return mechs;
        }
    
        public String getData() {
            return data;
        }
    
        public String getResponse() {
            return response;
        }
    
        public void setResponse(String s) {
            response = s;
        }
    
        @Override
        public String toString() {
            return "Command(" + command + ", " + mechs + ", " + data + ")";
        }
    }

    public boolean isFileDescriptorSupported() {
        return fileDescriptorSupported;
    }
    
}