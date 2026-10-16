package fr.univamu.ism.docometre.chartxyz;

import java.util.Objects;

import org.eclipse.swt.SWT;

/**
 * Police de caractères utilisée par un graphe (graduations, noms d'axes,
 * légende) : famille, taille en points et style. Immuable.
 */
public final class ChartFont {

    /** Famille de police : null = police du système. */
    private final String family;
    /** Taille en points. */
    private final int size;
    /** Style SWT : SWT.NORMAL, SWT.BOLD, SWT.ITALIC ou SWT.BOLD | SWT.ITALIC. */
    private final int style;

    /**
     * @param family famille ("Arial", "Helvetica", "Times New Roman", "Courier New"...),
     *               ou null pour la police du système ; une famille absente du
     *               système est remplacée par une police par défaut
     * @param size   taille en points (strictement positive)
     * @param style  SWT.NORMAL, SWT.BOLD, SWT.ITALIC ou SWT.BOLD | SWT.ITALIC
     */
    public ChartFont(String family, int size, int style) {
        if (size <= 0) throw new IllegalArgumentException("La taille de police doit être strictement positive.");
        this.family = family;
        this.size = size;
        this.style = style & (SWT.BOLD | SWT.ITALIC);
    }

    /** Police du système, à la taille et au style donnés. */
    public ChartFont(int size, int style) {
        this(null, size, style);
    }

    public String getFamily() { return family; }
    public int getSize() { return size; }
    public int getStyle() { return style; }

    /** Même famille et même style, autre taille. */
    public ChartFont withSize(int newSize) { return new ChartFont(family, newSize, style); }
    /** Même famille et même taille, autre style. */
    public ChartFont withStyle(int newStyle) { return new ChartFont(family, size, newStyle); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChartFont)) return false;
        ChartFont other = (ChartFont) o;
        return size == other.size && style == other.style && Objects.equals(family, other.family);
    }

    @Override
    public int hashCode() { return Objects.hash(family, size, style); }

    @Override
    public String toString() {
        return (family != null ? family : "<système>") + " " + size + "pt"
                + ((style & SWT.BOLD) != 0 ? " gras" : "") + ((style & SWT.ITALIC) != 0 ? " italique" : "");
    }
}
