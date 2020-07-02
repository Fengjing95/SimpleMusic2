package com.example.musicplayer;

import android.app.AlertDialog;
import android.content.Context;

public class LoadingDialog extends AlertDialog {
    public LoadingDialog(Context context) {
        super(context);
    }

    public LoadingDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    public LoadingDialog(Context context, int themeResId) {
        super(context, themeResId);
    }
}
