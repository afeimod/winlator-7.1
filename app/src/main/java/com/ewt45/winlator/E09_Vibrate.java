package com.ewt45.winlator;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class E09_Vibrate {
    static VibrationEffect ve = VibrationEffect.createOneShot(100, 16);
    static AudioAttributes aa = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .build();
//    VibrationAttributes va = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH);

    public static void on(Context c) {
        Vibrator vibrator = c.getSystemService(Vibrator.class);

        vibrator.vibrate(ve, aa);
//        vibrator.vibrate(100);
    }
}
