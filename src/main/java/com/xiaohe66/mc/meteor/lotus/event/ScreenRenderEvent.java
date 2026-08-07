package com.xiaohe66.mc.meteor.lotus.event;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ScreenRenderEvent extends Event {
   private static final ScreenRenderEvent INSTANCE = new ScreenRenderEvent();
   private GuiGraphicsExtractor drawContext;
   private Font textRenderer;
   private int mouseX;
   private int mouseY;

   public ScreenRenderEvent() {
      super(Event.Stage.Post);
   }

   public static ScreenRenderEvent get(GuiGraphicsExtractor drawContext, Font textRenderer, int mouseX, int mouseY) {
      INSTANCE.drawContext = drawContext;
      INSTANCE.textRenderer = textRenderer;
      INSTANCE.mouseX = mouseX;
      INSTANCE.mouseY = mouseY;
      return INSTANCE;
   }

   public GuiGraphicsExtractor getDrawContext() {
      return this.drawContext;
   }

   public Font getTextRenderer() {
      return this.textRenderer;
   }

   public int getMouseX() {
      return this.mouseX;
   }

   public int getMouseY() {
      return this.mouseY;
   }
}
