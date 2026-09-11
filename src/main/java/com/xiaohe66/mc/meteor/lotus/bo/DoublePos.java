/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Vec3i
 */
package com.xiaohe66.mc.meteor.lotus.bo;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

public class DoublePos {
    private final BlockPos pos1;
    private final BlockPos pos2;

    public DoublePos(BlockPos pos1, BlockPos pos2) {
        if (pos1.compareTo((Vec3i)pos2) > 0) {
            this.pos1 = pos2;
            this.pos2 = pos1;
        } else {
            this.pos1 = pos1;
            this.pos2 = pos2;
        }
    }

    public BlockPos getPos1() {
        return this.pos1;
    }

    public BlockPos getPos2() {
        return this.pos2;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        DoublePos that = (DoublePos)o;
        return Objects.equals(this.pos1, that.pos1) && Objects.equals(this.pos2, that.pos2);
    }

    public int hashCode() {
        return Objects.hash(this.pos1, this.pos2);
    }
}

