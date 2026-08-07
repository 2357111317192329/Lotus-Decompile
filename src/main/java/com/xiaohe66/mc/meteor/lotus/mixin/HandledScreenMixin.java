package com.xiaohe66.mc.meteor.lotus.mixin;

import com.xiaohe66.mc.meteor.lotus.event.HandledScreenRenderEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseClickEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseDragEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseReleaseEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseScrollEvent;
import com.xiaohe66.mc.meteor.lotus.event.ScreenCloseEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin<T extends AbstractContainerMenu> implements MenuAccess<T> {
   @Shadow
   protected int leftPos;
   @Shadow
   protected int topPos;

   @Inject(
      method = "extractContents",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlots(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V", shift = Shift.AFTER)
   )
   private void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      Screen screen = (Screen)(Object)this;
      MeteorClient.EVENT_BUS.post(HandledScreenRenderEvent.get(context, screen.getFont(), mouseX, mouseY));
   }

   @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
   private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
      MouseScrollEvent event = MouseScrollEvent.get(mouseX, mouseY, verticalAmount, this.leftPos, this.topPos);
      MeteorClient.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         cir.setReturnValue(true);
         cir.cancel();
      }
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void onMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
      MouseClickEvent event = MouseClickEvent.get(click.x(), click.y(), click.button(), doubled, this.leftPos, this.topPos);
      MeteorClient.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         cir.setReturnValue(true);
         cir.cancel();
      }
   }

   @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
   private void onMouseReleased(MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {
      MouseReleaseEvent event = MouseReleaseEvent.get(click.x(), click.y(), click.button(), this.leftPos, this.topPos);
      MeteorClient.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         cir.setReturnValue(true);
         cir.cancel();
      }
   }

   @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
   private void onMouseDragged(MouseButtonEvent click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
      MouseDragEvent event = MouseDragEvent.get(click.x(), click.y(), click.button(), offsetX, offsetY, this.leftPos, this.topPos);
      MeteorClient.EVENT_BUS.post(event);
      if (event.isCancelled()) {
         cir.setReturnValue(true);
         cir.cancel();
      }
   }

   @Inject(method = "onClose", at = @At("HEAD"))
   private void onClose(CallbackInfo ci) {
      MeteorClient.EVENT_BUS.post(ScreenCloseEvent.get());
   }

   @Inject(method = "removed", at = @At("HEAD"))
   private void onRemoved(CallbackInfo ci) {
      MeteorClient.EVENT_BUS.post(ScreenCloseEvent.get());
   }
}
