/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.test.helper;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DirectConnection;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface;
import org.freedesktop.dbus.test.helper.structs.IntStruct;
import org.freedesktop.dbus.test.helper.structs.SampleStruct3;
import org.freedesktop.dbus.test.helper.structs.SampleStruct4;
import org.freedesktop.dbus.types.UInt16;

public class P2pTestServer implements SampleRemoteInterface {
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
    public String getNameAndThrow() {
        return getName();
    }

    @Override
    public String getName() {
        System.out.println("getName called");
        return "Peer2Peer Server";
    }

    @Override
    public <T> int frobnicate(List<Long> n, Map<String, Map<UInt16, Short>> m, T v) {
        return 3;
    }

    @Override
    public void throwme() throws SampleException {
        System.out.println("throwme called");
        throw new SampleException("BOO");
    }

    @Override
    public void waitawhile() {
        return;
    }

    @Override
    public int overload() {
        return 1;
    }

    @Override
    public void sig(Type[] s) {
    }

    @Override
    public void newpathtest(DBusPath p) {
    }

    @Override
    public void reg13291(byte[] as, byte[] bs) {
    }

    @Override
    public DBusPath pathrv(DBusPath a) {
        return a;
    }

    @Override
    public List<DBusPath> pathlistrv(List<DBusPath> a) {
        return a;
    }

    @Override
    public Map<DBusPath, DBusPath> pathmaprv(Map<DBusPath, DBusPath> a) {
        return a;
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public String getObjectPath() {
        return null;
    }

    @Override
    public float testfloat(float[] f) {
        System.out.println("got float: " + Arrays.toString(f));
        return f[0];
    }

    public static void main(String[] args) throws Exception {
        String address = DirectConnection.createDynamicSession();
        // String address = "tcp:host=localhost,port=12344,guid="+Transport.genGUID();
        PrintWriter w = new PrintWriter(new FileOutputStream("address"));
        w.println(address);
        w.flush();
        w.close();
        try (DirectConnection dc = new DirectConnection(address + ",listen=true")) {            
            System.out.println("Connected");
            dc.exportObject("/Test", new P2pTestServer());
        }
    }
}
