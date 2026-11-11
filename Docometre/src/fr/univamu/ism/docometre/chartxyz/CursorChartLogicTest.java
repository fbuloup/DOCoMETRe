package fr.univamu.ism.docometre.chartxyz;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

/** Test without OpenGL of the 2D chart without history (CursorChart2DRenderer, CursorStyle). */
public class CursorChartLogicTest {
    static int failures = 0;
    static void check(String what, boolean ok) {
        System.out.println((ok ? "OK     " : "FAILED ") + what);
        if (!ok) failures++;
    }
    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-3; }

    static void frame(CursorChart2DRenderer r) {
        r.reshape(null, 0, 0, 800, 600);
        r.marginLeft = 50; r.marginBottom = 40; r.plotWidth = 700; r.plotHeight = 500;
        r.updateData();
        r.computeView();
    }

    public static void main(String[] args) throws Exception {
        // ---------------- Last sample only ----------------
        Curve2DData curve = new Curve2DData("hand", 1000);
        Curve2DData other = new Curve2DData("target", 1000);
        CursorChart2DRenderer r = new CursorChart2DRenderer(curve, other);
        check("a single sample kept per curve", curve.getCapacity() == 1 && other.getCapacity() == 1);
        curve.addSamples(new float[]{0, 1, 2}, new float[]{0, 1, 3});
        frame(r);
        check("snapshot = last sample received (2, 3)", curve.getSnapshotCount() == 1
                && near(curve.getSnapshotCoordinates()[0], 2) && near(curve.getSnapshotCoordinates()[1], 3));
        Curve2DData late = new Curve2DData("late", 1000);
        r.addCurve(late);
        check("curve added later: a single sample kept too", late.getCapacity() == 1);

        // ---------------- Dynamic scales: positions accumulated ----------------
        curve.addSamples(new float[]{-4}, new float[]{1});
        frame(r);
        check("view contains the current and the previous positions",
                r.viewMinX < -4 && r.viewMaxX > 2 && r.viewMinY < 1 && r.viewMaxY > 3);
        r.resetView();
        curve.addSamples(new float[]{10}, new float[]{10});
        frame(r);
        check("R: previous positions forgotten (view around (10, 10) only)", r.viewMinX > 2 && r.viewMinY > 3
                && r.viewMinX < 10 && r.viewMaxX > 10);
        curve.addSamples(new float[]{Float.NaN}, new float[]{0});
        frame(r);
        check("NaN position ignored by the bounds", r.viewMinX > 2 && r.viewMaxX < 20);

        // ---------------- Fixed scales ----------------
        r.setFixedScales(-1, 1, -2, 2);
        curve.addSamples(new float[]{50}, new float[]{50});
        frame(r);
        check("fixed scales kept whatever the position", near(r.viewMinX, -1) && near(r.viewMaxX, 1)
                && near(r.viewMinY, -2) && near(r.viewMaxY, 2));

        // ---------------- Styles ----------------
        check("default style: disk in the color of the curve", r.getCursorStyle(curve) == CursorStyle.DEFAULT
                && CursorStyle.DEFAULT.getShape() == CursorShape.CIRCLE && CursorStyle.DEFAULT.getFillColor() == null);
        CursorStyle square = CursorStyle.shape(CursorShape.SQUARE, 20).withFillColor(255, 0, 0).withOutline(0, 0, 255, 2);
        r.setCursorStyle(curve, square);
        check("own style of a curve", r.getCursorStyle(curve) == square && r.getCursorStyle(other) == CursorStyle.DEFAULT);
        CursorStyle star = CursorStyle.shape(CursorShape.STAR, 10);
        r.setCursorStyle(star);
        check("default style of the chart for the others", r.getCursorStyle(other) == star && r.getCursorStyle(curve) == square);
        r.setCursorStyle(curve, null);
        check("null: back to the default style of the chart", r.getCursorStyle(curve) == star && !r.hasOwnCursorStyle(curve));
        r.setCursorStyle(other, square);
        r.removeCurve(other);
        check("style removed with its curve", !r.hasOwnCursorStyle(other));
        boolean refused = false;
        try { r.setCursorStyle(other, square); } catch (IllegalArgumentException e) { refused = true; }
        check("style refused for a curve outside the chart", refused);

        check("copies: setters do not modify the original", square.getSize() == 20 && square.withSize(5).getSize() == 5
                && square.withoutOutline().getOutlineColor() == null && square.getOutlineWidth() == 2
                && square.withCurveColor().getFillColor() == null && square.getFillColor()[0] == 255);
        refused = false;
        try { CursorStyle.shape(CursorShape.CIRCLE, 0); } catch (IllegalArgumentException e) { refused = true; }
        check("size 0 refused", refused);
        refused = false;
        try { CursorStyle.image("/nonexistent/image.png"); } catch (IllegalArgumentException e) { refused = true; }
        check("missing image: IllegalArgumentException", refused);

        // ---------------- Shapes contained in their square ----------------
        boolean contained = true;
        for (CursorShape shape : CursorShape.values()) {
            float[] c = CursorChart2DRenderer.contour(shape, 10);
            float maxX = 0, maxY = 0;
            for (int k = 0; k < c.length; k += 2) {
                maxX = Math.max(maxX, Math.abs(c[k]));
                maxY = Math.max(maxY, Math.abs(c[k + 1]));
            }
            boolean ok = maxX <= 10.001f && maxY <= 10.001f && Math.max(maxX, maxY) > 9f;
            if (!ok) System.out.println("  " + shape + ": half extent " + maxX + " x " + maxY);
            contained &= ok;
        }
        check("each shape fills its square of side size", contained);

        // ---------------- Images ----------------
        ImageData rgb = new ImageData(2, 3, 24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
        rgb.setPixel(0, 0, 0xFF0000); // top left: red
        rgb.setAlpha(1, 2, 0);
        rgb.setAlpha(0, 0, 255);
        CursorStyle image = CursorStyle.image(rgb);
        check("image: natural size = largest dimension", image.isImage() && image.getSize() == 3
                && image.getImageWidth() == 2 && image.getImageHeight() == 3);
        CursorStyle.ImageRaster raster = image.imageRaster();
        // Rows from bottom to top: the top left pixel is in the last row
        int topLeft = (2 * 2 + 0) * 4;
        check("image: top left pixel red and opaque, rows from bottom to top",
                (raster.rgba[topLeft] & 0xFF) == 255 && raster.rgba[topLeft + 1] == 0 && (raster.rgba[topLeft + 3] & 0xFF) == 255);
        int bottomRight = (0 * 2 + 1) * 4;
        check("image: alpha channel kept", raster.rgba[bottomRight + 3] == 0);

        ImageData indexed = new ImageData(2, 1, 8, new PaletteData(new RGB(0, 0, 0), new RGB(0, 255, 0)));
        indexed.setPixel(0, 0, 1);
        indexed.transparentPixel = 0;
        CursorStyle.ImageRaster ir = CursorStyle.image(indexed).imageRaster();
        check("indexed image: palette color and transparent pixel",
                (ir.rgba[1] & 0xFF) == 255 && (ir.rgba[3] & 0xFF) == 255 && ir.rgba[7] == 0);

        System.out.println(failures == 0 ? "\nAll tests passed." : "\n" + failures + " failure(s).");
        System.exit(failures == 0 ? 0 : 1);
    }
}
