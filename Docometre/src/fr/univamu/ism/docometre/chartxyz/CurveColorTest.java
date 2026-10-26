package fr.univamu.ism.docometre.chartxyz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Test without OpenGL of the curve colors: integer API, palette, weights per chart. */
public class CurveColorTest {
    static int failures = 0;
    static void check(String what, boolean ok) {
        System.out.println((ok ? "OK     " : "FAILED ") + what);
        if (!ok) failures++;
    }
    static boolean throwsIAE(Runnable action) {
        try { action.run(); return false; } catch (IllegalArgumentException e) { return true; }
    }
    static int[] palette(int i) { return CurveStyles.getPaletteColor(i); }
    static boolean is(Curve1DData curve, int paletteIndex) { return Arrays.equals(curve.getColor(), palette(paletteIndex)); }
    static Curve1DData curve(String name) { return new Curve1DData(name, 100); }

    /** The weights of the chart are the number of its curves displayed in each palette color. */
    static boolean weightsConsistent(CurveChartRenderer<?> r) {
        int[] expected = new int[CurveStyles.getPaletteSize()];
        for (CurveData<?> c : r.getCurves()) {
            int i = CurveStyles.paletteIndex(c.color());
            if (i >= 0) expected[i]++;
        }
        return Arrays.equals(expected, r.getColorWeights());
    }

    public static void main(String[] args) throws Exception {
        // Palette and integer API
        check("Palette of " + CurveStyles.getPaletteSize() + " colors (at least 3), first one blue (89, 191, 255)",
                CurveStyles.getPaletteSize() >= 3 && Arrays.equals(palette(0), new int[]{89, 191, 255}));
        int[] copy = palette(0);
        copy[0] = 0;
        check("getPaletteColor returns a copy", palette(0)[0] == 89);
        Curve1DData free = curve("free");
        check("Curve in automatic color outside a chart: getColor() null", free.getColor() == null && free.isAutomaticColor());
        check("setColor: component > 255 refused", throwsIAE(() -> free.setColor(256, 0, 0)));
        check("setColor: negative component refused", throwsIAE(() -> free.setColor(0, -1, 0)));
        free.setColor(10, 20, 30);
        check("setColor outside a chart", Arrays.equals(free.getColor(), new int[]{10, 20, 30}) && !free.isAutomaticColor());
        free.getColor()[0] = 99;
        check("getColor returns a copy", free.getColor()[0] == 10);
        check("OpenGL components = component / 255", Math.abs(CurveStyles.colorOf(free)[1] - 20 / 255f) < 1e-7);
        free.setAutomaticColor();
        check("setAutomaticColor outside a chart: color chosen later", free.getColor() == null && free.isAutomaticColor());

        // First curves: palette order, as before
        Curve1DData a = curve("a"), b = curve("b"), c = curve("c");
        Chart1DRenderer r = new Chart1DRenderer(5, a, b, c);
        check("Three curves: blue, orange, green", is(a, 0) && is(b, 1) && is(c, 2) && weightsConsistent(r));

        // Removal in the middle: the others keep their color, the free color is reused
        r.removeCurve(b);
        check("Removing b: a and c keep their color", is(a, 0) && is(c, 2) && weightsConsistent(r));
        check("Removed curve in automatic color: no color any more", b.getColor() == null && b.isAutomaticColor());
        Curve1DData d = curve("d");
        r.addCurve(d);
        check("New curve d: orange, the free color of minimal weight", is(d, 1) && weightsConsistent(r));

        // One curve more than the palette: all the weights are 1, first color of the palette
        int paletteSize = CurveStyles.getPaletteSize();
        List<Curve1DData> more = new ArrayList<>();
        for (int i = 0; i < paletteSize - 3; i++) { Curve1DData m = curve("m" + i); more.add(m); r.addCurve(m); }
        int[] ones = new int[paletteSize];
        Arrays.fill(ones, 1);
        check(paletteSize + " curves: the " + paletteSize + " colors, once each", Arrays.equals(r.getColorWeights(), ones));
        Curve1DData ninth = curve("ninth");
        r.addCurve(ninth);
        check("One more curve: blue (all the weights equal)", is(ninth, 0) && r.getColorWeights()[0] == 2);

        // setColor towards a palette color: weights follow, even if the curve is not in automatic color
        r.removeCurve(ninth);
        c.setColor(89, 191, 255); // green -> blue (palette)
        check("setColor to a palette color: green weight -1, blue weight +1",
                r.getColorWeights()[2] == 0 && r.getColorWeights()[0] == 2 && !c.isAutomaticColor() && weightsConsistent(r));
        Curve1DData e = curve("e");
        r.addCurve(e);
        check("Next automatic curve: green (weight 0), not blue (weight 2)", is(e, 2) && weightsConsistent(r));
        c.setColor(1, 2, 3); // blue (explicit) -> outside the palette
        check("setColor outside the palette: blue weight -1, no other weight", r.getColorWeights()[0] == 1 && weightsConsistent(r));
        c.setColor(1, 2, 3);
        check("Same color again: weights unchanged", weightsConsistent(r));
        c.setAutomaticColor();
        check("setAutomaticColor: color of minimal weight (blue, weight 1, first of the ties)",
                c.isAutomaticColor() && is(c, 0) && weightsConsistent(r));

        // Removal of a curve with an explicit palette color
        d.setColor(255, 153, 51); // orange given explicitly (it was orange automatically): unchanged weights
        int[] before = r.getColorWeights();
        r.removeCurve(d);
        check("Removing a curve with an explicit palette color: weight -1",
                r.getColorWeights()[1] == before[1] - 1 && weightsConsistent(r));
        check("Explicit color kept outside the chart", Arrays.equals(d.getColor(), new int[]{255, 153, 51}) && !d.isAutomaticColor());
        Chart1DRenderer other = new Chart1DRenderer(5, d);
        check("Added to another chart with its explicit color: counted there", other.getColorWeights()[1] == 1 && is(d, 1));

        // A color read and given back keeps the weights right
        int[] read = a.getColor();
        a.setColor(read[0], read[1], read[2]);
        check("getColor() given back to setColor(): same palette color, weights unchanged", is(a, 0) && weightsConsistent(r));

        // Chart constructor refused: colors released
        Curve1DData x1 = curve("x"), x2 = curve("x");
        throwsIAE(() -> new Chart1DRenderer(5, x1, x2));
        check("Chart creation refused: the automatic color of the attached curve is released", x1.getColor() == null);

        // Concurrency: additions, removals and color changes from several threads
        Chart1DRenderer rc = new Chart1DRenderer(5);
        List<Curve1DData> pool = new ArrayList<>();
        for (int i = 0; i < 12; i++) pool.add(curve("p" + i));
        long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(1500);
        boolean finished = runTogether(6, i -> {
            Random random = new Random(i);
            while (System.nanoTime() < end) {
                Curve1DData p = pool.get(random.nextInt(pool.size()));
                try {
                    switch (random.nextInt(5)) {
                        case 0: rc.addCurve(p); break;
                        case 1: rc.removeCurve(p); break;
                        case 2: p.setAutomaticColor(); break;
                        case 3: int[] q = palette(random.nextInt(8)); p.setColor(q[0], q[1], q[2]); break;
                        default: p.setColor(random.nextInt(256), random.nextInt(256), random.nextInt(256)); break;
                    }
                } catch (IllegalStateException ex) { /* already in the chart: expected */ }
            }
        });
        boolean automaticColored = true;
        for (Curve1DData p : rc.getCurves()) automaticColored &= p.getColor() != null;
        check("Concurrent additions / removals / color changes (6 threads, 1.5 s): no deadlock, weights = curves per color",
                finished && weightsConsistent(rc) && automaticColored);

        System.out.println(failures == 0 ? "\nAll tests passed." : "\n" + failures + " failure(s).");
        System.exit(failures == 0 ? 0 : 1);
    }

    interface Task { void run(int index) throws Exception; }

    static boolean runTogether(int n, Task task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < n; i++) {
            int index = i;
            executor.execute(() -> {
                try { start.await(); task.run(index); } catch (Exception e) { e.printStackTrace(); }
            });
        }
        start.countDown();
        executor.shutdown();
        return executor.awaitTermination(10, TimeUnit.SECONDS);
    }
}
