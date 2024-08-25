package com.ewt45.winlator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.winlator.XServerDisplayActivity;
import com.winlator.core.EnvVars;
import com.winlator.core.FileUtils;
import com.winlator.xenvironment.ImageFs;

import org.apache.commons.compress.utils.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

class E14_MidiServer {
    private static final String TAG = "E14_MidiServer";
    private static final String MIDIMAP_DLL_ASSET_PATH = "extra/midimap.dll";
    private static final String MIDIMAP_DLL_EXTRACT_PATH = "home/xuser/.wine/drive_c/windows/syswow64/midimap.dll";
    private static final String SF_ASSET_PATH = "extra/sndfnt.sf2";
    private static final String SF_EXTRACT_PATH = "usr/share/sounds/sf2/default-GM.sf2";
    private static final String SF_EXTRACT_PATH_ENV = "MIDI_SF2_PATH"; //用于在native中获取路径
    private static final String SF_SOCKET_PORT = "43456"; //用于native和dll建立socket的端口
    private static final String SF_SOCKET_PORT_ENV = "MIDI_SOCKET_PORT";

    private static final String PREF_KEY_MIDI_SERVER_ENABLE = "PREF_KEY_MIDI_SERVER_ENABLE";

    private static File sndFntFile = null;
    private static File midimapFile = null;


    /**
     * 初始化，启动fluidsynth服务，并开始socket监听
     */
    private static void start(Context c) {
        ImageFs imageFs = ImageFs.find(c);
        if (sndFntFile == null) sndFntFile = new File(imageFs.getRootDir(), SF_EXTRACT_PATH);
        if (midimapFile == null) midimapFile = new File(imageFs.getRootDir(), MIDIMAP_DLL_EXTRACT_PATH);
        extractSoundFontAndDll(c);

        //设置sf2文件的路径（jni用到）和socket连接端口（jni与dll用到）
        QH.setenv(SF_EXTRACT_PATH_ENV, sndFntFile.getAbsolutePath(), true);
        QH.setenv(SF_SOCKET_PORT_ENV, SF_SOCKET_PORT, true);
        fluidSynthStart();
    }

    private static void stop() {
        fluidSynthClose();
        QH.unsetenv(SF_EXTRACT_PATH_ENV);
        QH.unsetenv(SF_SOCKET_PORT_ENV);
    }

    public static void addItemToSettings(AppCompatActivity a, LinearLayout hostRoot) {
        CheckBox check = new CheckBox(a);
        check.setText(QH.string.处理midi音乐);
        check.setChecked(getIsEnabledFromPref(a));
        check.setOnCheckedChangeListener((v, isChecked) -> setIsEnabledToPref(v.getContext(), isChecked));

        Button btnTest = new Button(a);
        btnTest.setAllCaps(false);
        btnTest.setText(QH.string.测试);
        btnTest.setOnClickListener(v -> new Thread(() -> {
            start(v.getContext());
            fluidSynthPlayTest();
            stop();
        }).start());

        LinearLayout linearBtns = new LinearLayout(a);
        linearBtns.setOrientation(LinearLayout.VERTICAL);
        linearBtns.addView(check);
        linearBtns.addView(btnTest);

        LinearLayout linear = QH.wrapHelpBtnWithLinear(a, linearBtns, QH.string.处理midi音乐说明);
        hostRoot.addView(linear, QH.lpLinear(-1, -2).top().to());
    }

    /** 解压sontfont文件和midimap.dll */
    private static void extractSoundFontAndDll(Context c) {
        if (!sndFntFile.exists()) {
            sndFntFile.getParentFile().mkdirs();
            QH.extractFromAsset(c, SF_ASSET_PATH, sndFntFile);
        }

        if (midimapFile.getParentFile().exists()) { //确保有syswow64这个文件夹
            QH.extractFromAsset(c, MIDIMAP_DLL_ASSET_PATH, midimapFile);
        }

    }

    public static boolean getIsEnabledFromPref(Context c) {
        return QH.getPreference(c).getBoolean(PREF_KEY_MIDI_SERVER_ENABLE, false);
    }

    public static void setIsEnabledToPref(Context c, boolean isEnabled) {
        QH.getPreference(c).edit().putBoolean(PREF_KEY_MIDI_SERVER_ENABLE, isEnabled).apply();
    }

    /** 启动容器时，根据用户设置，启动或停止midi服务 */
    public static void startOrStopByPref(XServerDisplayActivity c) {
        boolean enabled = getIsEnabledFromPref(c);
        EnvVars envVars = UtilsReflect.getFieldObject(XServerDisplayActivity.class, c, "envVars");
        String ovr = envVars.get("WINEDLLOVERRIDES");
        if (!ovr.isEmpty() && !ovr.endsWith(";")) ovr += ";";

        Log.d(TAG, "startOrStopByPref: midi server: "+ (enabled ? "开" : "关"));
        if (enabled) {
            //应用切后台时应停止正在播放的音符。退出容器时application会重建，不解除注册也无所谓
            c.getApplication().registerActivityLifecycleCallbacks(new QH.SimpleActivityLifecycleCallbacks() {
                @Override
                public void onActivityStopped(@NonNull Activity activity) {
                    if (activity instanceof XServerDisplayActivity)
                        fluidSynthAllNoteOff();
                }
            });
            //添加环境变量指定为native
            envVars.put("WINEDLLOVERRIDES", ovr + "midimap=n");
            envVars.put(SF_SOCKET_PORT_ENV, SF_SOCKET_PORT); //不知为何在Os设置的不生效。

            start(c);
        }
        else {
            //环境变量设置dll为builtin
            envVars.put("WINEDLLOVERRIDES", ovr + "midimap=b");

            stop();
        }
    }

    static {
        System.loadLibrary("midi_server");
    }

    /**
     * 启动fluidsynth服务，并开始socket监听。
     * <br/> 如果fluidsynth已经启动则不做任何操作
     */
    private native static void fluidSynthStart() ;
    private native static void fluidSynthPlayTest();
    private native static void fluidSynthClose();
    /** 全部通道的音符停止播放。用于app切到后台时 */
    private native static void fluidSynthAllNoteOff();
}
