package com.fsm.navigator.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.fsm.navigator.R;

import java.util.ArrayList;
import java.util.List;

/**
 * FsmMapView.java – Photo satellite + marqueurs cliquables
 *
 * Coordonnées extraites par analyse de pixels sur l'image originale
 * Image de référence : 722 x 758 pixels
 * Origine : coin haut-gauche
 */
public class FsmMapView extends View {

    // ===== MODÈLE MARQUEUR =====
    public static class Bloc {
        public String  id;
        public String  nom;
        public float   cx, cy;  // coordonnées en pixels sur l'image originale
        public boolean isSelected;
        public boolean isCurrentLocation;

        public Bloc(String id, String nom, float cx, float cy) {
            this.id  = id;
            this.nom = nom;
            this.cx  = cx;
            this.cy  = cy;
        }
    }

    // ===== INTERFACE =====
    public interface OnBlocClickListener {
        void onBlocClick(Bloc bloc);
    }

    // ===== ATTRIBUTS =====
    private List<Bloc> blocs        = new ArrayList<>();
    private Bitmap     satelliteBmp;
    private Paint      paintNormal, paintSelected, paintCurrent;
    private Paint      paintBorder, paintLabel, paintLabelBg, paintShadow;
    private OnBlocClickListener listener;

    // Zoom
    private float zoomFactor = 1.0f;
    private float minZoom    = 0.8f;
    private float maxZoom    = 4.0f;
    private float panX       = 0f;
    private float panY       = 0f;

    // Taille de référence = taille réelle de l'image en pixels
    private static final float REF_W = 722f;
    private static final float REF_H = 758f;

    private static final float MARKER_R   = 15f;
    private static final float LABEL_SIZE = 15f;

    // ===== CONSTRUCTEURS =====
    public FsmMapView(Context context) {
        super(context);
        init(context);
    }

    public FsmMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    // ===== INIT =====
    private void init(Context context) {
        try {
            satelliteBmp = BitmapFactory.decodeResource(
                    context.getResources(), R.drawable.campus);
        } catch (Exception e) {
            satelliteBmp = null;
        }

        paintNormal = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintNormal.setColor(Color.parseColor("#003B7A"));
        paintNormal.setStyle(Paint.Style.FILL);
        paintNormal.setAlpha(230);

        paintSelected = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSelected.setColor(Color.parseColor("#1565C0"));
        paintSelected.setStyle(Paint.Style.FILL);

        paintCurrent = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCurrent.setColor(Color.parseColor("#E53935"));
        paintCurrent.setStyle(Paint.Style.FILL);

        paintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBorder.setColor(Color.WHITE);
        paintBorder.setStyle(Paint.Style.STROKE);
        paintBorder.setStrokeWidth(2.5f);

        paintLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLabel.setColor(Color.WHITE);
        paintLabel.setTextSize(LABEL_SIZE);
        paintLabel.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paintLabel.setTextAlign(Paint.Align.CENTER);

        paintLabelBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLabelBg.setColor(Color.parseColor("#CC003B7A"));
        paintLabelBg.setStyle(Paint.Style.FILL);

        paintShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintShadow.setColor(Color.parseColor("#55000000"));
        paintShadow.setStyle(Paint.Style.FILL);

        initBlocs();
    }

    // ===== BLOCS — coordonnées pixel exactes depuis l'image =====
    private void initBlocs() {
        blocs.clear();
        blocs.add(new Bloc("B4",    "Bloc 4",        665f, 235f));
        blocs.add(new Bloc("B3",    "Bloc 3",        576f, 248f));
        blocs.add(new Bloc("BC2",   "Chimie 2",      584f, 388f));
        blocs.add(new Bloc("BC1",   "Chimie 1",      431f, 398f));
        blocs.add(new Bloc("AMPHIS",    "Amphis 1 à 6",        307f, 445f));
        blocs.add(new Bloc("BP1",   "Physique 1",    440f, 458f));
        blocs.add(new Bloc("BM",    "Math",          209f, 481f));
        blocs.add(new Bloc("BP2",   "Physique 2",    599f, 517f));
        blocs.add(new Bloc("COUR",  "Cour Rouge",    315f, 541f));
        blocs.add(new Bloc("BIB",   "Bib Centrale",  216f, 548f));
        blocs.add(new Bloc("BC",  "Bib C1 à C3",    153f, 552f));
        blocs.add(new Bloc("DOCT",  "Salle des doctorants",        100f, 559f));
        blocs.add(new Bloc("HORS", "D1, D2, Salle Thése",          88f, 608f));
        blocs.add(new Bloc("B1",    "Palestine",     529f, 627f));
        blocs.add(new Bloc("ADMIN", "Administration",   238f, 635f));
        blocs.add(new Bloc("PCOUR","Petite Cour",    334f, 648f));
    }

    // ===== DESSIN =====
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        canvas.save();
        canvas.translate(panX, panY);
        canvas.scale(zoomFactor, zoomFactor);

        // 1. Photo satellite
        if (satelliteBmp != null) {
            canvas.drawBitmap(satelliteBmp, null, new RectF(0, 0, w, h), null);
        } else {
            Paint bg = new Paint();
            bg.setColor(Color.parseColor("#C8D8C8"));
            canvas.drawRect(0, 0, w, h, bg);
        }

        // 2. Marqueurs — mise à l'échelle depuis pixels référence → taille écran
        float scaleX = (float) w / REF_W;
        float scaleY = (float) h / REF_H;

        for (Bloc bloc : blocs) {
            drawMarker(canvas, bloc, scaleX, scaleY);
        }

        canvas.restore();
    }

    private void drawMarker(Canvas canvas, Bloc bloc, float sx, float sy) {
        float x = bloc.cx * sx;
        float y = bloc.cy * sy;

        Paint fill = bloc.isCurrentLocation ? paintCurrent
                : bloc.isSelected        ? paintSelected
                : paintNormal;

        // Ombre
        canvas.drawCircle(x + 2f, y + 2f, MARKER_R + 1f, paintShadow);

        // Cercle principal
        canvas.drawCircle(x, y, MARKER_R, fill);
        canvas.drawCircle(x, y, MARKER_R, paintBorder);

        // Anneau position actuelle
        if (bloc.isCurrentLocation) {
            Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
            ring.setColor(Color.parseColor("#88E53935"));
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(3f);
            canvas.drawCircle(x, y, MARKER_R + 6f, ring);
        }

        // Label
        String label = (bloc.isSelected || bloc.isCurrentLocation)
                ? bloc.nom
                : bloc.nom.split(" ")[0];

        Paint lp = new Paint(paintLabel);
        lp.setTextSize(LABEL_SIZE);

        float lw = lp.measureText(label) + 8f;
        float lh = lp.getTextSize() + 6f;
        float ly = y - MARKER_R - 4f;

        RectF lRect = new RectF(x - lw/2f, ly - lh, x + lw/2f, ly);
        canvas.drawRoundRect(lRect, 4f, 4f, paintLabelBg);
        canvas.drawText(label, x, ly - 3f, lp);
    }

    // ===== ZOOM =====
    public void zoomIn() {
        zoomFactor = Math.min(zoomFactor + 0.3f, maxZoom);
        invalidate();
    }

    public void zoomOut() {
        zoomFactor = Math.max(zoomFactor - 0.3f, minZoom);
        clampPan();
        invalidate();
    }

    public void resetZoom() {
        zoomFactor = 1.0f;
        panX = 0f;
        panY = 0f;
        invalidate();
    }

    private void clampPan() {
        int w = getWidth(), h = getHeight();
        float maxPanX = w * (zoomFactor - 1);
        float maxPanY = h * (zoomFactor - 1);
        panX = Math.max(-maxPanX, Math.min(0, panX));
        panY = Math.max(-maxPanY, Math.min(0, panY));
    }

    // ===== TOUCH =====
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float tx = (event.getX() - panX) / zoomFactor;
            float ty = (event.getY() - panY) / zoomFactor;

            int w = getWidth(), h = getHeight();
            float scaleX = (float) w / REF_W;
            float scaleY = (float) h / REF_H;
            float touchR = 35f;

            for (Bloc bloc : blocs) {
                float mx = bloc.cx * scaleX;
                float my = bloc.cy * scaleY;
                double dist = Math.sqrt(Math.pow(tx-mx, 2) + Math.pow(ty-my, 2));
                if (dist <= touchR) {
                    for (Bloc b : blocs) b.isSelected = false;
                    bloc.isSelected = true;
                    invalidate();
                    if (listener != null) listener.onBlocClick(bloc);
                    return true;
                }
            }
            for (Bloc b : blocs) b.isSelected = false;
            invalidate();
        }
        return true;
    }

    // ===== API =====
    public void setOnBlocClickListener(OnBlocClickListener l) { this.listener = l; }

    public void setCurrentLocation(String blocId) {
        for (Bloc b : blocs) b.isCurrentLocation = b.id.equals(blocId);
        invalidate();
    }

    public void clearSelection() {
        for (Bloc b : blocs) { b.isSelected = false; b.isCurrentLocation = false; }
        invalidate();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        float density = getResources().getDisplayMetrics().density;
        // Ratio 722/758 ≈ 0.952
        int w = (int)(900 * density);
        int h = (int)(945 * density);
        setMeasuredDimension(w, h);
    }
}