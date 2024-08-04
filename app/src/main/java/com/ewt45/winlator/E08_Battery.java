package com.ewt45.winlator;

import android.app.GameManager;
import android.app.GameState;
import android.content.Context;
import android.media.midi.MidiDevice;
import android.media.midi.MidiManager;
import android.os.Build;
import android.os.Process;

import androidx.annotation.RequiresApi;

/**
 * 一些电池选项，可能提升游戏性能
 */
public class E08_Battery {
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void testGameState(Context c, int newState) {
        GameManager gameManager = c.getSystemService(GameManager.class);
        int currState = gameManager.getGameMode();
        gameManager.setGameState(new GameState(false, newState));
    }

    public void test() {
        Process.getExclusiveCores();
        MidiManager m;
        MidiDevice device;
        //device.
    }
}
