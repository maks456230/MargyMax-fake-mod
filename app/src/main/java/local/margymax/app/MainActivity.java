package local.margymax.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.core.graphics.ColorUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.shape.ShapeAppearancePathProvider;
import com.google.android.material.textview.MaterialTextView;

/** A single local message. No storage, accounts, services, telemetry, or network. */
public final class MainActivity extends Activity {
    private static final int FILL = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    private EntranceMotion entrance;
    private boolean skipRestoredEntrance;
    private boolean started;

    @Override protected void onCreate(Bundle state) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(state);
        int surface = role(com.google.android.material.R.attr.colorSurface);
        int paper = role(com.google.android.material.R.attr.colorPrimaryContainer);
        int ink = role(com.google.android.material.R.attr.colorOnPrimaryContainer);
        int primary = role(com.google.android.material.R.attr.colorPrimary);

        // Center at normal font scales. If accessibility text or a small freeform
        // window makes the card taller than the viewport, it scrolls instead.
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scroll.setBackgroundColor(surface);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        int gutter = dim(R.dimen.page_gutter);
        page.setPadding(gutter, dp(16), gutter, dp(16));
        scroll.addView(page, new ScrollView.LayoutParams(FILL, WRAP));

        CappedCard card = new CappedCard(this);
        card.setId(R.id.message_card);
        ShapeAppearanceModel shape = ShapeAppearanceModel.builder()
                .setAllCornerSizes(dim(R.dimen.card_corner))
                .setBottomLeftCornerSize(dp(12)).build();
        card.setShapeAppearanceModel(shape);
        card.setUseCompatPadding(false);
        card.setPreventCornerOverlap(false);
        card.setCardElevation(0f);
        card.setStrokeWidth(0);
        card.setContentPadding(0, 0, 0, 0);
        card.setCardBackgroundColor(paper);
        card.setClickable(false);
        card.setFocusable(false);
        card.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        page.addView(card, new LinearLayout.LayoutParams(FILL, WRAP));

        FrameLayout sheet = new TonalSheet(this, shape, paper, primary);
        card.addView(sheet, new FrameLayout.LayoutParams(FILL, WRAP));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.LEFT);
        int inset = dim(R.dimen.card_inset), vertical = dim(R.dimen.card_vertical);
        content.setPadding(inset, vertical, inset, vertical);
        sheet.addView(content, new FrameLayout.LayoutParams(FILL, WRAP));

        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.inline_mark);
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        int markSize = dim(R.dimen.mark_size);
        content.addView(mark, new LinearLayout.LayoutParams(markSize, markSize));

        MaterialTextView text = new MaterialTextView(this);
        text.setId(R.id.quote);
        text.setTextAppearance(this, com.google.android.material.R.style.TextAppearance_Material3_DisplayMedium);
        text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        text.setTextColor(ink);
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.display_size));
        text.setLetterSpacing(-0.012f);
        text.setIncludeFontPadding(false);
        text.setElegantTextHeight(false);
        text.setLineSpacing(0f, 1.02f);
        text.setGravity(Gravity.LEFT);
        text.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
        text.setTextDirection(View.TEXT_DIRECTION_LTR);
        text.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
        text.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
        String original = getString(R.string.message);
        int split = original.indexOf(' ');
        Configuration cfg = getResources().getConfiguration();
        boolean wide = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE && cfg.screenWidthDp >= 500;
        SpannableString message = new SpannableString(wide ? original : original.substring(0, split) + "\n" + original.substring(split + 1));
        message.setSpan(new ForegroundColorSpan(primary), 0, split, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        message.setSpan(new TypefaceSpan("sans-serif"), 0, split, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setText(message);
        text.setContentDescription(original);
        androidx.core.view.ViewCompat.setAccessibilityHeading(text, true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(FILL, WRAP);
        lp.topMargin = dim(R.dimen.mark_to_text);
        content.addView(text, lp);

        entrance = new EntranceMotion(card, text);
        skipRestoredEntrance = state != null;
        configureSystemUi(scroll, surface);
        setContentView(scroll);
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController bars = getWindow().getInsetsController();
            if (bars != null) {
                int light = WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS | WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
                bars.setSystemBarsAppearance(ColorUtils.calculateLuminance(surface) > 0.45 ? light : 0, light);
                bars.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                bars.hide(WindowInsets.Type.statusBars());
            }
        }
        scroll.requestApplyInsets();
        if (state == null && Build.VERSION.SDK_INT >= 31) LaunchSplash.handoff(this);
    }

    @Override protected void onStart() {
        super.onStart();
        started = true;
        // A genuine launch or return from the background is animated. A rotation,
        // locale/theme change or saved-state restore simply shows the finished screen.
        if (skipRestoredEntrance) { skipRestoredEntrance = false; entrance.settle(); }
        else entrance.enter();
    }
    @Override protected void onStop() {
        started = false;
        entrance.settle();
        super.onStop();
    }

    @androidx.annotation.RequiresApi(31)
    private static final class LaunchSplash {
        static void handoff(MainActivity activity) {
            android.window.SplashScreen splash = activity.getSplashScreen();
            splash.setOnExitAnimationListener(view -> {
                // If the OS covered the first draw with its launch icon, hand off
                // to the card just as that cover leaves. Warm launches need no cover.
                if (activity.started && !activity.isFinishing()) activity.entrance.enter();
                view.remove();
                splash.clearOnExitAnimationListener();
            });
        }
    }

    private int role(int attr) { return MaterialColors.getColor(this, attr, "Margy Max"); }
    private int dim(int res) { return getResources().getDimensionPixelSize(res); }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void configureSystemUi(View root, int surface) {
        Window w = getWindow();
        boolean light = ColorUtils.calculateLuminance(surface) > 0.45;
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Build.VERSION.SDK_INT < 26 && light ? 0xff231f29 : Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) w.setNavigationBarContrastEnforced(false);
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
        } else {
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
            if (light && Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            w.getDecorView().setSystemUiVisibility(flags);
        }
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets i = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                v.setPadding(i.left, i.top, i.right, i.bottom);
            } else {
                v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
    }

    private static final class CappedCard extends MaterialCardView {
        CappedCard(Context c) { super(c); }
        @Override protected void onMeasure(int w, int h) {
            int limit = getResources().getDimensionPixelSize(R.dimen.card_max_width);
            int width = MeasureSpec.getMode(w) == MeasureSpec.UNSPECIFIED ? limit : Math.min(limit, MeasureSpec.getSize(w));
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), h);
        }
    }

    /** Three small, flat scalloped arcs. Never animated; use resolved M3 tones. */
    private static final class TonalSheet extends FrameLayout {
        private final ShapeAppearanceModel shape;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path clip = new Path();
        private final Path[] rings = { new Path(), new Path(), new Path() };
        private final int[] colors = new int[3];
        private final float density;
        TonalSheet(Context c, ShapeAppearanceModel shape, int paper, int primary) {
            super(c); this.shape = shape; density = getResources().getDisplayMetrics().density;
            setWillNotDraw(false);
            colors[0] = ColorUtils.blendARGB(paper, primary, 0.10f);
            colors[1] = ColorUtils.blendARGB(paper, primary, 0.15f);
            colors[2] = ColorUtils.blendARGB(paper, primary, 0.20f);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }
        @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
            super.onSizeChanged(w, h, oldW, oldH);
            clip.rewind();
            ShapeAppearancePathProvider.getInstance().calculatePath(shape, 1f, new RectF(0, 0, w, h), clip);
            float r = Math.min(116 * density, Math.min(w * 0.40f, h * 0.41f));
            float cx = w + 3 * density, cy = -6 * density;
            paint.setStrokeWidth(r * 0.104f);
            for (int j = 0; j < rings.length; j++) {
                Path p = rings[j]; p.rewind();
                for (int i = 0; i <= 180; i++) {
                    double a = 2.0 * Math.PI * i / 180.0;
                    double radius = r * (1f - j * 0.229f) * (1.0 + 0.072 * Math.cos(8.0 * a + 0.4));
                    float x = (float) (cx + radius * Math.cos(a)), y = (float) (cy + radius * Math.sin(a));
                    if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
                }
                p.close();
            }
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int save = canvas.save(); canvas.clipPath(clip);
            for (int i = 0; i < rings.length; i++) { paint.setColor(colors[i]); canvas.drawPath(rings[i], paint); }
            canvas.restoreToCount(save);
        }
    }
}
