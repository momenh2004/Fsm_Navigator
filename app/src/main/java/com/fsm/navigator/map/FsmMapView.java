package com.fsm.navigator.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.fsm.navigator.R;

import java.util.ArrayList;
import java.util.List;

public class FsmMapView extends View {

    public static class Bloc {
        public String id, nom;
        public float cx, cy;
        public boolean isSelected, isCurrentLocation;
        public Bloc(String id, String nom, float cx, float cy) {
            this.id = id; this.nom = nom; this.cx = cx; this.cy = cy;
        }
    }

    public interface OnBlocClickListener {
        void onBlocClick(Bloc bloc);
    }

    private List<Bloc>          blocs = new ArrayList<>();
    private Bitmap              satelliteBmp;
    private OnBlocClickListener clickListener;

    private Paint pNormal, pSelected, pLocation, pLabel, pLabelBg;

    private float zoomFactor = 1.0f;
    private float translateX = 0f, translateY = 0f;
    private float lastTouchX, lastTouchY;
    private boolean isDragging = false;
    private ScaleGestureDetector scaleDetector;

    private static final float REF_W      = 1024f;
    private static final float REF_H      = 1024f;
    private static final float HIT_RADIUS = 50f;

    public FsmMapView(Context context, AttributeSet attrs) { super(context, attrs); init(context); }
    public FsmMapView(Context context)                     { super(context); init(context); }

    private void init(Context context) {
        satelliteBmp = BitmapFactory.decodeResource(getResources(), R.drawable.campus_visible);

        pNormal = new Paint(Paint.ANTI_ALIAS_FLAG);
        pNormal.setColor(Color.parseColor("#00D4FF"));
        pNormal.setStyle(Paint.Style.FILL);
        pNormal.setAlpha(180);

        pSelected = new Paint(Paint.ANTI_ALIAS_FLAG);
        pSelected.setColor(Color.parseColor("#FF4B6E"));
        pSelected.setStyle(Paint.Style.FILL);

        pLocation = new Paint(Paint.ANTI_ALIAS_FLAG);
        pLocation.setColor(Color.parseColor("#00C853"));
        pLocation.setStyle(Paint.Style.FILL);

        pLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
        pLabel.setColor(Color.WHITE);
        pLabel.setTextAlign(Paint.Align.CENTER);
        pLabel.setTextSize(28f);
        pLabel.setFakeBoldText(true);

        pLabelBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        pLabelBg.setColor(Color.parseColor("#CC000000"));
        pLabelBg.setStyle(Paint.Style.FILL);

        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector d) {
                        zoomFactor *= d.getScaleFactor();
                        zoomFactor  = Math.max(0.8f, Math.min(3.0f, zoomFactor));
                        invalidate();
                        return true;
                    }
                });

        initBlocs();
    }

    private void initBlocs() {
        blocs.clear();
        blocs.add(new Bloc("BP2",   "Bloc Physique 2",  943f, 231f));
        blocs.add(new Bloc("BP1",   "Bloc Physique 1",  808f, 271f));
        blocs.add(new Bloc("BM",    "Bloc Math",        418f, 462f));
        blocs.add(new Bloc("B4",    "Bloc 4",           835f, 490f));
        blocs.add(new Bloc("COUR",  "Cour Rouge",       621f, 502f));
        blocs.add(new Bloc("B2",    "Bloc 2",           432f, 583f));
        blocs.add(new Bloc("PCOUR", "Petite Cour",      624f, 600f));
        blocs.add(new Bloc("BPAL",  "Bloc Palestine",   273f, 623f));
        blocs.add(new Bloc("BC",    "Bloc C",           865f, 683f));
        blocs.add(new Bloc("BC2",   "Bloc Chimie 2",    195f, 729f));
        blocs.add(new Bloc("BIB",   "Bibliothèque",     449f, 729f));
        blocs.add(new Bloc("BC1",   "Bloc Chimie 1",    271f, 731f));
        blocs.add(new Bloc("ADM",   "Administration",    79f, 744f));
        blocs.add(new Bloc("INF",   "Infirmerie",        96f, 814f));
        blocs.add(new Bloc("B3",    "Bloc 3",           763f, 867f));
        blocs.add(new Bloc("STH",   "Salle Thèse",      475f, 880f));
        blocs.add(new Bloc("D1D2",  "D1 et D2",         332f, 886f));
        blocs.add(new Bloc("SRV",   "Bloc Service",     159f, 906f));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (satelliteBmp == null) return;

        canvas.save();
        canvas.translate(translateX, translateY);
        canvas.scale(zoomFactor, zoomFactor, getWidth() / 2f, getHeight() / 2f);

        canvas.drawBitmap(satelliteBmp, null, new RectF(0, 0, getWidth(), getHeight()), null);

        float scaleX = getWidth()  / REF_W;
        float scaleY = getHeight() / REF_H;

        for (Bloc bloc : blocs) {
            float dx = bloc.cx * scaleX;
            float dy = bloc.cy * scaleY;
            float r  = 22f;

            Paint p = bloc.isCurrentLocation ? pLocation
                    : bloc.isSelected        ? pSelected
                    : pNormal;

            // Anneau extérieur
            Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
            ring.setColor(p.getColor());
            ring.setAlpha(80);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(6f);
            canvas.drawCircle(dx, dy, r + 8f, ring);

            // Cercle principal
            canvas.drawCircle(dx, dy, r, p);

            // Label avec fond
            float lw = pLabel.measureText(bloc.nom) + 16f;
            float lh = 36f;
            float lx = dx - lw / 2f;
            float ly = dy + r + 8f;
            canvas.drawRoundRect(new RectF(lx, ly, lx + lw, ly + lh), 8f, 8f, pLabelBg);
            canvas.drawText(bloc.nom, dx, ly + lh - 8f, pLabel);
        }

        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isDragging = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - lastTouchX;
                float dy = event.getY() - lastTouchY;
                if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                    isDragging  = true;
                    translateX += dx;
                    translateY += dy;
                    lastTouchX  = event.getX();
                    lastTouchY  = event.getY();
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (!isDragging && !scaleDetector.isInProgress())
                    handleClick(event.getX(), event.getY());
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void handleClick(float touchX, float touchY) {
        float scaleX = getWidth()  / REF_W;
        float scaleY = getHeight() / REF_H;

        float imgX = (touchX - translateX - (1 - zoomFactor) * getWidth()  / 2f) / zoomFactor;
        float imgY = (touchY - translateY - (1 - zoomFactor) * getHeight() / 2f) / zoomFactor;

        Bloc  closest = null;
        float minDist = Float.MAX_VALUE;

        for (Bloc bloc : blocs) {
            float bx   = bloc.cx * scaleX;
            float by   = bloc.cy * scaleY;
            float dist = (float) Math.hypot(imgX - bx, imgY - by);
            if (dist < HIT_RADIUS && dist < minDist) {
                minDist = dist;
                closest = bloc;
            }
        }

        if (closest != null) {
            for (Bloc b : blocs) b.isSelected = false;
            closest.isSelected = true;
            invalidate();
            if (clickListener != null) clickListener.onBlocClick(closest);
        }
    }

    public void setOnBlocClickListener(OnBlocClickListener l) { this.clickListener = l; }
    public void setCurrentLocation(String id) {
        for (Bloc b : blocs) b.isCurrentLocation = b.id.equals(id);
        invalidate();
    }
    public void zoomIn()    { zoomFactor = Math.min(3.0f, zoomFactor + 0.2f); invalidate(); }
    public void zoomOut()   { zoomFactor = Math.max(0.8f, zoomFactor - 0.2f); invalidate(); }
    public void resetZoom() { zoomFactor = 1.0f; translateX = 0; translateY = 0; invalidate(); }
}