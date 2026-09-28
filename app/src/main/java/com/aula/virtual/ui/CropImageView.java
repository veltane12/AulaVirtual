package com.aula.virtual.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import androidx.annotation.Nullable;

public class CropImageView extends View {
    private Bitmap rawBitmap;
    private Bitmap rotatedBitmap;
    private final Matrix matrix = new Matrix();

    private int rotationDegrees = 0;

    private float lastTouchX;
    private float lastTouchY;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private boolean isDragging = false;

    private ScaleGestureDetector scaleDetector;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

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

        maskPaint.setColor(0x99000000); // Dark semi-transparent mask overlay

        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    public void setImageBitmap(Bitmap bitmap) {
        if (bitmap == null) return;
        this.rawBitmap = bitmap;
        this.rotationDegrees = 0;
        updateRotatedBitmap();
        if (getWidth() > 0 && getHeight() > 0) {
            resetMatrix();
        } else {
            post(this::resetMatrix);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed && rotatedBitmap != null) {
            resetMatrix();
        }
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

        float scale = Math.max(cropDiameter / imgW, cropDiameter / imgH);
        float focusX = (viewW - imgW * scale) / 2f;
        float focusY = (viewH - imgH * scale) / 2f;

        matrix.postScale(scale, scale);
        matrix.postTranslate(focusX, focusY);
        checkAndClampBounds();
        invalidate();
    }

    private float getCurrentScale() {
        float[] values = new float[9];
        matrix.getValues(values);
        float scaleX = values[Matrix.MSCALE_X];
        float skewY = values[Matrix.MSKEW_Y];
        return (float) Math.sqrt(scaleX * scaleX + skewY * skewY);
    }

    private void checkAndClampBounds() {
        if (rotatedBitmap == null || getWidth() == 0 || getHeight() == 0) return;

        float viewW = getWidth();
        float viewH = getHeight();
        float radius = Math.min(viewW, viewH) * 0.40f;
        float diameter = radius * 2f;

        float cropLeft = (viewW / 2f) - radius;
        float cropTop = (viewH / 2f) - radius;
        float cropRight = (viewW / 2f) + radius;
        float cropBottom = (viewH / 2f) + radius;

        float imgW = rotatedBitmap.getWidth();
        float imgH = rotatedBitmap.getHeight();

        // Ensure minimum scale covers the crop diameter completely
        float minScale = Math.max(diameter / imgW, diameter / imgH);
        float currentScale = getCurrentScale();

        if (currentScale < minScale) {
            float scaleCorrection = minScale / currentScale;
            matrix.postScale(scaleCorrection, scaleCorrection, viewW / 2f, viewH / 2f);
        }

        // Clamp translation so crop circle is ALWAYS inside image bounds
        RectF imgRect = new RectF(0, 0, imgW, imgH);
        matrix.mapRect(imgRect);

        float deltaX = 0f;
        float deltaY = 0f;

        if (imgRect.left > cropLeft) {
            deltaX = cropLeft - imgRect.left;
        } else if (imgRect.right < cropRight) {
            deltaX = cropRight - imgRect.right;
        }

        if (imgRect.top > cropTop) {
            deltaY = cropTop - imgRect.top;
        } else if (imgRect.bottom < cropBottom) {
            deltaY = cropBottom - imgRect.bottom;
        }

        if (deltaX != 0 || deltaY != 0) {
            matrix.postTranslate(deltaX, deltaY);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (rotatedBitmap == null) return;

        if (matrix.isIdentity() && getWidth() > 0 && getHeight() > 0) {
            resetMatrix();
        }

        // Draw transformed image
        canvas.drawBitmap(rotatedBitmap, matrix, paint);

        // Calculate Crop Hole dimensions
        float viewW = getWidth();
        float viewH = getHeight();
        float cx = viewW / 2f;
        float cy = viewH / 2f;
        float radius = Math.min(viewW, viewH) * 0.40f;

        // Draw Mask Overlay
        int saveCount = canvas.saveLayer(0, 0, viewW, viewH, null);
        canvas.drawRect(0, 0, viewW, viewH, maskPaint);

        // Clear circle in center
        canvas.drawCircle(cx, cy, radius, clearPaint);
        canvas.restoreToCount(saveCount);

        // Draw Circle Accent Border
        canvas.drawCircle(cx, cy, radius, borderPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (rotatedBitmap == null) return super.onTouchEvent(event);

        scaleDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                int pointerIndex = event.getActionIndex();
                lastTouchX = event.getX(pointerIndex);
                lastTouchY = event.getY(pointerIndex);
                activePointerId = event.getPointerId(0);
                isDragging = true;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (!scaleDetector.isInProgress() && isDragging) {
                    int pointerIndex = event.findPointerIndex(activePointerId);
                    if (pointerIndex != -1) {
                        float x = event.getX(pointerIndex);
                        float y = event.getY(pointerIndex);

                        float dx = x - lastTouchX;
                        float dy = y - lastTouchY;

                        if (Math.abs(dx) > 0.2f || Math.abs(dy) > 0.2f) {
                            matrix.postTranslate(dx, dy);
                            checkAndClampBounds();
                            lastTouchX = x;
                            lastTouchY = y;
                            invalidate();
                        }
                    }
                }
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                int pointerIndex = event.getActionIndex();
                int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == activePointerId) {
                    int newPointerIndex = (pointerIndex == 0) ? 1 : 0;
                    activePointerId = event.getPointerId(newPointerIndex);
                    lastTouchX = event.getX(newPointerIndex);
                    lastTouchY = event.getY(newPointerIndex);
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                isDragging = false;
                checkAndClampBounds();
                invalidate();
                break;
            }
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = detector.getScaleFactor();
            float currentScale = getCurrentScale();
            float minScale = 0.2f;
            float maxScale = 5.0f;

            if (currentScale * scale < minScale) {
                scale = minScale / currentScale;
            } else if (currentScale * scale > maxScale) {
                scale = maxScale / currentScale;
            }

            matrix.postScale(scale, scale, detector.getFocusX(), detector.getFocusY());
            checkAndClampBounds();
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
