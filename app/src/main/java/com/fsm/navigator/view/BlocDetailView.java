package com.fsm.navigator.view;

import com.fsm.navigator.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * BlocDetailView.java – Vue Canvas pour le plan interne d'un bloc.
 * Supporte : clic sur salle, changement d'étage, callback OnSalleClickListener.
 */
public class BlocDetailView extends View {

    // ===== INTERFACE CALLBACK =====
    public interface OnSalleClickListener {
        void onSalleClick(String salleNom, int etage);
    }

    // ===== MODÈLE SALLE CLIQUABLE =====
    private static class SalleRect {
        String nom;
        RectF  rect;
        int    etage;
        SalleRect(String nom, RectF rect, int etage) {
            this.nom = nom; this.rect = rect; this.etage = etage;
        }
    }

    // ===== ATTRIBUTS =====
    private String  blocId  = "B3";
    private int     etage   = 0;
    private String  selectedSalle = null;
    private OnSalleClickListener listener;
    private List<SalleRect> salleRects = new ArrayList<>();

    private Paint pWall, pWallFill, pRoom, pRoomFill, pRoomSelected;
    private Paint pText, pTextSmall, pBg;
    private Paint pEntree, pSortie, pEscalier, pCour, pDoor;
    private float realWidth = 17.76f;
    private float realHeight = 30.74f;
    private float scale;

    public BlocDetailView(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }
    public BlocDetailView(Context ctx)                     { super(ctx); init(); }

    private void init() {
        pBg = new Paint();
        pBg.setColor(Color.parseColor("#0D1B2A"));

        pWall = new Paint(Paint.ANTI_ALIAS_FLAG);
        pWall.setColor(Color.parseColor("#4A90D9"));
        pWall.setStyle(Paint.Style.STROKE);
        pWall.setStrokeWidth(3f);

        pWallFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        pWallFill.setColor(Color.parseColor("#0A1628"));
        pWallFill.setStyle(Paint.Style.FILL);

        pRoom = new Paint(Paint.ANTI_ALIAS_FLAG);
        pRoom.setColor(Color.parseColor("#4A90D9"));
        pRoom.setStyle(Paint.Style.STROKE);
        pRoom.setStrokeWidth(2f);

        pRoomFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        pRoomFill.setColor(Color.parseColor("#1A3A5C"));
        pRoomFill.setStyle(Paint.Style.FILL);

        pRoomSelected = new Paint(Paint.ANTI_ALIAS_FLAG);
        pRoomSelected.setColor(Color.parseColor("#4400D4FF"));
        pRoomSelected.setStyle(Paint.Style.FILL);

        pText = new Paint(Paint.ANTI_ALIAS_FLAG);
        pText.setColor(Color.WHITE);
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        pTextSmall = new Paint(Paint.ANTI_ALIAS_FLAG);
        pTextSmall.setColor(Color.parseColor("#A0C4E8"));
        pTextSmall.setTextAlign(Paint.Align.CENTER);

        pEntree = new Paint(Paint.ANTI_ALIAS_FLAG);
        pEntree.setColor(Color.parseColor("#00694A"));
        pEntree.setStyle(Paint.Style.FILL);

        pSortie = new Paint(Paint.ANTI_ALIAS_FLAG);
        pSortie.setColor(Color.parseColor("#8B3A00"));
        pSortie.setStyle(Paint.Style.FILL);

        pEscalier = new Paint(Paint.ANTI_ALIAS_FLAG);
        pEscalier.setColor(Color.parseColor("#1A4A6C"));
        pEscalier.setStyle(Paint.Style.FILL);

        pCour = new Paint(Paint.ANTI_ALIAS_FLAG);
        pCour.setColor(Color.parseColor("#061018"));
        pCour.setStyle(Paint.Style.FILL);

        pDoor = new Paint(Paint.ANTI_ALIAS_FLAG);
        pDoor.setColor(Color.parseColor("#00A878"));
        pDoor.setStyle(Paint.Style.STROKE);
        pDoor.setStrokeWidth(3f);
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if (w == 0) w = 900;
        if (h == 0) h = 800;
        setMeasuredDimension(w, h);
    }
    // ===== API PUBLIQUE =====
    public void setBlocId(String id)                       { this.blocId = id; invalidate(); }
    public void setEtage(int etage)                        { this.etage = etage; selectedSalle = null; invalidate(); }
    public void setOnSalleClickListener(OnSalleClickListener l) { this.listener = l; }

    // ===== TOUCH =====
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float tx = event.getX(), ty = event.getY();
            for (SalleRect sr : salleRects) {
                if (sr.rect.contains(tx, ty)) {
                    selectedSalle = sr.nom;
                    invalidate();
                    if (listener != null) listener.onSalleClick(sr.nom, sr.etage);
                    return true;
                }
            }
        }
        return true;
    }

    // ===== DESSIN =====
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        salleRects.clear();
        canvas.drawRect(0, 0, getWidth(), getHeight(), pBg);

        switch (blocId != null ? blocId : "B3") {
            case "B3":
                if (etage == 0) drawBloc3RDC(canvas);
                else            drawBloc3Etage1(canvas);
                break;
            case "B1":    drawBlocPalestine(canvas); break;
            case "A1-6":  drawAmphis16(canvas);      break;
            case "BM":
            case "BMATH": drawBlocMath(canvas);       break;
            default:      drawGeneric(canvas);        break;
        }
    }

    // =========================================================
    // BLOC 3 — RDC
    // =========================================================
    private void drawBloc3RDC(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float mL=w*0.08f, mT=h*0.08f;
        float dW=w-2*mL, dH=h-mT-h*0.14f;
        float sx=dW/17f, sy=dH/16f;

        canvas.drawRect(mL,mT,mL+dW,mT+dH,pWallFill);
        canvas.drawRect(mL,mT,mL+dW,mT+dH,pWall);

        // Cour
        float cX0=mL+3.8f*sx, cY0=mT+2f*sy, cX1=mL+13.2f*sx, cY1=mT+11.5f*sy;
        canvas.drawRect(cX0,cY0,cX1,cY1,pCour);
        pWall.setAlpha(100);
        canvas.drawRect(cX0,cY0,cX1,cY1,pWall);
        pWall.setAlpha(255);
        pTextSmall.setTextSize(sy*1.1f);
        canvas.drawText("Cour intérieure",(cX0+cX1)/2f,(cY0+cY1)/2f,pTextSmall);

        // Salles gauche
        String[] nomsG = {"305","306","307"};
        float[][] sG = {{0f,2f,3.6f,4.8f},{0f,5.2f,3.6f,8f},{0f,9f,3.6f,11.8f}};
        for (int i=0;i<3;i++) drawRoom(canvas,mL,mT,sx,sy,sG[i],nomsG[i],0);

        // Salles droite
        String[] nomsD = {"304","303","302"};
        float[][] sD = {{13.4f,2f,17f,4.8f},{13.4f,5.2f,17f,8f},{13.4f,9f,17f,11.8f}};
        for (int i=0;i<3;i++) drawRoom(canvas,mL,mT,sx,sy,sD[i],nomsD[i],0);

        // 308, 301
        drawRoom(canvas,mL,mT,sx,sy,new float[]{0.5f,12.2f,5.5f,14.5f},"308",0);
        drawRoom(canvas,mL,mT,sx,sy,new float[]{11.5f,12.2f,16.5f,14.5f},"301",0);

        // Escalier
        float eX0=mL+6.5f*sx,eY0=mT+12f*sy,eX1=mL+10.5f*sx,eY1=mT+14f*sy;
        canvas.drawRect(eX0,eY0,eX1,eY1,pEscalier);
        canvas.drawRect(eX0,eY0,eX1,eY1,pRoom);
        pTextSmall.setTextSize(sy*0.85f);
        canvas.drawText("Escalier",(eX0+eX1)/2f,(eY0+eY1)/2f+sy*0.3f,pTextSmall);

        // Entrée / Sortie
        float enX=mL+8.5f*sx;
        canvas.drawRoundRect(new RectF(enX-2.2f*sx,mT+dH-0.8f*sy,enX+2.2f*sx,mT+dH-0.1f*sy),6f,6f,pEntree);
        pText.setTextSize(sy*0.85f);
        canvas.drawText("ENTRÉE",enX,mT+dH-0.3f*sy,pText);
        canvas.drawRoundRect(new RectF(enX-2f*sx,mT,enX+2f*sx,mT+0.8f*sy),6f,6f,pSortie);
        canvas.drawText("SORTIE",enX,mT+0.58f*sy,pText);

        drawLegend(canvas,w,h);
        pText.setColor(Color.WHITE);
        pText.setTextSize(h*0.03f);
        canvas.drawText("Bloc 3 — RDC",w/2f,h*0.97f,pText);
    }

    // =========================================================
    // BLOC 3 — 1er ÉTAGE
    // =========================================================
    private void drawBloc3Etage1(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float mL=w*0.08f, mT=h*0.08f;
        float dW=w-2*mL, dH=h-mT-h*0.14f;
        float sx=dW/17f, sy=dH/16f;

        canvas.drawRect(mL,mT,mL+dW,mT+dH,pWallFill);
        canvas.drawRect(mL,mT,mL+dW,mT+dH,pWall);

        float cX0=mL+3.8f*sx,cY0=mT+2f*sy,cX1=mL+13.2f*sx,cY1=mT+11.5f*sy;
        canvas.drawRect(cX0,cY0,cX1,cY1,pCour);
        pWall.setAlpha(100);
        canvas.drawRect(cX0,cY0,cX1,cY1,pWall);
        pWall.setAlpha(255);
        pTextSmall.setTextSize(sy*1.1f);
        canvas.drawText("Cour intérieure",(cX0+cX1)/2f,(cY0+cY1)/2f,pTextSmall);

        // Salles gauche
        String[] nomsG = {"313","314","315"};
        float[][] sG = {{0f,2f,3.6f,4.8f},{0f,5.2f,3.6f,8f},{0f,9f,3.6f,11.8f}};
        for (int i=0;i<3;i++) drawRoom(canvas,mL,mT,sx,sy,sG[i],nomsG[i],1);

        // Salles droite
        String[] nomsD = {"312","311","310"};
        float[][] sD = {{13.4f,2f,17f,4.8f},{13.4f,5.2f,17f,8f},{13.4f,9f,17f,11.8f}};
        for (int i=0;i<3;i++) drawRoom(canvas,mL,mT,sx,sy,sD[i],nomsD[i],1);

        // 316, 309
        drawRoom(canvas,mL,mT,sx,sy,new float[]{0.5f,12.2f,5.5f,14.5f},"316",1);
        drawRoom(canvas,mL,mT,sx,sy,new float[]{11.5f,12.2f,16.5f,14.5f},"309",1);

        // Escalier
        float eX0=mL+6.5f*sx,eY0=mT+12f*sy,eX1=mL+10.5f*sx,eY1=mT+14f*sy;
        canvas.drawRect(eX0,eY0,eX1,eY1,pEscalier);
        canvas.drawRect(eX0,eY0,eX1,eY1,pRoom);
        pTextSmall.setTextSize(sy*0.85f);
        canvas.drawText("Escalier",(eX0+eX1)/2f,(eY0+eY1)/2f+sy*0.3f,pTextSmall);

        drawLegend(canvas,w,h);
        pText.setColor(Color.WHITE);
        pText.setTextSize(h*0.03f);
        canvas.drawText("Bloc 3 — 1er étage",w/2f,h*0.97f,pText);
    }

    // =========================================================
    // BLOC 1 — Palestine
    // =========================================================
    // =========================================================
// BLOC PALESTINE — RDC (Basé sur le schéma Draw.io)
// =========================================================
    private void drawBlocPalestine(Canvas canvas) {
        int w = getWidth(), h = getHeight();

        // 1. Marges et zone de dessin
        float mL = w * 0.10f, mT = h * 0.10f;
        float dW = w - 2 * mL, dH = h - mT - h * 0.15f;

        // 2. Échelles (25.53m x 26.32m)
        float sx = dW / 25.53f;
        float sy = dH / 26.32f;

        // 3. Structure extérieure
        canvas.drawRect(mL, mT, mL + dW, mT + dH, pWallFill);
        canvas.drawRect(mL, mT, mL + dW, mT + dH, pWall);

        // 4. Cour Centrale
        float cX0 = mL + 5.0f * sx, cY0 = mT + 5.0f * sy;
        float cX1 = mL + 20.5f * sx, cY1 = mT + 21.0f * sy;
        canvas.drawRect(cX0, cY0, cX1, cY1, pCour);
        canvas.drawRect(cX0, cY0, cX1, cY1, pWall);
        pTextSmall.setTextSize(sy * 1.5f);
        pTextSmall.setTypeface(Typeface.DEFAULT);
        canvas.drawText("Cour Centrale", (cX0 + cX1) / 2f, (cY0 + cY1) / 2f, pTextSmall);

        // 5. Salles - 101 et 107 remontées pour créer un gap
        // Côté DROIT
        drawRoom(canvas, mL, mT, sx, sy, new float[]{22.0f, 18.5f, 25.53f, 22.5f}, "101", 0);
        drawRoom(canvas, mL, mT, sx, sy, new float[]{22.0f, 10.0f, 25.53f, 14.0f}, "102", 0);
        drawRoom(canvas, mL, mT, sx, sy, new float[]{22.0f, 5.0f, 25.53f, 9.0f}, "103", 0);

        // Côté GAUCHE
        drawRoom(canvas, mL, mT, sx, sy, new float[]{0.0f, 5.0f, 3.5f, 9.0f}, "105", 0);
        drawRoom(canvas, mL, mT, sx, sy, new float[]{0.0f, 10.0f, 3.5f, 14.0f}, "106", 0);
        drawRoom(canvas, mL, mT, sx, sy, new float[]{0.0f, 18.5f, 3.5f, 22.5f}, "107", 0);

        // Côté HAUT
        drawRoom(canvas, mL, mT, sx, sy, new float[]{14.0f, 0.0f, 17.5f, 3.5f}, "104", 0);

        // 6. Amphithéâtres (cliquables)
        drawAmphiRoom(canvas, mL, mT, sx, sy, 19.5f,  0.0f, 25.53f,  5.0f, "Amphi B");
        drawAmphiRoom(canvas, mL, mT, sx, sy,  0.0f,  0.0f,  5.0f,   5.0f, "Amphi C");
        drawAmphiRoom(canvas, mL, mT, sx, sy,  0.0f, 22.5f,  5.0f,  26.32f, "Amphi D");
        drawAmphiRoom(canvas, mL, mT, sx, sy, 19.5f, 22.5f, 25.53f, 26.32f, "Amphi A");

        // 7. Circulation et Issues
        // Entrée Principale (Milieu Bas)
        drawFeature(canvas, mL + 12.76f * sx, mT + dH, sx * 4, sy * 1.2f, "ENTRÉE", pEntree);
        // Sortie (Haut)
        drawFeature(canvas, mL + 12.0f * sx, mT, sx * 3, sy * 1.2f, "SORTIE", pSortie);

        // Escalier DROIT (Existant)
        float exR0 = mL + 16.5f * sx, eyR0 = mT + 24.5f * sy;
        canvas.drawRect(exR0, eyR0, exR0 + 3f * sx, eyR0 + 1.8f * sy, pEscalier);
        canvas.drawRect(exR0, eyR0, exR0 + 3f * sx, eyR0 + 1.8f * sy, pRoom);

        // Escalier GAUCHE (Nouveau - à gauche de l'entrée)
        float exL0 = mL + 6.5f * sx, eyL0 = mT + 24.5f * sy;
        canvas.drawRect(exL0, eyL0, exL0 + 3f * sx, eyL0 + 1.8f * sy, pEscalier);
        canvas.drawRect(exL0, eyL0, exL0 + 3f * sx, eyL0 + 1.8f * sy, pRoom);

        pTextSmall.setTypeface(Typeface.DEFAULT);
        canvas.drawText("Esc.", exR0 + 1.5f * sx, eyR0 + 1.2f * sy, pTextSmall);
        canvas.drawText("Esc.", exL0 + 1.5f * sx, eyL0 + 1.2f * sy, pTextSmall);

        // Entrées latérales
        drawFeature(canvas, mL, mT + 3.0f * sy, sx * 0.5f, sy * 2, "E2", pEntree);
        drawFeature(canvas, mL, mT + 23.0f * sy, sx * 0.5f, sy * 2, "E3", pEntree);

        // 8. Titre
        drawLegend(canvas, w, h);
        pText.setColor(Color.WHITE);
        pText.setTextSize(h * 0.035f);
        canvas.drawText("Bloc Palestine — RDC", w / 2f, h * 0.97f, pText);
    }

    // Fonction utilitaire pour dessiner les entrées/sorties rapidement
    private void drawFeature(Canvas canvas, float cx, float cy, float w, float h, String label, Paint paint) {
        RectF r = new RectF(cx - w/2, cy - h/2, cx + w/2, cy + h/2);
        canvas.drawRoundRect(r, 4f, 4f, paint);
        pText.setTextSize(h * 0.8f);
        canvas.drawText(label, cx, cy + h/4, pText);
    }

    // =========================================================
    // AMPHIS 1→6  (21.65 m × 41.65 m)
    // Grille 2 colonnes × 3 rangées :
    //   Col gauche  : Amphi 6 (haut)  / Amphi 5 (milieu) / Amphi 4 (bas)
    //   Col droite  : Amphi 1 (haut)  / Amphi 2 (milieu) / Amphi 3 (bas)
    // Points blancs = entrées (mur latéral extérieur)
    // =========================================================
    private void drawAmphis16(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float mL = w * 0.06f, mT = h * 0.05f;
        float dW = w - 2 * mL, dH = h - mT - h * 0.12f;

        // Échelle réelle
        final float RW = 21.65f, RH = 41.65f;
        float sx = dW / RW, sy = dH / RH;

        // Structure extérieure
        canvas.drawRect(mL, mT, mL + dW, mT + dH, pWallFill);
        canvas.drawRect(mL, mT, mL + dW, mT + dH, pWall);

        // Limites de la grille (en mètres)
        float midX = RW / 2f;          // 10.825 m
        float row1 = RH / 3f;          // 13.883 m
        float row2 = 2f * RH / 3f;     // 27.767 m

        // 6 amphis cliquables
        drawAmphiRoom(canvas, mL, mT, sx, sy,    0f,   0f,  midX,  row1,  "Amphi 6");
        drawAmphiRoom(canvas, mL, mT, sx, sy,  midX,   0f,   RW,   row1,  "Amphi 1");
        drawAmphiRoom(canvas, mL, mT, sx, sy,    0f,  row1,  midX,  row2,  "Amphi 5");
        drawAmphiRoom(canvas, mL, mT, sx, sy,  midX,  row1,  RW,   row2,  "Amphi 2");
        drawAmphiRoom(canvas, mL, mT, sx, sy,    0f,  row2,  midX,  RH,   "Amphi 4");
        drawAmphiRoom(canvas, mL, mT, sx, sy,  midX,  row2,  RW,   RH,   "Amphi 3");

        // Points d'entrée — cliquables comme entrées générales
        float dotR = sx * 0.55f;
        float midR1 = (row1 / 2f) * sy;
        float midR2 = (row1 + row1 / 2f) * sy;
        float midR3 = (row2 + row1 / 2f) * sy;

        // Gauche : Amphi 6, 5, 4
        drawEntreePoint(canvas, mL,       mT + midR1, dotR, "Entrée Amphi 6");
        drawEntreePoint(canvas, mL,       mT + midR2, dotR, "Entrée Amphi 5");
        drawEntreePoint(canvas, mL,       mT + midR3, dotR, "Entrée Amphi 4");

        // Droite : Amphi 1, 2, 3
        drawEntreePoint(canvas, mL + dW, mT + midR1, dotR, "Entrée Amphi 1");
        drawEntreePoint(canvas, mL + dW, mT + midR2, dotR, "Entrée Amphi 2");
        drawEntreePoint(canvas, mL + dW, mT + midR3, dotR, "Entrée Amphi 3");

        drawLegend(canvas, w, h);
        pText.setColor(Color.WHITE);
        pText.setTextSize(h * 0.03f);
        canvas.drawText("Amphis 1→6", w / 2f, h * 0.97f, pText);
    }

    // =========================================================
    // HELPER — Point d'entrée cliquable (cercle sur mur)
    // =========================================================
    private void drawEntreePoint(Canvas canvas, float cx, float cy, float dotR, String nom) {
        boolean sel = nom.equals(selectedSalle);

        // Fond du point
        Paint pFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        pFill.setStyle(Paint.Style.FILL);
        pFill.setColor(sel ? Color.parseColor("#00D4FF") : Color.WHITE);
        canvas.drawCircle(cx, cy, dotR, pFill);

        // Anneau cyan quand sélectionné
        if (sel) {
            Paint pRing = new Paint(Paint.ANTI_ALIAS_FLAG);
            pRing.setStyle(Paint.Style.STROKE);
            pRing.setColor(Color.parseColor("#00D4FF"));
            pRing.setStrokeWidth(3f);
            canvas.drawCircle(cx, cy, dotR * 2f, pRing);
        }

        // Zone de clic (RectF centré sur le point, rayon 2.5× dotR)
        float hr = dotR * 2.5f;
        salleRects.add(new SalleRect(nom, new RectF(cx - hr, cy - hr, cx + hr, cy + hr), 0));
    }

    // =========================================================
    // BLOC MATH — RDC (17.21m × 48.27m, couloir central x=7→10.5)
    // Sortie en haut, Entrée en bas
    // =========================================================
    private void drawBlocMath(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float mL = w * 0.08f, mT = h * 0.05f;
        float dW = w - 2 * mL, dH = h - mT - h * 0.12f;
        float sx = dW / 17.21f, sy = dH / 48.27f;

        // Structure extérieure
        canvas.drawRect(mL, mT, mL + dW, mT + dH, pWallFill);
        canvas.drawRect(mL, mT, mL + dW, mT + dH, pWall);

        // Couloir central
        canvas.drawRect(mL + 7.0f * sx, mT, mL + 10.5f * sx, mT + dH, pCour);

        // Rangée 1 — 102M (gauche), 101M (droite)  y≈[38.7, 44.7]
        drawRoom(canvas, mL, mT, sx, sy, new float[]{ 0.0f, 38.7f,  7.0f, 44.7f}, "102M", 0);
        drawRoom(canvas, mL, mT, sx, sy, new float[]{10.5f, 38.7f, 17.21f, 44.7f}, "101M", 0);

        // Rangée 2 — Bureaux  y≈[30.8, 36.8]
        drawAmphiRoom(canvas, mL, mT, sx, sy,  0.0f, 30.8f,  7.0f, 36.8f, "Bureau");
        drawAmphiRoom(canvas, mL, mT, sx, sy, 10.5f, 30.8f, 17.21f, 36.8f, "Bureau");

        // Rangée 3 — Bureaux  y≈[22.7, 28.7]
        drawAmphiRoom(canvas, mL, mT, sx, sy,  0.0f, 22.7f,  7.0f, 28.7f, "Bureau");
        drawAmphiRoom(canvas, mL, mT, sx, sy, 10.5f, 22.7f, 17.21f, 28.7f, "Bureau");

        // Rangée 4 — Bureaux  y≈[14.6, 20.6]
        drawAmphiRoom(canvas, mL, mT, sx, sy,  0.0f, 14.6f,  7.0f, 20.6f, "Bureau");
        drawAmphiRoom(canvas, mL, mT, sx, sy, 10.5f, 14.6f, 17.21f, 20.6f, "Bureau");

        // Rangée 5 — Bureau (gauche), 117M (droite)  y≈[4.3, 10.3]
        drawAmphiRoom(canvas, mL, mT, sx, sy,  0.0f,  4.3f,  7.0f, 10.3f, "Bureau");
        drawRoom(canvas, mL, mT, sx, sy, new float[]{10.5f,  4.3f, 17.21f, 10.3f}, "117M", 0);

        // Sortie (haut)
        float cxS = mL + 7.0f * sx;
        canvas.drawRoundRect(new RectF(cxS, mT, cxS + 3.5f * sx, mT + 2.0f * sy), 6f, 6f, pSortie);
        pText.setTextSize(sy * 1.1f);
        canvas.drawText("SORTIE", cxS + 1.75f * sx, mT + 1.35f * sy, pText);

        // Entrée (bas)
        canvas.drawRoundRect(new RectF(cxS, mT + 46.0f * sy, cxS + 3.5f * sx, mT + dH), 6f, 6f, pEntree);
        canvas.drawText("ENTRÉE", cxS + 1.75f * sx, mT + 47.3f * sy, pText);

        drawLegend(canvas, w, h);
        pText.setColor(Color.WHITE);
        pText.setTextSize(h * 0.03f);
        canvas.drawText("Bloc Math — RDC", w / 2f, h * 0.97f, pText);
    }

    // =========================================================
    // GÉNÉRIQUE
    // =========================================================
    private void drawGeneric(Canvas canvas) {
        pText.setColor(Color.parseColor("#4A90D9"));
        pText.setTextSize(getHeight()*0.04f);
        canvas.drawText("Plan à venir",getWidth()/2f,getHeight()/2f,pText);
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private void drawRoom(Canvas c, float mL, float mT, float sx, float sy,
                          float[] coords, String nom, int etageRoom) {
        float x0=mL+coords[0]*sx, y0=mT+coords[1]*sy;
        float x1=mL+coords[2]*sx, y1=mT+coords[3]*sy;
        RectF r = new RectF(x0,y0,x1,y1);

        // Surligner si sélectionné
        String displayNom = "Salle " + nom;
        Paint fill = displayNom.equals(selectedSalle) ? pRoomSelected : pRoomFill;
        c.drawRect(x0,y0,x1,y1,fill);
        c.drawRect(x0,y0,x1,y1,pRoom);

        // Bordure cyan si sélectionné
        if (displayNom.equals(selectedSalle)) {
            Paint border = new Paint(pRoom);
            border.setColor(Color.parseColor("#00D4FF"));
            border.setStrokeWidth(3f);
            c.drawRect(x0,y0,x1,y1,border);
        }

        pText.setTextSize((y1-y0)*0.38f);
        c.drawText(nom,(x0+x1)/2f,(y0+y1)/2f+pText.getTextSize()/3f,pText);
        salleRects.add(new SalleRect(displayNom, r, etageRoom));
    }

    private void drawAmphiRoom(Canvas c, float mL, float mT, float sx, float sy,
                              float wx0, float wy0, float wx1, float wy1, String label) {
        float x0 = mL + wx0 * sx, y0 = mT + wy0 * sy;
        float x1 = mL + wx1 * sx, y1 = mT + wy1 * sy;
        RectF r = new RectF(x0, y0, x1, y1);

        Paint fill = label.equals(selectedSalle) ? pRoomSelected : pRoomFill;
        c.drawRect(x0, y0, x1, y1, fill);
        c.drawRect(x0, y0, x1, y1, pRoom);

        if (label.equals(selectedSalle)) {
            Paint border = new Paint(pRoom);
            border.setColor(Color.parseColor("#00D4FF"));
            border.setStrokeWidth(3f);
            c.drawRect(x0, y0, x1, y1, border);
        }

        pTextSmall.setTextSize((y1 - y0) * 0.25f);
        c.drawText(label, (x0 + x1) / 2f, (y0 + y1) / 2f + pTextSmall.getTextSize() / 3f, pTextSmall);
        salleRects.add(new SalleRect(label, r, 0));
    }

    private void drawLegend(Canvas canvas, int w, int h) {
        float y=h*0.915f, x=w*0.05f, s=h*0.022f, gap=w*0.28f;
        canvas.drawRect(x,y,x+s*1.6f,y+s,pRoomFill);
        canvas.drawRect(x,y,x+s*1.6f,y+s,pRoom);
        pTextSmall.setTextSize(s*0.9f);
        pTextSmall.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Salle",x+s*1.8f,y+s*0.8f,pTextSmall);
        canvas.drawRect(x+gap,y,x+gap+s*1.6f,y+s,pEntree);
        canvas.drawText("Entrée",x+gap+s*1.8f,y+s*0.8f,pTextSmall);
        canvas.drawRect(x+gap*2,y,x+gap*2+s*1.6f,y+s,pEscalier);
        canvas.drawText("Escalier",x+gap*2+s*1.8f,y+s*0.8f,pTextSmall);
        pTextSmall.setTextAlign(Paint.Align.CENTER);
    }
}
