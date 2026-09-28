package com.aula.virtual.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import androidx.cardview.widget.CardView;
import com.aula.virtual.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.ByteArrayOutputStream;

public class ImageUtils {

    public interface OnImageCroppedListener {
        void onCropped(Bitmap croppedBitmap);
    }

    public static void showCropAndAdjustDialog(Context context, Bitmap rawBitmap, OnImageCroppedListener listener) {
        if (context == null || rawBitmap == null) return;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_crop_image, null);
        CropImageView cropImageView = dialogView.findViewById(R.id.cropImageView);
        View btnRotate = dialogView.findViewById(R.id.btnRotate90);

        cropImageView.setImageBitmap(rawBitmap);

        if (btnRotate != null) {
            btnRotate.setOnClickListener(v -> cropImageView.rotate90Degrees());
        }

        new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    Bitmap cropped = cropImageView.getCroppedBitmap();
                    if (cropped != null && listener != null) {
                        listener.onCropped(cropped);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        
        // Resize to max 400px to save DB space
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap cropped = Bitmap.createBitmap(bitmap, 0, 0, size, size);
        Bitmap scaled = Bitmap.createScaledBitmap(cropped, 400, 400, true);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public static Bitmap base64ToBitmap(String base64Str) {
        if (base64Str == null || base64Str.isEmpty()) return null;
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    public static void setProfileImage(ImageView iv, String profileImage, int defaultPaddingPx) {
        if (iv == null) return;
        Context context = iv.getContext();
        
        if (profileImage != null && !profileImage.isEmpty()) {
            Bitmap bitmap = base64ToBitmap(profileImage);
            if (bitmap != null) {
                iv.setPadding(0, 0, 0, 0);
                iv.setImageBitmap(bitmap);
                iv.setImageTintList(null);
                if (iv.getParent() instanceof CardView) {
                    CardView card = (CardView) iv.getParent();
                    card.setCardBackgroundColor(Color.TRANSPARENT);
                    card.setCardElevation(dpToPx(context, 4));
                }
                return;
            }
        }

        iv.setPadding(0, 0, 0, 0);
        iv.setImageResource(R.drawable.ic_person_badge);
        iv.setImageTintList(null);
        if (iv.getParent() instanceof CardView) {
            CardView card = (CardView) iv.getParent();
            card.setCardBackgroundColor(Color.TRANSPARENT);
            card.setCardElevation(dpToPx(context, 4));
        }
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
