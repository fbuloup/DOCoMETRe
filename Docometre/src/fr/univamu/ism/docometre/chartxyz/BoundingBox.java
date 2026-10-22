package fr.univamu.ism.docometre.chartxyz;

/**
 * Immutable 3D bounding box, with its center and its radius (half diagonal,
 * at least 0.001 to avoid a degenerate camera on flat data).
 */
public final class BoundingBox {

    /** Default box, from -1 to 1 on each axis (no data). */
    public static final BoundingBox DEFAULT = new BoundingBox(-1f, 1f, -1f, 1f, -1f, 1f);

    public final float minX, maxX, minY, maxY, minZ, maxZ;
    public final float centerX, centerY, centerZ;
    public final float radius;

    public BoundingBox(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        this.minX = minX; this.maxX = maxX;
        this.minY = minY; this.maxY = maxY;
        this.minZ = minZ; this.maxZ = maxZ;
        this.centerX = (minX + maxX) / 2f;
        this.centerY = (minY + maxY) / 2f;
        this.centerZ = (minZ + maxZ) / 2f;
        float dx = maxX - minX, dy = maxY - minY, dz = maxZ - minZ;
        this.radius = Math.max((float) Math.sqrt(dx * dx + dy * dy + dz * dz) / 2f, 0.001f);
    }

    /** Smallest box containing this one and other (other can be null). */
    public BoundingBox union(BoundingBox other) {
        if (other == null) return this;
        return new BoundingBox(Math.min(minX, other.minX), Math.max(maxX, other.maxX),
                               Math.min(minY, other.minY), Math.max(maxY, other.maxY),
                               Math.min(minZ, other.minZ), Math.max(maxZ, other.maxZ));
    }
}
