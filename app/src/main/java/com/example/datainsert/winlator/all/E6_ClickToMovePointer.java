package com.example.datainsert.winlator.all;

import android.app.Activity;
import android.content.Context;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.winlator.R;
import com.winlator.XServerDisplayActivity;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.widget.TouchpadView;
import com.winlator.widget.XServerView;
import com.winlator.xserver.XServer;

/**
 * 绝对位置点击
 */
public class E6_ClickToMovePointer {
    private static final String PREF_KEY_IS_CUR_MOVE_REL = "IS_CUR_MOVE_REL";

    public static void addInputControlsItems(XServerDisplayActivity a, ContentDialog dialog) {
//        QH.refreshIsTest(a);
        try {
            LinearLayout linearRoot = (LinearLayout) dialog.findViewById(R.id.CBRelativeMouseMovement).getParent();

            CheckBox checkBox2 = new CheckBox(a);
            checkBox2.setText(QH.string.绝对位置点击选项);
            checkBox2.setChecked(!getIsCurMoveRelFromPref(a));
            checkBox2.setOnCheckedChangeListener((compoundButton, isChecked) -> setIsCurMoveRel(a, !isChecked, true));
            //初始化TouchAreaView变量 在ExtraFeatures.XMenuExtra中
            linearRoot.addView(QH.wrapHelpBtnWithLinear(a, checkBox2, QH.string.绝对位置点击选项说明));

            //内容太多，设置为可滚动
            QH.makeDialogContentScrollable(a, dialog);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean getIsCurMoveRelFromPref(Context a) {
        return QH.getPreference(a).getBoolean(PREF_KEY_IS_CUR_MOVE_REL, true);
    }

    public static void setIsCurMoveRel(Context a, boolean isRel, boolean updatePref) {
        if (updatePref)
            QH.getPreference(a).edit().putBoolean(PREF_KEY_IS_CUR_MOVE_REL, isRel).apply();
        try {
            TouchpadView.isRelativeOnStart = isRel;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    //点击切换拉伸全屏时 需要重新设置matrix
    //但update里用isFullScreen判断，fullscreen要等待下一次渲染时才会刷新，因此不能直接加在点击监听里。
    public static void updateXFormInTouchpadView(XServerView v) {
        Activity a = QH.getActivity(v);

        if(!(a instanceof XServerDisplayActivity aa))
            return;

        XServer xServer = UtilsReflect.getFieldObject(XServerDisplayActivity.class, aa, "xServer");
        TouchpadView tpv = UtilsReflect.getFieldObject(XServerDisplayActivity.class, aa, "touchpadView");
        UtilsReflect.invokeMethod(UtilsReflect.getMethod(TouchpadView.class, "updateXform", int.class, int.class, int.class, int.class),
                tpv, tpv.getWidth(), tpv.getHeight(), xServer.screenInfo.width, xServer.screenInfo.height);
    }
}
