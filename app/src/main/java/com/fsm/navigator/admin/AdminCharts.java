package com.fsm.navigator.admin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;

/**
 * Charts interactifs pour le dashboard admin.
 * Tap sur une barre / segment → callback avec l'index sélectionné.
 */
public class AdminCharts {

    public interface OnItemClick { void onClick(int index, String label, float value); }

    // =========================================================
    // BAR CHART — barres verticales interactives
    // =========================================================
    public static class BarChartView extends View {
        private final List<String>  labels;
        private final List<Float>   values;
        private final List<Integer> colors;
        private final int           defaultColor;
        private final Paint         paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private OnItemClick         listener;
        private int                 selectedIndex = -1;

        public BarChartView(Context ctx, List<String> labels, List<Float> values,
                            List<Integer> colors, int defaultColor) {
            super(ctx);
            this.labels       = labels;
            this.values       = values;
            this.colors       = colors;
            this.defaultColor = defaultColor;
        }

        public void setOnItemClickListener(OnItemClick l) { this.listener = l; }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_UP && listener != null) {
                int w = getWidth(); int padL = dp(16), padR = dp(16);
                int n = values.size();
                float gap = (float)(w - padL - padR) / n;
                int idx = (int)((e.getX() - padL) / gap);
                if (idx >= 0 && idx < n) {
                    selectedIndex = idx;
                    invalidate();
                    listener.onClick(idx, labels.get(idx), values.get(idx));
                }
            }
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (values.isEmpty()) return;
            int w = getWidth(), h = getHeight();
            int padL = dp(16), padR = dp(16), padT = dp(16), padB = dp(48);
            int chartH = h - padT - padB;
            int chartW = w - padL - padR;

            float max = 0;
            for (float v : values) if (v > max) max = v;
            if (max == 0) max = 1;

            int n = values.size();
            float barW = (float)chartW / n * 0.6f;
            float gap  = (float)chartW / n;

            // Grille horizontale
            paint.setColor(0x22FFFFFF); paint.setStrokeWidth(1f); paint.setStyle(Paint.Style.STROKE);
            for (int i = 0; i <= 4; i++) {
                float y = padT + chartH - (chartH * i / 4f);
                canvas.drawLine(padL, y, w - padR, y, paint);
            }

            for (int i = 0; i < n; i++) {
                float barH = chartH * values.get(i) / max;
                float x    = padL + gap * i + (gap - barW) / 2f;
                float top  = padT + chartH - barH;
                float bot  = padT + chartH;

                int color = (colors != null && i < colors.size()) ? colors.get(i) : defaultColor;
                boolean sel = (i == selectedIndex);

                // Highlight sélection
                if (sel) {
                    paint.setColor(color & 0x44FFFFFF | 0x44000000);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRect(x - dp(4), padT, x + barW + dp(4), h - padB, paint);
                }

                paint.setColor(sel ? 0xFFFFFFFF : color);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(new RectF(x, top, x + barW, bot), dp(4), dp(4), paint);

                // Valeur
                paint.setColor(0xFFFFFFFF); paint.setTextSize(dp(11));
                paint.setTextAlign(Paint.Align.CENTER); paint.setStyle(Paint.Style.FILL);
                canvas.drawText(String.valueOf((int)values.get(i).floatValue()),
                        x + barW / 2, top - dp(4), paint);

                // Label
                paint.setColor(sel ? 0xFF00D4FF : 0xFF90A4AE); paint.setTextSize(dp(10));
                canvas.drawText(labels.get(i), x + barW / 2, h - dp(6), paint);
            }
        }

        private int dp(int v) {
            return (int)(v * getContext().getResources().getDisplayMetrics().density);
        }
    }

    // =========================================================
    // PIE CHART — camembert interactif
    // =========================================================
    public static class PieChartView extends View {
        private final List<String>  labels;
        private final List<Float>   values;
        private final List<Integer> colors;
        private final Paint         paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private OnItemClick         listener;
        private int                 selectedIndex = -1;

        public PieChartView(Context ctx, List<String> labels, List<Float> values,
                            List<Integer> colors) {
            super(ctx);
            this.labels = labels; this.values = values; this.colors = colors;
        }

        public void setOnItemClickListener(OnItemClick l) { this.listener = l; }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_UP && listener != null) {
                int w = getWidth(), h = getHeight();
                int size   = Math.min(w, h - dp(80));
                float cx   = w / 2f, cy = size / 2f + dp(20);
                float radius = size / 2f - dp(20);
                float innerR = radius * 0.5f;

                float dx = e.getX() - cx, dy = e.getY() - cy;
                float dist = (float)Math.sqrt(dx*dx + dy*dy);

                if (dist < innerR || dist > radius) return true;

                // Angle du tap
                double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
                if (angle < 0) angle += 360;

                float total = 0; for (float v : values) total += v;
                float start = 0;
                for (int i = 0; i < values.size(); i++) {
                    float sweep = 360f * values.get(i) / total;
                    if (angle >= start && angle < start + sweep) {
                        selectedIndex = i;
                        invalidate();
                        if (listener != null)
                            listener.onClick(i, labels.get(i), values.get(i));
                        break;
                    }
                    start += sweep;
                }
            }
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (values.isEmpty()) return;
            int w = getWidth(), h = getHeight();
            int size   = Math.min(w, h - dp(80));
            float cx   = w / 2f, cy = size / 2f + dp(20);
            float radius = size / 2f - dp(20);

            float total = 0; for (float v : values) total += v;
            if (total == 0) return;

            float startAngle = -90f;
            for (int i = 0; i < values.size(); i++) {
                float sweep = 360f * values.get(i) / total;
                boolean sel = (i == selectedIndex);

                // Décaler segment sélectionné
                float offset = sel ? dp(8) : 0;
                float midAngle = (float)Math.toRadians(startAngle + sweep / 2);
                float ox = (float)(Math.cos(midAngle) * offset);
                float oy = (float)(Math.sin(midAngle) * offset);

                paint.setColor(colors.get(i));
                paint.setStyle(Paint.Style.FILL);
                canvas.drawArc(new RectF(cx - radius + ox, cy - radius + oy,
                                cx + radius + ox, cy + radius + oy),
                        startAngle, sweep, true, paint);

                // Pourcentage sur le segment
                if (sweep > 20) {
                    float midA = (float)Math.toRadians(startAngle + sweep / 2);
                    float tx = cx + (float)(Math.cos(midA) * radius * 0.7f) + ox;
                    float ty = cy + (float)(Math.sin(midA) * radius * 0.7f) + oy;
                    paint.setColor(0xFFFFFFFF);
                    paint.setTextSize(dp(11));
                    paint.setTextAlign(Paint.Align.CENTER);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawText(Math.round(values.get(i) * 100 / total) + "%", tx, ty + dp(4), paint);
                }

                startAngle += sweep;
            }

            // Trou central
            paint.setColor(0xFF1A1A2E); paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, radius * 0.5f, paint);

            // Label central si sélection
            if (selectedIndex >= 0 && selectedIndex < labels.size()) {
                paint.setColor(0xFFFFFFFF); paint.setTextSize(dp(12));
                paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(Typeface.DEFAULT_BOLD);
                canvas.drawText(labels.get(selectedIndex), cx, cy - dp(6), paint);
                paint.setColor(colors.get(selectedIndex)); paint.setTextSize(dp(16));
                canvas.drawText(String.valueOf((int)values.get(selectedIndex).floatValue()), cx, cy + dp(12), paint);
                paint.setTypeface(Typeface.DEFAULT);
            } else {
                paint.setColor(0xFF607080); paint.setTextSize(dp(11));
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("Tap pour détails", cx, cy + dp(6), paint);
            }

            // Légende
            float legendY = size + dp(8);
            float legendX = dp(12);
            paint.setTextSize(dp(10)); paint.setTextAlign(Paint.Align.LEFT);
            for (int i = 0; i < labels.size(); i++) {
                if (legendX + dp(160) > w) { legendX = dp(12); legendY += dp(22); }
                paint.setColor(colors.get(i)); paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(legendX + dp(6), legendY, dp(5), paint);
                paint.setColor(i == selectedIndex ? 0xFFFFFFFF : 0xFFB0BEC5);
                canvas.drawText(labels.get(i) + " (" + (int)values.get(i).floatValue() + ")",
                        legendX + dp(16), legendY + dp(4), paint);
                legendX += dp(160);
            }
        }

        private int dp(int v) {
            return (int)(v * getContext().getResources().getDisplayMetrics().density);
        }
    }
}