package com.xiaohe66.mc.meteor.lotus.mixin;

import com.xiaohe66.mc.meteor.lotus.event.ScreenRenderEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
   @Shadow
   public abstract TextRenderer getTextRenderer();
   @Inject(method = "render", at = @At("RETURN"))
   private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      MeteorClient.EVENT_BUS.post(ScreenRenderEvent.get(context, this.getTextRenderer(), mouseX, mouseY));
   }
}
