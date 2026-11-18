package local.margymax.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.FrameLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, qualifiers="ru-rRU-w393dp-h852dp-port-notnight-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class EntranceMotionTest {
    private static void step(long ms) { shadowOf(Looper.getMainLooper()).idleFor(ms, TimeUnit.MILLISECONDS); }
    private static void assertSettled(MainActivity a) {
        View card = a.findViewById(R.id.message_card), text = a.findViewById(R.id.quote);
        assertEquals(1f, card.getAlpha(), .001f);
        assertEquals(1f, text.getAlpha(), .001f);
        assertEquals(1f, card.getScaleX(), .001f);
        assertEquals(1f, card.getScaleY(), .001f);
        assertEquals(0f, card.getTranslationY(), .01f);
        assertEquals(0f, text.getTranslationY(), .01f);
    }
    private static void frame(View root, int index) throws Exception {
        String output = System.getenv("MARGY_MOTION_FRAMES");
        if (output == null) return;
        int w = 393, h = 852;
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, w, h);
        Bitmap snapshot = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(snapshot));
        File dir = new File(output); dir.mkdirs();
        File f = new File(dir, String.format(Locale.ROOT, "%03d.png", index));
        try (FileOutputStream stream = new FileOutputStream(f)) {
            assertTrue(snapshot.compress(Bitmap.CompressFormat.PNG, 100, stream));
        }
        snapshot.recycle();
    }
    @Test public void launchActuallyAnimatesThenSettlesAndCanReplay() throws Exception {
        // A test display normally auto-drains every vsync during visible(). Drive it
        // at a fixed cadence instead, to inspect the same animator between frames.
        org.robolectric.shadows.ShadowChoreographer.setPaused(true);
        org.robolectric.shadows.ShadowChoreographer.setFrameDelay(java.time.Duration.ofMillis(16));
        try (ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).create().start()) {
            MainActivity a = c.get();
            View card = a.findViewById(R.id.message_card), text = a.findViewById(R.id.quote);
            assertEquals(0f, card.getAlpha(), .001f);
            assertTrue(card.getTranslationY() > 0f);
            c.postCreate(null).resume().postResume().visible();
            FrameLayout host = a.findViewById(android.R.id.content);
            View root = host.getChildAt(0);
            card.getViewTreeObserver().dispatchOnPreDraw();
            boolean duringFade = false, staggered = false;
            for (int i = 0; i <= 26; i++) {
                float alpha = card.getAlpha();
                duringFade |= alpha > .03f && alpha < .97f;
                staggered |= alpha > text.getAlpha() + .10f;
                frame(root, i);
                step(20);
            }
            assertTrue("At least one genuinely intermediate frame", duringFade);
            assertTrue("Text follows the card", staggered);
            assertSettled(a);
            c.pause().stop().restart().start();
            assertEquals(0f, card.getAlpha(), .001f);
            c.resume().visible();
            card.getViewTreeObserver().dispatchOnPreDraw();
            step(80);
            c.pause().stop(); // interruption must not leave stale transforms or callbacks
            assertSettled(a); step(600); assertSettled(a);
        }
    }
    @Test public void systemAnimationsOffShowsEverythingImmediately() {
        android.content.ContentResolver cr = RuntimeEnvironment.getApplication().getContentResolver();
        float old = Settings.Global.getFloat(cr, Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        try {
            Settings.Global.putFloat(cr, Settings.Global.ANIMATOR_DURATION_SCALE, 0f);
            try (ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup()) {
                assertSettled(c.get()); step(100); assertSettled(c.get());
            }
        } finally { Settings.Global.putFloat(cr, Settings.Global.ANIMATOR_DURATION_SCALE, old); }
    }
    @Test public void restoringAfterRotationSkipsEntrance() {
        try (ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup(new Bundle())) {
            assertSettled(c.get());
            step(100); assertSettled(c.get());
        }
    }
}
