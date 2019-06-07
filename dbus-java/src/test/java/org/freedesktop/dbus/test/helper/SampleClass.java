package org.freedesktop.dbus.test.helper;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DBusCallInfo;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.test.TestAll;
import org.freedesktop.dbus.test.helper.interfaces.SampleNewInterface;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface2;
import org.freedesktop.dbus.test.helper.structs.IntStruct;
import org.freedesktop.dbus.test.helper.structs.SampleStruct;
import org.freedesktop.dbus.test.helper.structs.SampleStruct3;
import org.freedesktop.dbus.test.helper.structs.SampleStruct4;
import org.freedesktop.dbus.test.helper.structs.SampleTuple;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

public class SampleClass implements SampleRemoteInterface, SampleRemoteInterface2, Properties {
    private DBusConnection conn;

    public SampleClass(DBusConnection _conn) {
        this.conn = _conn;
    }

    @Override
    public String Introspect() {
        return "Not XML";
    }

    @Override
    public int[][] teststructstruct(SampleStruct3 in) {
        List<List<Integer>> lli = in.getInnerListOfLists();
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
	public int[][] testListstruct(SampleStruct4 in) {
		List<IntStruct> list = in.getInnerListOfLists();
		int size = list.size();
		int[][] retVal = new int [size][];
		for(int i = 0; i < size; i++) {
			IntStruct elem = list.get(i);
			retVal[i] = new int [] { elem.getValue1(), elem.getValue2()}; 
		}
		return retVal;
	}

    @Override
    public float testfloat(float[] f) {
        if (f.length < 4 || f[0] != 17.093f || f[1] != -23f || f[2] != 0.0f || f[3] != 31.42f) {
            fail("testfloat got incorrect array");
        }
        return f[0];
    }

    @Override
    public void newpathtest(DBusPath p) {
        if (!p.toString().equals("/new/path/test")) {
            fail("new path test got wrong path");
        }
    }

    @Override
    public void waitawhile() {
        System.out.println("Sleeping.");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
        }
        System.out.println("Done sleeping.");
    }

    @Override
    public <A> SampleTuple<String, List<Integer>, Boolean> show(A in) {
        System.out.println("Showing Stuff: " + in.getClass() + "(" + in + ")");
        if (!(in instanceof Integer) || ((Integer) in).intValue() != 234) {
            fail("show received the wrong arguments");
        }
        DBusCallInfo info = AbstractConnection.getCallInfo();
        List<Integer> l = new ArrayList<>();
        l.add(1953);
        return new SampleTuple<>(info.getSource(), l, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T dostuff(SampleStruct foo) {
        System.out.println("Doing Stuff " + foo);
        System.out.println(" -- (" + foo.getStringValue().getClass() + ", " + foo.getInt32Value().getClass() + ", " + foo.getVariantValue().getClass() + ")");
        if (!(foo instanceof SampleStruct) || !(foo.getStringValue() instanceof String) || !(foo.getInt32Value() instanceof UInt32) || !(foo.getVariantValue() instanceof Variant) || !"bar".equals(foo.getStringValue()) || foo.getInt32Value().intValue() != 52 || !(foo.getVariantValue().getValue() instanceof Boolean) || ((Boolean) foo.getVariantValue().getValue()).booleanValue() != true) {
            fail("dostuff received the wrong arguments");
        }
        return (T) foo.getVariantValue().getValue();
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
    public List<Integer> sampleArray(List<String> ss, Integer[] is, long[] ls) {
        System.out.println("Got an array:");
        for (String s : ss) {
            System.out.println("--" + s);
        }
        if (ss.size() != 5 || !"hi".equals(ss.get(0)) || !"hello".equals(ss.get(1)) || !"hej".equals(ss.get(2)) || !"hey".equals(ss.get(3)) || !"aloha".equals(ss.get(4))) {
            fail("sampleArray, String array contents incorrect");
        }
        System.out.println("Got an array:");
        for (Integer i : is) {
            System.out.println("--" + i);
        }
        if (is.length != 4 || is[0].intValue() != 1 || is[1].intValue() != 5 || is[2].intValue() != 7 || is[3].intValue() != 9) {
            fail("sampleArray, Integer array contents incorrect");
        }
        System.out.println("Got an array:");
        for (long l : ls) {
            System.out.println("--" + l);
        }
        if (ls.length != 4 || ls[0] != 2 || ls[1] != 6 || ls[2] != 8 || ls[3] != 12) {
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
        System.out.println("Being checked");
        return false;
    }

    @Override
    public <T> int frobnicate(List<Long> n, Map<String, Map<UInt16, Short>> m, T v) {
        if (null == n) {
            fail("List was null");
        }
        if (n.size() != 3) {
            fail("List was wrong size (expected 3, actual " + n.size() + ")");
        }
        if (n.get(0) != 2L || n.get(1) != 5L || n.get(2) != 71L) {
            fail("List has wrong contents");
        }
        if (!(v instanceof Integer)) {
            fail("v not an Integer");
        }
        if (((Integer) v) != 13) {
            fail("v is incorrect");
        }
        if (null == m) {
            fail("Map was null");
        }
        if (m.size() != 1) {
            fail("Map was wrong size");
        }
        if (!m.keySet().contains("stuff")) {
            fail("Incorrect key");
        }
        Map<UInt16, Short> mus = m.get("stuff");
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
    public DBusInterface getThis(DBusInterface t) {
        if (!t.equals(this)) {
            fail("Didn't get this properly");
        }
        return this;
    }

    @Override
    public void throwme() throws SampleException {
        throw new SampleException("test");
    }

    @Override
    public SampleSerializable<String> testSerializable(byte b, SampleSerializable<String> s, int i) {
        System.out.println("Recieving TestSerializable: " + s);
        if (b != 12 || i != 13 || !(s.getInt() == 1) || !(s.getString().equals("woo")) || !(s.getList().size() == 3) || !(s.getList().get(0) == 1) || !(s.getList().get(1) == 2) || !(s.getList().get(2) == 3)) {
            fail("Error in recieving custom synchronisation");
        }
        return s;
    }

    @Override
    public String recursionTest() {
        try {
            SampleRemoteInterface tri = conn.getRemoteObject("foo.bar.Test", TestAll.TEST_OBJECT_PATH, SampleRemoteInterface.class);
            return tri.getName();
        } catch (DBusException exDb) {
            fail("Failed with error: " + exDb);
            return "";
        }
    }

    @Override
    public int overload(String s) {
        return 1;
    }

    @Override
    public int overload(byte b) {
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
    public List<List<Integer>> checklist(List<List<Integer>> lli) {
        return lli;
    }

    @Override
    public SampleNewInterface getNew() {
        SampleNewInterfaceClass n = new SampleNewInterfaceClass();
        try {
            conn.exportObject("/new", n);
        } catch (DBusException ex) {
            throw new DBusExecutionException(ex.getMessage());
        }
        return n;
    }

    @Override
    public void sig(Type[] s) {
        if (s.length != 2 || !s[0].equals(Byte.class)
                || !(s[1] instanceof ParameterizedType)
                || !Map.class.equals(((ParameterizedType) s[1]).getRawType())
                || ((ParameterizedType) s[1]).getActualTypeArguments().length != 2
                || !CharSequence.class.equals(((ParameterizedType) s[1]).getActualTypeArguments()[0])
                || !Integer.class.equals(((ParameterizedType) s[1]).getActualTypeArguments()[1])) {
            fail("Didn't send types correctly: " + Arrays.toString(s));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void complexv(Variant<? extends Object> v) {
        if (!"a{ss}".equals(v.getSig()) || !(v.getValue() instanceof Map) || ((Map<Object, Object>) v.getValue()).size() != 1 || !"moo".equals(((Map<Object, Object>) v.getValue()).get("cow"))) {
            fail("Didn't send variant correctly");
        }
    }

    @Override
    public void reg13291(byte[] as, byte[] bs) {
        if (as.length != bs.length) {
            fail("didn't receive identical byte arrays");
        }
        for (int i = 0; i < as.length; i++) {
            if (as[i] != bs[i]) {
                fail("didn't receive identical byte arrays");
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A Get(String interface_name, String property_name) {
        return (A) new DBusPath("/nonexistant/path");
    }

    @Override
    public <A> void Set(String interface_name, String property_name, A value) {
    }

    @Override
    public Map<String, Variant<?>> GetAll(String interface_name) {
        return new HashMap<>();
    }

    @Override
    public DBusPath pathrv(DBusPath a) {
        return a;
    }

    @Override
    public List<DBusPath> pathlistrv(List<DBusPath> list) {
        return list;
    }

    @Override
    public Map<DBusPath, DBusPath> pathmaprv(Map<DBusPath, DBusPath> map) {
        return map;
    }
    
    @Override
    public SampleStruct returnSamplestruct( SampleStruct s ){
        return s;
    }
}