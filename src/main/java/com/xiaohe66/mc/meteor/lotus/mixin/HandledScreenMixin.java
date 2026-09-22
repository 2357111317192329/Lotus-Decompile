/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.MeteorClient
 *  meteordevelopment.orbit.ICancellable
 *  net.minecraft.client.gui.Click
 *  net.minecraft.screen.ScreenHandler
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider
 *  net.minecraft.client.gui.screen.ingame.HandledScreen
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.xiaohe66.mc.meteor.lotus.mixin;

import com.xiaohe66.mc.meteor.lotus.event.ScreenCloseEvent;
import com.xiaohe66.mc.meteor.lotus.event.DrawMouseoverTooltipEvent;
import com.xiaohe66.mc.meteor.lotus.event.HandledScreenRenderEvent;
import com.xiaohe66.mc.meteor.lotus.event.IsPointOverSlotEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseClickEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseDragEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseReleaseEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseScrollEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.orbit.ICancellable;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={HandledScreen.class})
public abstract class HandledScreenMixin<T extends ScreenHandler>
implements ScreenHandlerProvider<T> {
    @Shadow
    protected int x;
    @Shadow
    protected int y;
    @Shadow
    protected Slot focusedSlot;

    @Inject(
        method = "renderMain",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlots(Lnet/minecraft/client/gui/DrawContext;)V", shift = Shift.AFTER)
    )
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen screen = (Screen)(Object)this;
        MeteorClient.EVENT_BUS.post(HandledScreenRenderEvent.get(context, screen.getTextRenderer(), mouseX, mouseY, this.focusedSlot));
    }

    @Inject(method = "renderMain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;getSlotAt(DD)Lnet/minecraft/screen/slot/Slot;", shift = Shift.AFTER))
    private void onSlotHover(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Slot slot = this.focusedSlot;
        if (slot == null) {
            return;
        }
        IsPointOverSlotEvent event = IsPointOverSlotEvent.get(slot, mouseX, mouseY);
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            this.focusedSlot = event.getResult() ? slot : null;
        }
    }

    @Inject(method={"drawMouseoverTooltip(Lnet/minecraft/client/gui/DrawContext;II)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void onDrawMouseoverTooltip(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        DrawMouseoverTooltipEvent event = DrawMouseoverTooltipEvent.get(context, mouseX, mouseY);
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method={"mouseScrolled"}, at={@At(value="HEAD")}, cancellable=true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        MouseScrollEvent event = MouseScrollEvent.get(mouseX, mouseY, verticalAmount, this.x, this.y);
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method={"mouseClicked"}, at={@At(value="HEAD")}, cancellable=true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        MouseClickEvent event = MouseClickEvent.get(mouseX, mouseY, button, false, this.x, this.y);
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method={"mouseReleased"}, at={@At(value="HEAD")}, cancellable=true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        MouseReleaseEvent event = MouseReleaseEvent.get(mouseX, mouseY, button, this.x, this.y);
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method={"mouseDragged"}, at={@At(value="HEAD")}, cancellable=true)
    private void onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        MouseDragEvent event = MouseDragEvent.get(mouseX, mouseY, button, deltaX, deltaY, this.x, this.y);
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method={"close"}, at={@At(value="HEAD")})
    private void onClose(CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(ScreenCloseEvent.get());
    }

    @Inject(method={"removed"}, at={@At(value="HEAD")})
    private void onRemoved(CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(ScreenCloseEvent.get());
    }
}

