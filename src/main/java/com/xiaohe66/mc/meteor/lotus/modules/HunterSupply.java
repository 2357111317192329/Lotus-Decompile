package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.util.Const;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeRotationUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HunterSupply extends Module {
   private static final Logger log = LoggerFactory.getLogger(HunterSupply.class);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> delay = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("延迟")).description("操作延迟（刻）")).defaultValue(2)).min(0).build());
   private final Setting<Integer> range = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("范围")).description("末影箱的放置范围、盒子的搜索范围")).defaultValue(3)).min(0).build());
   private final Setting<Integer> kitRange = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("盒子范围")).description("范围")).defaultValue(1)).min(0).build());
   private final Setting<Integer> elytraQty = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("鞘翅数量")).description("鞘翅的补货量")).defaultValue(3)).range(0, 27).build());
   private final Setting<Integer> elytraDamaged = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("鞘翅损坏阈值")).description("剩余多少耐久被视为损坏")).defaultValue(5)).range(0, 20).build());
   private final VoxelShape enderChestShape = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);
   private final VoxelShape kitShape = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);
   private BlockPos enderChestPos = null;
   private BlockPos kitPos = null;
   private HunterSupply.Stage stage = HunterSupply.Stage.PlaceEnderChest;
   private int waitTicks;
   private int needElytraQty;
   private int takeIndex;
   private float progress;
   private boolean mineKitIng;
   private FindItemResult pickResult;

   public HunterSupply() {
      super(Const.CATEGORY, "!猎人补给", "猎人补给(未完成), 当前仅可补给鞘翅, 计划补给鞘翅、烟花、图腾、黑曜石等猎人物资");
   }

   public void onActivate() {
   }

   private void onTick(Pre event) {
      if (this.isActive()) {
         if (this.waitTicks > 0) {
            this.waitTicks--;
         } else {
            try {
               switch (this.stage) {
                  case FindEnderChest:
                     this.findEnderChest();
                     break;
                  case PlaceEnderChest:
                     this.placeEnderChest();
                     break;
                  case OpenEnderChest:
                     this.openEnderChest();
                     break;
                  case TakeKit:
                     this.takeKit();
                     break;
                  case FindKit:
                     this.findKit();
                     break;
                  case SwapKit:
                     this.swapKitToMainHand();
                     break;
                  case PlaceKit:
                     this.placeKit();
                     break;
                  case OpenKit:
                     this.openKit();
                     break;
                  case TakeItems:
                     this.takeItems();
                     break;
                  case MineKit:
                     this.mineKit();
                     break;
                  case PickUpKit:
                     this.pickUpKit();
                     break;
                  case Complete:
                     this.complete();
               }
            } catch (Exception e) {
               log.error("发生未知错误, state : {}", this.stage, e);
               this.error("发生未知错误: " + e.getMessage(), new Object[0]);
               this.toggle();
            }
         }
      }
   }

   private void findEnderChest() {
      int rangeValue = (Integer)this.range.get();
      BlockPos bestPos = null;
      double distance = 100.0;

      for (BlockPos blockPos : HeBlockUtils.listPosInSphere(rangeValue, this.mc.player.blockPosition())) {
         if (this.mc.level.isEmptyBlock(blockPos.above())) {
            if (this.mc.level.getBlockState(blockPos).getBlock() instanceof EnderChestBlock) {
               this.enderChestPos = blockPos;
               this.stage = HunterSupply.Stage.OpenEnderChest;
               return;
            }

            if (BlockUtils.canPlace(blockPos, true)
               && (bestPos == null || Mth.sqrt((float)this.mc.player.distanceToSqr(blockPos.getCenter())) < distance)) {
               distance = Mth.sqrt((float)this.mc.player.distanceToSqr(blockPos.getCenter()));
               bestPos = blockPos;
            }
         }
      }

      this.enderChestPos = bestPos;
      this.stage = HunterSupply.Stage.PlaceEnderChest;
   }

   private void placeEnderChest() {
      if (this.enderChestPos == null) {
         this.error("未找到放置末影箱合适的位置", new Object[0]);
         this.toggle();
      } else if (!BlockUtils.canPlace(this.enderChestPos, true)) {
         this.stage = HunterSupply.Stage.FindEnderChest;
      } else {
         FindItemResult enderChestResult = HeInvUtils.findAndMoveHotbar(Items.ENDER_CHEST);
         if (enderChestResult == null) {
            this.error("背包中没有末影箱", new Object[0]);
            this.toggle();
         } else {
            HeRotationUtils.rotate(this.enderChestPos);
            BlockUtils.place(this.enderChestPos, enderChestResult, true, 0, true);
            this.stage = HunterSupply.Stage.OpenEnderChest;
         }
      }
   }

   private void openEnderChest() {
      if (!(this.mc.level.getBlockState(this.enderChestPos).getBlock() instanceof EnderChestBlock)) {
         this.error("末影箱放置失败", new Object[0]);
         this.stage = HunterSupply.Stage.PlaceEnderChest;
      } else {
         this.stage = HunterSupply.Stage.TakeKit;
      }
   }

   private void takeKit() {
      if (this.mc.player.containerMenu instanceof ChestMenu screenHandler) {
         for (int slotIndex = 0; slotIndex < 27; slotIndex++) {
            Slot slotObj = screenHandler.getSlot(slotIndex);
            ItemStack stack = slotObj.getItem();
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock && this.containsElytra(stack, false)) {
               FindItemResult emptyItemResult = InvUtils.findEmpty();
               if (!emptyItemResult.found()) {
                  this.error("没有空位, 待实现自动丢垃圾", new Object[0]);
                  this.toggle();
                  return;
               }

               int emptyItemSlot = emptyItemResult.slot();
               if (emptyItemResult.isHotbar()) {
                  InvUtils.move().fromId(slotIndex).to(emptyItemSlot);
               } else {
                  InvUtils.move().fromId(slotIndex).to(HeInvUtils.getMainSlot());
                  InvUtils.click().to(emptyItemSlot);
               }

               HeInvUtils.closeCurScreen();
               if (this.needElytraQty > 0) {
                  this.stage = HunterSupply.Stage.SwapKit;
               } else {
                  this.complete();
               }

               return;
            }
         }

         this.error("未找到含有鞘翅的潜影盒", new Object[0]);
         this.toggle();
      } else {
         this.stage = HunterSupply.Stage.OpenEnderChest;
      }
   }

   private void findKit() {
      int rangeValue = (Integer)this.range.get();

      for (BlockPos blockPos : HeBlockUtils.listPosInSphere(rangeValue, this.mc.player.blockPosition())) {
         if (this.mc.level.getBlockState(blockPos).getBlock() instanceof ShulkerBoxBlock) {
            this.kitPos = blockPos;
            this.next(HunterSupply.Stage.OpenKit);
            return;
         }
      }

      this.next(HunterSupply.Stage.FindEnderChest);
   }

   private void swapKitToMainHand() {
      FindItemResult findElytraResult = InvUtils.find(itemstack -> this.containsElytra(itemstack, false));
      if (findElytraResult.found()) {
         int slot = findElytraResult.slot();
         log.info("swapKitToMainHand, source slot : {}", slot);
         HeInvUtils.swapMainHand(slot);
         this.next(HunterSupply.Stage.PlaceKit);
      } else {
         this.next(HunterSupply.Stage.FindKit);
      }
   }

   private void placeKit() {
      if (!HeInvUtils.isKitInMainHand()) {
         log.error("placeKit main hand is not kit : {}", HeInvUtils.getMainSlot());
         this.stage = HunterSupply.Stage.SwapKit;
      } else {
         int rangeValue = (Integer)this.kitRange.get();
         BlockPos bestPos = null;
         double distance = 100.0;

         for (BlockPos blockPos : HeBlockUtils.listPosInSphere(rangeValue, this.mc.player.blockPosition())) {
            if (this.mc.level.isEmptyBlock(blockPos.above())) {
               if (this.mc.level.getBlockState(blockPos).getBlock() instanceof ShulkerBoxBlock) {
                  this.info("地上已存在kit", new Object[0]);
                  this.kitPos = blockPos;
                  this.next(HunterSupply.Stage.OpenKit);
                  return;
               }

               if (BlockUtils.canPlace(blockPos, true)
                  && (bestPos == null || Mth.sqrt((float)this.mc.player.distanceToSqr(blockPos.getCenter())) < distance)) {
                  distance = Mth.sqrt((float)this.mc.player.distanceToSqr(blockPos.getCenter()));
                  bestPos = blockPos;
               }
            }
         }

         if (bestPos == null) {
            this.error("未找到放置kit合适的位置", new Object[0]);
            this.toggle();
         } else {
            this.kitPos = bestPos;
            HeRotationUtils.rotate(this.kitPos);
            log.info("放置kit : {}", this.kitPos);
            FindItemResult findItemResult = new FindItemResult(HeInvUtils.getMainSlot(), 1);
            BlockUtils.place(bestPos, findItemResult, true, 0, true);
            this.next(HunterSupply.Stage.OpenKit);
         }
      }
   }

   private void openKit() {
      if (!(this.mc.level.getBlockState(this.kitPos).getBlock() instanceof ShulkerBoxBlock)) {
         this.error("kit放置失败", new Object[0]);
         this.next(HunterSupply.Stage.PlaceKit);
      } else {
         this.takeIndex = 0;
         this.updateNeedElytraQty();
         this.next(HunterSupply.Stage.TakeItems);
      }
   }

   private void takeItems() {
      if (!(this.mc.player.containerMenu instanceof ShulkerBoxMenu)) {
         log.error("打开kit失败");
         this.next(HunterSupply.Stage.OpenKit);
      } else if (this.takeIndex < 27 && this.needElytraQty > 0) {
         ItemStack stack = this.mc.player.containerMenu.getSlot(this.takeIndex).getItem();
         if (stack.getItem() == Items.ELYTRA) {
            FindItemResult damagedElytraResult = this.findElytra(true);
            if (damagedElytraResult.found()) {
               InvUtils.move().from(damagedElytraResult.slot()).toId(this.takeIndex);
               InvUtils.click().to(damagedElytraResult.slot());
            } else {
               FindItemResult emptyResult = InvUtils.findEmpty();
               if (emptyResult.found()) {
                  log.info("move : {}->{}", this.takeIndex, emptyResult.slot());
                  InvUtils.move().fromId(this.takeIndex).to(emptyResult.slot());
               } else {
                  this.error("没有空位了", new Object[0]);
                  HeInvUtils.closeCurScreen();
                  this.complete();
               }
            }
         }

         this.takeIndex++;
      } else {
         HeInvUtils.closeCurScreen();
         this.next(HunterSupply.Stage.MineKit);
      }
   }

   private void mineKit() {
      if (!this.mineKitIng) {
         this.mineKitIng = true;
         this.progress = 0.0F;
         this.pickResult = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);
         if (!this.pickResult.isHotbar()) {
            this.error("未找到稿子", new Object[0]);
            this.complete();
         } else {
            this.mine(false);
         }
      } else {
         if (this.progress < 1.0F) {
            this.pickResult = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);
            if (!this.pickResult.isHotbar()) {
               this.error("未找到稿子", new Object[0]);
               this.complete();
               return;
            }

            this.progress = (float)(this.progress + BlockUtils.getBreakDelta(this.pickResult.slot(), this.mc.level.getBlockState(this.kitPos)));
            if (this.progress < 1.0F) {
               return;
            }
         }

         this.mine(true);
         this.stage = HunterSupply.Stage.PickUpKit;
      }
   }

   private void pickUpKit() {
      this.stage = HunterSupply.Stage.Complete;
   }

   private void mine(boolean done) {
      log.info("mine, done : {}", done);
      InvUtils.swap(this.pickResult.slot(), true);
      HeRotationUtils.rotate(this.kitPos);
      Direction direction = BlockUtils.getDirection(this.kitPos);
      if (!done) {
         this.mc.getConnection().send(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, this.kitPos, direction));
      }

      this.mc.getConnection().send(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, this.kitPos, direction));
   }

   private boolean containsElytra(ItemStack itemStack, boolean isDamaged) {
      return itemStack.isEmpty() ? false : false;
   }

   private FindItemResult findElytra(boolean isDamaged) {
      return InvUtils.find(itemStack -> {
         if (itemStack.getItem() == Items.ELYTRA) {
            int surplusDamage = itemStack.getMaxDamage() - itemStack.getDamageValue();
            return isDamaged == surplusDamage <= (Integer)this.elytraDamaged.get();
         } else {
            return false;
         }
      });
   }

   private void updateNeedElytraQty() {
      FindItemResult notDamagedResult = this.findElytra(false);
      this.needElytraQty = notDamagedResult.found() ? (Integer)this.elytraQty.get() - notDamagedResult.count() : (Integer)this.elytraQty.get();
      log.info("updateNeedElytraQty : {}", this.needElytraQty);
   }

   private void complete() {
      this.enderChestPos = null;
      this.kitPos = null;
      this.mineKitIng = false;
      this.info("补给结束", new Object[0]);
      this.toggle();
   }

   private void onRender(Render3DEvent event) {
      if (this.enderChestPos != null) {
         AABB box = (AABB)this.enderChestShape.toAabbs().getFirst();
         event.renderer
            .box(
               this.enderChestPos.getX() + box.minX,
               this.enderChestPos.getY() + box.minY,
               this.enderChestPos.getZ() + box.minZ,
               this.enderChestPos.getX() + box.maxX,
               this.enderChestPos.getY() + box.maxY,
               this.enderChestPos.getZ() + box.maxZ,
               Color.BLUE,
               Color.BLUE,
               ShapeMode.Lines,
               0
            );
      }

      if (this.kitPos != null) {
         AABB box = (AABB)this.kitShape.toAabbs().getFirst();
         event.renderer
            .box(
               this.kitPos.getX() + box.minX,
               this.kitPos.getY() + box.minY,
               this.kitPos.getZ() + box.minZ,
               this.kitPos.getX() + box.maxX,
               this.kitPos.getY() + box.maxY,
               this.kitPos.getZ() + box.maxZ,
               Color.GREEN,
               Color.GREEN,
               ShapeMode.Lines,
               0
            );
      }
   }

   private void next(HunterSupply.Stage next) {
      this.next(next, true);
   }

   private void next(HunterSupply.Stage next, boolean needDelay) {
      if (needDelay) {
         this.waitTicks = (Integer)this.delay.getDefaultValue();
      }

      this.stage = next;
   }

   private enum Stage {
      FindEnderChest,
      PlaceEnderChest,
      OpenEnderChest,
      TakeKit,
      FindKit,
      SwapKit,
      PlaceKit,
      OpenKit,
      TakeItems,
      MineKit,
      PickUpKit,
      Complete;
   }
}
