package fr.univamu.ism.docometre.chartxyz;

import java.io.InputStream;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;

/**
 * Appearance of the point displayed by a {@link GLCursorChart2D}: either a
 * geometric shape ({@link CursorShape}) filled with a color and optionally
 * outlined, or an image (PNG, JPEG, GIF, BMP...: any format read by SWT;
 * the transparency of PNG images is kept).
 *
 * The size is the width of the square that contains the symbol, in pixels:
 * an image is scaled so that its largest dimension is equal to the size
 * (its proportions are kept). By default, a shape is filled with the color
 * of its curve and is not outlined.
 *
 * Instances are immutable: the with...() methods return a modified copy, so
 * that settings can be chained, for example
 * {@code CursorStyle.shape(CursorShape.SQUARE, 16).withFillColor(255, 0, 0).withOutline(255, 255, 255, 2)}.
 */
public final class CursorStyle {

    /** Size of the symbols until another one is given, in pixels. */
    public static final float DEFAULT_SIZE = 12f;
    /** Largest size accepted, in pixels. */
    public static final float MAX_SIZE = 1000f;

    /** Style of the curves without their own style: disk of DEFAULT_SIZE pixels, in the color of the curve. */
    public static final CursorStyle DEFAULT = shape(CursorShape.CIRCLE, DEFAULT_SIZE);

    /** Image converted for OpenGL: RGBA, 8 bits per component, rows from bottom to top. */
    static final class ImageRaster {
        final int width, height;
        final byte[] rgba;

        ImageRaster(int width, int height, byte[] rgba) {
            this.width = width;
            this.height = height;
            this.rgba = rgba;
        }
    }

    private final CursorShape shape;        // null for an image
    private final ImageRaster image;        // null for a shape
    private final float size;               // pixels
    private final CurveColor fillColor;     // null = color of the curve
    private final CurveColor outlineColor;  // null = no outline
    private final float outlineWidth;       // pixels

    private CursorStyle(CursorShape shape, ImageRaster image, float size, CurveColor fillColor,
                        CurveColor outlineColor, float outlineWidth) {
        this.shape = shape;
        this.image = image;
        this.size = checkSize(size);
        this.fillColor = fillColor;
        this.outlineColor = outlineColor;
        this.outlineWidth = outlineWidth;
    }

    private static float checkSize(float size) {
        if (!(size > 0f) || size > MAX_SIZE) {
            throw new IllegalArgumentException("The cursor size must be between 0 (excluded) and " + MAX_SIZE + " pixels.");
        }
        return size;
    }

    // ------------------------------------------------------------------
    // Creation

    /**
     * Geometric shape filled with the color of its curve, without outline.
     *
     * @param size width of the square containing the shape, in pixels
     * @throws IllegalArgumentException if shape is null or if the size is not in ]0, MAX_SIZE]
     */
    public static CursorStyle shape(CursorShape shape, float size) {
        if (shape == null) throw new IllegalArgumentException("The cursor shape cannot be null.");
        return new CursorStyle(shape, null, size, null, null, 0f);
    }

    /**
     * Image read from a file (PNG, JPEG...), displayed at its natural size.
     *
     * @throws IllegalArgumentException if the file cannot be read or decoded
     */
    public static CursorStyle image(String path) {
        return image(read(() -> new ImageData(path), path));
    }

    /**
     * Image read from a file (PNG, JPEG...), scaled so that its largest
     * dimension is size pixels.
     *
     * @throws IllegalArgumentException if the file cannot be read or decoded, or if the size is not in ]0, MAX_SIZE]
     */
    public static CursorStyle image(String path, float size) {
        return image(path).withSize(size);
    }

    /**
     * Image read from a stream (PNG, JPEG...), displayed at its natural size.
     * The stream is not closed.
     *
     * @throws IllegalArgumentException if the stream cannot be read or decoded
     */
    public static CursorStyle image(InputStream stream) {
        return image(read(() -> new ImageData(stream), "stream"));
    }

    /**
     * Image already loaded by SWT, displayed at its natural size (its pixels
     * are copied: modifying data afterwards has no effect).
     *
     * @throws IllegalArgumentException if data is null or empty
     */
    public static CursorStyle image(ImageData data) {
        if (data == null || data.width <= 0 || data.height <= 0) {
            throw new IllegalArgumentException("The cursor image cannot be empty.");
        }
        ImageRaster raster = toRaster(data);
        return new CursorStyle(null, raster, Math.min(MAX_SIZE, Math.max(raster.width, raster.height)), null, null, 0f);
    }

    private interface ImageReader {
        ImageData read();
    }

    /** Reads an image, the SWT errors (file not found, unknown format...) becoming IllegalArgumentException. */
    private static ImageData read(ImageReader reader, String source) {
        try {
            return reader.read();
        } catch (SWTException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to read the cursor image (" + source + "): " + e.getMessage(), e);
        }
    }

    /**
     * Converts an SWT image (any depth, direct or indexed palette) into RGBA
     * rows from bottom to top. Transparency: alpha channel, global alpha and
     * transparent pixel (GIF, indexed PNG).
     */
    static ImageRaster toRaster(ImageData data) {
        int w = data.width, h = data.height;
        byte[] rgba = new byte[w * h * 4];
        int[] pixels = new int[w];
        byte[] alphas = new byte[w];
        for (int y = 0; y < h; y++) {
            data.getPixels(0, y, w, pixels, 0);
            if (data.alphaData != null) data.getAlphas(0, y, w, alphas, 0);
            int k = (h - 1 - y) * w * 4; // OpenGL: first row at the bottom
            for (int x = 0; x < w; x++, k += 4) {
                int pixel = pixels[x];
                RGB rgb = data.palette.getRGB(pixel);
                int alpha = (data.alphaData != null) ? (alphas[x] & 0xFF) : 255;
                if (data.alpha != -1) alpha = alpha * data.alpha / 255;
                if (data.transparentPixel != -1 && pixel == data.transparentPixel) alpha = 0;
                rgba[k] = (byte) rgb.red;
                rgba[k + 1] = (byte) rgb.green;
                rgba[k + 2] = (byte) rgb.blue;
                rgba[k + 3] = (byte) alpha;
            }
        }
        return new ImageRaster(w, h, rgba);
    }

    // ------------------------------------------------------------------
    // Modified copies

    /**
     * Same style with another size: width of the square containing the
     * shape, or largest dimension of the image, in pixels.
     *
     * @throws IllegalArgumentException if the size is not in ]0, MAX_SIZE]
     */
    public CursorStyle withSize(float size) {
        return new CursorStyle(shape, image, size, fillColor, outlineColor, outlineWidth);
    }

    /**
     * Same shape filled with this color (components between 0 and 255)
     * instead of the color of the curve. No effect on an image.
     *
     * @throws IllegalArgumentException if a component is outside 0..255
     */
    public CursorStyle withFillColor(int r, int g, int b) {
        return new CursorStyle(shape, image, size, new CurveColor(r, g, b), outlineColor, outlineWidth);
    }

    /** Same shape filled with the color of its curve (default). */
    public CursorStyle withCurveColor() {
        return new CursorStyle(shape, image, size, null, outlineColor, outlineWidth);
    }

    /**
     * Same shape outlined with this color (components between 0 and 255) and
     * this line width, in pixels. No effect on an image.
     *
     * @throws IllegalArgumentException if a component is outside 0..255 or if the width is not strictly positive
     */
    public CursorStyle withOutline(int r, int g, int b, float width) {
        if (!(width > 0f) || Float.isInfinite(width)) {
            throw new IllegalArgumentException("The outline width must be strictly positive.");
        }
        return new CursorStyle(shape, image, size, fillColor, new CurveColor(r, g, b), width);
    }

    /** Same shape without outline (default). */
    public CursorStyle withoutOutline() {
        return new CursorStyle(shape, image, size, fillColor, null, 0f);
    }

    // ------------------------------------------------------------------
    // Characteristics

    /** True if the symbol is an image, false if it is a geometric shape. */
    public boolean isImage() { return image != null; }

    /** Geometric shape, or null for an image. */
    public CursorShape getShape() { return shape; }

    /** Width of the square containing the symbol, in pixels. */
    public float getSize() { return size; }

    /** Fill color {r, g, b} (0..255, copy), or null if the shape takes the color of its curve. */
    public int[] getFillColor() { return (fillColor != null) ? fillColor.toRGB() : null; }

    /** Outline color {r, g, b} (0..255, copy), or null if the shape is not outlined. */
    public int[] getOutlineColor() { return (outlineColor != null) ? outlineColor.toRGB() : null; }

    /** Outline width in pixels (0 without outline). */
    public float getOutlineWidth() { return outlineWidth; }

    /** Natural width of the image in pixels (0 for a shape). */
    public int getImageWidth() { return (image != null) ? image.width : 0; }

    /** Natural height of the image in pixels (0 for a shape). */
    public int getImageHeight() { return (image != null) ? image.height : 0; }

    // Rendering (internal)
    ImageRaster imageRaster() { return image; }
    CurveColor fillColor() { return fillColor; }
    CurveColor outlineColor() { return outlineColor; }
}
