package com.dzaitsev.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.parseColor;
import static android.graphics.Paint.Style.STROKE;
import static com.dzaitsev.widget.Utils.createPaint;
import static com.dzaitsev.widget.Utils.createPoint;
import static com.dzaitsev.widget.Utils.createPoints;
import static com.dzaitsev.widget.Utils.gradient;
import static com.dzaitsev.widget.Utils.mutatePaint;
import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.ceil;
import static java.lang.StrictMath.cos;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;

/**
 * ~ ~ ~ ~ Description ~ ~ ~ ~
 *
 * @author Dmytro Zaitsev
 * @since 2016-Sep-28, 14:15
 */
@SuppressWarnings("ClassWithTooManyFields")
public class RadarChartView extends View {
  private final LinkedHashMap<String, Float> axis      = new LinkedHashMap<>();
  private final Rect                         rect      = new Rect();
  private final Path                         path      = new Path();
  private final TextPaint                    textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
  private final Paint                        paint     = createPaint(BLACK);

  private int     startColor;
  private int     endColor;
  private int     axisColor;
  private int     graphColor;
  private float   axisMax;
  private float   axisTick;
  private float   axisWidth;
  private float   graphWidth;
  private int     graphStyle;
  private int     centerX;
  private int     centerY;
  private Ring[]  rings;
  private boolean circlesOnly;
  private boolean autoSize;
  private boolean smoothGradient;
  private float[] vertices;

  public RadarChartView(Context context) {
    this(context, null);
  }

  public RadarChartView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public RadarChartView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    final TypedArray colors = context.obtainStyledAttributes(attrs, new int[] {
        R.attr.colorAccent, R.attr.colorPrimary, R.attr.colorPrimaryDark
    }, defStyleAttr, 0);
    final int colorAccent = colors.getColor(0, parseColor("#22737b"));
    final int colorPrimary = colors.getColor(1, parseColor("#c3e3e5"));
    final int colorPrimaryDark = colors.getColor(2, parseColor("#5f9ca1"));
    colors.recycle();

    final TypedArray values = context.obtainStyledAttributes(attrs, R.styleable.RadarChartView, defStyleAttr, 0);
    startColor = values.getColor(R.styleable.RadarChartView_startColor, colorPrimaryDark);
    endColor = values.getColor(R.styleable.RadarChartView_endColor, colorPrimary);
    axisColor = values.getColor(R.styleable.RadarChartView_axisColor, BLACK);
    graphColor = values.getColor(R.styleable.RadarChartView_graphColor, colorAccent);
    axisMax = values.getFloat(R.styleable.RadarChartView_axisMax, 20);
    axisTick = values.getFloat(R.styleable.RadarChartView_axisTick, axisMax / 5);
    final int textSize = values.getDimensionPixelSize(R.styleable.RadarChartView_textSize, 15);
    circlesOnly = values.getBoolean(R.styleable.RadarChartView_circlesOnly, false);
    autoSize = values.getBoolean(R.styleable.RadarChartView_autoSize, true);
    axisWidth = values.getFloat(R.styleable.RadarChartView_axisWidth, 1);
    graphWidth = values.getFloat(R.styleable.RadarChartView_graphWidth, 3);
    graphStyle = values.getInt(R.styleable.RadarChartView_graphStyle, STROKE.ordinal());
    smoothGradient = values.getBoolean(R.styleable.RadarChartView_smoothGradient, false);
    values.recycle();

    textPaint.setTextSize(textSize);
    textPaint.density = getResources().getDisplayMetrics().density;
  }

  public final void addOrReplace(String axisName, float value) {
    axis.put(axisName, value);
    onAxisChanged();
  }

  public final void clearAxis() {
    axis.clear();
    onAxisChanged();
  }

  public final Map<String, Float> getAxis() {
    return Collections.unmodifiableMap(axis);
  }

  public final void setAxis(Map<String, Float> axis) {
    this.axis.clear();
    this.axis.putAll(axis);
    onAxisChanged();
  }

  public final int getAxisColor() {
    return axisColor;
  }

  public final void setAxisColor(int axisColor) {
    this.axisColor = axisColor;
    invalidate();
  }

  public final float getAxisMax() {
    return axisMax;
  }

  public final void setAxisMax(float axisMax) {
    setAutoSize(false);
    setAxisMaxInternal(axisMax);
  }

  public final float getAxisTick() {
    return axisTick;
  }

  public final void setAxisTick(float axisTick) {
    this.axisTick = axisTick;
    buildRings();
    invalidate();
  }

  public final float getAxisWidth() {
    return axisWidth;
  }

  public final void setAxisWidth(float axisWidth) {
    this.axisWidth = axisWidth;
    invalidate();
  }

  public final int getEndColor() {
    return endColor;
  }

  public final void setEndColor(int endColor) {
    this.endColor = endColor;
    invalidate();
    invalidate();
  }

  public final int getGraphColor() {
    return graphColor;
  }

  public final void setGraphColor(int graphColor) {
    this.graphColor = graphColor;
    invalidate();
  }

  public final int getGraphStyle() {
    return graphStyle;
  }

  public final void setGraphStyle(int graphStyle) {
    this.graphStyle = graphStyle;
    invalidate();
  }

  public final float getGraphWidth() {
    return graphWidth;
  }

  public final void setGraphWidth(float graphWidth) {
    this.graphWidth = graphWidth;
    invalidate();
  }

  public final int getStartColor() {
    return startColor;
  }

  public final void setStartColor(int startColor) {
    this.startColor = startColor;
    invalidate();
  }

  public final boolean isAutoSize() {
    return autoSize;
  }

  public final void setAutoSize(boolean autoSize) {
    this.autoSize = autoSize;

    if (autoSize && !axis.isEmpty()) {
      setAxisMaxInternal(Collections.max(axis.values()));
    }
  }

  public final boolean isCirclesOnly() {
    return circlesOnly;
  }

  public final void setCirclesOnly(boolean circlesOnly) {
    this.circlesOnly = circlesOnly;
    invalidate();
  }

  public final boolean isSmoothGradient() {
    return smoothGradient;
  }

  public final void setSmoothGradient(boolean smoothGradient) {
    this.smoothGradient = smoothGradient;
    invalidate();
  }

  public final void remove(String axisName) {
    axis.remove(axisName);
    onAxisChanged();
  }

  public final void setTextSize(float textSize) {
    textPaint.setTextSize(textSize);
    invalidate();
  }

  @Override protected void onDraw(Canvas canvas) {
    if (isInEditMode()) {
      calculateCenter();
      buildVertices();
    }

    final int count = axis.size();
    if (count < 3 || circlesOnly) {
      drawCircles(canvas);
    } else {
      drawPolygons(canvas, count);
    }
    drawValues(canvas, count);
    drawAxis(canvas);
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int width = MeasureSpec.getSize(widthMeasureSpec);
    final int height = MeasureSpec.getSize(heightMeasureSpec);
    if (width != height) {
      final int size = MeasureSpec.makeMeasureSpec(min(width, height), MeasureSpec.EXACTLY);
      super.onMeasure(size, size);
    }
    calculateCenter();
    buildRings();
  }

  private float axisMax() {
    return max(0,
        min(getMeasuredWidth() - getPaddingRight() - getPaddingLeft(), getMeasuredHeight() - getPaddingBottom() - getPaddingTop())) * 0.5F;
  }

  private float axisTick() {
    return axisTick * ratio();
  }

  private void buildRings() {
    final float axisTick = axisTick();
    final float axisMax = axisMax();
    @SuppressWarnings("NumericCastThatLosesPrecision")
    final int ringsCount = (int) max(ceil(axisMax / axisTick), 1);
    if (ringsCount == 0) {
      return;
    }

    rings = new Ring[ringsCount];
    if (ringsCount == 1) {
      rings[0] = new Ring(axisMax, axisMax, startColor);
    } else {
      for (int i = 0; i < ringsCount; i++) {
        rings[i] = new Ring(axisTick * (i + 1), axisTick, gradient(startColor, endColor, i, ringsCount));
      }
      rings[ringsCount - 1] = new Ring(axisMax, axisMax - rings[ringsCount - 2].radius, endColor);
    }

    buildVertices();
  }

  private void buildVertices() {
    final int count = axis.size();
    for (Ring ring : rings) {
      ring.vertices = createPoints(count, ring.fixedRadius, centerX, centerY);
    }
    vertices = createPoints(count, axisMax(), centerX, centerY);
  }

  private void calculateCenter() {
    centerX = (getMeasuredWidth() >> 1) + getPaddingLeft() - getPaddingRight();
    centerY = (getMeasuredHeight() >> 1) + getPaddingTop() - getPaddingBottom();
  }

  private void drawAxis(Canvas canvas) {
    final Iterator<String> axisNames = axis.keySet().iterator();
    mutatePaint(paint, axisColor, axisWidth, STROKE);
    final int length = vertices.length;
    for (int i = 0; i < length; i += 2) {
      path.reset();
      path.moveTo(centerX, centerY);
      final float pointX = vertices[i];
      final float pointY = vertices[i + 1];
      path.lineTo(pointX, pointY);
      path.close();
      canvas.drawPath(path, paint);

      final String axisName = axisNames.next();
      textPaint.getTextBounds(axisName, 0, axisName.length(), rect);
      final float x = pointX > centerX ? pointX : pointX - rect.width();
      final float y = pointY > centerY ? pointY + rect.height() : pointY;
      canvas.drawText(axisName, x, y, textPaint);
    }
  }

  private void drawCircles(Canvas canvas) {
    for (final Ring ring : rings) {
      mutatePaint(paint, ring.color, ring.width + 2, STROKE);
      canvas.drawCircle(centerX, centerY, ring.fixedRadius, paint);
    }
  }

  private void drawPolygons(Canvas canvas, int count) {
    for (final Ring ring : rings) {
      final float[] points = ring.vertices;
      final float startX = points[0];
      final float startY = points[1];

      path.reset();
      path.moveTo(startX, startY);
      path.setLastPoint(startX, startY);
      for (int j = 2; j < count + count; j += 2) {
        path.lineTo(points[j], points[j + 1]);
      }
      path.close();

      //noinspection NumericCastThatLosesPrecision
      mutatePaint(paint, ring.color, (float) (ring.width * cos(PI / count)) + 2, STROKE);
      canvas.drawPath(path, paint);
    }
  }

  private void drawValues(Canvas canvas, int count) {
    if (count == 0) {
      return;
    }

    path.reset();

    Float[] values = new Float[count];
    values = axis.values().toArray(values);
    if (count > 0) {
      final float ratio = ratio();
      final float[] first = createPoint(values[0] * ratio, -PI / 2, centerX, centerY);
      if (count == 1) {
        path.moveTo(centerX, centerY);
      } else {
        path.moveTo(first[0], first[1]);
        path.setLastPoint(first[0], first[1]);
        for (int i = 1; i < count; i++) {
          final float[] point = createPoint(values[i] * ratio, (2 * PI / count) * i - PI / 2, centerX, centerY);
          path.lineTo(point[0], point[1]);
        }
      }
    }
    path.close();

    mutatePaint(paint, graphColor, graphWidth, Paint.Style.values()[graphStyle]);
    canvas.drawPath(path, paint);
  }

  private void onAxisChanged() {
    if (autoSize && !axis.isEmpty()) {
      setAxisMaxInternal(Collections.max(axis.values()));
    } else {
      buildVertices();
      invalidate();
    }
  }

  private float ratio() {
    final float axisMax = axisMax();
    return axisMax > 0 ? axisMax / this.axisMax : 1;
  }

  private void setAxisMaxInternal(float axisMax) {
    this.axisMax = axisMax;
    buildRings();
    invalidate();
  }

  private static class Ring {
    final float width;
    final float radius;
    final float fixedRadius;
    final int   color;
    float[] vertices;

    Ring(float radius, float width, int color) {
      this.radius = radius;
      this.width = width;
      this.color = color;
      fixedRadius = radius - width / 2;
    }

    @Override public String toString() {
      return "Ring{" +
          "radius=" + radius +
          ", width=" + width +
          ", fixedRadius=" + fixedRadius +
          '}';
    }
  }
}
