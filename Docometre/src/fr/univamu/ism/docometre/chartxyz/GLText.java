package fr.univamu.ism.docometre.chartxyz;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.jogamp.opengl.GL2;

/**
 * Text display in OpenGL with any system font.
 *
 * Each string is drawn once by SWT (GC) into an image, converted into an
 * "alpha" texture, then displayed on a quad in pixel coordinates, in the
 * current OpenGL color. Textures are cached (the least recently used ones
 * are released beyond MAX_CACHED_TEXTS).
 *
 * SWT only allows drawing in its user interface thread. If OpenGL rendering
 * takes place in that thread (SWT GLCanvas), the text is prepared
 * immediately; otherwise (NEWT, animator thread), it is prepared
 * asynchronously and appears at the next frame. Meanwhile, the returned
 * dimensions are estimates.
 *
 * An instance is specific to a renderer: its drawing methods must be called
 * from its OpenGL rendering thread.
 */
public final class GLText {

    private static final int MAX_CACHED_TEXTS = 512;
    
    /** Maximum number of texts being prepared
     * asynchronously (avoids flooding the SWT thread).**/
    private static final int MAX_PENDING = 64;

    /** Image of a text: 8-bit alpha, rows from bottom to top (OpenGL convention). */
    private static final class Raster {
        final int width, height;
        final byte[] alpha;

        Raster(int width, int height, byte[] alpha) {
            this.width = width;
            this.height = height;
            this.alpha = alpha;
        }
    }

    /** Metrics of a font, in pixels. */
    private static final class FontMetrics {
        final float lineHeight, averageCharWidth;

        FontMetrics(float lineHeight, float averageCharWidth) {
            this.lineHeight = lineHeight;
            this.averageCharWidth = averageCharWidth;
        }
    }

    private static final class Key {
        final String text;
        final ChartFont font;
        final float scale;

        Key(String text, ChartFont font, float scale) {
            this.text = text;
            this.font = font;
            this.scale = scale;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key)) return false;
            Key k = (Key) o;
            return scale == k.scale && text.equals(k.text) && font.equals(k.font);
        }

        @Override
        public int hashCode() { return Objects.hash(text, font, scale); }
    }

    private static final class Entry {
        final Raster raster;
        int texture; // 0 = not created yet

        Entry(Raster raster) { this.raster = raster; }
    }

    private Display display;
    private float pixelScale = 1f;

    // LRU cache of the textures (OpenGL thread only)
    private final LinkedHashMap<Key, Entry> cache = new LinkedHashMap<>(64, 0.75f, true);
    // Images prepared by the SWT thread, waiting to be transferred to the cache
    private final Map<Key, Raster> ready = new ConcurrentHashMap<>();
    private final Set<Key> pending = ConcurrentHashMap.newKeySet();
    private final Map<String, FontMetrics> metrics = new ConcurrentHashMap<>(); // per font and scale
    // Textures to release (removed from the cache), at the next pass in the OpenGL thread
    private final java.util.List<Integer> texturesToDelete = new java.util.ArrayList<>();

    /**
     * Factor between OpenGL pixels and user interface points (2 on a Retina
     * screen with NEWT, 1 otherwise). Fonts are enlarged accordingly to keep
     * the same apparent size.
     */
    public void setPixelScale(float pixelScale) {
        this.pixelScale = (pixelScale > 0) ? pixelScale : 1f;
    }

    /**
     * Pixels / points factor of an OpenGL surface: the one reported by JOGL
     * for the surfaces that support it (NEWT window on a Retina screen), 1 otherwise.
     */
    public static float surfaceScale(com.jogamp.opengl.GLAutoDrawable drawable) {
        if (drawable instanceof com.jogamp.nativewindow.ScalableSurface) {
            float[] scale = ((com.jogamp.nativewindow.ScalableSurface) drawable).getCurrentSurfaceScale(new float[2]);
            if (scale != null && scale[0] > 0) return scale[0];
        }
        return 1f;
    }

    /** Height of a line of text in this font, in pixels. */
    public float lineHeight(ChartFont font) {
        return metricsOf(font).lineHeight;
    }

    /** Width of the text in pixels (estimated if the text is not ready yet). */
    public float width(String text, ChartFont font) {
        Entry entry = entry(text, font);
        if (entry != null) return entry.raster.width;
        return text.length() * metricsOf(font).averageCharWidth;
    }

    /**
     * Draws the text in the current color, lower left corner at (x, y). The
     * current projection must be in pixels.
     */
    public void draw(GL2 gl, String text, ChartFont font, float x, float y) {
        deletePendingTextures(gl);
        Entry entry = entry(text, font);
        if (entry == null || entry.raster.width == 0) return;
        Raster raster = entry.raster;
        if (entry.texture == 0) entry.texture = createTexture(gl, raster);

        // Integer position: each pixel of the text falls on a screen pixel (sharp text)
        float x0 = Math.round(x), y0 = Math.round(y);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, entry.texture);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);
        gl.glBegin(GL2.GL_QUADS);
        gl.glTexCoord2f(0, 0); gl.glVertex2f(x0, y0);
        gl.glTexCoord2f(1, 0); gl.glVertex2f(x0 + raster.width, y0);
        gl.glTexCoord2f(1, 1); gl.glVertex2f(x0 + raster.width, y0 + raster.height);
        gl.glTexCoord2f(0, 1); gl.glVertex2f(x0, y0 + raster.height);
        gl.glEnd();
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
        gl.glDisable(GL2.GL_TEXTURE_2D);
    }

    /** Releases all the textures (to be called from the dispose() of the renderer). */
    public void dispose(GL2 gl) {
        for (Entry entry : cache.values()) {
            if (entry.texture != 0) texturesToDelete.add(entry.texture);
        }
        cache.clear();
        deletePendingTextures(gl);
        ready.clear();
    }

    // ------------------------------------------------------------------

    /**
     * Prepares the text if it is not already prepared (immediately in the SWT
     * thread, asynchronously otherwise) and tells whether it is ready to draw.
     */
    public boolean prepare(String text, ChartFont font) {
        return entry(text, font) != null;
    }

    /** Text ready to draw, or null if it is being prepared. */
    private Entry entry(String text, ChartFont font) {
    	drainReady(); // ready images go into the LRU cache, which is bounded
    	Key key = new Key(text, font, pixelScale);
        Entry entry = cache.get(key);
        if (entry != null) return entry;

        Display swt = display();
        if (swt == null || swt.isDisposed()) return null;
        if (Display.getCurrent() != swt) {
        	if (pending.size() < MAX_PENDING && pending.add(key)) { // do not flood the SWT thread
                try {
					swt.asyncExec(() -> {
					    try {
					        if (!swt.isDisposed()) ready.put(key, rasterize(swt, key));
					    } finally {
					        pending.remove(key);
					    }
					});
				} catch (Exception e) {
					pending.remove(key);
				}
            }
            return null;
        }
        entry = new Entry(rasterize(swt, key));
        cache.put(key, entry);
        evictOldEntries();
        return entry;
    }
    
    /** Transfers the images prepared by the SWT thread into the cache (even those that are no longer requested). */
    private void drainReady() {
        if (ready.isEmpty()) return;
        for (Iterator<Map.Entry<Key, Raster>> it = ready.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Key, Raster> e = it.next();
            it.remove();
            cache.putIfAbsent(e.getKey(), new Entry(e.getValue()));
        }
        evictOldEntries();
    }

    private Display display() {
        if (display == null) display = Display.getDefault();
        return display;
    }

    private void evictOldEntries() {
        Iterator<Entry> it = cache.values().iterator();
        while (cache.size() > MAX_CACHED_TEXTS && it.hasNext()) {
            Entry oldest = it.next();
            if (oldest.texture != 0) texturesToDelete.add(oldest.texture);
            it.remove();
        }
    }

    private void deletePendingTextures(GL2 gl) {
        if (texturesToDelete.isEmpty()) return;
        int[] ids = new int[texturesToDelete.size()];
        for (int i = 0; i < ids.length; i++) ids[i] = texturesToDelete.get(i);
        gl.glDeleteTextures(ids.length, ids, 0);
        texturesToDelete.clear();
    }

    private static int createTexture(GL2 gl, Raster raster) {
        int[] id = new int[1];
        gl.glGenTextures(1, id, 0);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, id[0]);
        gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 1);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_ALPHA, raster.width, raster.height, 0,
                        GL2.GL_ALPHA, GL2.GL_UNSIGNED_BYTE, ByteBuffer.wrap(raster.alpha));
        return id[0];
    }

    /** Metrics of the font (estimate as long as no text has been prepared with it). */
    private FontMetrics metricsOf(ChartFont font) {
        FontMetrics m = metrics.get(metricsKey(font, pixelScale));
        if (m != null) return m;
        Display swt = display();
        if (swt != null && !swt.isDisposed() && Display.getCurrent() == swt) {
            return measure(swt, font, pixelScale);
        }
        float pixels = font.getSize() * pixelScale * 96f / 72f; // estimate (96 dpi screen)
        return new FontMetrics(pixels * 1.25f, pixels * 0.6f);
    }

    private static String metricsKey(ChartFont font, float scale) {
        return font.toString() + "@" + scale;
    }

    // ---------------- SWT thread only ----------------

    private Font createFont(Display swt, ChartFont font, float scale) {
        String family = font.getFamily();
        if (family == null) family = swt.getSystemFont().getFontData()[0].getName();
        int height = Math.max(1, Math.round(font.getSize() * scale));
        return new Font(swt, family, height, font.getStyle());
    }

    private FontMetrics measure(Display swt, ChartFont font, float scale) {
        Font swtFont = createFont(swt, font, scale);
        try {
			Image image = new Image(swt, 1, 1);
			try {
				GC gc = new GC(image);
				try {
					gc.setFont(swtFont);
					Point sample = gc.textExtent("0123456789");
					FontMetrics m = new FontMetrics(gc.textExtent("Ag").y, sample.x / 10f);
					metrics.put(metricsKey(font, scale), m);
					return m;
				} finally {
					gc.dispose();
				} 
			} finally {
				image.dispose();
			} 
		} finally {
			swtFont.dispose();
		}
    }

    private Raster rasterize(Display swt, Key key) {
    	// Font metrics: only once per font and scale
        if (!metrics.containsKey(metricsKey(key.font, key.scale))) measure(swt, key.font, key.scale);

        ImageData data;
        Font swtFont = createFont(swt, key.font, key.scale);
        try {
            Point extent;
            Image probe = new Image(swt, 1, 1);
            try {
                GC gc = new GC(probe);
                try {
                    gc.setFont(swtFont);
                    extent = gc.textExtent(key.text);
                } finally {
                    gc.dispose();
                }
            } finally {
                probe.dispose();
            }
            int width = Math.max(1, extent.x), height = Math.max(1, extent.y);

            // White text on a black background: luminance becomes opacity
            Image image = new Image(swt, width, height);
            try {
                GC gc = new GC(image);
                try {
                    gc.setAntialias(SWT.ON);
                    gc.setTextAntialias(SWT.ON);
                    gc.setBackground(swt.getSystemColor(SWT.COLOR_BLACK));
                    gc.fillRectangle(0, 0, width, height);
                    gc.setForeground(swt.getSystemColor(SWT.COLOR_WHITE));
                    gc.setFont(swtFont);
                    gc.drawText(key.text, 0, 0, true);
                } finally {
                    gc.dispose();
                }
                data = image.getImageData();
            } finally {
                image.dispose();
            }
        } finally {
            swtFont.dispose();
        }

        int width = data.width, height = data.height;
        byte[] alpha = new byte[width * height];
        for (int y = 0; y < height; y++) {
            int row = (height - 1 - y) * width; // OpenGL: first row at the bottom
            for (int x = 0; x < width; x++) {
                RGB rgb = data.palette.getRGB(data.getPixel(x, y));
                alpha[row + x] = (byte) Math.max(rgb.red, Math.max(rgb.green, rgb.blue));
            }
        }
        return new Raster(width, height, alpha);
        
    }
}
