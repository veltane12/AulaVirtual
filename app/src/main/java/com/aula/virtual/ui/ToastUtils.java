package com.aula.virtual.ui;

import android.content.Context;
import android.widget.Toast;

public class ToastUtils {
    private static Toast currentToast = null;

    public static void showToast(Context context, String message, int duration) {
        if (context == null || message == null || message.trim().isEmpty()) return;
        try {
            if (currentToast != null) {
                currentToast.cancel();
            }
            currentToast = Toast.makeText(context.getApplicationContext(), message, duration);
            currentToast.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showToast(Context context, String message) {
        showToast(context, message, Toast.LENGTH_SHORT);
    }
}
