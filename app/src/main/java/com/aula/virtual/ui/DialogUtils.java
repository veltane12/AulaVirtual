package com.aula.virtual.ui;

import android.content.Context;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import com.aula.virtual.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DialogUtils {

    public static MaterialAlertDialogBuilder createMaterialDialog(Context context, String title) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        if (title != null && !title.isEmpty()) {
            builder.setTitle(title);
        }
        return builder;
    }

    public static LinearLayout createDialogContainer(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * context.getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, padding);
        return layout;
    }

    public static EditText createStyledEditText(Context context, String hint, int inputType) {
        EditText et = new EditText(context);
        et.setHint(hint);
        int paddingH = (int) (14 * context.getResources().getDisplayMetrics().density);
        int paddingV = (int) (12 * context.getResources().getDisplayMetrics().density);
        et.setPadding(paddingH, paddingV, paddingH, paddingV);
        et.setTextSize(15f);
        et.setBackgroundResource(R.drawable.bg_bootstrap_input);
        if (inputType != 0) {
            et.setInputType(inputType);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int marginV = (int) (6 * context.getResources().getDisplayMetrics().density);
        lp.setMargins(0, marginV, 0, marginV);
        et.setLayoutParams(lp);
        return et;
    }
}
