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
        pNormal.setAlpha(200);

        pSelected = new Paint(Paint.ANTI_ALIAS_FLAG);
        pSelected.setColor(Color.parseColor("#FF4B6E"));
        pSelected.setStyle(Paint.Style.FILL);

        pLocation = new Paint(Paint.ANTI_ALIAS_FLAG);
        pLocation.setColor(Color.parseColor("#00C853"));
        pLocation.setStyle(Paint.Style.FILL);

        // Labels plus petits et nets
        pLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
        pLabel.setColor(Color.WHITE);
        pLabel.setTextAlign(Paint.Align.CENTER);
        pLabel.setTextSize(22f);
        pLabel.setFakeBoldText(false);

        pLabelBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        pLabelBg.setColor(Color.parseColor("#99000000")); // Plus transparent
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
        // Coordonnées synchronisées avec ton fichier JSON (1024x1024)
        blocs.add(new Bloc("BP2",   "Bloc Physique 2",  870f, 691f));
        blocs.add(new Bloc("BP1",   "Bloc Physique 1",  624f, 600f));
        blocs.add(new Bloc("BM",    "Bloc Math",        275f, 629f));
        blocs.add(new Bloc("B4",    "Bloc 4",           945f, 230f));
        blocs.add(new Bloc("COUR",  "Cour Rouge",       438f, 727f));
        blocs.add(new Bloc("B2",    "Bloc 2",           432f, 586f));
        blocs.add(new Bloc("PCOUR", "Petite Cour",      472f, 881f));
        blocs.add(new Bloc("B1",  "Bloc Palestine",   765f, 868f));
        blocs.add(new Bloc("BC",    "Bloc C",           199f, 732f));
        blocs.add(new Bloc("BC2",   "Bloc Chimie 2",    837f, 495f));
        blocs.add(new Bloc("BIB",   "Bibliothèque",     277f, 730f));
        blocs.add(new Bloc("BC1",   "Bloc Chimie 1",    623f, 505f));
        blocs.add(new Bloc("ADM",   "Administration",   341f, 884f));
        blocs.add(new Bloc("INF",   "Infirmerie",       169f, 912f));
        blocs.add(new Bloc("B3",    "Bloc 3",           807f, 271f));
        blocs.add(new Bloc("STH",   "Salle Thèse",      94f, 824f));
        blocs.add(new Bloc("D2",    "D2",               78f, 760f));
        blocs.add(new Bloc("D1",    "D1",               76f, 736f));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (satelliteBmp == null) return;

        canvas.save();
        canvas.translate(translateX, translateY);
        canvas.scale(zoomFactor, zoomFactor, getWidth() / 2f, getHeight() / 2f);

        // Dessin de l'image sur toute la surface de la vue
        canvas.drawBitmap(satelliteBmp, null, new RectF(0, 0, getWidth(), getHeight()), null);

        float scaleX = getWidth()  / REF_W;
        float scaleY = getHeight() / REF_H;

        for (Bloc bloc : blocs) {
            float dx = bloc.cx * scaleX;
            float dy = bloc.cy * scaleY;

            // Puces réduites pour ne pas cacher les bâtiments
            float r = 12f;

            Paint p = bloc.isCurrentLocation ? pLocation
                    : bloc.isSelected        ? pSelected
                    : pNormal;

            // Cercle principal avec contour blanc fin
            Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setColor(Color.WHITE);
            stroke.setStrokeWidth(3f);

            canvas.drawCircle(dx, dy, r, stroke);
            canvas.drawCircle(dx, dy, r, p);

            // Affichage intelligent des labels (Zoom > 1.2 ou Sélection)
            if (zoomFactor > 1.2f || bloc.isSelected) {
                String txt = bloc.nom;
                float lw = pLabel.measureText(txt) + 12f;
                float lh = 28f;
                float lx = dx - lw / 2f;
                float ly = dy + r + 5f;

                // Fond arrondi
                canvas.drawRoundRect(new RectF(lx, ly, lx + lw, ly + lh), 12f, 12f, pLabelBg);
                canvas.drawText(txt, dx, ly + lh - 8f, pLabel);
            }
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
        // 1. Calcul des échelles d'affichage
        float scaleX = getWidth()  / REF_W;
        float scaleY = getHeight() / REF_H;

        // 2. Inverser les transformations de la vue (Zoom et Translation)
        // On calcule où le clic se situe sur le canvas "virtuel" avant les transformations
        float unzoomedX = (touchX - translateX - (1 - zoomFactor) * getWidth() / 2f) / zoomFactor;
        float unzoomedY = (touchY - translateY - (1 - zoomFactor) * getHeight() / 2f) / zoomFactor;

        Bloc closest = null;
        float minDist = Float.MAX_VALUE;

        // 3. Rayon de détection dynamique
        // On divise par zoomFactor pour que la zone de clic reste cohérente visuellement
        float adjustedHitRadius = HIT_RADIUS / zoomFactor;

        for (Bloc bloc : blocs) {
            // Coordonnées du bloc scalées au canvas (sans zoom)
            float bx = bloc.cx * scaleX;
            float by = bloc.cy * scaleY;

            // Calcul de la distance Euclidienne
            float dist = (float) Math.hypot(unzoomedX - bx, unzoomedY - by);

            if (dist < adjustedHitRadius && dist < minDist) {
                minDist = dist;
                closest = bloc;
            }
        }

        // 4. Mise à jour de l'état et notification
        if (closest != null) {
            // Déselectionner les autres et sélectionner le nouveau
            for (Bloc b : blocs) b.isSelected = false;
            closest.isSelected = true;

            // Rafraîchir la vue pour afficher le changement de couleur (pSelected)
            invalidate();

            // Notifier MapActivity
            if (clickListener != null) {
                clickListener.onBlocClick(closest);
            }
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