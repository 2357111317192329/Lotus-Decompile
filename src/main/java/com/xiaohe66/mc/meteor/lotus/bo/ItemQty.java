/*
 * Decompiled with CFR 0.152.
 */
package com.xiaohe66.mc.meteor.lotus.bo;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class ItemQty {
    private final ItemBo item;
    private int count;

    public ItemQty(ItemStack stack) {
        this(new ItemBo(stack), stack.getCount());
    }

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

    @Override
    public boolean equals(Object o) {
        if (o != null && this.getClass() == o.getClass()) {
            ItemQty itemQty = (ItemQty) o;
            return this.count == itemQty.count && Objects.equals(this.item, itemQty.item);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(this.item);
        return 31 * result + this.count;
    }
}

