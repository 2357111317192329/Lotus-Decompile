package com.xiaohe66.mc.meteor.lotus.mixin;

import com.xiaohe66.mc.meteor.lotus.event.ContainerTooltipTextEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ContainerComponent.class)
public abstract class ContainerComponentMixin {
    @Inject(method = "appendTooltip", at = @At("HEAD"), cancellable = true)
    private void onAppendTooltip(Item.TooltipContext context, Consumer<Text> consumer, TooltipType flag, ComponentsAccess components, CallbackInfo ci) {
        ContainerTooltipTextEvent event = ContainerTooltipTextEvent.get();
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
