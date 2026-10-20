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
 * Affichage de texte dans OpenGL avec n'importe quelle police du système.
 *
 * Chaque chaîne est dessinée une fois par SWT (GC) dans une image, convertie
 * en texture "alpha" puis affichée sur un quadrilatère en coordonnées pixels,
 * dans la couleur OpenGL courante. Les textures sont gardées en cache (les
 * moins récemment utilisées sont libérées au-delà de MAX_CACHED_TEXTS).
 *
 * SWT n'autorise le dessin que dans son thread d'interface. Si l'affichage
 * OpenGL a lieu dans ce thread (GLCanvas SWT), le texte est préparé
 * immédiatement ; sinon (NEWT, thread de l'animateur), il est préparé de
 * manière asynchrone et apparaît à l'image suivante. En attendant, les
 * dimensions renvoyées sont des estimations.
 *
 * Une instance est propre à un renderer : ses méthodes de dessin doivent
 * être appelées depuis son thread d'affichage OpenGL.
 */
public final class GLText {

    private static final int MAX_CACHED_TEXTS = 512;
    
    /** Nombre maximal de textes en cours de préparation 
     * asynchrone (évite d'engorger le thread SWT).**/
    private static final int MAX_PENDING = 64;

    /** Image d'un texte : alpha sur 8 bits, lignes de bas en haut (convention OpenGL). */
    private static final class Raster {
        final int width, height;
        final byte[] alpha;

        Raster(int width, int height, byte[] alpha) {
            this.width = width;
            this.height = height;
            this.alpha = alpha;
        }
    }

    /** Mesures d'une police, en pixels. */
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
        int texture; // 0 = pas encore créée

        Entry(Raster raster) { this.raster = raster; }
    }

    private Display display;
    private float pixelScale = 1f;

    // Cache LRU des textures (thread OpenGL uniquement)
    private final LinkedHashMap<Key, Entry> cache = new LinkedHashMap<>(64, 0.75f, true);
    // Images préparées par le thread SWT, en attente de transfert vers le cache
    private final Map<Key, Raster> ready = new ConcurrentHashMap<>();
    private final Set<Key> pending = ConcurrentHashMap.newKeySet();
    private final Map<String, FontMetrics> metrics = new ConcurrentHashMap<>(); // par police et échelle
    // Textures à libérer (retirées du cache), au prochain passage dans le thread OpenGL
    private final java.util.List<Integer> texturesToDelete = new java.util.ArrayList<>();

    /**
     * Facteur entre pixels OpenGL et points de l'interface (2 sur un écran
     * Retina avec NEWT, 1 sinon). Les polices sont agrandies d'autant pour
     * garder la même taille apparente.
     */
    public void setPixelScale(float pixelScale) {
        this.pixelScale = (pixelScale > 0) ? pixelScale : 1f;
    }

    /**
     * Facteur pixels / points d'une surface OpenGL : celui que rapporte JOGL
     * pour les surfaces qui le gèrent (fenêtre NEWT sur écran Retina), 1 sinon.
     */
    public static float surfaceScale(com.jogamp.opengl.GLAutoDrawable drawable) {
        if (drawable instanceof com.jogamp.nativewindow.ScalableSurface) {
            float[] scale = ((com.jogamp.nativewindow.ScalableSurface) drawable).getCurrentSurfaceScale(new float[2]);
            if (scale != null && scale[0] > 0) return scale[0];
        }
        return 1f;
    }

    /** Hauteur d'une ligne de texte dans cette police, en pixels. */
    public float lineHeight(ChartFont font) {
        return metricsOf(font).lineHeight;
    }

    /** Largeur du texte en pixels (estimée si le texte n'est pas encore prêt). */
    public float width(String text, ChartFont font) {
        Entry entry = entry(text, font);
        if (entry != null) return entry.raster.width;
        return text.length() * metricsOf(font).averageCharWidth;
    }

    /**
     * Dessine le texte dans la couleur courante, coin inférieur gauche en
     * (x, y). La projection courante doit être en pixels.
     */
    public void draw(GL2 gl, String text, ChartFont font, float x, float y) {
        deletePendingTextures(gl);
        Entry entry = entry(text, font);
        if (entry == null || entry.raster.width == 0) return;
        Raster raster = entry.raster;
        if (entry.texture == 0) entry.texture = createTexture(gl, raster);

        // Position entière : chaque pixel du texte tombe sur un pixel de l'écran (texte net)
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

    /** Libère toutes les textures (à appeler depuis dispose() du renderer). */
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
     * Prépare le texte s'il ne l'est pas déjà (immédiatement dans le thread
     * SWT, de manière asynchrone sinon) et indique s'il est prêt à dessiner.
     */
    public boolean prepare(String text, ChartFont font) {
        return entry(text, font) != null;
    }

    /** Texte prêt à dessiner, ou null s'il est en cours de préparation. */
    private Entry entry(String text, ChartFont font) {
    	drainReady(); // les images prêtes passent dans le cache LRU, qui est borné
    	Key key = new Key(text, font, pixelScale);
        Entry entry = cache.get(key);
        if (entry != null) return entry;

        Display swt = display();
        if (swt == null || swt.isDisposed()) return null;
        if (Display.getCurrent() != swt) {
        	if (pending.size() < MAX_PENDING && pending.add(key)) { // ne pas inonder le thread SWT
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
    
    /** Transfère dans le cache les images préparées par le thread SWT (même celles qui ne sont plus demandées). */
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

    /** Mesures de la police (estimation tant qu'aucun texte n'a été préparé avec elle). */
    private FontMetrics metricsOf(ChartFont font) {
        FontMetrics m = metrics.get(metricsKey(font, pixelScale));
        if (m != null) return m;
        Display swt = display();
        if (swt != null && !swt.isDisposed() && Display.getCurrent() == swt) {
            return measure(swt, font, pixelScale);
        }
        float pixels = font.getSize() * pixelScale * 96f / 72f; // estimation (écran à 96 ppp)
        return new FontMetrics(pixels * 1.25f, pixels * 0.6f);
    }

    private static String metricsKey(ChartFont font, float scale) {
        return font.toString() + "@" + scale;
    }

    // ---------------- thread SWT uniquement ----------------

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
    	// Mesures de la police : une seule fois par police et par échelle
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

            // Texte blanc sur fond noir : la luminance devient l'opacité
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
            int row = (height - 1 - y) * width; // OpenGL : première ligne en bas
            for (int x = 0; x < width; x++) {
                RGB rgb = data.palette.getRGB(data.getPixel(x, y));
                alpha[row + x] = (byte) Math.max(rgb.red, Math.max(rgb.green, rgb.blue));
            }
        }
        return new Raster(width, height, alpha);
        
    }
}
