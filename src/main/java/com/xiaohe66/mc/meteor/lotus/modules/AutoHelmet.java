package com.xiaohe66.mc.meteor.lotus.modules;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Set;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class AutoHelmet extends BaseModule {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> distance = this.sgGeneral.add(new IntSetting.Builder()
      .name("检测距离")
      .description("检测猪灵的距离。")
      .defaultValue(10)
      .min(1)
      .sliderMax(40)
      .build()
   );
   private final Setting<Integer> minTime = this.sgGeneral.add(new IntSetting.Builder()
      .name("检测时间-tick")
      .description("检测持续多少时间后开始切换头盔")
      .defaultValue(5)
      .min(1)
      .sliderMax(64)
      .build()
   );
   private final Setting<Boolean> ignoreEmpty = this.sgGeneral.add(new BoolSetting.Builder()
      .name("忽略空")
      .description("没有穿戴头盔时, 不做切换")
      .defaultValue(true)
      .build()
   );
   private final Setting<Set<ResourceKey<Enchantment>>> avoidedEnchantments = this.sgGeneral.add(new EnchantmentListSetting.Builder()
      .name("避免的附魔")
      .description("应该避免的附魔.")
      .defaultValue(new ResourceKey[]{Enchantments.BINDING_CURSE, Enchantments.FROST_WALKER})
      .build()
   );
   private final Object2IntMap<Holder<Enchantment>> enchantments = new Object2IntOpenHashMap();
   private long lastPiglinTime;
   private boolean timing;

   public AutoHelmet() {
      super("自动头盔", "附近有猪灵时自动换上金头盔", 20);
   }

   public void onActivate() {
      this.timing = false;
   }

   @EventHandler
   private void onPreTick(Pre event) {
      if (this.isReady()) {
         if (this.checkAndDecrement()) {
            ItemStack currentItemStack = this.mc.player.getItemBySlot(EquipmentSlot.HEAD);
            Utils.getEnchantments(currentItemStack, this.enchantments);
            if (!this.enchantments.containsKey(Enchantments.BINDING_CURSE)) {
               boolean needGold = this.needGold();
               Item currentItem = currentItemStack.getItem();
               if (needGold) {
                  if (currentItem == Items.GOLDEN_HELMET) {
                     //this.info("currentItem = Items.GOLDEN_HELMET");
                     return;
                  }

                  if (!this.timing) {
                     //this.info("!this.timing");
                     this.timing = true;
                     this.lastPiglinTime = this.mcTime();
                     return;
                  }
                  
                  ItemStack nextStack = this.nextPlayerStack(itemStack -> itemStack.getItem() == Items.GOLDEN_HELMET && !this.hasAvoidedEnchantment());
                  if (nextStack.isEmpty()) {
                     //this.info("nextStack.isEmpty()");
                     nextStack = this.nextPlayerStack(
                        itemStack -> (itemStack.getItem() == Items.DIAMOND_HELMET || itemStack.getItem() == Items.NETHERITE_HELMET)
                           && !this.hasAvoidedEnchantment()
                     );
                  }
                  //this.info("currentItem="+currentItem.toString());
                  //this.info("nextStack="+nextStack.getItem().toString());
                  //this.info("first="+this.lastPiglinTime + ((Integer)this.minTime.get()).intValue());
                  //this.info("mcTime="+this.mcTime());
                  if (!nextStack.isEmpty()
                     && currentItem != nextStack.getItem()
                     && this.lastPiglinTime + ((Integer)this.minTime.get()).intValue() < this.mcTime()) {
                     //this.info("!nextStack.isEmpty()ver2");
                     if ((Boolean)this.ignoreEmpty.get() && currentItem == Items.AIR) {
                        return;
                     }

                     this.swap(this.getCurPlayerSlot(), 3);
                     this.setDelay();
                  }
               } else {
                  this.timing = false;
                  if (currentItem == Items.DIAMOND_HELMET || currentItem == Items.NETHERITE_HELMET) {
                     return;
                  }

                  ItemStack nextStack = this.nextPlayerStack(
                     itemStack -> (itemStack.getItem() == Items.DIAMOND_HELMET || itemStack.getItem() == Items.NETHERITE_HELMET)
                        && !this.hasAvoidedEnchantment()
                  );
                  if (!nextStack.isEmpty() && currentItem != nextStack.getItem()) {
                     if ((Boolean)this.ignoreEmpty.get() && currentItem == Items.AIR) {
                        return;
                     }

                     this.swap(this.getCurPlayerSlot(), 3);
                     this.setDelay();
                  }
               }
            }
         }
      }
   }

   private boolean needGold() {
      boolean hasPiglin = false;

      for (Entity entity : this.mc.level.entitiesForRendering()) {
         if (entity != this.mc.player) {
            if (entity instanceof Player) {
               if (isWithinDistance(entity, this.mc.player, ((Integer)this.distance.get()).intValue())) {
                  return false;
               }
            } else if (entity instanceof Piglin && isWithinDistance(entity, this.mc.player, ((Integer)this.distance.get()).intValue())) {
               hasPiglin = true;
            }
         }
      }

      return hasPiglin;
   }

   public static boolean isWithinDistance(Entity a, Entity b, double distance) {
      return a.distanceToSqr(b) <= distance * distance;
   }

   private boolean hasAvoidedEnchantment() {
      ObjectIterator var1 = this.enchantments.keySet().iterator();

      while (var1.hasNext()) {
         Holder<Enchantment> enchantment = (Holder<Enchantment>)var1.next();
         if (enchantment.is(((Set)this.avoidedEnchantments.get())::contains)) {
            return true;
         }
      }

      return false;
   }

   private void swap(int from, int armorSlotId) {
      InvUtils.move().from(from).toArmor(armorSlotId);
      this.info("切换盔甲", new Object[0]);
   }
}
