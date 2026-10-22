package fr.univamu.ism.docometre.chartxyz;

import java.util.Objects;

import org.eclipse.swt.SWT;

/**
 * Font used by a chart (tick labels, axis names, legend): family, size in
 * points and style. Immutable.
 */
public final class ChartFont {

    /** Font family: null = system font. */
    private final String family;
    /** Size in points. */
    private final int size;
    /** SWT style: SWT.NORMAL, SWT.BOLD, SWT.ITALIC or SWT.BOLD | SWT.ITALIC. */
    private final int style;

    /**
     * @param family family ("Arial", "Helvetica", "Times New Roman", "Courier New"...),
     *               or null for the system font; a family missing from the
     *               system is replaced by a default font
     * @param size   size in points (strictly positive)
     * @param style  SWT.NORMAL, SWT.BOLD, SWT.ITALIC or SWT.BOLD | SWT.ITALIC
     */
    public ChartFont(String family, int size, int style) {
        if (size <= 0) throw new IllegalArgumentException("The font size must be strictly positive.");
        this.family = family;
        this.size = size;
        this.style = style & (SWT.BOLD | SWT.ITALIC);
    }

    /** System font, with the given size and style. */
    public ChartFont(int size, int style) {
        this(null, size, style);
    }

    public String getFamily() { return family; }
    public int getSize() { return size; }
    public int getStyle() { return style; }

    /** Same family and style, other size. */
    public ChartFont withSize(int newSize) { return new ChartFont(family, newSize, style); }
    /** Same family and size, other style. */
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
        return (family != null ? family : "<system>") + " " + size + "pt"
                + ((style & SWT.BOLD) != 0 ? " bold" : "") + ((style & SWT.ITALIC) != 0 ? " italic" : "");
    }
}
