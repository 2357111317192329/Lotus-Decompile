/*
 * Decompiled with CFR 0.152.
 */
package com.xiaohe66.mc.meteor.lotus.util;

public class HeRotation {
    private float yaw;
    private float pitch;

    public HeRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
}
