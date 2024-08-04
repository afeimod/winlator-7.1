package com.ewt45.winlator;

import android.util.Log;

public class Test {
    private static final String TAG = "MyTest";
    public static void log(int i){
        Log.d("MyTest", "log: "+i);
    }
    public static void log(String s){
        Log.d(TAG, "log: "+s);
    }

    public void testCallLog(){
        int i = 1+2;
        log(i);
    }

    public long stringToLong(String str) {
        return Long.parseUnsignedLong(str);
    }
}
