package com.xiaohe66.mc.meteor.lotus.bo;

import net.minecraft.world.item.Item;

public class ItemWarp {
   private Item item;
   private int count;
   private float minDistance = Float.MAX_VALUE;

   public Item getItem() {
      return this.item;
   }

   public void setItem(Item item) {
      this.item = item;
   }

   public int getCount() {
      return this.count;
   }

   public void setCount(int count) {
      this.count = count;
   }

   public float getMinDistance() {
      return this.minDistance;
   }

   public void setMinDistance(float minDistance) {
      this.minDistance = minDistance;
   }
}
