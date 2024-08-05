package com.ewt45.winlator;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.winlator.MainActivity;
import com.winlator.xenvironment.ImageFsInstaller;

/**
 * 增强型文件权限，可以使用文件路径访问外接设备
 */
public class E11_ManageStorage {
    public static final int PERMISSION_MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 44;
    //当sdk>=R时应该执行这个
    @RequiresApi(api = Build.VERSION_CODES.R)
    public static boolean requestPermission(Activity a) {
        //来自termux https://github.com/termux/termux-app/blob/master/termux-shared/src/main/java/com/termux/shared/android/PermissionUtils.java#L353

        if(Environment.isExternalStorageManager())
            return false;

        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setData(Uri.parse("package:" + a.getPackageName()));
        a.startActivityForResult(intent, PERMISSION_MANAGE_EXTERNAL_STORAGE_REQUEST_CODE);

//        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
//        ActivityCompat.requestPermissions(a, permissions, PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        return true;
    }

    //参考MainActivity.onRequestPermissionsResult
    public static void onActivityResult(MainActivity a, int requestCode) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R || requestCode != PERMISSION_MANAGE_EXTERNAL_STORAGE_REQUEST_CODE)
            return;

        if(Environment.isExternalStorageManager())
            ImageFsInstaller.installIfNeeded(a);
        else
            a.finish();
    }


    /**
     * 主界面 - 设置中 添加选项。
     */

    public static void addItemToSettings(AppCompatActivity a, LinearLayout hostRoot) {
        Button btn = new Button(a);
        btn.setAllCaps(false);
        btn.setText(QH.string.获取管理全部文件权限);
        btn.setEnabled(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R);
        btn.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
                return;

            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.addCategory("android.intent.category.DEFAULT");
            intent.setData(Uri.parse("package:" + a.getPackageName()));
            a.startActivity(intent);
        });

        LinearLayout linear = QH.wrapHelpBtnWithLinear(a, btn, QH.string.获取管理全部文件权限说明);
        HorizontalScrollView scroll = new HorizontalScrollView(a);
        scroll.addView(linear);
        hostRoot.addView(scroll, QH.lpLinear(-1, -2).top().to());
    }
}
