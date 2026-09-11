/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntMap$Entry
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.EnchantmentListSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.utils.misc.Names
 *  meteordevelopment.meteorclient.utils.player.FindItemResult
 *  meteordevelopment.meteorclient.utils.player.InvUtils
 *  meteordevelopment.meteorclient.utils.world.BlockUtils
 *  net.minecraft.client.sound.PositionedSoundInstance
 *  net.minecraft.client.sound.SoundInstance
 *  net.minecraft.util.Hand
 *  net.minecraft.util.ActionResult
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.passive.VillagerEntity
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.entity.projectile.ProjectileUtil
 *  net.minecraft.screen.ScreenHandler
 *  net.minecraft.screen.MerchantScreenHandler
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.enchantment.Enchantment
 *  net.minecraft.enchantment.Enchantments
 *  net.minecraft.village.TradeOffer
 *  net.minecraft.block.Blocks
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.util.math.Direction$Type
 *  net.minecraft.util.math.Box
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.network.packet.Packet
 *  net.minecraft.block.BlockState
 *  net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket
 *  net.minecraft.sound.SoundEvent
 *  net.minecraft.sound.SoundEvents
 *  net.minecraft.block.LecternBlock
 *  net.minecraft.village.VillagerProfession
 *  net.minecraft.util.hit.EntityHitResult
 *  net.minecraft.registry.RegistryKey
 *  net.minecraft.registry.entry.RegistryEntry
 *  net.minecraft.component.type.ItemEnchantmentsComponent
 *  net.minecraft.village.TradedItem
 *  net.minecraft.component.DataComponentTypes
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.modules.villager.VillagerEntityWarp;
import com.xiaohe66.mc.meteor.lotus.modules.villager.VillagerType;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeRotationUtils;
import com.xiaohe66.mc.meteor.lotus.modules.WalkModule;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.village.TradeOffer;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.Packet;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.block.LecternBlock;
import net.minecraft.village.VillagerProfession;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.village.TradedItem;
import net.minecraft.component.DataComponentTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagerBookRoller extends WalkModule {
    private static final Logger log = LoggerFactory.getLogger(VillagerBookRoller.class);
    private final Setting<Set<RegistryKey<Enchantment>>> targetEnchants = sgGeneral.add(new EnchantmentListSetting.Builder()
        .name("目标附魔")
        .description("想要刷取的附魔书类型（等级自动使用最高等级），默认排除消失诅咒、绑定诅咒、冰霜行者")
        .defaultValue(new RegistryKey[]{Enchantments.PROTECTION, Enchantments.FEATHER_FALLING, Enchantments.BLAST_PROTECTION, Enchantments.RESPIRATION, Enchantments.AQUA_AFFINITY, Enchantments.DEPTH_STRIDER, Enchantments.THORNS, Enchantments.SHARPNESS, Enchantments.KNOCKBACK, Enchantments.FIRE_ASPECT, Enchantments.LOOTING, Enchantments.SWEEPING_EDGE, Enchantments.EFFICIENCY, Enchantments.SILK_TOUCH, Enchantments.FORTUNE, Enchantments.MENDING, Enchantments.UNBREAKING})
        .build());
    private final Setting<Integer> maxCost = sgGeneral.add(new IntSetting.Builder()
        .name("价格上限")
        .description("可接受的最高价格（绿宝石数量）")
        .range(1, 64)
        .sliderRange(1, 64)
        .defaultValue(26)
        .build());
    private final Setting<Integer> searchRange = sgGeneral.add(new IntSetting.Builder()
        .name("搜索范围")
        .description("搜索村民和岩浆块的范围")
        .range(8, 64)
        .sliderRange(8, 64)
        .defaultValue(32)
        .build());
    private final Setting<Integer> professionTimeout = sgGeneral.add(new IntSetting.Builder()
        .name("职业获取超时")
        .description("等待村民获得职业的毫秒数")
        .range(1000, 10000)
        .sliderRange(1000, 10000)
        .defaultValue(5000)
        .build());
    private final Setting<Boolean> removeWhenFound = sgGeneral.add(new BoolSetting.Builder()
        .name("找到后移除")
        .description("找到目标附魔书后从列表中移除，继续刷其他附魔")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> playSound = sgGeneral.add(new BoolSetting.Builder()
        .name("播放提示音")
        .description("找到目标附魔书时播放提示音")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
        .name("调试模式")
        .description("输出调试信息到日志")
        .defaultValue(false)
        .build());
    public static final Set<Item> axeTypes = Set.of(Items.NETHERITE_AXE, Items.DIAMOND_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.STONE_AXE, Items.WOODEN_AXE);
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
    protected boolean useQuickStopKeybind() {
        return true;
    }

    @Override
    protected boolean allowQuickStop() {
        return this.step != Steps.WALKING;
    }

    @Override
    public void onActivate() {
        super.onActivate();
        if (this.targetEnchants.get().isEmpty()) {
            this.warning("未设置目标附魔", new Object[0]);
            this.toggle();
            return;
        }
        this.step = Steps.FINDING_TARGET;
        this.info("开始刷附魔书", new Object[0]);
    }

    private void findingTarget() {
        if (this.targetEnchants.get().isEmpty()) {
            this.info("§a所有目标附魔书已找到！", new Object[0]);
            this.toggle();
            return;
        }
        this.currentTarget = this.findNearestValidVillager();
        if (this.currentTarget == null) {
            this.warning("附近没有符合条件的失业村民", new Object[0]);
            this.toggle();
            return;
        }
        this.printLog("找到目标村民: " + String.valueOf(this.currentTarget.getUuid()) + " 朝向: " + String.valueOf(this.currentTarget.getFacing()));
        this.delayNext(Steps.GOTO_TARGET);
    }

    private void gotoTarget() {
        if (this.currentTarget == null) {
            this.step = Steps.FINDING_TARGET;
            return;
        }
        double distance = this.mc.player.getEntityPos().distanceTo(this.currentTarget.getOperatePos().toCenterPos());
        if (distance <= 1.5) {
            this.delayNext(Steps.PLACE_LECTERN);
            return;
        }
        this.gotoTargetIfNeed(this.currentTarget.getOperatePos(), 0, Steps.PLACE_LECTERN, "前往目标村民");
    }

    private void placeLectern() {
        if (this.currentTarget == null || this.currentTarget.getWorkPos() == null) {
            this.step = Steps.FINDING_TARGET;
            return;
        }
        BlockState workPosState = this.mc.world.getBlockState(this.currentTarget.getWorkPos());
        if (!workPosState.isAir()) {
            if (workPosState.getBlock() instanceof LecternBlock) {
                this.step = Steps.WAIT_PROFESSION;
            } else {
                this.breakStep("<讲台位置>被占用");
            }
            return;
        }
        HeInvUtils.swapItemToSelectedSlot(Items.LECTERN);
        boolean placed = HeBlockUtils.clickAdjacentBlock(this.currentTarget.getWorkPos());
        if (placed) {
            this.printLog("放置讲台成功");
            this.professionWaitStartTime = System.currentTimeMillis();
            this.step = Steps.WAIT_PROFESSION;
        } else {
            this.warning("放置讲台失败，重试中...", new Object[0]);
        }
        this.setDelay();
    }

    private void waitProfession() {
        if (this.currentTarget == null) {
            this.step = Steps.FINDING_TARGET;
            return;
        }
        VillagerEntity villager = this.currentTarget.getVillager();
        if (villager == null) {
            this.step = Steps.FINDING_TARGET;
            return;
        }
        if (System.currentTimeMillis() - this.professionWaitStartTime > this.professionTimeout.get().intValue()) {
            this.warning("等待村民获得职业超时，挖掉讲台重试", new Object[0]);
            this.delayNext(Steps.BREAK_LECTERN);
            return;
        }
        Optional professionKey = villager.getVillagerData().profession().getKey();
        if (professionKey.isPresent() && professionKey.get() == VillagerProfession.LIBRARIAN) {
            this.printLog("村民已成为图书管理员");
            this.delayNext(Steps.OPEN_TRADE);
            return;
        }
        this.setDelay();
    }

    private void openTrade() {
        if (this.currentTarget == null) {
            this.step = Steps.FINDING_TARGET;
            return;
        }
        VillagerEntity villager = this.currentTarget.getVillager();
        if (villager == null) {
            this.step = Steps.FINDING_TARGET;
            return;
        }
        if (!villager.isAlive()) {
            this.warning("村民已死亡", new Object[0]);
            this.delayCloseNext(Steps.FINDING_TARGET);
            return;
        }
        Vec3d playerEyePos = this.mc.player.getEyePos();
        Vec3d villagerEyePos = villager.getEyePos();
        EntityHitResult entityHitResult = ProjectileUtil.raycast(this.mc.player, playerEyePos, villagerEyePos, villager.getBoundingBox(), Entity::canHit, playerEyePos.squaredDistanceTo(villagerEyePos));
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

    private void checkTrades() {
        ScreenHandler screenHandler = this.mc.player.currentScreenHandler;
        if (!(screenHandler instanceof MerchantScreenHandler)) {
            this.delayNext(Steps.OPEN_TRADE);
            return;
        }
        MerchantScreenHandler handler = (MerchantScreenHandler)screenHandler;
        var tradeOfferList = handler.getRecipes();
        Set targetEnchantSet = this.targetEnchants.get();
        for (int i = 0; i < tradeOfferList.size(); ++i) {
            ItemEnchantmentsComponent storedEnchants;
            TradeOffer offer = (TradeOffer)tradeOfferList.get(i);
            ItemStack sellItem = offer.getSellItem();
            if (!sellItem.isOf(Items.ENCHANTED_BOOK) || offer.getUses() >= offer.getMaxUses() || (storedEnchants = (ItemEnchantmentsComponent)sellItem.get(DataComponentTypes.STORED_ENCHANTMENTS)) == null) continue;
            for (Object2IntMap.Entry entry : storedEnchants.getEnchantmentEntries()) {
                RegistryKey enchantKey;
                RegistryEntry enchantEntry = (RegistryEntry)entry.getKey();
                int level = entry.getIntValue();
                Optional enchantKeyOptional = enchantEntry.getKey();
                if (enchantKeyOptional.isEmpty() || !targetEnchantSet.contains(enchantKey = (RegistryKey)enchantKeyOptional.get())) continue;
                int maxLevel = ((Enchantment)enchantEntry.value()).getMaxLevel();
                if (level < maxLevel) {
                    this.printLog("找到附魔但等级不足: " + String.valueOf(enchantKey.getValue()) + " " + level + "/" + maxLevel);
                    continue;
                }
                int cost = offer.getOriginalFirstBuyItem().getCount();
                if (cost > this.maxCost.get()) {
                    this.printLog("找到附魔但价格过高: " + cost + " > " + String.valueOf(this.maxCost.get()));
                    continue;
                }
                String enchantName = Names.get(enchantKey);
                this.info("§a找到目标附魔书: §f" + enchantName + " §a价格: §f" + cost, new Object[0]);
                if (this.playSound.get()) {
                    this.mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.0f));
                }
                if (this.removeWhenFound.get()) {
                    targetEnchantSet.remove(enchantKey);
                    this.printLog("已从目标列表中移除: " + enchantName);
                }
                this.tradeIndex = i;
                this.delayNext(Steps.EXECUTE_TRADE);
                return;
            }
        }
        this.printLog("未找到目标附魔书，挖掉讲台重试");
        this.delayCloseNext(Steps.BREAK_LECTERN);
    }

    private void executeTrade() {
        ItemStack secondItem;
        ScreenHandler screenHandler = this.mc.player.currentScreenHandler;
        if (!(screenHandler instanceof MerchantScreenHandler)) {
            this.delayNext(Steps.OPEN_TRADE);
            return;
        }
        MerchantScreenHandler handler = (MerchantScreenHandler)screenHandler;
        var tradeOfferList = handler.getRecipes();
        if (this.tradeIndex >= tradeOfferList.size()) {
            this.delayCloseNext(Steps.BREAK_LECTERN);
            return;
        }
        TradeOffer offer = (TradeOffer)tradeOfferList.get(this.tradeIndex);
        FindItemResult emeraldResult = InvUtils.find(new Item[]{Items.EMERALD});
        int requiredCount = offer.getOriginalFirstBuyItem().getCount();
        int emeraldCount = emeraldResult.found() ? emeraldResult.count() : 0;
        ItemStack slot0 = handler.getSlot(0).getStack();
        if (slot0.isOf(Items.EMERALD)) {
            emeraldCount += slot0.getCount();
        }
        if (emeraldCount < requiredCount) {
            this.warning("绿宝石不足，需要 " + requiredCount + " 个", new Object[0]);
            this.toggle();
            return;
        }
        if (offer.getSecondBuyItem().isPresent() && (secondItem = ((TradedItem)offer.getSecondBuyItem().get()).itemStack()).isOf(Items.BOOK) && !(InvUtils.find(new Item[]{Items.BOOK})).found()) {
            this.warning("需要书作为交易材料，但背包中没有", new Object[0]);
            this.toggle();
            return;
        }
        handler.setRecipeIndex(this.tradeIndex);
        handler.switchTo(this.tradeIndex);
        this.mc.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(this.tradeIndex));
        FindItemResult emptyResult = InvUtils.findEmpty();
        if (!emptyResult.found()) {
            this.warning("背包没有格子了", new Object[0]);
            this.toggle();
            return;
        }
        InvUtils.move().fromId(2).to(emptyResult.slot());
        this.info("§a已锁定交易！", new Object[0]);
        this.delayCloseNext(Steps.FINDING_TARGET);
    }

    private void breakLectern() {
        if (this.currentTarget == null || this.currentTarget.getWorkPos() == null) {
            this.delayCloseNext(Steps.FINDING_TARGET);
            return;
        }
        BlockPos lecternPos = this.currentTarget.getWorkPos();
        BlockState state = this.mc.world.getBlockState(lecternPos);
        if (!state.isOf(Blocks.LECTERN)) {
            this.clearProfessionWaitStartTime = System.currentTimeMillis();
            this.delayNext(Steps.WAIT_PROFESSION_CLEAR);
            return;
        }
        boolean hasAxe = this.ensureItemInMainHand((ItemStack itemStack) -> axeTypes.contains(itemStack.getItem()), "未找到<斧头>");
        if (!hasAxe) {
            return;
        }
        HeRotationUtils.rotate(lecternPos, () -> BlockUtils.breakBlock(lecternPos, true));
    }

    private void waitProfessionClear() {
        if (this.currentTarget == null) {
            this.delayCloseNext(Steps.FINDING_TARGET);
            return;
        }
        VillagerEntity villager = this.currentTarget.getVillager();
        if (villager == null) {
            this.delayCloseNext(Steps.FINDING_TARGET);
            return;
        }
        if (System.currentTimeMillis() - this.clearProfessionWaitStartTime > this.professionTimeout.get().intValue()) {
            this.printLog("等待失业超时，继续下一步");
            this.delayCloseNext(Steps.FINDING_TARGET);
            return;
        }
        Optional professionKey = villager.getVillagerData().profession().getKey();
        if (professionKey.isPresent() && professionKey.get() == VillagerProfession.NONE) {
            this.printLog("村民已失业，继续刷取");
            this.delayNext(Steps.PLACE_LECTERN);
            return;
        }
        this.setDelay();
    }

    private VillagerEntityWarp findNearestValidVillager() {
        VillagerEntityWarp nearestTarget = null;
        double nearestDistance = Double.MAX_VALUE;
        List<VillagerEntity> villagerList = this.mc.world.getEntitiesByClass(VillagerEntity.class, this.mc.player.getBoundingBox().expand(this.searchRange.get().intValue()), frame -> true);
        for (VillagerEntity villager : villagerList) {
            double distance;
            BlockPos lecternPos;
            Optional professionKey = villager.getVillagerData().profession().getKey();
            if (professionKey.isEmpty() || professionKey.get() != VillagerProfession.NONE) continue;
            BlockPos villagerPos = villager.getBlockPos();
            Direction validDirection = null;
            BlockPos workPos = null;
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos offsetPos;
                BlockState lecternState;
                BlockState offsetUpState;
                lecternPos = villagerPos.offset(dir);
                BlockPos magmaPos = lecternPos.down();
                BlockState magmaState = this.mc.world.getBlockState(magmaPos);
                if (!magmaState.isOf(Blocks.MAGMA_BLOCK) || !(lecternState = this.mc.world.getBlockState(lecternPos)).isAir() || !(offsetUpState = this.mc.world.getBlockState((offsetPos = lecternPos.offset(dir)).up())).isAir()) continue;
                validDirection = dir;
                workPos = lecternPos;
                break;
            }
            if (validDirection == null || !((distance = this.mc.player.getEntityPos().distanceTo(villagerPos.toCenterPos())) < nearestDistance)) continue;
            nearestDistance = distance;
            BlockPos operatePos = workPos.offset(validDirection);
            nearestTarget = new VillagerEntityWarp(VillagerType.图书管理员, villager.getUuid(), operatePos, validDirection, workPos);
        }
        return nearestTarget;
    }

    private boolean checkLecternReady() {
        FindItemResult findItemResult = InvUtils.find(new Item[]{Items.LECTERN});
        if (!findItemResult.found()) {
            this.error("缺少<讲台>", new Object[0]);
            this.toggle();
            return false;
        }
        if (!findItemResult.isMainHand()) {
            HeInvUtils.swap(findItemResult.slot(), this.getMainSlot());
            return false;
        }
        return true;
    }

    private boolean checkAxeReady() {
        FindItemResult findItemResult;
        List<Item> axeList = List.of(Items.NETHERITE_AXE, Items.DIAMOND_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.STONE_AXE, Items.WOODEN_AXE);
        for (Item axe : axeList) {
            findItemResult = InvUtils.findInHotbar(new Item[]{axe});
            if (!findItemResult.found()) continue;
            InvUtils.swap(findItemResult.slot(), false);
            return false;
        }
        for (Item axe : axeList) {
            findItemResult = InvUtils.find(new Item[]{axe});
            if (!findItemResult.found()) continue;
            InvUtils.move().from(findItemResult.slot()).to(HeInvUtils.getMainSlot());
            return false;
        }
        return true;
    }

    private void printLog(String message) {
        if (this.debug.get()) {
            log.info(message);
        }
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        this.currentTarget = null;
    }

    public String getInfoString() {
        if (this.currentTarget != null) {
            return "刷取中";
        }
        return "搜索中";
    }
}
