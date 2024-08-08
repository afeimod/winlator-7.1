package com.ewt45.winlator;

import android.util.Log;

import com.winlator.xserver.ClientOpcodes;

import java.lang.reflect.Field;

public class TestXserver {
    private static final String TAG = "TestXserver";
    static String[] names = new String[127];
    static {
        try {
            Field[] fields = ClientOpcodes.class.getFields();
            for(Field field : fields) {
                names[field.getInt(null)] = field.getName();
            }
            names[36] = "GRAB_SERVER";
            names[37] = "UNGRAB_SERVER";
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    public static void logOpName(int opcode) {
        String name = (opcode > 0 && opcode <= 127) ? names[opcode] : "未知："+opcode;
        Log.d(TAG, "x请求 操作码："+name);
    }
}
