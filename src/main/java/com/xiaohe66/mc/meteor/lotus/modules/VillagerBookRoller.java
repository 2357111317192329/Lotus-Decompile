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
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagerBookRoller extends WalkModule {
    private static final Logger log = LoggerFactory.getLogger(VillagerBookRoller.class);
    private static final Set<ResourceKey<Enchantment>> TREASURE_ENCHANTS = Set.of(Enchantments.BINDING_CURSE, Enchantments.FROST_WALKER, Enchantments.MENDING, Enchantments.VANISHING_CURSE);
    private final Setting<Set<ResourceKey<Enchantment>>> targetEnchants = sgGeneral.add(new EnchantmentListSetting.Builder()
        .name("目标附魔")
        .description("想要刷取的附魔书类型（等级自动使用最高等级），默认排除消失诅咒、绑定诅咒、冰霜行者")
        .defaultValue(new ResourceKey[]{Enchantments.PROTECTION, Enchantments.FEATHER_FALLING, Enchantments.BLAST_PROTECTION, Enchantments.RESPIRATION, Enchantments.AQUA_AFFINITY, Enchantments.DEPTH_STRIDER, Enchantments.THORNS, Enchantments.SHARPNESS, Enchantments.KNOCKBACK, Enchantments.FIRE_ASPECT, Enchantments.LOOTING, Enchantments.SWEEPING_EDGE, Enchantments.EFFICIENCY, Enchantments.SILK_TOUCH, Enchantments.FORTUNE, Enchantments.MENDING, Enchantments.UNBREAKING})
        .build());
    private final Setting<PriceLimitMode> priceLimitMode = sgGeneral.add(new EnumSetting.Builder<PriceLimitMode>()
        .name("价格上限模式")
        .description("绝对上限: 固定绿宝石数量上限; 相对上限: 按附魔种类与等级计算上限(基础价格3n+2, 宝藏附魔再乘2, 再乘倍率)")
        .defaultValue(PriceLimitMode.绝对上限)
        .build());
    private final Setting<Integer> maxCost = sgGeneral.add(new IntSetting.Builder()
        .name("价格上限(绝对)")
        .description("可接受的最高价格（绿宝石数量），仅在绝对上限模式下生效")
        .range(1, 64)
        .sliderRange(1, 64)
        .defaultValue(21)
        .visible(() -> this.priceLimitMode.get() == PriceLimitMode.绝对上限)
        .build());
    private final Setting<Double> priceMultiplier = sgGeneral.add(new DoubleSetting.Builder()
        .name("价格倍率(相对)")
        .description("相对上限模式的倍率: 上限 = 基础价格 × 倍率。基础价格 = 3×最高等级+2, 宝藏附魔(绑定诅咒/冰霜行者/修补/消失诅咒)再×2")
        .range(1.0, 13.0)
        .sliderRange(1.0, 13.0)
        .defaultValue(2.0)
        .visible(() -> this.priceLimitMode.get() == PriceLimitMode.相对上限)
        .build());
    private final Setting<Integer> searchRange = sgGeneral.add(new IntSetting.Builder()
        .name("搜索范围")
        .description("搜索村民的范围")
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
    private long pickupWaitStartTime;
    private long pickupMoveStartTime;
    private long pickupNotFoundStartTime;
    private Vec3 pickupMoveStartPos;
    private BlockPos pickupMoveTarget;
    private BlockPos pickupBadPos;
    private Vec3 pickupDropPos;
    private long placeMoveStartTime;
    private Vec3 placeMoveStartPos;
    private BlockPos placeMoveTarget;
    private BlockPos microAdjustTarget;
    private long microAdjustStartTime;
    private boolean microAdjustKeysHeld;
    private int tradeIndex;

    public VillagerBookRoller() {
        super("A刷附魔书", "自动寻找失业村民，放置讲台刷取指定附魔书. (村民旁有上表面完整的方块可放置讲台，且目标位置未被占用)");
        this.addStep(Steps.FINDING_TARGET, this::findingTarget);
        this.addStep(Steps.GOTO_TARGET, this::gotoTarget);
        this.addStep(Steps.PLACE_LECTERN, this::placeLectern);
        this.addStep(Steps.WAIT_PROFESSION, this::waitProfession);
        this.addStep(Steps.OPEN_TRADE, this::openTrade);
        this.addStep(Steps.CHECK_TRADES, this::checkTrades);
        this.addStep(Steps.EXECUTE_TRADE, this::executeTrade);
        this.addStep(Steps.BREAK_LECTERN, this::breakLectern);
        this.addStep(Steps.PICKUP_LECTERN, this::pickupLectern);
        this.addStep(Steps.WAIT_PROFESSION_CLEAR, this::waitProfessionClear);
        this.addStep(Steps.GOTO_PLACE_POS, this::gotoPlacePos);
        this.addStep(Steps.MICRO_ADJUST, this::microAdjust);
        this.addStep(Steps.WALKING, this::checkWalkingStuck);
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
        double distance = this.mc.player.position().distanceTo(this.currentTarget.getOperatePos().getCenter());
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
        BlockState workPosState = this.mc.level.getBlockState(this.currentTarget.getWorkPos());
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
            // 放置失敗: 很可能是碰撞箱與講台放置面相交(沒站到格中心), 調整位置後重試
            this.releaseMicroAdjustKeys();
            this.warning("放置讲台失败，调整位置后重试", new Object[0]);
            BlockPos standPos = this.findPlaceViewPos();
            if (standPos == null) {
                this.delayCloseNext(Steps.FINDING_TARGET);
            } else if (this.isPlayerInBlock(standPos)) {
                // 玩家已在目標格內 → 微調到格中心
                this.startMicroAdjust(standPos);
            } else {
                // 玩家不在目標格內(跨格) → 先由 Baritone range=0 尋路到目標格
                if (!standPos.equals(this.placeMoveTarget)) {
                    this.placeMoveTarget = standPos;
                    this.placeMoveStartTime = System.currentTimeMillis();
                    this.placeMoveStartPos = this.mc.player.position();
                }
                this.gotoTargetIfNeed(standPos, 0, Steps.PLACE_LECTERN, "前往讲台放置位置");
            }
        }
        this.setDelay();
    }

    private void waitProfession() {
        if (this.currentTarget == null) {
            this.step = Steps.FINDING_TARGET;
            return;
        }
        Villager villager = this.currentTarget.getVillager();
        if (villager == null) {
            this.step = Steps.FINDING_TARGET;
            return;
        }
        if (System.currentTimeMillis() - this.professionWaitStartTime > this.professionTimeout.get().intValue()) {
            this.warning("等待村民获得职业超时，挖掉讲台重试", new Object[0]);
            this.delayNext(Steps.BREAK_LECTERN);
            return;
        }
        Optional professionKey = villager.getVillagerData().profession().unwrapKey();
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
        Villager villager = this.currentTarget.getVillager();
        if (villager == null) {
            this.step = Steps.FINDING_TARGET;
            return;
        }
        if (!villager.isAlive()) {
            this.warning("村民已死亡", new Object[0]);
            this.delayCloseNext(Steps.FINDING_TARGET);
            return;
        }
        Vec3 playerEyePos = this.mc.player.getEyePosition();
        Vec3 villagerEyePos = villager.getEyePosition();
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(this.mc.player, playerEyePos, villagerEyePos, villager.getBoundingBox(), Entity::isPickable, playerEyePos.distanceToSqr(villagerEyePos));
        if (entityHitResult == null) {
            HeRotationUtils.rotate(villager.getEyePosition(), () -> {
                EntityHitResult location = new EntityHitResult(villager, villager.getBoundingBox().getCenter());
                this.mc.gameMode.interact(this.mc.player, villager,location, InteractionHand.MAIN_HAND);
                this.step = Steps.CHECK_TRADES;
            });
        } else {
            HeRotationUtils.rotate(entityHitResult.getLocation(), () -> {
                InteractionResult actionResult = this.mc.gameMode.interact(this.mc.player, villager, entityHitResult, InteractionHand.MAIN_HAND);
                if (!actionResult.consumesAction()) {
                    EntityHitResult location2 = new EntityHitResult(villager, villager.getBoundingBox().getCenter());
                    this.mc.gameMode.interact(this.mc.player, villager,location2, InteractionHand.MAIN_HAND);
                }
                this.step = Steps.CHECK_TRADES;
            });
        }
    }

    /*
     * 依價格上限模式計算該附魔書可接受的最高價格:
     * 绝对上限: 固定 maxCost 值
     * 相对上限: 基础价格(3×最高等级+2, 宝藏附魔再×2) × 倍率, 取整數
     */
    private int getPriceLimit(ResourceKey<Enchantment> enchantKey, int maxLevel) {
        if (this.priceLimitMode.get() == PriceLimitMode.相对上限) {
            int basePrice = 3 * maxLevel + 2;
            if (TREASURE_ENCHANTS.contains(enchantKey)) {
                basePrice *= 2;
            }
            return (int)Math.floor(basePrice * this.priceMultiplier.get().doubleValue());
        }
        return this.maxCost.get().intValue();
    }

    private void checkTrades() {
        AbstractContainerMenu screenHandler = this.mc.player.containerMenu;
        if (!(screenHandler instanceof MerchantMenu)) {
            this.delayNext(Steps.OPEN_TRADE);
            return;
        }
        MerchantMenu handler = (MerchantMenu)screenHandler;
        var tradeOfferList = handler.getOffers();
        Set targetEnchantSet = this.targetEnchants.get();
        for (int i = 0; i < tradeOfferList.size(); ++i) {
            ItemEnchantments storedEnchants;
            MerchantOffer offer = (MerchantOffer)tradeOfferList.get(i);
            ItemStack sellItem = offer.getResult();
            if (!sellItem.is(Items.ENCHANTED_BOOK) || offer.getUses() >= offer.getMaxUses() || (storedEnchants = (ItemEnchantments)sellItem.get(DataComponents.STORED_ENCHANTMENTS)) == null) continue;
            for (Object2IntMap.Entry entry : storedEnchants.entrySet()) {
                ResourceKey enchantKey;
                Holder enchantEntry = (Holder)entry.getKey();
                int level = entry.getIntValue();
                Optional enchantKeyOptional = enchantEntry.unwrapKey();
                if (enchantKeyOptional.isEmpty() || !targetEnchantSet.contains(enchantKey = (ResourceKey)enchantKeyOptional.get())) continue;
                int maxLevel = ((Enchantment)enchantEntry.value()).getMaxLevel();
                if (level < maxLevel) {
                    this.printLog("找到附魔但等级不足: " + String.valueOf(enchantKey.identifier()) + " " + level + "/" + maxLevel);
                    continue;
                }
                int cost = offer.getBaseCostA().getCount();
                int priceLimit = this.getPriceLimit(enchantKey, maxLevel);
                if (cost > priceLimit) {
                    this.printLog("找到附魔但价格过高: " + cost + " > " + String.valueOf(priceLimit));
                    continue;
                }
                String enchantName = Names.get(enchantKey);
                this.info("§a找到目标附魔书: §f" + enchantName + " §a价格: §f" + cost, new Object[0]);
                if (this.playSound.get()) {
                    this.mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, 1.0f));
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
        AbstractContainerMenu screenHandler = this.mc.player.containerMenu;
        if (!(screenHandler instanceof MerchantMenu)) {
            this.delayNext(Steps.OPEN_TRADE);
            return;
        }
        MerchantMenu handler = (MerchantMenu)screenHandler;
        var tradeOfferList = handler.getOffers();
        if (this.tradeIndex >= tradeOfferList.size()) {
            this.delayCloseNext(Steps.BREAK_LECTERN);
            return;
        }
        MerchantOffer offer = (MerchantOffer)tradeOfferList.get(this.tradeIndex);
        FindItemResult emeraldResult = InvUtils.find(new Item[]{Items.EMERALD});
        int requiredCount = offer.getBaseCostA().getCount();
        int emeraldCount = emeraldResult.found() ? emeraldResult.count() : 0;
        ItemStack slot0 = handler.getSlot(0).getItem();
        if (slot0.is(Items.EMERALD)) {
            emeraldCount += slot0.getCount();
        }
        if (emeraldCount < requiredCount) {
            this.warning("绿宝石不足，需要 " + requiredCount + " 个", new Object[0]);
            this.toggle();
            return;
        }
        if (offer.getItemCostB().isPresent() && (secondItem = ((ItemCost)offer.getItemCostB().get()).itemStack()).is(Items.BOOK) && !(InvUtils.find(new Item[]{Items.BOOK})).found()) {
            this.warning("需要书作为交易材料，但背包中没有", new Object[0]);
            this.toggle();
            return;
        }
        handler.setSelectionHint(this.tradeIndex);
        handler.tryMoveItems(this.tradeIndex);
        this.mc.getConnection().send(new ServerboundSelectTradePacket(this.tradeIndex));
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
        BlockState state = this.mc.level.getBlockState(lecternPos);
        if (!state.is(Blocks.LECTERN)) {
            // 講台已挖掉 → 先撿起講台掉落物
            this.delayNext(Steps.PICKUP_LECTERN);
            return;
        }
        // 有斧頭時切換到斧頭挖, 沒有斧頭就用當前主手(空手也能挖)
        this.swapToMainHand((ItemStack itemStack) -> axeTypes.contains(itemStack.getItem()));
        HeRotationUtils.rotate(lecternPos, () -> BlockUtils.breakBlock(lecternPos, true));
    }

    /*
     * 撿起講台掉落物: 只搜尋講台位置附近小範圍的講台掉落物,
     * 避免走遠去撿遠方的講台掉落物。不需要完全走到掉落物位置,
     * 只要能站到掉落物拾取範圍(約1.5格)內的目標點即可。
     * 目標點不需要視線檢測, 只需要可尋路(排除村民所站格子)。
     * 撿完(或附近沒有掉落物)後進入下一步。
     */
    private void pickupLectern() {
        if (this.currentTarget == null || this.currentTarget.getWorkPos() == null) {
            this.delayCloseNext(Steps.FINDING_TARGET);
            return;
        }
        // 尋路卡住檢測: 長時間不動且未到達 → 目標點不可達, 換一個目標點
        if (this.pickupMoveStartTime != 0L && System.currentTimeMillis() - this.pickupMoveStartTime > 5000L && this.pickupDropPos != null) {
            if (this.mc.player.position().distanceTo(this.pickupDropPos) > 1.5) {
                double moved = this.mc.player.position().distanceTo(this.pickupMoveStartPos);
                if (moved < 0.5) {
                    // 已夠近則直接拾取, 不夠近且不動 → 目標點不可達, 換一個目標點
                    this.info("讲台掉落物目标点不可达, 换一个目标点", new Object[0]);
                    if (this.pickupMoveTarget != null) {
                        this.pickupBadPos = this.pickupMoveTarget;
                    }
                    this.pickupMoveStartTime = 0L;
                    this.pickupMoveTarget = null;
                } else {
                    this.pickupMoveStartTime = System.currentTimeMillis();
                    this.pickupMoveStartPos = this.mc.player.position();
                }
            }
        }
        BlockPos workPos = this.currentTarget.getWorkPos();
        List<ItemEntity> drops = this.mc.level.getEntitiesOfClass(ItemEntity.class, new AABB(workPos).inflate(2.0, 1.0, 2.0), entity -> entity.isAlive() && entity.getItem().is(Items.LECTERN));
        if (drops.isEmpty()) {
            // 剛挖掉講台時掉落物可能尚未同步到客戶端, 先等待一小段時間確認, 避免跳過撿取
            if (this.pickupNotFoundStartTime == 0L) {
                this.pickupNotFoundStartTime = System.currentTimeMillis();
            }
            if (System.currentTimeMillis() - this.pickupNotFoundStartTime <= 3000L) {
                this.setDelay();
                return;
            }
            // 確認附近確實沒有講台掉落物(被撿走/掉落太遠) → 進入等待失業
            this.pickupNotFoundStartTime = 0L;
            this.pickupWaitStartTime = 0L;
            this.pickupMoveStartTime = 0L;
            this.pickupMoveTarget = null;
            this.pickupDropPos = null;
            this.delayNext(Steps.WAIT_PROFESSION_CLEAR);
            return;
        }
        this.pickupNotFoundStartTime = 0L;
        ItemEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ItemEntity drop : drops) {
            double distance = this.mc.player.position().distanceTo(drop.position());
            if (!(distance < nearestDistance)) continue;
            nearest = drop;
            nearestDistance = distance;
        }
        if (nearestDistance <= 1.5) {
            // 已進入拾取範圍: 距離夠近就直接取消尋路(不需精確走到目標點), 等待物品吸入
            if (this.walkingNext == Steps.PICKUP_LECTERN) {
                this.cancelPickupPath();
            }
            this.pickupMoveStartTime = 0L;
            this.pickupMoveTarget = null;
            this.pickupDropPos = nearest.position();
            if (this.pickupWaitStartTime == 0L) {
                this.pickupWaitStartTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - this.pickupWaitStartTime > 5000L) {
                // 超時仍撿不到(可能卡住), 放棄撿取繼續流程
                this.pickupWaitStartTime = 0L;
                this.delayNext(Steps.WAIT_PROFESSION_CLEAR);
                return;
            }
            this.setDelay();
            return;
        }
        // 搜尋靠近掉落物(拾取範圍內)的可站立目標點, 排除村民所站格與已知不可達點
        BlockPos standPos = this.findPickupStandPos(nearest.blockPosition(), this.currentTarget.getVillager(), this.pickupBadPos);
        if (standPos == null) {
            this.info("找不到可接近讲台掉落物的位置, 放弃拾取", new Object[0]);
            this.pickupWaitStartTime = 0L;
            this.pickupMoveStartTime = 0L;
            this.pickupMoveTarget = null;
            this.pickupDropPos = null;
            this.delayNext(Steps.WAIT_PROFESSION_CLEAR);
            return;
        }
        this.pickupWaitStartTime = 0L;
        this.pickupDropPos = nearest.position();
        // 換了新目標點才重置移動計時(避免卡住檢測被反覆重置)
        if (!standPos.equals(this.pickupMoveTarget)) {
            this.pickupMoveTarget = standPos;
            this.pickupMoveStartTime = System.currentTimeMillis();
            this.pickupMoveStartPos = this.mc.player.position();
        }
        // 撿取目標點使用精確尋路(range=0), 避免 range=1 停在目標點1格外超過拾取範圍;
        // 精確尋路可能永遠無法到達, 由 checkPickupStuck/距離兜底 在足夠近時強制停止尋路
        this.gotoTargetIfNeed(standPos, 0, Steps.PICKUP_LECTERN, "捡取讲台掉落物");
    }

    /*
     * 尋路卡住/距離兜底檢測(在 WALKING 期間每 tick 執行):
     * 撿講台: 距離夠近(≤1.5格, 拾取範圍內) → 強制停止尋路直接拾取; 卡住 → 換目標點
     * 放講台: 碰撞箱已不擋講台 → 強制停止尋路直接放置; 卡住 → 回重新尋找村民
     */
    private void checkWalkingStuck() {
        if (this.walkingNext == Steps.PICKUP_LECTERN) {
            this.checkPickupStuck();
        } else if (this.walkingNext == Steps.PLACE_LECTERN) {
            this.checkPlaceStuck();
        }
    }

    private void checkPickupStuck() {
        // 距離兜底: 已進入拾取範圍就停止尋路(精確尋路 range=0 可能永遠無法到達)
        if (this.pickupDropPos != null && this.mc.player.position().distanceTo(this.pickupDropPos) <= 1.5) {
            this.cancelPickupPath();
            return;
        }
        if (this.pickupMoveStartTime == 0L) {
            return;
        }
        if (System.currentTimeMillis() - this.pickupMoveStartTime <= 5000L) {
            return;
        }
        if (this.mc.player.position().distanceTo(this.pickupMoveStartPos) >= 0.5) {
            // 還在移動(可能繞路), 重置計時繼續
            this.pickupMoveStartTime = System.currentTimeMillis();
            this.pickupMoveStartPos = this.mc.player.position();
            return;
        }
        // 卡住: 目標不可達
        this.info("讲台掉落物目标点不可达, 换一个目标点", new Object[0]);
        if (this.pickupMoveTarget != null) {
            this.pickupBadPos = this.pickupMoveTarget;
        }
        this.pickupMoveStartTime = 0L;
        this.pickupMoveTarget = null;
        this.cancelPickupPath();
    }

    /*
     * 放置兜底/卡住檢測(在 WALKING 期間 walkingNext == PLACE_LECTERN 時每 tick 執行):
     * 1. 碰撞箱已不擋講台放置位置 → 強制停止尋路直接放置(不需精確站到格中心)
     * 2. 超過5秒幾乎沒移動且仍未不擋 → 站位不可達, 回重新尋找村民
     */
    private void checkPlaceStuck() {
        // 兜底: 玩家碰撞箱已不擋講台且距離夠近 → 停止尋路, 直接放置
        if (this.isPlacePosReady()) {
            this.walkingNext = null;
            WalkModule.baritone.getCommandManager().execute("cancel");
            this.step = Steps.PLACE_LECTERN;
            this.setDelay();
            return;
        }
        // 已走到目標格內: Baritone range=0 到達後玩家中心已與目標格同格,
        // 停止尋路改用「微調到方塊中心」的方式讓碰撞箱不再擋住講台放置面
        if (this.placeMoveTarget != null && this.isPlayerInBlock(this.placeMoveTarget)) {
            this.walkingNext = null;
            WalkModule.baritone.getCommandManager().execute("cancel");
            this.startMicroAdjust(this.placeMoveTarget);
            return;
        }
        if (this.placeMoveStartTime == 0L) {
            return;
        }
        if (System.currentTimeMillis() - this.placeMoveStartTime <= 5000L) {
            return;
        }
        if (this.mc.player.position().distanceTo(this.placeMoveStartPos) >= 0.5) {
            // 還在移動(可能繞路), 重置計時繼續
            this.placeMoveStartTime = System.currentTimeMillis();
            this.placeMoveStartPos = this.mc.player.position();
            return;
        }
        // 卡住: 站位不可達
        this.warning("讲台放置位置不可达, 重新寻找村民", new Object[0]);
        this.placeMoveStartTime = 0L;
        this.placeMoveTarget = null;
        this.walkingNext = null;
        WalkModule.baritone.getCommandManager().execute("cancel");
        this.step = Steps.FINDING_TARGET;
        this.setDelay();
    }

    /*
     * 停止撿取尋路並回到 PICKUP_LECTERN 狀態(取消後物品吸入, 下個 tick 再次檢查)
     */
    private void cancelPickupPath() {
        this.walkingNext = null;
        WalkModule.baritone.getCommandManager().execute("cancel");
        this.step = Steps.PICKUP_LECTERN;
        this.setDelay();
    }

    /*
     * 在掉落物附近搜尋可站立目標點:
     * 1. 可站立(該格與上方一格都是空氣)
     * 2. 排除村民自身所站/所佔的格子(尋路不到)
     * 3. 排除已知不可達的目標點
     * 4. 距離掉落物在拾取範圍內(≤1.5格), 不需要完全走到掉落物位置
     * 5. 不需要視線檢測
     * 返回距離掉落物最近的可行點; 找不到返回 null。
     */
    private BlockPos findPickupStandPos(BlockPos dropPos, Villager villager, BlockPos excludePos) {
        BlockPos villagerFootPos = villager == null ? null : villager.blockPosition();
        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos standPos = dropPos.offset(dx, dy, dz);
                    // 排除村民自身所站/所佔的格子(不可能尋路到)
                    if (villagerFootPos != null && (standPos.equals(villagerFootPos) || standPos.equals(villagerFootPos.above()))) continue;
                    // 排除已知不可達的目標點
                    if (excludePos != null && standPos.equals(excludePos)) continue;
                    if (!this.mc.level.getBlockState(standPos).isAir() || !this.mc.level.getBlockState(standPos.above()).isAir()) continue;
                    double distance = standPos.getCenter().distanceToSqr(dropPos.getCenter());
                    if (distance > 1.5 * 1.5) continue;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestPos = standPos;
                    }
                }
            }
        }
        return bestPos;
    }

    /*
     * 移動回能放置講台的位置:
     * 1. 自身碰撞箱不能擋住講台的放置位置(排除講台位置本身及上方)
     * 2. 站過去後能點擊到村民上半部(放完講台仍要能看到村民)
     * 找不到合適站位 → 回到重新尋找村民。
     */
    private void gotoPlacePos() {
        if (this.currentTarget == null || this.currentTarget.getWorkPos() == null) {
            this.delayCloseNext(Steps.FINDING_TARGET);
            return;
        }
        BlockPos workPos = this.currentTarget.getWorkPos();
        // 兜底: 玩家碰撞箱已不擋講台放置位置 → 直接放置
        if (this.isPlacePosReady()) {
            this.delayNext(Steps.PLACE_LECTERN);
            return;
        }
        BlockPos standPos = this.findPlaceViewPos();
        if (standPos == null) {
            this.warning("找不到可放置讲台且能看到村民的位置", new Object[0]);
            this.delayCloseNext(Steps.FINDING_TARGET);
            return;
        }
        // 換了新目標點才重置移動計時(避免卡住檢測被反覆重置)
        if (!standPos.equals(this.placeMoveTarget)) {
            this.placeMoveTarget = standPos;
            this.placeMoveStartTime = System.currentTimeMillis();
            this.placeMoveStartPos = this.mc.player.position();
        }
        if (this.isPlayerInBlock(standPos)) {
            // 玩家中心已在目標格內 → 只需要微調到格中心, 讓碰撞箱不再擋住講台放置面
            this.startMicroAdjust(standPos);
            return;
        }
        // 玩家不在目標格內(跨格) → 由 Baritone range=0 尋路走到目標格, 到達後再微調
        this.gotoTargetIfNeed(standPos, 0, Steps.PLACE_LECTERN, "前往讲台放置位置");
    }

    /*
     * 玩家中心(腳底)是否已在目標格內(水平方向同格)
     */
    private boolean isPlayerInBlock(BlockPos pos) {
        return Math.floor(this.mc.player.getX()) == (double)pos.getX() && Math.floor(this.mc.player.getZ()) == (double)pos.getZ();
    }

    /*
     * 開始微調: 記錄目標格中心, 進入 MICRO_ADJUST 步驟。
     * 微調直接操作玩家移動(面向目標 + 前進 + 潜行減速), 不依賴 Baritone。
     */
    private void startMicroAdjust(BlockPos standPos) {
        this.microAdjustTarget = standPos;
        this.microAdjustStartTime = System.currentTimeMillis();
        this.step = Steps.MICRO_ADJUST;
        this.setDelay();
    }

    /*
     * 微調到方塊中心: 只在玩家中心已在目標格內時使用(同格微調)。
     * 開始時轉一次頭面向格中心, 之後保持朝向按前進+潜行走直線, 到達中心附近即停止。
     * 不會每 tick 重新面向目標, 避免越過中心時方向突變造成反覆來回轉頭。
     * 玩家不在目標格內(被推動/偏移) → 退回 Baritone range=0 尋路。
     */
    private void microAdjust() {
        if (this.microAdjustTarget == null || this.currentTarget == null) {
            this.releaseMicroAdjustKeys();
            this.delayCloseNext(Steps.FINDING_TARGET);
            return;
        }
        BlockPos standPos = this.microAdjustTarget;
        // 玩家中心已不在目標格內 → 不該微調, 改用 Baritone range=0 尋路
        if (!this.isPlayerInBlock(standPos)) {
            this.releaseMicroAdjustKeys();
            this.microAdjustTarget = null;
            this.gotoTargetIfNeed(standPos, 0, Steps.PLACE_LECTERN, "前往讲台放置位置");
            return;
        }
        // 超時保護: 5秒仍沒到中心 → 放棄
        if (System.currentTimeMillis() - this.microAdjustStartTime > 5000L) {
            this.releaseMicroAdjustKeys();
            this.warning("微调讲台放置位置超时, 重新寻找村民", new Object[0]);
            this.microAdjustTarget = null;
            this.delayCloseNext(Steps.FINDING_TARGET);
            return;
        }
        Vec3 targetCenter = new Vec3((double)standPos.getX() + 0.5, this.mc.player.getY(), (double)standPos.getZ() + 0.5);
        Vec3 pos = this.mc.player.position();
        double dx = targetCenter.x - pos.x;
        double dz = targetCenter.z - pos.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.2) {
            // 已到格中心附近: 碰撞箱(中心±0.3)完全在格內, 不會再與講台放置面相交
            this.releaseMicroAdjustKeys();
            this.microAdjustTarget = null;
            BlockPos workPos = this.currentTarget.getWorkPos();
            if (workPos == null) {
                this.delayCloseNext(Steps.FINDING_TARGET);
                return;
            }
            // 先平滑旋轉到講台放置面(workPos 中心), 旋轉完成後再放置
            HeRotationUtils.rotate(workPos.getCenter(), () -> {
                this.step = Steps.PLACE_LECTERN;
                this.setDelay();
            });
            return;
        }
        // 只在第一次(尚未按住按鍵)時面向目標一次, 之後保持朝向走直線, 不再重定向
        if (!this.microAdjustKeysHeld) {
            float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
            this.mc.player.setYRot(yaw);
            this.mc.player.setYHeadRot(yaw);
            this.mc.options.keyUp.setDown(true);
            this.mc.options.keyShift.setDown(true);
            this.microAdjustKeysHeld = true;
        }
        this.setDelay();
    }

    /*
     * 釋放微調期間按住的按鍵(前進/潜行), 避免卡鍵
     */
    private void releaseMicroAdjustKeys() {
        if (this.microAdjustKeysHeld) {
            this.mc.options.keyUp.setDown(false);
            this.mc.options.keyShift.setDown(false);
            this.microAdjustKeysHeld = false;
        }
    }

    /*
     * 放置兜底判定: 玩家碰撞箱是否已不擋講台放置位置(且距離夠近可交互)。
     * 0.6x0.6 的碰撞箱完全能裝進一格, 相交只是因為沒站到格中心,
     * 此時不應排除座標, 而應微調位置(range=0 尋路)直到碰撞箱不擋。
     */
    private boolean isPlacePosReady() {
        BlockPos workPos = this.currentTarget == null ? null : this.currentTarget.getWorkPos();
        if (workPos == null) {
            return false;
        }
        AABB playerBox = this.mc.player.getBoundingBox();
        AABB workBox = new AABB(workPos);
        if (playerBox.intersects(workBox)) {
            return false;
        }
        // 距離夠近才能點擊放置
        return this.mc.player.position().distanceTo(workPos.getCenter()) <= 3.0;
    }

    /*
     * 在講台位置周圍搜尋站立點:
     * 1. 可站立(該格與上方一格都是空氣)
     * 2. 排除講台位置本身及上方(自身碰撞箱不能擋住講台的放置位置)
     * 3. 排除村民自身所佔格子
     * 4. 從該點眼睛位置向村民上半部(眼睛)發射線, 命中的第一個實體必須是該村民
     * 返回距離最近的可行點; 找不到返回 null。
     */
    private BlockPos findPlaceViewPos() {
        Villager villager = this.currentTarget.getVillager();
        BlockPos workPos = this.currentTarget.getWorkPos();
        if (villager == null || workPos == null) {
            return null;
        }
        BlockPos villagerFootPos = villager.blockPosition();
        Vec3 targetEye = villager.getEyePosition();
        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos standPos = workPos.offset(dx, dy, dz);
                    // 排除玩家中心所在格 == 講台位置(自身中心站進講台格必擋住放置)
                    if (standPos.equals(workPos)) continue;
                    // 排除村民自身所站/所佔的格子
                    if (standPos.equals(villagerFootPos) || standPos.equals(villagerFootPos.above())) continue;
                    if (!this.mc.level.getBlockState(standPos).isAir() || !this.mc.level.getBlockState(standPos.above()).isAir()) continue;
                    Vec3 eyePos = standPos.getCenter().add(0.0, 1.62, 0.0);
                    double distance = eyePos.distanceToSqr(targetEye);
                    if (distance > 4.5 * 4.5) continue;
                    EntityHitResult hit = ProjectileUtil.getEntityHitResult(this.mc.player, eyePos, targetEye, villager.getBoundingBox(), Entity::isPickable, distance);
                    if (hit == null || hit.getEntity() != villager) continue;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestPos = standPos;
                    }
                }
            }
        }
        return bestPos;
    }

    private void waitProfessionClear() {
        if (this.currentTarget == null) {
            this.delayCloseNext(Steps.FINDING_TARGET);
            return;
        }
        Villager villager = this.currentTarget.getVillager();
        if (villager == null) {
            this.delayCloseNext(Steps.FINDING_TARGET);
            return;
        }
        if (System.currentTimeMillis() - this.clearProfessionWaitStartTime > this.professionTimeout.get().intValue()) {
            this.printLog("等待失业超时，继续下一步");
            this.delayCloseNext(Steps.FINDING_TARGET);
            return;
        }
        Optional professionKey = villager.getVillagerData().profession().unwrapKey();
        if (professionKey.isPresent() && professionKey.get() == VillagerProfession.NONE) {
            this.printLog("村民已失业，移动回讲台放置位置");
            this.delayNext(Steps.GOTO_PLACE_POS);
            return;
        }
        this.setDelay();
    }

    private VillagerEntityWarp findNearestValidVillager() {
        VillagerEntityWarp nearestTarget = null;
        double nearestDistance = Double.MAX_VALUE;
        List<Villager> villagerList = this.mc.level.getEntitiesOfClass(Villager.class, this.mc.player.getBoundingBox().inflate(this.searchRange.get().intValue()), frame -> true);
        for (Villager villager : villagerList) {
            double distance;
            BlockPos lecternPos;
            Optional professionKey = villager.getVillagerData().profession().unwrapKey();
            if (professionKey.isEmpty() || professionKey.get() != VillagerProfession.NONE) continue;
            BlockPos villagerPos = villager.blockPosition();
            Direction validDirection = null;
            BlockPos workPos = null;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos offsetPos;
                BlockState lecternState;
                BlockState offsetUpState;
                lecternPos = villagerPos.relative(dir);
                BlockPos supportPos = lecternPos.below();
                if (!Block.canSupportCenter(this.mc.level, supportPos, Direction.UP) || !(lecternState = this.mc.level.getBlockState(lecternPos)).isAir() || !(offsetUpState = this.mc.level.getBlockState((offsetPos = lecternPos.relative(dir)).above())).isAir()) continue;
                validDirection = dir;
                workPos = lecternPos;
                break;
            }
            if (validDirection == null || !((distance = this.mc.player.position().distanceTo(villagerPos.getCenter())) < nearestDistance)) continue;
            nearestDistance = distance;
            BlockPos operatePos = workPos.relative(validDirection);
            nearestTarget = new VillagerEntityWarp(VillagerType.图书管理员, villager.getUUID(), operatePos, validDirection, workPos);
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
        this.releaseMicroAdjustKeys();
        this.currentTarget = null;
    }

    public String getInfoString() {
        if (this.currentTarget != null) {
            return "刷取中";
        }
        return "搜索中";
    }
    public static enum PriceLimitMode {
        绝对上限,
        相对上限
    }
}
