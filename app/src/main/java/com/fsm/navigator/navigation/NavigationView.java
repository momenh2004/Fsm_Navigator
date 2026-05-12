package com.fsm.navigator.navigation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/**
 * NavigationView.java – Vue Canvas pour l'itinéraire Bloc 3
 *
 * Référentiel MONDE (portrait, comme à l'écran) :
 *   - x_réel : axe horizontal  → largeur 17.76 m  (de gauche à droite)
 *   - y_réel : axe vertical    → longueur 30.74 m (de haut en bas)
 *
 * Repère :
 *   (0, 0) = coin haut-gauche du plan
 *   x grandit vers la DROITE
 *   y grandit vers le BAS
 *
 * Disposition (vue de haut, portrait) :
 *   - SORTIE en haut (y ≈ 0)
 *   - ENTRÉE en bas (y ≈ 30.74)
 *   - Salles 305, 306, 307 à gauche (x ≈ 0..3.5)
 *   - Salles 304, 303, Bureau, 302 à droite (x ≈ 14..17.76)
 *   - 308 et 301 en bas, encadrant l'ENTRÉE
 *   - Cour Centrale au milieu, avec Escalier juste au-dessus de l'entrée
 */
public class NavigationView extends View {

    private Paint pWall, pWallFill, pRoom, pRoomFill, pCorridor, pCour;
    private Paint pPath, pPathArrow;
    private Paint pCurrent, pDestination;
    private Paint pText, pTextSmall, pBg;

    private NavigationGraph.NavPath currentPath = null;
    private NavigationNode          currentPos  = null;
    private NavigationNode          destination = null;

    // Dimensions réelles Bloc 3 (mètres) — orienté portrait
    private static final float REAL_W = 17.76f;  // largeur (horizontal)
    private static final float REAL_H = 30.74f;  // longueur (vertical)

    private float scale;
    private float offsetX, offsetY;

    public NavigationView(Context ctx, AttributeSet attrs) { super(ctx, attrs); init(); }
    public NavigationView(Context ctx)                     { super(ctx); init(); }

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

        pCorridor = new Paint(Paint.ANTI_ALIAS_FLAG);
        pCorridor.setColor(Color.parseColor("#061018"));
        pCorridor.setStyle(Paint.Style.FILL);

        pCour = new Paint(Paint.ANTI_ALIAS_FLAG);
        pCour.setColor(Color.parseColor("#0D1B2A"));
        pCour.setStyle(Paint.Style.STROKE);
        pCour.setStrokeWidth(2f);

        pPath = new Paint(Paint.ANTI_ALIAS_FLAG);
        pPath.setColor(Color.parseColor("#2196F3"));
        pPath.setStyle(Paint.Style.STROKE);
        pPath.setStrokeWidth(6f);
        pPath.setStrokeCap(Paint.Cap.ROUND);
        pPath.setStrokeJoin(Paint.Join.ROUND);
        pPath.setAlpha(220);

        pPathArrow = new Paint(Paint.ANTI_ALIAS_FLAG);
        pPathArrow.setColor(Color.parseColor("#2196F3"));
        pPathArrow.setStyle(Paint.Style.FILL);

        pCurrent = new Paint(Paint.ANTI_ALIAS_FLAG);
        pCurrent.setColor(Color.parseColor("#E53935"));
        pCurrent.setStyle(Paint.Style.FILL);

        pDestination = new Paint(Paint.ANTI_ALIAS_FLAG);
        pDestination.setColor(Color.parseColor("#00C853"));
        pDestination.setStyle(Paint.Style.FILL);

        pText = new Paint(Paint.ANTI_ALIAS_FLAG);
        pText.setColor(Color.WHITE);
        pText.setTextAlign(Paint.Align.CENTER);
        pText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        pTextSmall = new Paint(Paint.ANTI_ALIAS_FLAG);
        pTextSmall.setColor(Color.parseColor("#A0C4E8"));
        pTextSmall.setTextAlign(Paint.Align.CENTER);
    }

    public void setNavigationData(NavigationGraph graph,
                                  NavigationGraph.NavPath path,
                                  NavigationNode current,
                                  NavigationNode destination) {
        this.currentPath = path;
        this.currentPos  = current;
        this.destination = destination;
        invalidate();
    }

    // =========================================================
    // CONVERSION MONDE → ÉCRAN  (pas de rotation)
    //   screen_x = offsetX + world_x * scale
    //   screen_y = offsetY + world_y * scale
    // =========================================================
    private float wx(float worldX) { return offsetX + worldX * scale; }
    private float wy(float worldY) { return offsetY + worldY * scale; }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        android.util.Log.d("NAV", "onDraw w=" + w + " h=" + h + " scale=" + scale);

        canvas.drawRect(0, 0, w, h, pBg);

        float marginTop    = h * 0.06f;
        float marginBottom = h * 0.12f;
        float marginH      = w * 0.05f;

        float availW = w - 2f * marginH;
        float availH = h - marginTop - marginBottom;

        float scaleW = availW / REAL_W;
        float scaleH = availH / REAL_H;
        scale = Math.min(scaleW, scaleH);

        float drawnW = REAL_W * scale;
        float drawnH = REAL_H * scale;
        offsetX = (w - drawnW) / 2f;
        offsetY = marginTop + (availH - drawnH) / 2f;

        drawBloc3Plan(canvas);

        if (currentPath != null) drawPath(canvas);
        if (currentPos  != null) drawCurrentPosition(canvas);
        if (destination != null) drawDestination(canvas);

        drawLegend(canvas, w, h);
    }

    // =========================================================
    // PLAN BLOC 3 — Tout en portrait, sans rotation
    // Repère monde : (0,0) en haut-gauche, x → droite, y → bas
    // Largeur 17.76 m, hauteur 30.74 m
    // =========================================================
    private void drawBloc3Plan(Canvas canvas) {

        // ---- Périmètre extérieur du bloc ----
        canvas.drawRect(wx(0), wy(0), wx(REAL_W), wy(REAL_H), pWallFill);
        canvas.drawRect(wx(0), wy(0), wx(REAL_W), wy(REAL_H), pWall);

        // ---- Cour centrale (rectangle vide au milieu) ----
        // monde x ∈ [4.5, 13.5]  (largeur ≈ 9 m)
        // monde y ∈ [3, 26]      (longueur ≈ 23 m)
        float cx0 = 4.5f, cy0 = 3f, cx1 = 13.5f, cy1 = 26f;
        canvas.drawRect(wx(cx0), wy(cy0), wx(cx1), wy(cy1), pCour);
        // Label "Cour Centrale" au milieu
        pText.setTextSize(scale * 0.9f);
        canvas.drawText("Cour Centrale",
                wx((cx0 + cx1) / 2f),
                wy((cy0 + cy1) / 2f) + pText.getTextSize() / 3f,
                pText);

        // ---- Salles côté GAUCHE (x ≈ 0..3.5) ----
        // 305 en haut, 306 en dessous, 307 en bas
        drawWorldRoom(canvas, 0f,  3.5f,  3.5f,  6.5f,  "305");
        drawWorldRoom(canvas, 0f,  6.8f,  3.5f,  9.8f,  "306");
        drawWorldRoom(canvas, 0f, 22f,    3.5f, 25f,    "307");

        // ---- Salles côté DROIT (x ≈ 14.26..17.76) ----
        drawWorldRoom(canvas, 14.26f,  3.5f,  17.76f,  6.5f,  "304");
        drawWorldRoom(canvas, 14.26f,  6.8f,  17.76f,  9.8f,  "303");
        drawWorldRoom(canvas, 14.26f, 13.5f,  17.76f, 16.5f,  "Bureau");
        drawWorldRoom(canvas, 14.26f, 22f,    17.76f, 25f,    "302");

        // ---- Salles BAS (autour de l'entrée) ----
        // 308 à gauche, 301 à droite
        drawWorldRoom(canvas, 1.5f,  27.5f,  6f,    30f,  "308");
        drawWorldRoom(canvas, 11.76f, 27.5f, 16.26f, 30f, "301");

        // ---- Escalier (au-dessus de l'entrée, dans le couloir bas) ----
        drawWorldRect(canvas, 6.5f, 25.5f, 11.26f, 26.8f, pRoomFill, "Escalier");

        // ---- ENTRÉE (en bas, au centre) ----
        drawWorldRect(canvas, 7f, 30f, 10.76f, 30.74f, makeFill("#00694A"), "ENTRÉE");

        // ---- SORTIE (en haut, au centre) ----
        drawWorldRect(canvas, 7f, 0f, 10.76f, 0.74f, makeFill("#8B3A00"), "SORTIE");
    }

    private void drawWorldRect(Canvas canvas, float wx0, float wy0,
                               float wx1, float wy1, Paint fill, String label) {
        float left   = wx(wx0);
        float top    = wy(wy0);
        float right  = wx(wx1);
        float bottom = wy(wy1);
        canvas.drawRect(left, top, right, bottom, fill);
        if (label != null) {
            pText.setTextSize(Math.min(scale * 0.65f, (bottom - top) * 0.5f));
            canvas.drawText(label, (left + right) / 2f,
                    (top + bottom) / 2f + pText.getTextSize() / 3f, pText);
        }
    }

    private void drawWorldRoom(Canvas canvas, float wx0, float wy0,
                               float wx1, float wy1, String nom) {
        float left   = wx(wx0);
        float top    = wy(wy0);
        float right  = wx(wx1);
        float bottom = wy(wy1);

        canvas.drawRect(left, top, right, bottom, pRoomFill);
        canvas.drawRect(left, top, right, bottom, pRoom);
        pText.setTextSize(Math.min(scale * 0.8f, (bottom - top) * 0.4f));
        canvas.drawText(nom, (left + right) / 2f,
                (top + bottom) / 2f + pText.getTextSize() / 3f, pText);
    }

    // =========================================================
    // CHEMIN A*
    // =========================================================
    private void drawPath(Canvas canvas) {
        List<NavigationNode> nodes = currentPath.nodes;
        if (nodes == null || nodes.size() < 2) return;

        Path path = new Path();
        NavigationNode first = nodes.get(0);
        path.moveTo(wx(first.x), wy(first.y));

        for (int i = 1; i < nodes.size(); i++) {
            NavigationNode n = nodes.get(i);
            path.lineTo(wx(n.x), wy(n.y));
        }
        canvas.drawPath(path, pPath);

        for (int i = 0; i < nodes.size() - 1; i++) {
            NavigationNode a = nodes.get(i);
            NavigationNode b = nodes.get(i + 1);
            float ax = wx(a.x);
            float ay = wy(a.y);
            float bx = wx(b.x);
            float by = wy(b.y);
            float mx = (ax + bx) / 2f;
            float my = (ay + by) / 2f;
            float angle = (float) Math.toDegrees(Math.atan2(by - ay, bx - ax));
            drawArrow(canvas, mx, my, angle);
        }
    }

    private void drawArrow(Canvas canvas, float x, float y, float angleDeg) {
        float r = scale * 0.4f;
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(angleDeg);
        Path arrow = new Path();
        arrow.moveTo(r, 0);
        arrow.lineTo(-r/2f, -r/2f);
        arrow.lineTo(-r/2f, r/2f);
        arrow.close();
        canvas.drawPath(arrow, pPathArrow);
        canvas.restore();
    }

    private void drawCurrentPosition(Canvas canvas) {
        float x = wx(currentPos.x);
        float y = wy(currentPos.y);
        float radius = scale * 0.5f;

        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setColor(Color.parseColor("#88E53935"));
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(3f);
        canvas.drawCircle(x, y, radius * 1.5f, ring);

        canvas.drawCircle(x, y, radius, pCurrent);

        Paint lp = new Paint(pText);
        lp.setTextSize(scale * 0.6f);
        canvas.drawText("📍 Vous", x, y - radius - scale * 0.3f, lp);
    }

    private void drawDestination(Canvas canvas) {
        float x = wx(destination.x);
        float y = wy(destination.y);
        float radius = scale * 0.5f;

        canvas.drawCircle(x, y, radius, pDestination);

        Paint lp = new Paint(pText);
        lp.setTextSize(scale * 0.6f);
        canvas.drawText("🎯 " + destination.nom, x, y - radius - scale * 0.3f, lp);
    }

    private void drawLegend(Canvas canvas, int w, int h) {
        float y   = h * 0.93f;
        float x   = w * 0.05f;
        float s   = h * 0.02f;
        float gap = w * 0.30f;

        canvas.drawCircle(x + s/2f, y + s/2f, s/2f, pCurrent);
        pTextSmall.setTextSize(s * 0.85f);
        pTextSmall.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Votre position", x + s + 4f, y + s * 0.8f, pTextSmall);

        canvas.drawCircle(x + gap + s/2f, y + s/2f, s/2f, pDestination);
        canvas.drawText("Destination", x + gap + s + 4f, y + s * 0.8f, pTextSmall);

        pPath.setStrokeWidth(3f);
        canvas.drawLine(x + gap*2f, y + s/2f,
                x + gap*2f + s*2f, y + s/2f, pPath);
        pPath.setStrokeWidth(6f);
        canvas.drawText("Itinéraire", x + gap*2f + s*2f + 4f, y + s * 0.8f, pTextSmall);

        pTextSmall.setTextAlign(Paint.Align.CENTER);
    }

    private Paint makeFill(String hex) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.parseColor(hex));
        p.setStyle(Paint.Style.FILL);
        return p;
    }
}