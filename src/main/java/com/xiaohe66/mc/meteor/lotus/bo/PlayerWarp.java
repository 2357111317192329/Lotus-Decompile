/*
 * Decompiled with CFR 0.152.
 */
package com.xiaohe66.mc.meteor.lotus.bo;

public class PlayerWarp {
    private String name;
    private float distance;

    public PlayerWarp(String name, float distance) {
        this.name = name;
        this.distance = distance;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getDistance() {
        return this.distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }
}

