package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.modules.villager.VillagerEntityWarp;
import com.xiaohe66.mc.meteor.lotus.modules.villager.VillagerType;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeRotationUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting.Builder;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.block.LecternBlock;
import net.minecraft.village.VillagerProfession;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.village.TradedItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.util.math.Direction.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagerBookRoller extends WalkModule {
   private static final Logger log = LoggerFactory.getLogger(VillagerBookRoller.class);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Set<RegistryKey<Enchantment>>> targetEnchants = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("目标附魔")).description("想要刷取的附魔书类型（等级自动使用最高等级），默认排除消失诅咒、绑定诅咒、冰霜行者"))
            .defaultValue(
               new RegistryKey[]{
                  Enchantments.PROTECTION,
                  Enchantments.FEATHER_FALLING,
                  Enchantments.BLAST_PROTECTION,
                  Enchantments.RESPIRATION,
                  Enchantments.AQUA_AFFINITY,
                  Enchantments.DEPTH_STRIDER,
                  Enchantments.THORNS,
                  Enchantments.SHARPNESS,
                  Enchantments.KNOCKBACK,
                  Enchantments.FIRE_ASPECT,
                  Enchantments.LOOTING,
                  Enchantments.SWEEPING_EDGE,
                  Enchantments.EFFICIENCY,
                  Enchantments.SILK_TOUCH,
                  Enchantments.FORTUNE,
                  Enchantments.MENDING,
                  Enchantments.UNBREAKING
               }
            )
            .build()
      );
   private final Setting<Integer> maxCost = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("价格上限"))
                  .description("可接受的最高价格（绿宝石数量）"))
               .range(1, 64)
               .sliderRange(1, 64)
               .defaultValue(26))
            .build()
      );
   private final Setting<Integer> searchRange = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("搜索范围"))
                  .description("搜索村民和岩浆块的范围"))
               .range(8, 64)
               .sliderRange(8, 64)
               .defaultValue(32))
            .build()
      );
   private final Setting<Integer> professionTimeout = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("职业获取超时"))
                  .description("等待村民获得职业的毫秒数"))
               .range(1000, 10000)
               .sliderRange(1000, 10000)
               .defaultValue(5000))
            .build()
      );
   private final Setting<Boolean> removeWhenFound = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("找到后移除"))
                  .description("找到目标附魔书后从列表中移除，继续刷其他附魔"))
               .defaultValue(false))
            .build()
      );
   private final Setting<Boolean> playSound = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("播放提示音"))
                  .description("找到目标附魔书时播放提示音"))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> debug = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("调试模式"))
                  .description("输出调试信息到日志"))
               .defaultValue(false))
            .build()
      );
   private VillagerEntityWarp currentTarget;
   private long professionWaitStartTime;
   private long clearProfessionWaitStartTime;
   private int tradeIndex;

   public VillagerBookRoller() {
      super("刷附魔书", "自动寻找失业村民，放置讲台刷取指定附魔书. (村民旁边是岩浆块且上方未放置工作方块)");
      this.addStep(Steps.FINDING_TARGET, this::findingTarget);
      this.addStep(Steps.GOTO_TARGET, this::gotoTarget);
      this.addStep(Steps.PLACE_LECTERN, this::placeLectern);
      this.addStep(Steps.WAIT_PROFESSION, this::waitProfession);
      this.addStep(Steps.OPEN_TRADE, this::openTrade);
      this.addStep(Steps.CHECK_TRADES, this::checkTrades);
      this.addStep(Steps.EXECUTE_TRADE, this::executeTrade);
      this.addStep(Steps.BREAK_LECTERN, this::breakLectern);
      this.addStep(Steps.WAIT_PROFESSION_CLEAR, this::waitProfessionClear);
   }

   @Override
   public void onActivate() {
      super.onActivate();
      if (((Set)this.targetEnchants.get()).isEmpty()) {
         this.warning("未设置目标附魔", new Object[0]);
         this.toggle();
      } else {
         this.step = Steps.FINDING_TARGET;
         this.info("开始刷附魔书", new Object[0]);
      }
   }

   private void findingTarget() {
      if (((Set)this.targetEnchants.get()).isEmpty()) {
         this.info("§a所有目标附魔书已找到！", new Object[0]);
         this.toggle();
      } else {
         this.currentTarget = this.findNearestValidVillager();
         if (this.currentTarget == null) {
            this.warning("附近没有符合条件的失业村民", new Object[0]);
            this.toggle();
         } else {
            this.printLog("找到目标村民: " + this.currentTarget.getUuid() + " 朝向: " + this.currentTarget.getFacing());
            this.delayNext(Steps.GOTO_TARGET);
         }
      }
   }

   private void gotoTarget() {
      if (this.currentTarget == null) {
         this.step = Steps.FINDING_TARGET;
      } else {
         double distance = this.mc.player.getEntityPos().distanceTo(this.currentTarget.getOperatePos().toCenterPos());
         if (distance <= 1.5) {
            this.delayNext(Steps.PLACE_LECTERN);
         } else {
            this.gotoTargetIfNeed(this.currentTarget.getOperatePos(), 0, Steps.PLACE_LECTERN, "前往目标村民");
         }
      }
   }

   private void placeLectern() {
      if (this.currentTarget != null && this.currentTarget.getWorkPos() != null) {
         BlockState workPosState = this.mc.world.getBlockState(this.currentTarget.getWorkPos());
         if (!workPosState.isAir()) {
            if (workPosState.getBlock() instanceof LecternBlock) {
               this.step = Steps.WAIT_PROFESSION;
            } else {
               this.breakStep("<讲台位置>被占用");
            }
         } else {
            FindItemResult lecternResult = InvUtils.findInHotbar(new Item[]{Items.LECTERN});
            if (!lecternResult.found()) {
               this.warning("背包中没有讲台，请先准备讲台", new Object[0]);
               this.toggle();
            } else {
               BlockPos lecternPos = this.currentTarget.getWorkPos();
               Vec3d lecternCenter = lecternPos.toCenterPos();
               HeRotationUtils.rotate(lecternCenter, () -> {
                  if (BlockUtils.place(lecternPos, lecternResult, true, 5)) {
                     this.printLog("放置讲台成功");
                     this.professionWaitStartTime = System.currentTimeMillis();
                     this.step = Steps.WAIT_PROFESSION;
                  } else {
                     this.warning("放置讲台失败，重试中...", new Object[0]);
                  }
               });
               this.setDelay();
            }
         }
      } else {
         this.step = Steps.FINDING_TARGET;
      }
   }

   private void waitProfession() {
      if (this.currentTarget == null) {
         this.step = Steps.FINDING_TARGET;
      } else {
         VillagerEntity villager = this.currentTarget.getVillager();
         if (villager == null) {
            this.step = Steps.FINDING_TARGET;
         } else if (System.currentTimeMillis() - this.professionWaitStartTime > ((Integer)this.professionTimeout.get()).intValue()) {
            this.warning("等待村民获得职业超时，挖掉讲台重试", new Object[0]);
            this.delayNext(Steps.BREAK_LECTERN);
         } else {
            Optional<RegistryKey<VillagerProfession>> professionOpt = villager.getVillagerData().profession().getKey();
            if (professionOpt.isPresent() && professionOpt.get() == VillagerProfession.LIBRARIAN) {
               this.printLog("村民已成为图书管理员");
               this.delayNext(Steps.OPEN_TRADE);
            } else {
               this.setDelay();
            }
         }
      }
   }

   private void openTrade() {
      if (this.currentTarget == null) {
         this.step = Steps.FINDING_TARGET;
      } else {
         VillagerEntity villager = this.currentTarget.getVillager();
         if (villager == null) {
            this.step = Steps.FINDING_TARGET;
         } else if (!villager.isAlive()) {
            this.warning("村民已死亡", new Object[0]);
            this.delayCloseNext(Steps.FINDING_TARGET);
         } else {
            Vec3d playerPos = this.mc.player.getEyePos();
            Vec3d villagerPos = villager.getEyePos();
            EntityHitResult entityHitResult = ProjectileUtil.raycast(
               this.mc.player, playerPos, villagerPos, villager.getBoundingBox(), Entity::canHit, playerPos.squaredDistanceTo(villagerPos)
            );
            if (entityHitResult == null) {
               HeRotationUtils.rotate(villager.getEyePos(), () -> {
                  this.mc.interactionManager.interactEntity(this.mc.player, villager, Hand.MAIN_HAND);
                  this.step = Steps.CHECK_TRADES;
               });
            } else {
               HeRotationUtils.rotate(entityHitResult.getPos(), () -> {
                  ActionResult actionResult = this.mc.interactionManager.interactEntityAtLocation(this.mc.player, villager, entityHitResult, Hand.MAIN_HAND);
                  if (!actionResult.isAccepted()) {
                     this.mc.interactionManager.interactEntity(this.mc.player, villager, Hand.MAIN_HAND);
                  }

                  this.step = Steps.CHECK_TRADES;
               });
            }
         }
      }
   }

   private void checkTrades() {
      if (!(this.mc.player.currentScreenHandler instanceof MerchantScreenHandler handler)) {
         this.delayNext(Steps.OPEN_TRADE);
      } else {
         TradeOfferList var17 = handler.getRecipes();
         Set targets = (Set)this.targetEnchants.get();

         for (int i = 0; i < var17.size(); i++) {
            TradeOffer offer = (TradeOffer)var17.get(i);
            ItemStack sellItem = offer.getSellItem();
            if (sellItem.isOf(Items.ENCHANTED_BOOK) && offer.getUses() < offer.getMaxUses()) {
               ItemEnchantmentsComponent storedEnchants = (ItemEnchantmentsComponent)sellItem.get(DataComponentTypes.STORED_ENCHANTMENTS);
               if (storedEnchants != null) {
                  for (Entry<RegistryEntry<Enchantment>> entry : storedEnchants.getEnchantmentEntries()) {
                     RegistryEntry<Enchantment> enchantEntry = (RegistryEntry<Enchantment>)entry.getKey();
                     int level = entry.getIntValue();
                     Optional<RegistryKey<Enchantment>> enchantKeyOpt = enchantEntry.getKey();
                     if (!enchantKeyOpt.isEmpty()) {
                        RegistryKey<Enchantment> enchantKey = enchantKeyOpt.get();
                        if (targets.contains(enchantKey)) {
                           int maxLevel = ((Enchantment)enchantEntry.value()).getMaxLevel();
                           if (level < maxLevel) {
                              this.printLog("找到附魔但等级不足: " + enchantKey.getValue() + " " + level + "/" + maxLevel);
                           } else {
                              int cost = offer.getOriginalFirstBuyItem().getCount();
                              if (cost <= (Integer)this.maxCost.get()) {
                                 String enchantmentName = Names.get(enchantKey);
                                 this.info("§a找到目标附魔书: §f" + enchantmentName + " §a价格: §f" + cost, new Object[0]);
                                 if ((Boolean)this.playSound.get()) {
                                    this.mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0F, 1.0F));
                                 }

                                 if ((Boolean)this.removeWhenFound.get()) {
                                    targets.remove(enchantKey);
                                    this.printLog("已从目标列表中移除: " + enchantmentName);
                                 }

                                 this.tradeIndex = i;
                                 this.delayNext(Steps.EXECUTE_TRADE);
                                 return;
                              }

                              this.printLog("找到附魔但价格过高: " + cost + " > " + this.maxCost.get());
                           }
                        }
                     }
                  }
               }
            }
         }

         this.printLog("未找到目标附魔书，挖掉讲台重试");
         this.delayCloseNext(Steps.BREAK_LECTERN);
      }
   }

   private void executeTrade() {
      if (this.mc.player.currentScreenHandler instanceof MerchantScreenHandler handler) {
         TradeOfferList var10 = handler.getRecipes();
         if (this.tradeIndex >= var10.size()) {
            this.delayCloseNext(Steps.BREAK_LECTERN);
         } else {
            TradeOffer offer = (TradeOffer)var10.get(this.tradeIndex);
            FindItemResult emeraldResult = InvUtils.find(new Item[]{Items.EMERALD});
            int needEmerald = offer.getOriginalFirstBuyItem().getCount();
            int hasEmerald = emeraldResult.found() ? emeraldResult.count() : 0;
            ItemStack slot0 = handler.getSlot(0).getStack();
            if (slot0.isOf(Items.EMERALD)) {
               hasEmerald += slot0.getCount();
            }

            if (hasEmerald < needEmerald) {
               this.warning("绿宝石不足，需要 " + needEmerald + " 个", new Object[0]);
               this.toggle();
            } else {
               if (offer.getSecondBuyItem().isPresent()) {
                  ItemStack secondItem = ((TradedItem)offer.getSecondBuyItem().get()).itemStack();
                  if (secondItem.isOf(Items.BOOK)) {
                     FindItemResult bookResult = InvUtils.find(new Item[]{Items.BOOK});
                     if (!bookResult.found()) {
                        this.warning("需要书作为交易材料，但背包中没有", new Object[0]);
                        this.toggle();
                        return;
                     }
                  }
               }

               handler.setRecipeIndex(this.tradeIndex);
               handler.switchTo(this.tradeIndex);
               this.mc.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(this.tradeIndex));
               FindItemResult emptyResult = InvUtils.findEmpty();
               if (!emptyResult.found()) {
                  this.warning("背包没有格子了", new Object[0]);
                  this.toggle();
               } else {
                  InvUtils.move().fromId(2).to(emptyResult.slot());
                  this.info("§a已锁定交易！", new Object[0]);
                  this.delayCloseNext(Steps.FINDING_TARGET);
               }
            }
         }
      } else {
         this.delayNext(Steps.OPEN_TRADE);
      }
   }

   private void breakLectern() {
      if (this.currentTarget != null && this.currentTarget.getWorkPos() != null) {
         BlockPos lecternPos = this.currentTarget.getWorkPos();
         BlockState state = this.mc.world.getBlockState(lecternPos);
         if (!state.isOf(Blocks.LECTERN)) {
            this.clearProfessionWaitStartTime = System.currentTimeMillis();
            this.delayNext(Steps.WAIT_PROFESSION_CLEAR);
         } else {
            this.switchToAxe();
            BlockUtils.breakBlock(lecternPos, true);
         }
      } else {
         this.delayCloseNext(Steps.FINDING_TARGET);
      }
   }

   private void waitProfessionClear() {
      if (this.currentTarget == null) {
         this.delayCloseNext(Steps.FINDING_TARGET);
      } else {
         VillagerEntity villager = this.currentTarget.getVillager();
         if (villager == null) {
            this.delayCloseNext(Steps.FINDING_TARGET);
         } else if (System.currentTimeMillis() - this.clearProfessionWaitStartTime > ((Integer)this.professionTimeout.get()).intValue()) {
            this.printLog("等待失业超时，继续下一步");
            this.delayCloseNext(Steps.FINDING_TARGET);
         } else {
            Optional<RegistryKey<VillagerProfession>> professionOpt = villager.getVillagerData().profession().getKey();
            if (professionOpt.isPresent() && professionOpt.get() == VillagerProfession.NONE) {
               this.printLog("村民已失业，继续刷取");
               this.delayNext(Steps.PLACE_LECTERN);
            } else {
               this.setDelay();
            }
         }
      }
   }

   private VillagerEntityWarp findNearestValidVillager() {
      VillagerEntityWarp nearest = null;
      double minDistance = Double.MAX_VALUE;

      for (VillagerEntity villager : this.mc
         .world
         .getEntitiesByClass(VillagerEntity.class, this.mc.player.getBoundingBox().expand(((Integer)this.searchRange.get()).intValue()), frame -> true)) {
         Optional<RegistryKey<VillagerProfession>> professionOpt = villager.getVillagerData().profession().getKey();
         if (!professionOpt.isEmpty() && professionOpt.get() == VillagerProfession.NONE) {
            BlockPos villagerPos = villager.getBlockPos();
            Direction validDirection = null;
            BlockPos workPos = null;

            for (Direction dir : Type.HORIZONTAL) {
               BlockPos lecternPos = villagerPos.offset(dir);
               BlockPos magmaPos = lecternPos.down();
               BlockState magmaState = this.mc.world.getBlockState(magmaPos);
               if (magmaState.isOf(Blocks.MAGMA_BLOCK)) {
                  BlockState lecternState = this.mc.world.getBlockState(lecternPos);
                  if (lecternState.isAir()) {
                     BlockPos opPos = lecternPos.offset(dir);
                     BlockState opState = this.mc.world.getBlockState(opPos);
                     BlockState opUpState = this.mc.world.getBlockState(opPos.up());
                     if (opState.isAir() && opUpState.isAir()) {
                        validDirection = dir;
                        workPos = lecternPos;
                        break;
                     }
                  }
               }
            }

            if (validDirection != null) {
               double distance = this.mc.player.getEntityPos().distanceTo(villagerPos.toCenterPos());
               if (distance < minDistance) {
                  minDistance = distance;
                  BlockPos operatePos = workPos.offset(validDirection);
                  nearest = new VillagerEntityWarp(VillagerType.图书管理员, villager.getUuid(), operatePos, validDirection, workPos);
               }
            }
         }
      }

      return nearest;
   }

   private void printLog(String msg) {
      if ((Boolean)this.debug.get()) {
         log.info(msg);
      }
   }

   @Override
   public void onDeactivate() {
      super.onDeactivate();
      this.currentTarget = null;
   }

   public String getInfoString() {
      return this.currentTarget != null ? "刷取中" : "搜索中";
   }

   private void switchToAxe() {
      List<Item> axeTypes = List.of(
         Items.NETHERITE_AXE, Items.DIAMOND_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.STONE_AXE, Items.WOODEN_AXE
      );

      for (Item axe : axeTypes) {
         FindItemResult result = InvUtils.findInHotbar(new Item[]{axe});
         if (result.found()) {
            InvUtils.swap(result.slot(), false);
            return;
         }
      }

      for (Item axe : axeTypes) {
         FindItemResult result = InvUtils.find(new Item[]{axe});
         if (result.found()) {
            InvUtils.move().from(result.slot()).to(HeInvUtils.getMainSlot());
            return;
         }
      }
   }
}
