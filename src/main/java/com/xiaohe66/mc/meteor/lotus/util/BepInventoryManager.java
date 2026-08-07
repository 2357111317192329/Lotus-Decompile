package com.xiaohe66.mc.meteor.lotus.util;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Receive;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.item.MaceItem;

public class BepInventoryManager {
   private static BepInventoryManager INSTANCE;
   private final List<BepInventoryManager.PreSwapData> swapData = new CopyOnWriteArrayList<>();
   private int serverSlot = -1;
   private boolean sendingPacket = false;
   private boolean isEating = false;
   private long lastSetbackTime = -1L;
   private final int[] transactions = new int[4];
   private int transactionIndex = 0;
   private boolean isGrim = false;

   private BepInventoryManager() {
      MeteorClient.EVENT_BUS.subscribe(this);
      Arrays.fill(this.transactions, -1);
   }

   public static BepInventoryManager getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new BepInventoryManager();
      }

      return INSTANCE;
   }

   @EventHandler
   public void onPacketSend(Send event) {
      if (!this.sendingPacket) {
         if (event.packet instanceof UpdateSelectedSlotC2SPacket packet) {
            int packetSlot = packet.getSelectedSlot();
            if (!PlayerInventory.isValidHotbarIndex(packetSlot) || this.serverSlot == packetSlot) {
               event.cancel();
               return;
            }

            this.serverSlot = packetSlot;
         }
      }
   }

   @EventHandler(priority = 200)
   public void onPacketReceive(Receive event) {
      if (event.packet instanceof UpdateSelectedSlotS2CPacket packet) {
         int slot = packet.slot();
         this.serverSlot = slot;
      } else if (event.packet instanceof CommonPingS2CPacket packet) {
         if (this.transactionIndex > 3) {
            return;
         }

         int uid = packet.getParameter();
         this.transactions[this.transactionIndex] = uid;
         this.transactionIndex++;
         if (this.transactionIndex == 4) {
            this.grimCheck();
         }
      } else if (event.packet instanceof PlayerPositionLookS2CPacket) {
         this.lastSetbackTime = System.currentTimeMillis();
      }
   }

   @EventHandler
   public void onTick(Post event) {
      if (MeteorClient.mc.player != null && this.serverSlot == -1) {
         this.serverSlot = MeteorClient.mc.player.getInventory().getSelectedSlot();
      }

      this.swapData.removeIf(BepInventoryManager.PreSwapData::isExpired);
   }

   @EventHandler
   public void onDisconnect(GameLeftEvent event) {
      Arrays.fill(this.transactions, -1);
      this.transactionIndex = 0;
      this.isGrim = false;
      this.lastSetbackTime = -1L;
   }

   private void grimCheck() {
      for (int i = 0; i < 4; i++) {
         if (this.transactions[i] != -i) {
            return;
         }
      }

      this.isGrim = true;
   }

   public boolean isGrim() {
      return this.isGrim;
   }

   public boolean hasPassed(long timeMS) {
      return this.lastSetbackTime != -1L && System.currentTimeMillis() - this.lastSetbackTime >= timeMS;
   }

   public void setSlot(int barSlot) {
      this.setSlot(barSlot, false);
   }

   public void setSlot(int barSlot, boolean highPriority) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.getNetworkHandler() != null) {
         if (!this.isEating || highPriority) {
            if (this.serverSlot == -1) {
               this.serverSlot = MeteorClient.mc.player.getInventory().getSelectedSlot();
            }

            if (this.serverSlot != barSlot && PlayerInventory.isValidHotbarIndex(barSlot)) {
               this.setSlotForced(barSlot);
               ItemStack[] hotbarCopy = new ItemStack[9];

               for (int i = 0; i < 9; i++) {
                  hotbarCopy[i] = MeteorClient.mc.player.getInventory().getStack(i);
               }

               this.swapData.add(new BepInventoryManager.PreSwapData(hotbarCopy, this.serverSlot, barSlot));
            }
         }
      }
   }

   public void setClientSlot(int barSlot) {
      if (MeteorClient.mc.player != null) {
         if (!this.isEating) {
            if (MeteorClient.mc.player.getInventory().getSelectedSlot() != barSlot && PlayerInventory.isValidHotbarIndex(barSlot)) {
               MeteorClient.mc.player.getInventory().setSelectedSlot(barSlot);
               this.setSlotForced(barSlot);
            }
         }
      }
   }

   public void setSlotForced(int barSlot) {
      if (MeteorClient.mc.getNetworkHandler() != null) {
         this.sendingPacket = true;

         try {
            MeteorClient.mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(barSlot));
            this.serverSlot = barSlot;
         } finally {
            this.sendingPacket = false;
         }
      }
   }

   public void syncToClient() {
      if (MeteorClient.mc.player != null) {
         if (this.isDesynced()) {
            this.setSlotForced(MeteorClient.mc.player.getInventory().getSelectedSlot());

            for (BepInventoryManager.PreSwapData data : this.swapData) {
               data.beginClear();
            }
         }
      }
   }

   public boolean isDesynced() {
      return MeteorClient.mc.player == null ? false : MeteorClient.mc.player.getInventory().getSelectedSlot() != this.serverSlot;
   }

   public int getServerSlot() {
      if (MeteorClient.mc.player == null) {
         return -1;
      } else {
         return this.serverSlot == -1 ? MeteorClient.mc.player.getInventory().getSelectedSlot() : this.serverSlot;
      }
   }

   public int getClientSlot() {
      return MeteorClient.mc.player == null ? -1 : MeteorClient.mc.player.getInventory().getSelectedSlot();
   }

   public ItemStack getServerItem() {
      return MeteorClient.mc.player != null && this.getServerSlot() != -1
         ? MeteorClient.mc.player.getInventory().getStack(this.getServerSlot())
         : ItemStack.EMPTY;
   }

   public void setEating(boolean eating) {
      this.isEating = eating;
   }

   public boolean isEating() {
      return this.isEating;
   }

   public static int getBestWeaponSlot() {
      float bestDamage = 0.0F;
      int bestSlot = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = MeteorClient.mc.player.getInventory().getStack(i);
         float damage = getWeaponDamage(stack);
         if (damage > bestDamage) {
            bestDamage = damage;
            bestSlot = i;
         }
      }

      return bestSlot;
   }

   public static float getWeaponDamage(ItemStack stack) {
      if (stack.isEmpty()) {
         return 0.0F;
      }

      Item item = stack.getItem();
      float baseDamage = 0.0F;
      if (item.toString().toLowerCase().contains("sword")) {
         baseDamage = 4.0F;
      } else if (item instanceof AxeItem axe) {
         baseDamage = 5.0F;
      } else if (item instanceof TridentItem) {
         baseDamage = 8.0F;
      } else {
         if (!(item instanceof MaceItem)) {
            return 0.0F;
         }

         baseDamage = 5.0F;
      }

      int sharpnessLevel = Utils.getEnchantmentLevel(stack, Enchantments.SHARPNESS);
      float sharpnessDamage = sharpnessLevel * 0.5F + 0.5F;
      return baseDamage + sharpnessDamage;
   }

   public static int getBestBreachMaceSlot() {
      int bestSlot = -1;
      int bestBreachLevel = 0;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = MeteorClient.mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof MaceItem) {
            int breachLevel = Utils.getEnchantmentLevel(stack, Enchantments.BREACH);
            if (breachLevel > bestBreachLevel) {
               bestBreachLevel = breachLevel;
               bestSlot = i;
            }
         }
      }

      return bestSlot;
   }

   public static boolean isHoldingWeapon() {
      ItemStack mainHand = MeteorClient.mc.player.getMainHandStack();
      Item item = mainHand.getItem();
      return item.toString().toLowerCase().contains("sword") || item instanceof AxeItem || item instanceof TridentItem || item instanceof MaceItem;
   }

   public static boolean isHoldingWeaponType(Class<? extends Item> weaponType) {
      return weaponType.isInstance(MeteorClient.mc.player.getMainHandStack().getItem());
   }

   public static ItemStack getCurrentWeapon() {
      ItemStack mainHand = MeteorClient.mc.player.getMainHandStack();
      return isHoldingWeapon() ? mainHand : ItemStack.EMPTY;
   }

   public static void swapToSlot(int slot) {
      if (slot >= 0 && slot < 9) {
         MeteorClient.mc.player.getInventory().setSelectedSlot(slot);
      }
   }

   public static double getAttackSpeed(ItemStack weapon) {
      if (weapon.isEmpty()) {
         return 4.0;
      } else {
         Item item = weapon.getItem();
         if (item.toString().toLowerCase().contains("sword")) {
            return 1.6;
         } else if (item instanceof AxeItem) {
            return 0.8;
         } else if (item instanceof TridentItem) {
            return 1.1;
         } else {
            return item instanceof MaceItem ? 0.6 : 4.0;
         }
      }
   }

   public static int getAttackCooldownTicks(ItemStack weapon) {
      double attackSpeed = getAttackSpeed(weapon);
      return (int)Math.ceil(20.0 / attackSpeed);
   }

   public static boolean isHolding32k() {
      if (MeteorClient.mc.player == null) {
         return false;
      }

      ItemStack mainHand = MeteorClient.mc.player.getMainHandStack();
      ItemStack offHand = MeteorClient.mc.player.getOffHandStack();
      return is32kWeapon(mainHand) || is32kWeapon(offHand);
   }

   private static boolean is32kWeapon(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }

      Item item = stack.getItem();
      boolean isWeaponOrTool = item.toString().toLowerCase().contains("sword")
         || item.toString().toLowerCase().contains("pickaxe")
         || item.toString().toLowerCase().contains("axe")
         || item.toString().toLowerCase().contains("shovel");
      return !isWeaponOrTool
         ? false
         : Utils.getEnchantmentLevel(stack, Enchantments.SHARPNESS) > 1000
            || Utils.getEnchantmentLevel(stack, Enchantments.SMITE) > 1000
            || Utils.getEnchantmentLevel(stack, Enchantments.BANE_OF_ARTHROPODS) > 1000;
   }

   public interface IPlayerInteractEntityC2SPacket {
      boolean isAttackPacket();

      int getTargetEntityId();
   }

   public static class PreSwapData {
      private final ItemStack[] preHotbar;
      private final int starting;
      private final int swapTo;
      private long clearTime = -1L;

      public PreSwapData(ItemStack[] preHotbar, int start, int swapTo) {
         this.preHotbar = preHotbar;
         this.starting = start;
         this.swapTo = swapTo;
      }

      public void beginClear() {
         this.clearTime = System.currentTimeMillis();
      }

      public boolean isExpired() {
         return this.clearTime != -1L && System.currentTimeMillis() - this.clearTime > 300L;
      }

      public ItemStack getPreHolding(int i) {
         return this.preHotbar[i];
      }

      public int getStarting() {
         return this.starting;
      }

      public int getSlot() {
         return this.swapTo;
      }
   }

   public enum SwapMode {
      Normal,
      Silent;
   }

   public enum VelocityMode {
      NORMAL,
      WALLS,
      GRIM,
      GRIM_V3;
   }
}
