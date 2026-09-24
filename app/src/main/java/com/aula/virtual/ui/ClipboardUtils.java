package com.aula.virtual.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

public class ClipboardUtils {

    public static void copyToClipboard(Context context, String label, String text) {
        if (text == null || text.isEmpty()) {
            Toast.makeText(context, "No hay datos para copiar", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, label + " copiado al portapapeles", Toast.LENGTH_SHORT).show();
        }
    }
}
