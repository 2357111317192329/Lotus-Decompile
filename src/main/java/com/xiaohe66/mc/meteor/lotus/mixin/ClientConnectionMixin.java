package com.xiaohe66.mc.meteor.lotus.mixin;

import com.xiaohe66.mc.meteor.lotus.util.TestPacketUtils;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ClientConnectionMixin {
   @Inject(method = "send", at = @At("TAIL"))
   private void onSetupPacketHandler(Packet<?> packet, CallbackInfo ci) {
      TestPacketUtils.sendPacketPrint(packet);
   }

   @Inject(method = "channelRead0", at = @At("TAIL"))
   private void test(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
      TestPacketUtils.readPacketPrint(packet);
   }
}
