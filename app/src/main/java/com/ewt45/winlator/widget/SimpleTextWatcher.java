package com.ewt45.winlator.widget;

import android.text.TextWatcher;

public interface SimpleTextWatcher extends TextWatcher {
    @Override
    default void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    default void beforeTextChanged(CharSequence s, int start, int count, int after) {}
}
