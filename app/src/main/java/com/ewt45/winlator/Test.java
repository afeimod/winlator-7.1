package com.ewt45.winlator;

import android.os.FileUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.compress.utils.IOUtils;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public class Test {
    private static final String TAG = "MyTest";
    public static void log(int i){
        Log.d("MyTest", "log: "+i);
    }
    public static void log(String s){
        Log.d(TAG, "log: "+s);
    }

    static {
        System.loadLibrary("midi_server");
    }
    public void testCallLog(){
        int i = 1+2;
        log(i);
    }

    public static void addItemToSettings(AppCompatActivity a, LinearLayout hostRoot) {
        Button btn = new Button(a);
        btn.setText("选择文件以解压...");
        btn.setOnClickListener(v -> {
            File sfFile = new File(v.getContext().getFilesDir(), "sndfnt.sf2");
            try (InputStream is = v.getContext().getAssets().open(sfFile.getName());
                 FileOutputStream fos = new FileOutputStream(sfFile)) {
                IOUtils.copy(is, fos);
                fluidsynthHelloWorld(sfFile.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        hostRoot.addView(btn, QH.lpLinear(-1, -2).top().to());
    }
    public static native void fluidsynthHelloWorld(String soundfontPath);

    public long stringToLong(String str) {
        return Long.parseUnsignedLong(str);
    }
}
