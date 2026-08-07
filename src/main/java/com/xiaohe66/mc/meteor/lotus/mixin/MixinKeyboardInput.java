package com.xiaohe66.mc.meteor.lotus.mixin;

import com.xiaohe66.mc.meteor.lotus.event.KeyboardInputTickEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.input.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class MixinKeyboardInput extends Input {
   @Inject(method = "tick", at = @At("TAIL"))
   private void isPressed(CallbackInfo ci) {
      KeyboardInputTickEvent event = new KeyboardInputTickEvent();
      event.setPlayerInput(this.playerInput);
      MeteorClient.EVENT_BUS.post(event);
      this.playerInput = event.getPlayerInput();
   }
}
