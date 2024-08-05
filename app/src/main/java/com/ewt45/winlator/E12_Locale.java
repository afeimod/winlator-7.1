package com.ewt45.winlator;

import android.app.LocaleManager;
import android.content.Context;
import android.os.Build;
import android.os.LocaleList;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

class E12_Locale {
    public static void addItemToSettings(AppCompatActivity a, LinearLayout hostRoot) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            return;
        Button btn = new Button(a);
        btn.setText("语言");
        PopupMenu menu = new PopupMenu(a, btn);
        menu.getMenu().add("zh").setCheckable(true).setChecked(QH.locale.equals("zh")).setOnMenuItemClickListener(item -> {
            changeLocaleIfNeeded(a, "zh");
            return true;
        });
        menu.getMenu().add("en").setCheckable(true).setChecked(QH.locale.equals("en")).setOnMenuItemClickListener(item -> {
            changeLocaleIfNeeded(a, "en");
            return true;
        });
        menu.getMenu().add("清空").setOnMenuItemClickListener(item -> {
            changeLocaleIfNeeded(a, null);
            return true;
        });
        btn.setOnClickListener(v -> menu.show());

        hostRoot.addView(btn, QH.lpLinear(-1, -2).top().to());
    }


    private static void changeLocaleIfNeeded(Context c, @Nullable String target) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        || QH.locale.equals(target))
            return;

        c.getSystemService(LocaleManager.class).setApplicationLocales(target == null
                        ? LocaleList.getEmptyLocaleList() :new LocaleList(Locale.forLanguageTag(target)));
    }
}
