package com.aula.virtual.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import androidx.annotation.Nullable;

public class CropImageView extends View {
    private Bitmap rawBitmap;
    private Bitmap rotatedBitmap;
    private final Matrix matrix = new Matrix();

    private float scaleFactor = 1.0f;
    private float focusX = 0f;
    private float focusY = 0f;
    private int rotationDegrees = 0;

    private float lastTouchX;
    private float lastTouchY;
    private boolean isDragging = false;

    private ScaleGestureDetector scaleDetector;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CropImageView(Context context) {
        super(context);
        init(context);
    }

    public CropImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6f);
        borderPaint.setColor(ThemeHelper.getSubjectColor(context, ThemeHelper.getAccentColorName(context)));

        maskPaint.setColor(0x99000000); // Semi-transparent dark overlay
    }

    public void setImageBitmap(Bitmap bitmap) {
        if (bitmap == null) return;
        this.rawBitmap = bitmap;
        this.rotationDegrees = 0;
        this.scaleFactor = 1.0f;
        this.focusX = 0f;
        this.focusY = 0f;
        updateRotatedBitmap();
        post(this::resetMatrix);
    }

    public void rotate90Degrees() {
        if (rawBitmap == null) return;
        rotationDegrees = (rotationDegrees + 90) % 360;
        updateRotatedBitmap();
        resetMatrix();
        invalidate();
    }

    private void updateRotatedBitmap() {
        if (rawBitmap == null) return;
        if (rotationDegrees == 0) {
            rotatedBitmap = rawBitmap;
        } else {
            Matrix rotMatrix = new Matrix();
            rotMatrix.postRotate(rotationDegrees);
            rotatedBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.getWidth(), rawBitmap.getHeight(), rotMatrix, true);
        }
    }

    private void resetMatrix() {
        if (rotatedBitmap == null || getWidth() == 0 || getHeight() == 0) return;
        matrix.reset();

        float viewW = getWidth();
        float viewH = getHeight();
        float imgW = rotatedBitmap.getWidth();
        float imgH = rotatedBitmap.getHeight();

        float cropRadius = Math.min(viewW, viewH) * 0.40f;
        float cropDiameter = cropRadius * 2f;

        scaleFactor = Math.max(cropDiameter / imgW, cropDiameter / imgH);

        focusX = (viewW - imgW * scaleFactor) / 2f;
        focusY = (viewH - imgH * scaleFactor) / 2f;

        updateMatrix();
    }

    private void updateMatrix() {
        matrix.reset();
        matrix.postScale(scaleFactor, scaleFactor);
        matrix.postTranslate(focusX, focusY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (rotatedBitmap == null) return;

        // Draw image
        canvas.drawBitmap(rotatedBitmap, matrix, paint);

        // Calculate Crop Hole
        float viewW = getWidth();
        float viewH = getHeight();
        float cx = viewW / 2f;
        float cy = viewH / 2f;
        float radius = Math.min(viewW, viewH) * 0.40f;

        // Draw Mask Overlay
        int saveCount = canvas.saveLayer(0, 0, viewW, viewH, null);
        canvas.drawRect(0, 0, viewW, viewH, maskPaint);

        // Clear circle in center
        Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawCircle(cx, cy, radius, clearPaint);
        canvas.restoreToCount(saveCount);

        // Draw Circle Accent Border
        canvas.drawCircle(cx, cy, radius, borderPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (rotatedBitmap == null) return super.onTouchEvent(event);

        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isDragging = true;
                break;

            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress() && isDragging) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    focusX += dx;
                    focusY += dy;
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    updateMatrix();
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = detector.getScaleFactor();
            scaleFactor *= scale;
            scaleFactor = Math.max(0.3f, Math.min(scaleFactor, 5.0f));

            float focusXTouch = detector.getFocusX();
            float focusYTouch = detector.getFocusY();

            focusX = focusXTouch - (focusXTouch - focusX) * scale;
            focusY = focusYTouch - (focusYTouch - focusY) * scale;

            updateMatrix();
            invalidate();
            return true;
        }
    }

    public Bitmap getCroppedBitmap() {
        if (rotatedBitmap == null) return null;

        float viewW = getWidth();
        float viewH = getHeight();
        float radius = Math.min(viewW, viewH) * 0.40f;
        float diameter = radius * 2f;

        float cropLeft = (viewW / 2f) - radius;
        float cropTop = (viewH / 2f) - radius;

        Bitmap cropped = Bitmap.createBitmap((int) diameter, (int) diameter, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(cropped);

        Matrix drawMatrix = new Matrix(matrix);
        drawMatrix.postTranslate(-cropLeft, -cropTop);

        canvas.drawBitmap(rotatedBitmap, drawMatrix, paint);
        return cropped;
    }
}
