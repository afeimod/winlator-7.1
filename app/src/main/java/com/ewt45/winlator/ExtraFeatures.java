package com.ewt45.winlator;

import android.view.SubMenu;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
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


    /**
     * 设置界面的额外功能
     */
    public static class SettingsExtra {
        public static void addItems(AppCompatActivity a, FrameLayout hostRoot) {
            LinearLayout myLinearRoot = hostRoot.findViewById(R.id.setting_linear_other_root);

            Logcat.addItemToSettings(a, myLinearRoot);

            PRootShell.addItemToSettings(a, myLinearRoot);

            ManageStorage.addItemToSettings(a, myLinearRoot);

//            if(QH.isTest) {
//                Button btn = new Button(a);
//                myLinearRoot.addView(btn, QH.lpLinear(-1, -2).top().to());
//                btn.setOnClickListener(v-> PRootShell.showTerminalDialog(a));
//            }

        }
    }

    /**
     * 启动容器后左侧菜单的额外功能
     */
    public static class XMenuExtra {
        public static void addItemsAndInit(XServerDisplayActivity a) {
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

            //初次启动设置
            //设置旋转方向
            E05_Orientation.setIsOrieLandFromPref(a, E05_Orientation.getIsOrieLandFromPref(a), false);
            //绝对位置点击
            E06_ClickToMovePointer.setIsCurMoveRel(a, E06_ClickToMovePointer.getIsCurMoveRelFromPref(a), false);
        }
    }

}
