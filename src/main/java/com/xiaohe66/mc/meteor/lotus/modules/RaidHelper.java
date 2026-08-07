package com.xiaohe66.mc.meteor.lotus.modules;

import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;

public class RaidHelper extends BaseModule {
   private final Setting<Integer> before = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("喝药提前量")).description("在<袭击之兆>结束的多少tick前喝药")).defaultValue(30)).sliderRange(0, 32).build());
   private final Setting<Boolean> one = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("保留一瓶"))
                  .description("当药快喝完时, 至少保留一瓶"))
               .defaultValue(true))
            .build()
      );
   public boolean drinking;
   private long drinkStartTime;
   private int slot;

   public RaidHelper() {
      super("袭击助手", "挂机袭击塔使用。定时从背包里拿药、自动喝药后开启杀戮光环", 560, 800);
   }

   public void onActivate() {
      this.setDelay(0);
      this.drinkStartTime = System.currentTimeMillis();
      if (!this.needDrink()) {
         this.stopDrinking();
         this.enableKillAura();
         this.setDelay();
      }

      this.drinking = false;
   }

   @EventHandler(priority = -100)
   private void onTick(Pre event) {
      if (!this.checkAndDecrement()) {
         int delayTimer = this.getDelayTimer();
         if (delayTimer % 20 == 0) {
            int s = delayTimer / 20;
            if (s % 5 == 0) {
               this.info("喝药计时 : " + s, new Object[0]);
            }
         }
      } else {
         if (this.drinking) {
            if (this.needDrink()) {
               this.drink();
            } else {
               this.info("喝药结束", new Object[0]);
               this.stopDrinking();
               this.enableKillAura();
               this.setDelay();
               this.drinkStartTime = System.currentTimeMillis();
            }
         } else {
            if (!this.needDrink()) {
               return;
            }

            this.disableKillAura();
            this.slot = this.findSlot();
            if (this.slot != -1) {
               this.info("开始喝药", new Object[0]);
               this.drink();
            } else {
               this.error("缺少<不详之瓶>", new Object[0]);
               this.toggle();
            }
         }
      }
   }

   private boolean needDrink() {
      return !this.hasBadOmen() && !this.hasRaidOmen() && System.currentTimeMillis() - this.drinkStartTime > 5000L;
   }

   private void drink() {
      this.changeSlot(this.slot);
      this.setPressed(true);
      if (!this.mc.player.isUsingItem()) {
         Utils.rightClick();
      }

      this.drinking = true;
   }

   private void stopDrinking() {
      this.setPressed(false);
      this.drinking = false;
   }

   private void enableKillAura() {
      KillAura killAura = (KillAura)Modules.get().get(KillAura.class);
      if (!killAura.isActive()) {
         killAura.toggle();
      }
   }

   private void disableKillAura() {
      KillAura killAura = (KillAura)Modules.get().get(KillAura.class);
      if (killAura.isActive()) {
         killAura.toggle();
      }
   }

   private void setPressed(boolean pressed) {
      this.mc.options.useKey.setPressed(pressed);
   }

   private void changeSlot(int slot) {
      InvUtils.swap(slot, false);
      this.slot = slot;
   }

   private int findSlot() {
      PlayerInventory inv = this.mc.player.getInventory();

      for (int i = 0; i < 9; i++) {
         ItemStack stack = inv.getStack(i);
         if (stack.getItem() == Items.OMINOUS_BOTTLE && (!(Boolean)this.one.get() || stack.getCount() > 1)) {
            return i;
         }
      }

      for (int i = 9; i < 36; i++) {
         ItemStack stack = inv.getStack(i);
         if (stack.getItem() == Items.OMINOUS_BOTTLE && (!(Boolean)this.one.get() || stack.getCount() > 1)) {
            int emptySlot = this.findEmptyHotbarSlot();
            if (emptySlot != -1) {
               InvUtils.move().from(i).toHotbar(emptySlot);
               return emptySlot;
            }
         }
      }

      return -1;
   }

   private int findEmptyHotbarSlot() {
      PlayerInventory inv = this.mc.player.getInventory();

      for (int i = 0; i < 9; i++) {
         if (inv.getStack(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   private boolean hasBadOmen() {
      for (StatusEffectInstance effect : this.mc.player.getStatusEffects()) {
         RegistryEntry<StatusEffect> effectType = effect.getEffectType();
         if (effectType == StatusEffects.BAD_OMEN) {
            return true;
         }
      }

      return false;
   }

   private boolean hasRaidOmen() {
      for (StatusEffectInstance effect : this.mc.player.getStatusEffects()) {
         RegistryEntry<StatusEffect> effectType = effect.getEffectType();
         if (effectType == StatusEffects.RAID_OMEN) {
            return effect.getDuration() >= (Integer)this.before.get();
         }
      }

      return false;
   }

   public void onDeactivate() {
      this.stopDrinking();
      this.disableKillAura();
   }
}
