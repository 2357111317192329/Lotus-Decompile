package com.xiaohe66.mc.meteor.lotus.event;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public class DrawItemEvent extends Event {
   private static final DrawItemEvent INSTANCE = new DrawItemEvent();
   private GuiGraphicsExtractor drawContext;
   private ItemStack itemStack;
   private int x;
   private int y;

   public static DrawItemEvent get(GuiGraphicsExtractor drawContext, ItemStack stack, int x, int y) {
      INSTANCE.drawContext = drawContext;
      INSTANCE.itemStack = stack;
      INSTANCE.x = x;
      INSTANCE.y = y;
      return INSTANCE;
   }

   public DrawItemEvent() {
      super(Event.Stage.Post);
   }

   public GuiGraphicsExtractor getDrawContext() {
      return this.drawContext;
   }

   public ItemStack getItemStack() {
      return this.itemStack;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }
}
