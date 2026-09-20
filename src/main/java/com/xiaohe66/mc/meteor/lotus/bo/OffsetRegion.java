package com.xiaohe66.mc.meteor.lotus.bo;

public final class OffsetRegion {
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public OffsetRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public int getMinX() {
        return this.minX;
    }

    public int getMinY() {
        return this.minY;
    }

    public int getMinZ() {
        return this.minZ;
    }

    public int getMaxX() {
        return this.maxX;
    }

    public int getMaxY() {
        return this.maxY;
    }

    public int getMaxZ() {
        return this.maxZ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OffsetRegion)) {
            return false;
        }
        OffsetRegion that = (OffsetRegion)o;
        return this.minX == that.minX && this.minY == that.minY && this.minZ == that.minZ
            && this.maxX == that.maxX && this.maxY == that.maxY && this.maxZ == that.maxZ;
    }

    @Override
    public int hashCode() {
        int result = this.minX;
        result = 31 * result + this.minY;
        result = 31 * result + this.minZ;
        result = 31 * result + this.maxX;
        result = 31 * result + this.maxY;
        return 31 * result + this.maxZ;
    }
}
