package com.fsm.navigator.navigation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/**
 * NavigationView.java – Vue Canvas pour l'itinéraire
 *
 * Affiche :
 *   - Le plan du bloc (murs, salles, couloirs)
 *   - Le chemin A* en bleu
 *   - La position actuelle (point rouge)
 *   - La destination (point vert)
 *   - Les flèches directionnelles
 */
public class NavigationView extends View {

    private Paint pWall, pWallFill, pRoom, pRoomFill, pCorridor;
    private Paint pPath, pPathArrow;
    private Paint pCurrent, pDestination;
    private Paint pText, pTextSmall, pBg;
    private Paint pNode;

    // Données de navigation
    private NavigationGraph.NavPath currentPath = null;
    private NavigationNode          currentPos  = null;
    private NavigationNode          destination = null;
    private NavigationGraph         graph       = null;

    // Dimensions réelles Bloc 3 (mètres)
    private static final float REAL_W = 17f;
    private static final float REAL_H = 15f;

    // Marges
    private float mL, mR, mT, mB;
    private float sx, sy;

    public NavigationView(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }
    public NavigationView(Context ctx)                     { super(ctx); init(); }

    private void init() {
        pBg = new Paint(); pBg.setColor(Color.parseColor("#0D1B2A"));

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

        pCorridor = new Paint(Paint.ANTI_ALIAS_FLAG);
        pCorridor.setColor(Color.parseColor("#061018"));
        pCorridor.setStyle(Paint.Style.FILL);

        // Chemin A* — ligne bleue
        pPath = new Paint(Paint.ANTI_ALIAS_FLAG);
        pPath.setColor(Color.parseColor("#2196F3"));
        pPath.setStyle(Paint.Style.STROKE);
        pPath.setStrokeWidth(6f);
        pPath.setStrokeCap(Paint.Cap.ROUND);
        pPath.setStrokeJoin(Paint.Join.ROUND);
        pPath.setAlpha(200);

        // Flèches du chemin
        pPathArrow = new Paint(Paint.ANTI_ALIAS_FLAG);
        pPathArrow.setColor(Color.parseColor("#2196F3"));
        pPathArrow.setStyle(Paint.Style.FILL);

        // Position actuelle — rouge
        pCurrent = new Paint(Paint.ANTI_ALIAS_FLAG);
        pCurrent.setColor(Color.parseColor("#E53935"));
        pCurrent.setStyle(Paint.Style.FILL);

        // Destination — vert
        pDestination = new Paint(Paint.ANTI_ALIAS_FLAG);
        pDestination.setColor(Color.parseColor("#00C853"));
        pDestination.setStyle(Paint.Style.FILL);

        pNode = new Paint(Paint.ANTI_ALIAS_FLAG);
        pNode.setColor(Color.parseColor("#4A90D9"));
        pNode.setStyle(Paint.Style.FILL);
        pNode.setAlpha(120);

        pText = new Paint(Paint.ANTI_ALIAS_FLAG);
        pText.setColor(Color.WHITE);
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        pTextSmall = new Paint(Paint.ANTI_ALIAS_FLAG);
        pTextSmall.setColor(Color.parseColor("#A0C4E8"));
        pTextSmall.setTextAlign(Paint.Align.CENTER);
    }

    // ===== SETTER =====
    public void setNavigationData(NavigationGraph graph,
                                  NavigationGraph.NavPath path,
                                  NavigationNode current,
                                  NavigationNode destination) {
        this.graph       = graph;
        this.currentPath = path;
        this.currentPos  = current;
        this.destination = destination;
        invalidate();
    }

    // ===== DESSIN =====
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();

        canvas.drawRect(0, 0, w, h, pBg);

        mL = w * 0.07f; mR = w * 0.07f;
        mT = h * 0.06f; mB = h * 0.10f;
        float dW = w - mL - mR;
        float dH = h - mT - mB;
        sx = dW / REAL_W;
        sy = dH / REAL_H;

        drawBloc3Plan(canvas);

        if (currentPath != null) drawPath(canvas);
        if (currentPos  != null) drawCurrentPosition(canvas);
        if (destination  != null) drawDestination(canvas);

        drawLegend(canvas, w, h);
    }

    // ===== PLAN BLOC 3 =====
    private void drawBloc3Plan(Canvas canvas) {
        // Périmètre
        canvas.drawRect(mL, mT, mL + REAL_W*sx, mT + REAL_H*sy, pWallFill);
        canvas.drawRect(mL, mT, mL + REAL_W*sx, mT + REAL_H*sy, pWall);

        // Cour intérieure
        canvas.drawRect(mL+3.5f*sx, mT+1.5f*sy,
                mL+13.5f*sx, mT+12.5f*sy, pCorridor);

        // Salles gauche
        drawRoom(canvas, 0f, 2.5f,  3.5f, 5f,   "305");
        drawRoom(canvas, 0f, 5.25f, 3.5f, 7.75f, "306");
        drawRoom(canvas, 0f, 10.85f,3.5f, 13.35f,"307");

        // Salles droite
        drawRoom(canvas, 13.5f, 2.5f,  17f, 5f,    "304");
        drawRoom(canvas, 13.5f, 5.25f, 17f, 7.75f,  "303");
        drawRoom(canvas, 13.5f, 10.85f,17f, 13.35f, "302");

        // Bas
        drawRoom(canvas, 0.5f, 13f,  7f,   15f, "308");
        drawRoom(canvas, 10f,  13f,  16.5f, 15f, "301");

        // Escalier
        float ex0 = mL+5.5f*sx, ey0 = mT+11.5f*sy;
        float ex1 = mL+11.5f*sx,ey1 = mT+13.5f*sy;
        canvas.drawRect(ex0, ey0, ex1, ey1, pRoomFill);
        canvas.drawRect(ex0, ey0, ex1, ey1, pRoom);
        pTextSmall.setTextSize(sy * 0.8f);
        canvas.drawText("Escalier", (ex0+ex1)/2f, (ey0+ey1)/2f+sy*0.3f, pTextSmall);

        // Entrée / Sortie
        float enX = mL+8.5f*sx;
        canvas.drawRoundRect(new RectF(enX-2f*sx, mT+REAL_H*sy-0.7f*sy,
                        enX+2f*sx, mT+REAL_H*sy), 6f, 6f,
                makeFill("#00694A"));
        pText.setTextSize(sy*0.8f);
        canvas.drawText("ENTRÉE", enX, mT+REAL_H*sy-0.2f*sy, pText);

        canvas.drawRoundRect(new RectF(enX-2f*sx, mT,
                        enX+2f*sx, mT+0.7f*sy), 6f, 6f,
                makeFill("#8B3A00"));
        canvas.drawText("SORTIE", enX, mT+0.5f*sy, pText);
    }

    private void drawRoom(Canvas canvas, float x0, float y0,
                          float x1, float y1, String nom) {
        float rx0=mL+x0*sx, ry0=mT+y0*sy, rx1=mL+x1*sx, ry1=mT+y1*sy;
        canvas.drawRect(rx0, ry0, rx1, ry1, pRoomFill);
        canvas.drawRect(rx0, ry0, rx1, ry1, pRoom);
        pText.setTextSize((ry1-ry0)*0.35f);
        canvas.drawText(nom, (rx0+rx1)/2f, (ry0+ry1)/2f+pText.getTextSize()/3f, pText);
    }

    // ===== CHEMIN A* =====
    private void drawPath(Canvas canvas) {
        List<NavigationNode> nodes = currentPath.nodes;
        if (nodes == null || nodes.size() < 2) return;

        Path path = new Path();
        NavigationNode first = nodes.get(0);
        path.moveTo(mL + first.x * sx, mT + first.y * sy);

        for (int i = 1; i < nodes.size(); i++) {
            NavigationNode n = nodes.get(i);
            path.lineTo(mL + n.x * sx, mT + n.y * sy);
        }
        canvas.drawPath(path, pPath);

        // Flèches directionnelles sur le chemin
        for (int i = 0; i < nodes.size() - 1; i++) {
            NavigationNode a = nodes.get(i);
            NavigationNode b = nodes.get(i + 1);
            float mx = mL + (a.x + b.x) / 2f * sx;
            float my = mT + (a.y + b.y) / 2f * sy;
            float dx = b.x - a.x;
            float dy = b.y - a.y;
            float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
            drawArrow(canvas, mx, my, angle);
        }
    }

    private void drawArrow(Canvas canvas, float x, float y, float angleDeg) {
        float r = 14f;
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(angleDeg);
        Path arrow = new Path();
        arrow.moveTo(r,  0);
        arrow.lineTo(-r/2f, -r/2f);
        arrow.lineTo(-r/2f,  r/2f);
        arrow.close();
        canvas.drawPath(arrow, pPathArrow);
        canvas.restore();
    }

    // ===== POSITION ACTUELLE =====
    private void drawCurrentPosition(Canvas canvas) {
        float x = mL + currentPos.x * sx;
        float y = mT + currentPos.y * sy;

        // Anneau extérieur
        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setColor(Color.parseColor("#88E53935"));
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(3f);
        canvas.drawCircle(x, y, 22f, ring);

        canvas.drawCircle(x, y, 15f, pCurrent);

        // Label
        Paint lp = new Paint(pText);
        lp.setTextSize(14f);
        canvas.drawText("📍 Vous", x, y - 25f, lp);
    }

    // ===== DESTINATION =====
    private void drawDestination(Canvas canvas) {
        float x = mL + destination.x * sx;
        float y = mT + destination.y * sy;

        canvas.drawCircle(x, y, 15f, pDestination);

        Paint lp = new Paint(pText);
        lp.setTextSize(14f);
        canvas.drawText("🎯 " + destination.nom, x, y - 25f, lp);
    }

    // ===== LÉGENDE =====
    private void drawLegend(Canvas canvas, int w, int h) {
        float y = h * 0.92f, x = w * 0.05f, s = h * 0.02f, gap = w * 0.32f;

        canvas.drawCircle(x+s/2f, y+s/2f, s/2f, pCurrent);
        pTextSmall.setTextSize(s*0.85f);
        pTextSmall.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Votre position", x+s+4f, y+s*0.8f, pTextSmall);

        canvas.drawCircle(x+gap+s/2f, y+s/2f, s/2f, pDestination);
        canvas.drawText("Destination", x+gap+s+4f, y+s*0.8f, pTextSmall);

        pPath.setStrokeWidth(3f);
        canvas.drawLine(x+gap*2f, y+s/2f, x+gap*2f+s*2f, y+s/2f, pPath);
        pPath.setStrokeWidth(6f);
        canvas.drawText("Itinéraire", x+gap*2f+s*2f+4f, y+s*0.8f, pTextSmall);

        pTextSmall.setTextAlign(Paint.Align.CENTER);
    }

    private Paint makeFill(String hex) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.parseColor(hex));
        p.setStyle(Paint.Style.FILL);
        return p;
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        float d = getResources().getDisplayMetrics().density;
        setMeasuredDimension((int)(900*d), (int)(800*d));
    }
}