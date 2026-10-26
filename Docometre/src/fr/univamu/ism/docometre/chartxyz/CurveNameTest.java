package fr.univamu.ism.docometre.chartxyz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Test without OpenGL of the curve names: mandatory, unique in a chart, lookup and renaming. */
public class CurveNameTest {
    static int failures = 0;
    static void check(String what, boolean ok) {
        System.out.println((ok ? "OK     " : "FAILED ") + what);
        if (!ok) failures++;
    }
    static boolean throwsIAE(Runnable action) {
        try { action.run(); return false; } catch (IllegalArgumentException e) { return true; }
    }

    public static void main(String[] args) throws Exception {
        // Mandatory name
        check("1D: null name refused", throwsIAE(() -> new Curve1DData(null, 100)));
        check("1D: blank name refused", throwsIAE(() -> new Curve1DData("  ", 100)));
        check("1D static: empty name refused", throwsIAE(() -> new Curve1DData("", 100, new float[]{1, 2})));
        check("2D: blank name refused", throwsIAE(() -> new Curve2DData(" ", 100)));
        check("3D: null name refused", throwsIAE(() -> new Curve3DData(null, 100,
                Arrays.asList(new Point3D(0, 0, 0), new Point3D(1, 1, 1)))));
        check("Name given to the constructor", "sine".equals(new Curve1DData("sine", 100).getName()));

        // Static 2D / 3D curves: real frequency, duration (N - 1) / f, at least two points
        Curve2DData line = new Curve2DData("line", 50, Arrays.asList(new Point2D(0, 0), new Point2D(1, 1), new Point2D(2, 0)));
        check("2D static: duration (3 - 1) / 50 = 0.04 s, 3 points kept",
                Math.abs(line.getWindowTimeWidth() - 0.04) < 1e-12 && line.getCapacity() == 3 && line.getPoints().size() == 3);
        check("2D static: a single point refused", throwsIAE(() -> new Curve2DData("p", 50, Arrays.asList(new Point2D(0, 0)))));
        check("3D static: zero frequency refused", throwsIAE(() -> new Curve3DData("p", 0,
                Arrays.asList(new Point3D(0, 0, 0), new Point3D(1, 1, 1)))));

        // Unique name in a chart
        Curve1DData a = new Curve1DData("a", 100), b = new Curve1DData("b", 100);
        Chart1DRenderer r = new Chart1DRenderer(5, a, b);
        check("getCurve by name", r.getCurve("a") == a && r.getCurve("b") == b && r.getCurve("c") == null && r.getCurve(null) == null);
        Curve1DData otherA = new Curve1DData("a", 100);
        check("Adding a curve with an existing name refused", throwsIAE(() -> r.addCurve(otherA)));
        check("... nothing changed: curve not attached, chart unchanged",
                !otherA.isAttached() && r.getCurves().equals(Arrays.asList(a, b)) && r.getCurve("a") == a);
        check("Names are compared exactly (case)", addOk(r, new Curve1DData("A", 100)) && r.getCurvesCount() == 3);
        r.removeCurve(r.getCurve("A"));

        Curve1DData x1 = new Curve1DData("x", 100), x2 = new Curve1DData("x", 100), y = new Curve1DData("y", 100);
        check("Chart constructor with two curves of the same name refused",
                throwsIAE(() -> new Chart1DRenderer(5, y, x1, x2)));
        check("... no curve kept attached", !x1.isAttached() && !x2.isAttached() && !y.isAttached());

        check("removeCurve(curve): true, then false", r.removeCurve(b) && !r.removeCurve(b));
        check("Removed curve: name available again", r.getCurve("b") == null && addOk(r, new Curve1DData("b", 100)));
        Curve1DData newB = r.getCurve("b");
        check("removeCurve(name)", r.removeCurve("b") && !newB.isAttached() && r.getCurve("b") == null && !r.removeCurve("b"));
        r.addCurve(b);

        // Renaming
        check("Renaming to the name of another curve of the chart refused", throwsIAE(() -> b.setName("a")));
        check("... name unchanged", "b".equals(b.getName()) && r.getCurve("b") == b && r.getCurve("a") == a);
        b.setName("b");
        check("Renaming to its own name: no change, no exception", "b".equals(b.getName()) && r.getCurve("b") == b);
        b.setName("c");
        check("Renaming: getCurve follows, old name available",
                r.getCurve("c") == b && r.getCurve("b") == null && "c".equals(b.getName()));
        check("Old name available for another curve", addOk(r, new Curve1DData("b", 100)));
        check("Blank name refused by setName", throwsIAE(() -> b.setName(" ")) && "c".equals(b.getName()));
        r.removeCurve(b);
        b.setName("a");
        check("Curve outside a chart: renamed without check", "a".equals(b.getName()));
        check("... then refused by the chart where \"a\" exists", throwsIAE(() -> r.addCurve(b)) && !b.isAttached());
        check("A curve leaves the chart: its name is free", r.removeCurve("a") && addOk(r, b) && r.getCurve("a") == b);

        // Concurrency: additions with the same name
        for (int round = 0; round < 20; round++) {
            Chart1DRenderer rc = new Chart1DRenderer(5);
            List<Curve1DData> candidates = new ArrayList<>();
            for (int i = 0; i < 8; i++) candidates.add(new Curve1DData("same", 100));
            AtomicInteger added = new AtomicInteger(), refused = new AtomicInteger();
            runTogether(candidates.size(), i -> {
                try { rc.addCurve(candidates.get(i)); added.incrementAndGet(); }
                catch (IllegalArgumentException e) { refused.incrementAndGet(); }
            });
            if (added.get() != 1 || refused.get() != 7 || rc.getCurvesCount() != 1) {
                check("Concurrent additions with the same name: round " + round, false);
            }
        }
        check("Concurrent additions with the same name (20 rounds x 8 threads): one addition each time", true);

        // Concurrency: renamings to the same name
        boolean renamesOk = true;
        for (int round = 0; round < 20; round++) {
            List<Curve1DData> curves = new ArrayList<>();
            for (int i = 0; i < 8; i++) curves.add(new Curve1DData("c" + i, 100));
            Chart1DRenderer rc = new Chart1DRenderer(5, curves.toArray(new Curve1DData[0]));
            AtomicInteger renamed = new AtomicInteger();
            runTogether(curves.size(), i -> {
                try { curves.get(i).setName("target"); renamed.incrementAndGet(); }
                catch (IllegalArgumentException e) { /* refused */ }
            });
            long named = curves.stream().filter(c -> "target".equals(c.getName())).count();
            if (renamed.get() != 1 || named != 1 || rc.getCurve("target") == null
                    || !"target".equals(rc.getCurve("target").getName())) renamesOk = false;
        }
        check("Concurrent renamings to the same name (20 rounds x 8 threads): one renaming each time", renamesOk);

        // Concurrency: additions, removals and renamings mixed (no deadlock, consistent index)
        Chart1DRenderer rm = new Chart1DRenderer(5);
        List<Curve1DData> pool = new ArrayList<>();
        for (int i = 0; i < 6; i++) pool.add(new Curve1DData("n" + i, 100));
        long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(1500);
        boolean finished = runTogether(6, i -> {
            java.util.Random random = new java.util.Random(i);
            while (System.nanoTime() < end) {
                Curve1DData c = pool.get(random.nextInt(pool.size()));
                try {
                    switch (random.nextInt(3)) {
                        case 0: rm.addCurve(c); break;
                        case 1: rm.removeCurve(c); break;
                        default: c.setName("n" + random.nextInt(8)); break;
                    }
                } catch (IllegalArgumentException | IllegalStateException e) { /* refused: expected */ }
            }
        });
        boolean consistent = true;
        java.util.Set<String> names = new java.util.HashSet<>();
        for (Curve1DData c : rm.getCurves()) {
            consistent &= names.add(c.getName()) && rm.getCurve(c.getName()) == c && c.isAttached();
        }
        for (Curve1DData c : pool) consistent &= c.isAttached() == rm.getCurves().contains(c);
        for (int k = 0; k < 8; k++) { // no stale entry in the index
            Curve1DData c = rm.getCurve("n" + k);
            if (c != null) consistent &= rm.getCurves().contains(c) && c.getName().equals("n" + k);
        }
        check("Mixed additions / removals / renamings (6 threads, 1.5 s): no deadlock, unique names, consistent index",
                finished && consistent);

        System.out.println(failures == 0 ? "\nAll tests passed." : "\n" + failures + " failure(s).");
        System.exit(failures == 0 ? 0 : 1);
    }

    static boolean addOk(Chart1DRenderer r, Curve1DData curve) {
        try { r.addCurve(curve); return true; } catch (IllegalArgumentException e) { return false; }
    }

    interface Task { void run(int index) throws Exception; }

    /** Runs n tasks started at the same time; false if they did not finish within 10 s (deadlock). */
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
