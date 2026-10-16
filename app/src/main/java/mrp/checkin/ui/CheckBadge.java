package mrp.checkin.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/**
 * Check desenhado em vetor (anti-aliased) para o bloco de sucesso.
 * Substitui o glifo de texto "✓" 56sp bold, que renderizava serrilhado
 * ("peludo") dependendo da fonte/DPI do aparelho.
 */
public final class CheckBadge extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color;

    public CheckBadge(Context context, int color) {
        super(context);
        this.color = color;
    }

    public void setCheckColor(int color) {
        this.color = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.min(w, h) * 0.14f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(color);
        Path p = new Path();
        p.moveTo(w * 0.26f, h * 0.55f);
        p.lineTo(w * 0.45f, h * 0.72f);
        p.lineTo(w * 0.76f, h * 0.30f);
        canvas.drawPath(p, paint);
    }
}
