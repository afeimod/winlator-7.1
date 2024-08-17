package com.ewt45.winlator;

import android.view.SubMenu;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.winlator.MainActivity;
import com.winlator.R;
import com.winlator.XServerDisplayActivity;


//TODO
// 1.wine等输出日志写到侧栏中？
// 2. logcat日志？
public class ExtraFeatures {
    public static class MyApplication extends E01_MyApplication { }

    public static class AndroidShortcut extends E10_ShortcutOnAndroidScreen { }

    public static class KeyInput extends E02_KeyInput { }

    public static class Logcat extends E03_Logcat { }

    public static class PRootShell extends E04_PRootShell { }

    public static class Rotate extends E05_Orientation { }

    public static class ClickToMovePointer extends E06_ClickToMovePointer { }

    public static class PIP extends E07_PIP { }

    public static class Vibrate extends E09_Vibrate { }

    public static class ManageStorage extends E11_ManageStorage { }

    public static class Locale extends E12_Locale {}

    public static class MidiServer extends E14_MidiServer {}

    /**
     * 在MainActivity onCreate 结尾调用, 进行一些app启动后需要自动执行的操作。
     * <br/> 注意目前QH初始化是在MainActivity中被调用，如果XServer退出，那么这个函数会被再次调用，
     * 而且全部变量会被清空（因为使用runtime.exit()结束了进程）
     */
    public static void onMainActivityCreate(MainActivity a) {
        //添加桌面快捷方式启动的判断
        AndroidShortcut.handleIfStartFromScreenShortcut(a);

        ExtraFeatures.Logcat.onCheckChange(a, ExtraFeatures.Logcat.isChecked(a));
    }

    /**
     * XServerDisplayActivity onCreate 结尾调用，在ui线程。
     * 添加左侧菜单的额外功能，做一些初始化
     * @param isGeneratePrefix 此次是否为生成wine prefix
     */
    public static void onXServerActivityCreate(XServerDisplayActivity a, boolean isGeneratePrefix) {
        if (isGeneratePrefix)
            return;

        //添加选项。顺带初始化
        SubMenu menu = ((NavigationView) a.findViewById(R.id.NavigationView)).getMenu().addSubMenu(QH.string.额外功能);

        menu.add(QH.string.旋转屏幕选项).setOnMenuItemClickListener(item -> {
            ((DrawerLayout) a.findViewById(R.id.DrawerLayout)).closeDrawers();
            return Rotate.onClick(a);
        });

        menu.add(QH.string.proot终端).setOnMenuItemClickListener(item -> {
            ((DrawerLayout) a.findViewById(R.id.DrawerLayout)).closeDrawers();
            return PRootShell.showTerminalDialog(a);
        });

        menu.add(QH.string.画中画模式).setOnMenuItemClickListener(item -> {
            ((DrawerLayout) a.findViewById(R.id.DrawerLayout)).closeDrawers();
            return PIP.enterPIP(a);
        });
    }

    /**
     * 和 onXServerActivityCreate 类似，也是初始化时
     * <br/> 注意是非ui线程，谨慎操作ui
     * <br/> 在 environment.startEnvironmentComponents(); 之前被调用
     * @param isGeneratePrefix 此次是否为生成wine prefix
     */
    public static void beforeXEnvironmentStart(XServerDisplayActivity a, boolean isGeneratePrefix) {
        if (isGeneratePrefix)
            return;

        //初次启动设置
        //设置旋转方向
        E05_Orientation.setIsOrieLandFromPref(a, E05_Orientation.getIsOrieLandFromPref(a), false);
        //绝对位置点击
        E06_ClickToMovePointer.setIsCurMoveRel(a, E06_ClickToMovePointer.getIsCurMoveRelFromPref(a), false);
        //MIDI
        MidiServer.startOrStopByPref(a);
    }

    /**
     * 设置界面的额外功能
     */
    public static class SettingsExtra {
        public static void addItems(AppCompatActivity a, FrameLayout hostRoot) {

            //style在代码里创建的话，margin那些不生效，然后FieldSet那个linear内容全部看不见（但是宽高还是有的，不知道问题是啥）
//            LinearLayout linearRoot = new LinearLayout(a);
//            linearRoot.setId(View.generateViewId());
//            linearRoot.setOrientation(LinearLayout.VERTICAL);
//            ((LinearLayout) ((ScrollView) hostRoot.getChildAt(0)).getChildAt(0))
//                    .addView(linearRoot, QH.LayoutParams.Linear.one(-1, -1).to());
//
//            //xml里的style属性，在new时作为defStyleRes参数传入即可
//            View splitView = new View(a, null, 0, R.style.HorizontalLine);
//            linearRoot.addView(splitView, QH.LayoutParams.Linear.one(-1, dp8 * 2).to());
//
//            FrameLayout frameSub = new FrameLayout(a);
//            linearRoot.addView(frameSub, QH.LayoutParams.Linear.one(-1, -2).to());
//            linearRoot.addView(new View(a), QH.LayoutParams.Linear.one(-1, dp8 * 10).to());
//
//            LinearLayout linearSub = new LinearLayout(a, null, 0, R.style.FieldSet);
//            frameSub.addView(linearSub);
//
//            TextView tv = new TextView(a, null, 0, R.style.FieldSetLabel);
//            tv.setText("额外");
//            frameSub.addView(tv);

            View host = a.getLayoutInflater().inflate(R.layout.zzz_settings_fragment_extra, (LinearLayout) ((ScrollView) hostRoot.getChildAt(0)).getChildAt(0), true);
            ((TextView) host.findViewById(R.id.TVSettingExtra)).setText(QH.string.额外功能);
            LinearLayout linearSub = host.findViewById(R.id.setting_linear_other_root);

            Logcat.addItemToSettings(a, linearSub);

            PRootShell.addItemToSettings(a, linearSub);

            ManageStorage.addItemToSettings(a, linearSub);

            MidiServer.addItemToSettings(a, linearSub);

            if(QH.isTest) {
                Locale.addItemToSettings(a, linearSub);
                E13_Extractor.addItemToSettings(a, linearSub);
            }



        }
    }



}
