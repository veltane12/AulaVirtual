package com.aula.virtual.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.aula.virtual.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

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

        boolean isDark = isNightMode(context);
        if (isDark) {
            et.setBackgroundResource(R.drawable.bg_bootstrap_input_dark);
            et.setTextColor(ContextCompat.getColor(context, R.color.white));
            et.setHintTextColor(ContextCompat.getColor(context, R.color.bs_secondary));
        } else {
            et.setBackgroundResource(R.drawable.bg_bootstrap_input);
            et.setTextColor(ContextCompat.getColor(context, R.color.black));
            et.setHintTextColor(ContextCompat.getColor(context, R.color.bs_secondary));
        }

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

    public static TextView createDialogOptionButton(Context context, String text, boolean isPlaceholder) {
        TextView tv = new TextView(context);
        int paddingH = (int) (14 * context.getResources().getDisplayMetrics().density);
        int paddingV = (int) (12 * context.getResources().getDisplayMetrics().density);
        tv.setPadding(paddingH, paddingV, paddingH, paddingV);
        tv.setTextSize(15f);

        boolean isDark = isNightMode(context);
        if (isDark) {
            tv.setBackgroundResource(R.drawable.bg_bootstrap_input_dark);
        } else {
            tv.setBackgroundResource(R.drawable.bg_bootstrap_input);
        }

        setOptionState(tv, text, isPlaceholder, context);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        int marginV = (int) (6 * context.getResources().getDisplayMetrics().density);
        lp.setMargins(0, marginV, 0, marginV);
        tv.setLayoutParams(lp);
        return tv;
    }

    public static TextView createDialogOptionButton(Context context, String text) {
        return createDialogOptionButton(context, text, false);
    }

    public static void setOptionState(TextView tv, String text, boolean isPlaceholder, Context context) {
        tv.setText(text);
        tv.setAlpha(1.0f);
        boolean isDark = isNightMode(context);
        if (isPlaceholder) {
            tv.setTextColor(ContextCompat.getColor(context, R.color.bs_secondary));
        } else {
            if (isDark) {
                tv.setTextColor(ContextCompat.getColor(context, R.color.white));
            } else {
                tv.setTextColor(ContextCompat.getColor(context, R.color.black));
            }
        }
    }

    public static void arrangeGridButtons(GridLayout gridLayout) {
        if (gridLayout == null) return;
        List<View> visibleButtons = new ArrayList<>();
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View child = gridLayout.getChildAt(i);
            if (child != null && child.getVisibility() == View.VISIBLE) {
                visibleButtons.add(child);
            }
        }

        int count = visibleButtons.size();
        for (int i = 0; i < count; i++) {
            View v = visibleButtons.get(i);
            GridLayout.LayoutParams params = (GridLayout.LayoutParams) v.getLayoutParams();
            if (params == null) {
                params = new GridLayout.LayoutParams();
            }

            boolean isLastOdd = (i == count - 1) && (count % 2 != 0);

            if (isLastOdd) {
                params.columnSpec = GridLayout.spec(0, 2, 1.0f);
                params.rowSpec = GridLayout.spec(i / 2);
                params.width = 0;
            } else {
                int col = i % 2;
                int row = i / 2;
                params.columnSpec = GridLayout.spec(col, 1, 1.0f);
                params.rowSpec = GridLayout.spec(row);
                params.width = 0;
            }
            v.setLayoutParams(params);
        }
    }

    private static final int LOADING_OVERLAY_ID = 0x7f090999;

    public static void showLoadingOverlay(View view, Context context) {
        if (view == null || context == null) return;

        view.post(() -> {
            ViewGroup targetContainer = null;
            if (view instanceof ViewGroup && !(view instanceof ScrollView) && !(view instanceof NestedScrollView)) {
                targetContainer = (ViewGroup) view;
            }
            if (targetContainer == null && view.getParent() instanceof ViewGroup) {
                targetContainer = (ViewGroup) view.getParent();
            }
            if (targetContainer == null && context instanceof Activity) {
                targetContainer = ((Activity) context).findViewById(android.R.id.content);
            }

            if (targetContainer == null) return;

            View existing = targetContainer.findViewById(LOADING_OVERLAY_ID);
            if (existing != null) {
                existing.setVisibility(View.VISIBLE);
                existing.setAlpha(1.0f);
                return;
            }

            FrameLayout overlay = new FrameLayout(context);
            overlay.setId(LOADING_OVERLAY_ID);
            overlay.setClickable(true);
            overlay.setFocusable(true);
            overlay.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            boolean isDark = isNightMode(context);
            overlay.setBackgroundColor(isDark ? 0xD9121212 : 0xD9FFFFFF);

            LinearLayout container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams containerLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
            container.setLayoutParams(containerLp);

            ProgressBar progressBar = new ProgressBar(context);
            progressBar.setIndeterminate(true);

            TextView tvMsg = new TextView(context);
            tvMsg.setText("Cargando información...");
            tvMsg.setTextSize(14f);
            tvMsg.setTypeface(null, Typeface.BOLD);
            tvMsg.setPadding(0, (int) (12 * context.getResources().getDisplayMetrics().density), 0, 0);
            tvMsg.setTextColor(isDark ? ContextCompat.getColor(context, R.color.white) : ContextCompat.getColor(context, R.color.black));

            container.addView(progressBar);
            container.addView(tvMsg);
            overlay.addView(container);

            try {
                targetContainer.addView(overlay);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void hideLoadingOverlay(View view) {
        if (view == null) return;
        view.post(() -> {
            ViewGroup targetContainer = null;
            if (view instanceof ViewGroup) {
                targetContainer = (ViewGroup) view;
            }
            
            View overlay = null;
            if (targetContainer != null) {
                overlay = targetContainer.findViewById(LOADING_OVERLAY_ID);
            }
            if (overlay == null && view.getParent() instanceof ViewGroup) {
                overlay = ((ViewGroup) view.getParent()).findViewById(LOADING_OVERLAY_ID);
            }
            if (overlay == null && view.getContext() instanceof Activity) {
                View content = ((Activity) view.getContext()).findViewById(android.R.id.content);
                if (content instanceof ViewGroup) {
                    overlay = content.findViewById(LOADING_OVERLAY_ID);
                }
            }

            if (overlay != null && overlay.getVisibility() == View.VISIBLE) {
                final View finalOverlay = overlay;
                finalOverlay.animate()
                        .alpha(0f)
                        .setDuration(180)
                        .withEndAction(() -> {
                            finalOverlay.setVisibility(View.GONE);
                            if (finalOverlay.getParent() instanceof ViewGroup) {
                                ((ViewGroup) finalOverlay.getParent()).removeView(finalOverlay);
                            }
                        })
                        .start();
            }
        });
    }

    private static boolean isNightMode(Context context) {
        if (ThemeHelper.isDarkMode(context)) return true;
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}
