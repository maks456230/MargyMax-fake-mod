package local.margymax.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.card.MaterialCardView;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public final class MainActivityTest {
    private void check(int width, int height, boolean dark, String preview) throws Exception {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity app = controller.get();
            assertEquals("Margy Max", app.getString(R.string.app_name));
            TextView quote = app.findViewById(R.id.quote);
            MaterialCardView card = app.findViewById(R.id.message_card);
            assertEquals("макс иди нахуй", quote.getContentDescription());
            assertEquals("макс иди нахуй", quote.getText().toString().replace('\n', ' '));
            FrameLayout content = app.findViewById(android.R.id.content);
            View root = content.getChildAt(0);
            card.getViewTreeObserver().dispatchOnPreDraw();
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
                    .idleFor(700, java.util.concurrent.TimeUnit.MILLISECONDS);
            assertEquals(1f, card.getAlpha(), 0.001f);
            assertEquals(1f, quote.getAlpha(), 0.001f);
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            root.layout(0,0,width,height);
            assertNotNull(quote.getLayout());
            assertTrue(quote.getLineCount() >= 1);
            // Robolectric API 23 stubs nComputeLineBreaks without writing any break
            // offsets. Layout metrics are checked only with the native text renderer.
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                assertEquals(quote.length(), quote.getLayout().getLineEnd(quote.getLineCount()-1));
                for (int i = 0; i < quote.getLineCount(); i++) {
                    assertEquals(0, quote.getLayout().getEllipsisCount(i));
                    assertTrue(quote.getLayout().getLineWidth(i) <= quote.getWidth() + 1);
                }
            }
            assertTrue(quote.getLayout().getHeight() <= quote.getHeight());
            assertTrue(card.getWidth() < width);
            assertTrue(card.getHeight() > quote.getHeight());
            if (height >= 700) {
                assertTrue("The message card fits its contents in portrait", card.getHeight() < height * 0.7);
                int[] xy = new int[2], at = new int[2];
                card.getLocationInWindow(xy); root.getLocationInWindow(at);
                assertEquals(height * .5, xy[1] - at[1] + .5 * card.getHeight(), 2.0);
            }
            int paper = card.getCardBackgroundColor().getDefaultColor();
            assertEquals(dark, ColorUtils.calculateLuminance(paper) < 0.35);
            assertTrue(ColorUtils.calculateContrast(quote.getCurrentTextColor(), paper) >= 4.5);
            String out = System.getenv("MARGY_TEST_PREVIEWS");
            if (out != null && preview != null) {
                Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                root.draw(new Canvas(image));
                File dir = new File(out); dir.mkdirs();
                try (FileOutputStream stream = new FileOutputStream(new File(dir, preview + ".png"))) {
                    assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, stream));
                }
                image.recycle();
            }
        }
    }
    @Test @Config(sdk=34, qualifiers="ru-rRU-w393dp-h852dp-port-notnight-mdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void lightPortrait() throws Exception { check(393,852,false,"light"); }
    @Test @Config(sdk=34, qualifiers="ru-rRU-w360dp-h740dp-port-night-mdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void darkPortrait() throws Exception { check(360,740,true,"dark"); }
    @Test @Config(sdk=34, qualifiers="ru-rRU-w760dp-h360dp-land-notnight-mdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void landscape() throws Exception { check(760,360,false,"landscape"); }
    @Test @Config(sdk=23, qualifiers="ru-rRU-w320dp-h480dp-port-notnight-mdpi")
    public void androidSix() throws Exception { check(320,480,false,null); }
}
