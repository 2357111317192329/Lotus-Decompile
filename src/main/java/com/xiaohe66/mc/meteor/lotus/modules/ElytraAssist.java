package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BoolSetting.Builder;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.util.Hand;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;

public class ElytraAssist extends StepModule {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgAutoTakeoff = this.settings.createGroup("自动起飞");
   private final Setting<Boolean> autoTakeoff = this.sgAutoTakeoff
      .add(((Builder)((Builder)((Builder)new Builder().name("自动起飞")).description("当不在飞行状态超过一定时间时，自动穿鞘翅并使用烟花起飞")).defaultValue(true)).build());
   private final Setting<Integer> takeoffDelay = this.sgAutoTakeoff
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("起飞延迟"))
                     .description("不在飞行状态超过多少tick后触发自动起飞"))
                  .defaultValue(20))
               .min(1)
               .sliderMax(100)
               .visible(this.autoTakeoff::get))
            .build()
      );
   private final Setting<Integer> minDurability = this.sgAutoTakeoff
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("最小耐久"))
                     .description("鞘翅耐久低于此值时不自动装备"))
                  .defaultValue(5))
               .min(1)
               .sliderMax(100)
               .visible(this.autoTakeoff::get))
            .build()
      );
   private final Setting<Double> takeoffPitch = this.sgAutoTakeoff
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("起飞角度"))
                  .description("起飞时的俯仰角，负数为向上，0为水平，正数为向下"))
               .defaultValue(-45.0)
               .min(-90.0)
               .max(90.0)
               .sliderMin(-90.0)
               .sliderMax(90.0)
               .visible(this.autoTakeoff::get))
            .build()
      );
   private long lastGlidingTime = 0L;
   private long lastJumpTime = 0L;

   public ElytraAssist() {
      super("鞘翅辅助", "提供自动起飞等鞘翅辅助功能");
      this.addStep(Steps.MONITOR, this::monitor);
      this.addStep(Steps.EQUIP_ELYTRA, this::equipElytra);
      this.addStep(Steps.SWITCH_FIREWORK, this::switchFirework);
      this.addStep(Steps.DEPLOY_ELYTRA, this::deployElytra);
      this.addStep(Steps.USE_FIREWORK, this::useFirework);
   }

   public void onActivate() {
      super.onActivate();
      this.step = Steps.MONITOR;
      this.lastGlidingTime = this.mcTime();
   }

   public void onDeactivate() {
      super.onDeactivate();
   }

   private void monitor() {
      if (this.mc.player.isGliding()) {
         this.lastGlidingTime = this.mcTime();
      } else {
         if ((Boolean)this.autoTakeoff.get()) {
            long timeSinceLastGlide = this.mcTime() - this.lastGlidingTime;
            if (timeSinceLastGlide >= ((Integer)this.takeoffDelay.get()).intValue()) {
               this.step = Steps.EQUIP_ELYTRA;
            }
         }
      }
   }

   private void equipElytra() {
      ItemStack chestItem = this.mc.player.getEquippedStack(EquipmentSlot.CHEST);
      if (chestItem.getItem() == Items.ELYTRA) {
         if (chestItem.getDamage() < chestItem.getMaxDamage() - (Integer)this.minDurability.get()) {
            this.step = Steps.SWITCH_FIREWORK;
            return;
         }

         this.info("当前鞘翅耐久不足，尝试更换", new Object[0]);
      }

      int elytraSlot = this.findElytraWithDurability();
      if (elytraSlot == -1) {
         this.warning("未找到耐久足够的鞘翅，无法自动起飞", new Object[0]);
         this.lastGlidingTime = this.mcTime();
         this.step = Steps.MONITOR;
      } else {
         InvUtils.move().from(elytraSlot).toArmor(2);
         this.step = Steps.SWITCH_FIREWORK;
      }
   }

   private void switchFirework() {
      int fireworkSlot = this.findFireworkRocket();
      if (fireworkSlot == -1) {
         this.warning("未找到烟花火箭，无法自动起飞", new Object[0]);
         this.step = Steps.MONITOR;
      } else {
         if (HeInvUtils.isHotbar(fireworkSlot)) {
            HeInvUtils.swapToSlot(fireworkSlot);
         } else {
            HeInvUtils.swapMainHand(fireworkSlot);
         }

         this.step = Steps.DEPLOY_ELYTRA;
      }
   }

   private void deployElytra() {
      ItemStack chestItem = this.mc.player.getEquippedStack(EquipmentSlot.CHEST);
      if (chestItem.getItem() != Items.ELYTRA) {
         this.warning("鞘翅装备失败，取消自动起飞", new Object[0]);
         this.step = Steps.MONITOR;
      } else if (this.mc.player.isGliding()) {
         this.step = Steps.USE_FIREWORK;
      } else if (this.mc.player.isOnGround()) {
         this.tryJump();
      } else if (!(this.mc.player.fallDistance <= 0.0)) {
         this.tryJump();
         this.mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(this.mc.player, Mode.START_FALL_FLYING));
         this.step = Steps.USE_FIREWORK;
      }
   }

   private void useFirework() {
      if (!this.mc.player.isGliding()) {
         this.info("鞘翅未展开，继续尝试", new Object[0]);
         this.deployElytra();
      } else {
         float targetPitch = ((Double)this.takeoffPitch.get()).floatValue();
         float currentPitch = this.mc.player.getPitch();
         if (Math.abs(currentPitch - targetPitch) > 1.0F) {
            Rotations.rotate(this.mc.player.getYaw(), targetPitch, this::useFireworkInternal);
         } else {
            this.useFireworkInternal();
         }
      }
   }

   private void useFireworkInternal() {
      if (this.mc.interactionManager != null) {
         this.mc.interactionManager.interactItem(this.mc.player, Hand.MAIN_HAND);
      }

      this.info("自动起飞完成，角度：" + String.format("%.1f", this.mc.player.getPitch()), new Object[0]);
      this.lastGlidingTime = this.mcTime();
      this.delayNext(Steps.MONITOR);
   }

   private void tryJump() {
      long curTime = this.mcTime();
      if (curTime - this.lastJumpTime > 20L) {
         this.lastJumpTime = curTime;
         this.mc.player.jump();
      }
   }

   private int findElytraWithDurability() {
      for (int i = 0; i < 36; i++) {
         ItemStack stack = this.mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.ELYTRA) {
            int remainingDurability = stack.getMaxDamage() - stack.getDamage();
            if (remainingDurability > (Integer)this.minDurability.get()) {
               return i;
            }
         }
      }

      return -1;
   }

   private int findFireworkRocket() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = this.mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.FIREWORK_ROCKET) {
            return i;
         }
      }

      for (int i = 9; i < 36; i++) {
         ItemStack stack = this.mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.FIREWORK_ROCKET) {
            return i;
         }
      }

      return -1;
   }
}
