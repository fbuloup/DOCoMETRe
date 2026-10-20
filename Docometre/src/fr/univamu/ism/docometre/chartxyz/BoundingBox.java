package fr.univamu.ism.docometre.chartxyz;

/**
 * Boîte englobante 3D immuable, avec son centre et son rayon (demi-diagonale,
 * au moins 0,001 pour éviter une caméra dégénérée sur des données plates).
 */
public final class BoundingBox {

    /** Boîte par défaut, de -1 à 1 sur chaque axe (données absentes). */
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

    /** Plus petite boîte contenant celle-ci et other (other peut être null). */
    public BoundingBox union(BoundingBox other) {
        if (other == null) return this;
        return new BoundingBox(Math.min(minX, other.minX), Math.max(maxX, other.maxX),
                               Math.min(minY, other.minY), Math.max(maxY, other.maxY),
                               Math.min(minZ, other.minZ), Math.max(maxZ, other.maxZ));
    }
}
