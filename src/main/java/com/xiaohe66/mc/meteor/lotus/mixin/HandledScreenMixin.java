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
import com.xiaohe66.mc.meteor.lotus.event.HandledScreenRenderEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseClickEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseDragEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseReleaseEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseScrollEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.orbit.ICancellable;
import net.minecraft.client.gui.Click;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={HandledScreen.class})
public abstract class HandledScreenMixin<T extends ScreenHandler>
implements ScreenHandlerProvider<T> {
    @Shadow
    protected int x;
    @Shadow
    protected int y;

    @Inject(method={"renderMain"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlots(Lnet/minecraft/client/gui/DrawContext;II)V", shift=At.Shift.AFTER)})
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        HandledScreen screen = (HandledScreen)(Object)this;
        MeteorClient.EVENT_BUS.post(HandledScreenRenderEvent.get(context, screen.getTextRenderer(), mouseX, mouseY));
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
    private void onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        MouseClickEvent event = MouseClickEvent.get(click.x(), click.y(), click.button(), doubled, this.x, this.y);
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method={"mouseReleased"}, at={@At(value="HEAD")}, cancellable=true)
    private void onMouseReleased(Click click, CallbackInfoReturnable<Boolean> cir) {
        MouseReleaseEvent event = MouseReleaseEvent.get(click.x(), click.y(), click.button(), this.x, this.y);
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method={"mouseDragged"}, at={@At(value="HEAD")}, cancellable=true)
    private void onMouseDragged(Click click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
        MouseDragEvent event = MouseDragEvent.get(click.x(), click.y(), click.button(), offsetX, offsetY, this.x, this.y);
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

