package com.xiaohe66.mc.meteor.lotus.util;

import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;

public class TestPacketUtils {
   public static void sendPacketPrint(Packet<?> packet) {
      if (packet instanceof PlayerInteractEntityC2SPacket interactEntityC2SPacket) {
         System.out.println("send packet : " + interactEntityC2SPacket.getPacketType());
      } else if (!(packet instanceof CommonPongC2SPacket) && !(packet instanceof KeepAliveC2SPacket) && !(packet instanceof PositionAndOnGround) && !(packet instanceof ClientTickEndC2SPacket)) {
         System.out.println("send : " + packet);
      }
   }

   public static void readPacketPrint(Packet<?> packet) {
      if (packet instanceof ScreenHandlerSlotUpdateS2CPacket packet1) {
         System.out.println("read 更新单个槽位 packet : " + packet1);
      } else if (packet instanceof InventoryS2CPacket packet1) {
         System.out.println("read 更新整个容器 packet : " + packet1);
      } else if (packet instanceof OpenScreenS2CPacket packet1) {
         System.out.println("read 打开屏幕 packet : " + packet1);
      } else if (packet instanceof ClickSlotC2SPacket packet1) {
         System.out.println("read 移动物品 packet : " + packet1);
      } else if (!(packet instanceof ClientTickEndC2SPacket)
         && !(packet instanceof BundleS2CPacket)
         && !(packet instanceof BlockEventS2CPacket)
         && !(packet instanceof BlockUpdateS2CPacket)
         && !(packet instanceof ParticleS2CPacket)
         && !(packet instanceof PlaySoundS2CPacket)
         && !(packet instanceof WorldEventS2CPacket)
         && !(packet instanceof WorldTimeUpdateS2CPacket)
         && !(packet instanceof KeepAliveS2CPacket)
         && !(packet instanceof ChunkDeltaUpdateS2CPacket)
         && !(packet instanceof PlayerListHeaderS2CPacket)
         && !(packet instanceof PlayerListS2CPacket)
         && !(packet instanceof PlayerRemoveS2CPacket)
         && !(packet instanceof ChunkDataS2CPacket)
         && !(packet instanceof CommonPingS2CPacket)
         && !(packet instanceof ItemPickupAnimationS2CPacket)
         && !(packet instanceof HealthUpdateS2CPacket)
         && !(packet instanceof EntitySetHeadYawS2CPacket)
         && !(packet instanceof EntityPositionSyncS2CPacket)
         && !(packet instanceof EntityDamageS2CPacket)
         && !(packet instanceof EntityStatusEffectS2CPacket)
         && !(packet instanceof EntityEquipmentUpdateS2CPacket)
         && !(packet instanceof EntityTrackerUpdateS2CPacket)
         && !(packet instanceof EntityVelocityUpdateS2CPacket)
         && !(packet instanceof EntityStatusS2CPacket)
         && !(packet instanceof EntitiesDestroyS2CPacket)
         && !(packet instanceof EntityAttributesS2CPacket)
         && !(packet instanceof PositionAndOnGround)
         && !(packet instanceof UnloadChunkS2CPacket)
         && !(packet instanceof EntityS2CPacket)) {
         System.out.println("read packet : " + packet);
      }
   }
}
