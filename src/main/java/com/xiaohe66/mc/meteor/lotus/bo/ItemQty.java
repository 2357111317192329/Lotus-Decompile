/*
 * Decompiled with CFR 0.152.
 */
package com.xiaohe66.mc.meteor.lotus.bo;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;

public class ItemQty {
    private final ItemBo item;
    private int count;

    public ItemQty(ItemBo item) {
        this.item = item;
    }

    public ItemQty(ItemBo item, int count) {
        this.item = item;
        this.count = count;
    }

    public ItemBo getItem() {
        return this.item;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}

