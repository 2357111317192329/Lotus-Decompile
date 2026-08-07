package com.xiaohe66.mc.meteor.lotus.util;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;

public class TestPacketUtils {
   public static void sendPacketPrint(Packet<?> packet) {
      if (packet instanceof ServerboundInteractPacket interactEntityC2SPacket) {
         System.out.println("send packet : " + interactEntityC2SPacket.type());
      } else if (!(packet instanceof ServerboundPongPacket) && !(packet instanceof ServerboundKeepAlivePacket) && !(packet instanceof Pos) && !(packet instanceof ServerboundClientTickEndPacket)) {
         System.out.println("send : " + packet);
      }
   }

   public static void readPacketPrint(Packet<?> packet) {
      if (packet instanceof ClientboundContainerSetSlotPacket packet1) {
         System.out.println("read 更新单个槽位 packet : " + packet1);
      } else if (packet instanceof ClientboundContainerSetContentPacket packet1) {
         System.out.println("read 更新整个容器 packet : " + packet1);
      } else if (packet instanceof ClientboundOpenScreenPacket packet1) {
         System.out.println("read 打开屏幕 packet : " + packet1);
      } else if (packet instanceof ServerboundContainerClickPacket packet1) {
         System.out.println("read 移动物品 packet : " + packet1);
      } else if (!(packet instanceof ServerboundClientTickEndPacket)
         && !(packet instanceof ClientboundBundlePacket)
         && !(packet instanceof ClientboundBlockEventPacket)
         && !(packet instanceof ClientboundBlockUpdatePacket)
         && !(packet instanceof ClientboundLevelParticlesPacket)
         && !(packet instanceof ClientboundSoundPacket)
         && !(packet instanceof ClientboundLevelEventPacket)
         && !(packet instanceof ClientboundSetTimePacket)
         && !(packet instanceof ClientboundKeepAlivePacket)
         && !(packet instanceof ClientboundSectionBlocksUpdatePacket)
         && !(packet instanceof ClientboundTabListPacket)
         && !(packet instanceof ClientboundPlayerInfoUpdatePacket)
         && !(packet instanceof ClientboundPlayerInfoRemovePacket)
         && !(packet instanceof ClientboundLevelChunkWithLightPacket)
         && !(packet instanceof ClientboundPingPacket)
         && !(packet instanceof ClientboundTakeItemEntityPacket)
         && !(packet instanceof ClientboundSetHealthPacket)
         && !(packet instanceof ClientboundRotateHeadPacket)
         && !(packet instanceof ClientboundEntityPositionSyncPacket)
         && !(packet instanceof ClientboundDamageEventPacket)
         && !(packet instanceof ClientboundUpdateMobEffectPacket)
         && !(packet instanceof ClientboundSetEquipmentPacket)
         && !(packet instanceof ClientboundSetEntityDataPacket)
         && !(packet instanceof ClientboundSetEntityMotionPacket)
         && !(packet instanceof ClientboundEntityEventPacket)
         && !(packet instanceof ClientboundRemoveEntitiesPacket)
         && !(packet instanceof ClientboundUpdateAttributesPacket)
         && !(packet instanceof Pos)
         && !(packet instanceof ClientboundForgetLevelChunkPacket)
         && !(packet instanceof ClientboundMoveEntityPacket)) {
         System.out.println("read packet : " + packet);
      }
   }
}
