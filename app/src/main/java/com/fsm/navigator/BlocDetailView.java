package com.fsm.navigator;

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
            case "B1": drawBloc1(canvas);    break;
            default:   drawGeneric(canvas);  break;
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
    private void drawBloc1(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float mx=w*0.06f, my=h*0.07f;
        float dw=w-2*mx, dh=h-my-h*0.12f;

        canvas.drawRect(mx,my,mx+dw,my+dh,pWallFill);
        canvas.drawRect(mx,my,mx+dw,my+dh,pWall);

        float cx0=mx+dw*0.25f,cy0=my+dh*0.2f,cx1=mx+dw*0.75f,cy1=my+dh*0.82f;
        canvas.drawRect(cx0,cy0,cx1,cy1,pCour);
        pWall.setAlpha(100); canvas.drawRect(cx0,cy0,cx1,cy1,pWall); pWall.setAlpha(255);
        pTextSmall.setTextSize(dh*0.06f);
        canvas.drawText("Cour",(cx0+cx1)/2f,(cy0+cy1)/2f,pTextSmall);

        // Amphi A,B droite
        String[] aD={"Amphi A","Amphi B"};
        for (int i=0;i<2;i++) {
            float ay0=my+dh*(0.05f+i*0.2f), ay1=ay0+dh*0.15f;
            float ax0=mx+dw*0.76f, ax1=mx+dw;
            RectF r=new RectF(ax0,ay0,ax1,ay1);
            canvas.drawRect(ax0,ay0,ax1,ay1,pRoomFill);
            canvas.drawRect(ax0,ay0,ax1,ay1,pRoom);
            pText.setTextSize(dh*0.055f);
            canvas.drawText(aD[i],(ax0+ax1)/2f,(ay0+ay1)/2f+dh*0.02f,pText);
            salleRects.add(new SalleRect(aD[i],r,0));
        }

        // Amphi C,D gauche
        String[] aG={"Amphi C","Amphi D"};
        for (int i=0;i<2;i++) {
            float ay0=my+dh*(0.05f+i*0.2f), ay1=ay0+dh*0.15f;
            float ax0=mx, ax1=mx+dw*0.24f;
            RectF r=new RectF(ax0,ay0,ax1,ay1);
            canvas.drawRect(ax0,ay0,ax1,ay1,pRoomFill);
            canvas.drawRect(ax0,ay0,ax1,ay1,pRoom);
            pText.setTextSize(dh*0.055f);
            canvas.drawText(aG[i],(ax0+ax1)/2f,(ay0+ay1)/2f+dh*0.02f,pText);
            salleRects.add(new SalleRect(aG[i],r,0));
        }

        // Salles 101→107
        float sw=dw/7f;
        for (int i=0;i<7;i++) {
            float sx0=mx+i*sw, sx1=sx0+sw-2;
            float sy0=my+dh*0.83f, sy1=my+dh;
            String nom=String.valueOf(101+i);
            RectF r=new RectF(sx0,sy0,sx1,sy1);
            Paint fill=nom.equals(selectedSalle)?pRoomSelected:pRoomFill;
            canvas.drawRect(sx0,sy0,sx1,sy1,fill);
            canvas.drawRect(sx0,sy0,sx1,sy1,pRoom);
            pText.setTextSize(dh*0.048f);
            canvas.drawText(nom,(sx0+sx1)/2f,(sy0+sy1)/2f+dh*0.018f,pText);
            salleRects.add(new SalleRect("Salle "+nom,r,0));
        }

        float entX=mx+dw*0.5f;
        canvas.drawRoundRect(new RectF(entX-dw*0.12f,my+dh,entX+dw*0.12f,my+dh+h*0.04f),6f,6f,pEntree);
        pText.setTextSize(dh*0.05f);
        canvas.drawText("ENTRÉE",entX,my+dh+h*0.028f,pText);

        pText.setTextSize(h*0.032f);
        canvas.drawText("Bloc 1 — RDC",w/2f,h*0.97f,pText);
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