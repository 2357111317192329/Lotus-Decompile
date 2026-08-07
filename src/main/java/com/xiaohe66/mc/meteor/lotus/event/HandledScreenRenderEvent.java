package com.xiaohe66.mc.meteor.lotus.event;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class HandledScreenRenderEvent extends Event {
   private static final HandledScreenRenderEvent INSTANCE = new HandledScreenRenderEvent();
   private DrawContext drawContext;
   private TextRenderer textRenderer;
   private int mouseX;
   private int mouseY;

   public HandledScreenRenderEvent() {
      super(Event.Stage.Post);
   }

   public static HandledScreenRenderEvent get(DrawContext drawContext, TextRenderer textRenderer, int mouseX, int mouseY) {
      INSTANCE.drawContext = drawContext;
      INSTANCE.textRenderer = textRenderer;
      INSTANCE.mouseX = mouseX;
      INSTANCE.mouseY = mouseY;
      return INSTANCE;
   }

   public DrawContext getDrawContext() {
      return this.drawContext;
   }

   public TextRenderer getTextRenderer() {
      return this.textRenderer;
   }

   public int getMouseX() {
      return this.mouseX;
   }

   public int getMouseY() {
      return this.mouseY;
   }
}
