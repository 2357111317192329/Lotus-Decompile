package com.xiaohe66.mc.meteor.lotus.mixin;

import com.xiaohe66.mc.meteor.lotus.event.HeOpenScreenEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignEditScreen.class)
public abstract class MixinAbstractSignEditScreen extends Screen {
   @Shadow
   private SignText text;
   @Shadow
   @Final
   private String[] messages;

   protected MixinAbstractSignEditScreen(Component textComponent) {
      super(textComponent);
   }

   @Inject(method = "init", at = @At("RETURN"))
   private void preventGuiOpen(CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      HeOpenScreenEvent event = HeOpenScreenEvent.get(mc.screen);
      MeteorClient.EVENT_BUS.post(event);
      if (event.getSignText() != null) {
         this.text = event.getSignText();

         for (int i = 0; i < this.messages.length; i++) {
            this.messages[i] = this.text.getMessage(i, false).getString();
         }
      }
   }
}
