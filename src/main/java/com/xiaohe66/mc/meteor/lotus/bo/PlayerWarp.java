/*
 * Decompiled with CFR 0.152.
 */
package com.xiaohe66.mc.meteor.lotus.bo;

import net.minecraft.world.item.Item;

public class PlayerWarp {
    private String name;
    private Item item;
    private float distance;

    public PlayerWarp(String name, Item item, float distance) {
        this.name = name;
        this.item = item;
        this.distance = distance;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Item getItem() {
        return this.item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public float getDistance() {
        return this.distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }
}