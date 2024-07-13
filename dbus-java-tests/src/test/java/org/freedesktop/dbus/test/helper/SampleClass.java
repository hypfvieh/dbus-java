package org.freedesktop.dbus.test.helper;

import static org.junit.jupiter.api.Assertions.fail;

import org.freedesktop.dbus.DBusCallInfo;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.test.helper.interfaces.SampleNewInterface;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface2;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterfaceEnum;
import org.freedesktop.dbus.test.helper.structs.*;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@SuppressWarnings({"checkstyle:methodname"})
public class SampleClass implements SampleRemoteInterface, SampleRemoteInterface2, SampleRemoteInterfaceEnum, Properties {

    private final transient Logger logger = LoggerFactory.getLogger(getClass());

    private final DBusConnection   conn;

    public SampleClass(DBusConnection _conn) {
        this.conn = _conn;
    }

    @Override
    public String Introspect() {
        return "Not XML";
    }

    @Override
    public int[][] teststructstruct(SampleStruct3 _in) {
        List<List<Integer>> lli = _in.getInnerListOfLists();
        int[][] out = new int[lli.size()][];
        for (int j = 0; j < out.length; j++) {
            out[j] = new int[lli.get(j).size()];
            for (int k = 0; k < out[j].length; k++) {
                out[j][k] = lli.get(j).get(k);
            }
        }
        return out;
    }

    @Override
    public int[][] testListstruct(SampleStruct4 _in) {
        List<IntStruct> list = _in.getInnerListOfLists();
        int size = list.size();
        int[][] retVal = new int[size][];
        for (int i = 0; i < size; i++) {
            IntStruct elem = list.get(i);
            retVal[i] = new int[] {elem.getValue1(), elem.getValue2()};
        }
        return retVal;
    }

    @Override
    public float testfloat(float[] _f) {
        if (_f.length < 4 || _f[0] != 17.093f || _f[1] != -23f || _f[2] != 0.0f || _f[3] != 31.42f) {
            fail("testfloat got incorrect array");
        }
        return _f[0];
    }

    @Override
    public void newpathtest(DBusPath _p) {
        if (!_p.toString().equals("/new/path/test")) {
            fail("new path test got wrong path");
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Override
    public void waitawhile() {
        logger.debug("Sleeping.");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException _ex) {
        }
        logger.debug("Done sleeping.");
    }

    @Override
    public <A> SampleTuple<String, List<Integer>, Boolean> show(A _in) {
        logger.debug("Showing Stuff: {} ({})", _in.getClass(), _in);
        if (!(_in instanceof Integer) || (Integer) _in != 234) {
            fail("show received the wrong arguments");
        }
        DBusCallInfo info = AbstractConnection.getCallInfo();
        List<Integer> l = new ArrayList<>();
        l.add(1953);
        return new SampleTuple<>(info.getSource(), l, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T dostuff(SampleStruct _foo) {
        logger.debug("Doing Stuff " + _foo);
        logger.debug(" -- (" + _foo.getStringValue().getClass() + ", " + _foo.getInt32Value().getClass() + ", " + _foo.getVariantValue().getClass() + ")");
        if (!(_foo instanceof SampleStruct) || !(_foo.getStringValue() instanceof String)
                || !(_foo.getInt32Value() instanceof UInt32) || !(_foo.getVariantValue() instanceof Variant)
                || !"bar".equals(_foo.getStringValue()) || _foo.getInt32Value().intValue() != 52
                || !(_foo.getVariantValue().getValue() instanceof Boolean)
                || !(Boolean) _foo.getVariantValue().getValue()) {
            fail("dostuff received the wrong arguments");
        }
        return (T) _foo.getVariantValue().getValue();
    }

    /** Local classes MUST implement this to return false */
    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public String getObjectPath() {
        return null;
    }

    /** The method we are exporting to the Bus. */
    @Override
    public List<Integer> sampleArray(List<String> _ss, Integer[] _is, long[] _ls) {
        logger.debug("Got an array:");
        for (String s : _ss) {
            logger.debug("--{}", s);
        }
        if (_ss.size() != 5 || !"hi".equals(_ss.get(0)) || !"hello".equals(_ss.get(1)) || !"hej".equals(_ss.get(2)) || !"hey".equals(_ss.get(3)) || !"aloha".equals(_ss.get(4))) {
            fail("sampleArray, String array contents incorrect");
        }
        logger.debug("Got an array:");
        for (Integer i : _is) {
            logger.debug("--{}", i);
        }
        if (_is.length != 4 || _is[0] != 1 || _is[1] != 5 || _is[2] != 7 || _is[3] != 9) {
            fail("sampleArray, Integer array contents incorrect");
        }
        logger.debug("Got an array:");
        for (long l : _ls) {
            logger.debug("--{}", l);
        }
        if (_ls.length != 4 || _ls[0] != 2 || _ls[1] != 6 || _ls[2] != 8 || _ls[3] != 12) {
            fail("sampleArray, Integer array contents incorrect");
        }
        List<Integer> v = new ArrayList<>();
        v.add(-1);
        v.add(-5);
        v.add(-7);
        v.add(-12);
        v.add(-18);
        return v;
    }

    @Override
    public String getName() {
        return "This Is A UTF-8 Name: س !!";
    }

    @Override
    public String getNameAndThrow() throws SampleException {
        throw new SampleException("test");
    }

    @Override
    public boolean check() {
        logger.debug("Being checked");
        return false;
    }

    @SuppressWarnings("PMD.UselessParentheses")
    @Override
    public <T> int frobnicate(List<Long> _n, Map<String, Map<UInt16, Short>> _m, T _v) {
        if (null == _n) {
            fail("List was null");
        }
        if (_n.size() != 3) {
            fail("List was wrong size (expected 3, actual " + _n.size() + ")");
        }
        if (_n.get(0) != 2L || _n.get(1) != 5L || _n.get(2) != 71L) {
            fail("List has wrong contents");
        }
        if (!(_v instanceof Integer)) {
            fail("v not an Integer");
        }
        if (((Integer) _v) != 13) {
            fail("v is incorrect");
        }
        if (null == _m) {
            fail("Map was null");
        }
        if (_m.size() != 1) {
            fail("Map was wrong size");
        }
        if (!_m.keySet().contains("stuff")) {
            fail("Incorrect key");
        }
        Map<UInt16, Short> mus = _m.get("stuff");
        if (null == mus) {
            fail("Sub-Map was null");
        }
        if (mus.size() != 3) {
            fail("Sub-Map was wrong size");
        }
        if (!(Short.valueOf((short) 5).equals(mus.get(new UInt16(4))))) {
            fail("Sub-Map has wrong contents");
        }
        if (!(Short.valueOf((short) 6).equals(mus.get(new UInt16(5))))) {
            fail("Sub-Map has wrong contents");
        }
        if (!(Short.valueOf((short) 7).equals(mus.get(new UInt16(6))))) {
            fail("Sub-Map has wrong contents");
        }
        return -5;
    }

    @Override
    public DBusInterface getThis(DBusInterface _t) {
        if (!_t.equals(this)) {
            fail("Didn't get this properly");
        }
        return this;
    }

    @Override
    public void throwme() throws SampleException {
        throw new SampleException("test");
    }

    @SuppressWarnings("PMD.UselessParentheses")
    @Override
    public SampleSerializable<String> testSerializable(byte _b, SampleSerializable<String> _s, int _i) {
        logger.debug("Recieving TestSerializable: {}", _s);
        if (_b != 12 || _i != 13 || !(_s.getInt() == 1) || !(_s.getString().equals("woo"))
                || !(_s.getList().size() == 3) || !(_s.getList().get(0) == 1)
                || !(_s.getList().get(1) == 2) || !(_s.getList().get(2) == 3)) {
            fail("Error in recieving custom synchronisation");
        }
        return _s;
    }

    @Override
    public String recursionTest(String _dbusName, String _path) {
        try {
            SampleRemoteInterface tri = conn.getRemoteObject(_dbusName, _path, SampleRemoteInterface.class);
            return tri.getName();
        } catch (DBusException _ex) {
            fail("Failed with error: " + _ex);
            return "";
        }
    }

    @Override
    public int overload(String _s) {
        return 1;
    }

    @Override
    public int overload(byte _b) {
        return 2;
    }

    @Override
    public int overload() {
        DBusCallInfo info = AbstractConnection.getCallInfo();
        if ("org.freedesktop.dbus.test.AlternateTestInterface".equals(info.getInterface())) {
            return 3;
        } else if (SampleRemoteInterface.class.getName().equals(info.getInterface())) {
            return 4;
        } else {
            return -1;
        }
    }

    @Override
    public List<List<Integer>> checklist(List<List<Integer>> _lli) {
        return _lli;
    }

    @Override
    public SampleNewInterface getNew() {
        SampleNewInterfaceClass n = new SampleNewInterfaceClass();
        try {
            conn.exportObject("/new", n);
        } catch (DBusException _ex) {
            throw new DBusExecutionException(_ex.getMessage(), _ex);
        }
        return n;
    }

    @Override
    public void sig(Type[] _s) {
        if (_s.length != 2 || !_s[0].equals(Byte.class)
                || !(_s[1] instanceof ParameterizedType)
                || !Map.class.equals(((ParameterizedType) _s[1]).getRawType())
                || ((ParameterizedType) _s[1]).getActualTypeArguments().length != 2
                || !CharSequence.class.equals(((ParameterizedType) _s[1]).getActualTypeArguments()[0])
                || !Integer.class.equals(((ParameterizedType) _s[1]).getActualTypeArguments()[1])) {
            fail("Didn't send types correctly: " + Arrays.toString(_s));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void complexv(Variant<? extends Object> _v) {
        if (!"a{ss}".equals(_v.getSig()) || !(_v.getValue() instanceof Map) || ((Map<Object, Object>) _v.getValue()).size() != 1 || !"moo".equals(((Map<Object, Object>) _v.getValue()).get("cow"))) {
            fail("Didn't send variant correctly");
        }
    }

    @Override
    public void reg13291(byte[] _as, byte[] _bs) {
        if (_as.length != _bs.length) {
            fail("didn't receive identical byte arrays");
        }
        for (int i = 0; i < _as.length; i++) {
            if (_as[i] != _bs[i]) {
                fail("didn't receive identical byte arrays");
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A Get(String _interfaceName, String _propertyName) {
        return (A) new DBusPath("/nonexistant/path");
    }

    @Override
    public <A> void Set(String _interfaceName, String _propertyName, A _value) {
    }

    @Override
    public Map<String, Variant<?>> GetAll(String _interfaceName) {
        return new HashMap<>();
    }

    @Override
    public DBusPath pathrv(DBusPath _a) {
        return _a;
    }

    @Override
    public List<DBusPath> pathlistrv(List<DBusPath> _list) {
        return _list;
    }

    @Override
    public Map<DBusPath, DBusPath> pathmaprv(Map<DBusPath, DBusPath> _map) {
        return _map;
    }

    @Override
    public SampleStruct returnSamplestruct(SampleStruct _s) {
        return _s;
    }

    @Override
    public TestEnum getEnumValue() {
        return TestEnum.TESTVAL2;
    }

    @Override
    public void thisShouldBeIgnored() {
        logger.error("You should never see this message!");
    }
}
