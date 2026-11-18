package local.margymax.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import androidx.core.view.OneShotPreDrawListener;

/** One short, cancellable Material entrance. No timer or recurring background work. */
final class EntranceMotion {
    static final long DURATION_MS = 400L;
    private final View card, quote;
    private final float cardLift, textLift;
    // Material 3 emphasized-decelerate: arrive gently without a bounce.
    private final PathInterpolator arrive = new PathInterpolator(0.05f, 0.70f, 0.10f, 1f);
    private final LinearInterpolator time = new LinearInterpolator();
    private OneShotPreDrawListener pending;
    private ValueAnimator clock;

    EntranceMotion(View card, View quote) {
        this.card = card;
        this.quote = quote;
        float density = card.getResources().getDisplayMetrics().density;
        cardLift = 18f * density;
        textLift = 6f * density;
    }

    void enter() {
        settle();
        if (!enabled()) return;
        apply(0f);
        pending = OneShotPreDrawListener.add(card, this::startClock);
        card.invalidate();
    }

    private boolean enabled() {
        if (Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled()) return false;
        return Settings.Global.getFloat(card.getContext().getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
    }

    private void startClock() {
        pending = null;
        if (!enabled()) { apply(1f); return; }
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        clock = animator;
        // ValueAnimator itself honors the system duration scale: never apply it twice.
        animator.setDuration(DURATION_MS);
        animator.setInterpolator(time);
        animator.addUpdateListener(a -> apply((float) a.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) {
                if (clock == a) { clock = null; apply(1f); }
            }
        });
        animator.start();
    }

    /** A stopped or recreated screen must never keep a half-transparent card. */
    void settle() {
        if (pending != null) { pending.removeListener(); pending = null; }
        if (clock != null) {
            clock.removeAllListeners();
            clock.removeAllUpdateListeners();
            clock.cancel();
            clock = null;
        }
        apply(1f);
    }

    private void apply(float progress) {
        float travel = arrive.getInterpolation(progress);
        float scale = 0.965f + 0.035f * travel;
        card.setTranslationY(cardLift * (1f - travel));
        card.setScaleX(scale);
        card.setScaleY(scale);
        card.setAlpha(fade(progress / 0.72f));
        // The message follows the card by 48 ms at the normal duration scale.
        float content = clamp((progress - 0.12f) / 0.88f);
        quote.setTranslationY(textLift * (1f - arrive.getInterpolation(content)));
        quote.setAlpha(fade((progress - 0.12f) / 0.72f));
    }

    private static float clamp(float t) { return Math.max(0f, Math.min(1f, t)); }
    private static float fade(float t) { t = clamp(t); return t * t * (3f - 2f * t); }
}
