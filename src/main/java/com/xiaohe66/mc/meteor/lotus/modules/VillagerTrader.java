/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  baritone.api.event.listener.AbstractGameEventListener
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.utils.Utils
 *  meteordevelopment.meteorclient.utils.misc.Names
 *  meteordevelopment.meteorclient.utils.player.ChatUtils
 *  meteordevelopment.meteorclient.utils.player.FindItemResult
 *  meteordevelopment.meteorclient.utils.player.InvUtils
 *  net.minecraft.util.Hand
 *  net.minecraft.util.ActionResult
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityType
 *  net.minecraft.entity.decoration.ItemFrameEntity
 *  net.minecraft.entity.passive.VillagerEntity
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.entity.player.PlayerInventory
 *  net.minecraft.entity.projectile.ProjectileUtil
 *  net.minecraft.screen.ScreenHandler
 *  net.minecraft.screen.MerchantScreenHandler
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.enchantment.Enchantment
 *  net.minecraft.village.TradeOffer
 *  net.minecraft.village.TradeOfferList
 *  net.minecraft.world.GameMode
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.util.math.Position
 *  net.minecraft.util.math.Box
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.block.entity.BlockEntityType
 *  net.minecraft.network.packet.Packet
 *  net.minecraft.block.BlockState
 *  net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.village.VillagerProfession
 *  net.minecraft.util.hit.EntityHitResult
 *  net.minecraft.registry.RegistryKey
 *  net.minecraft.registry.entry.RegistryEntry
 *  net.minecraft.village.TradedItem
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import baritone.api.event.listener.AbstractGameEventListener;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.modules.villager.VillagerEntityWarp;
import com.xiaohe66.mc.meteor.lotus.modules.villager.VillagerSettingWarp;
import com.xiaohe66.mc.meteor.lotus.modules.villager.VillagerType;
import com.xiaohe66.mc.meteor.lotus.util.EnchantmentUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HePosUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeRotationUtils;
import com.xiaohe66.mc.meteor.lotus.modules.WalkModule;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagerTrader extends WalkModule implements AbstractGameEventListener {
    private static final Logger log = LoggerFactory.getLogger(VillagerTrader.class);
    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
        .name("调试模式")
        .description("调试模式会将一些信息输出到log中")
        .defaultValue(false)
        .build());
    public final Setting<Integer> supplyRange = sgGeneral.add(new IntSetting.Builder()
        .name("搜索容器范围")
        .description("开启功能时, 搜索容器的范围")
        .min(6)
        .sliderMax(100)
        .defaultValue(32)
        .build());
    public final Setting<Integer> minDistance = sgGeneral.add(new IntSetting.Builder()
        .name("操作范围")
        .description("补给、卸货的距离")
        .min(2)
        .sliderMax(3)
        .defaultValue(2)
        .build());
    private final VillagerSettingWarp villagerSettingWarp1 = new VillagerSettingWarp(this.settings, sgGeneral, "牧师", VillagerType.牧师, 24);
    private final VillagerSettingWarp villagerSettingWarp2 = new VillagerSettingWarp(this.settings, sgGeneral, "农民", VillagerType.农民, 8);
    private final VillagerSettingWarp villagerSettingWarp3 = new VillagerSettingWarp(this.settings, sgGeneral, "图书管理员", VillagerType.图书管理员, 1);
    private final VillagerSettingWarp villagerSettingWarp4 = new VillagerSettingWarp(this.settings, sgGeneral, "盔甲匠", VillagerType.盔甲匠, 1);
    private final VillagerSettingWarp villagerSettingWarp5 = new VillagerSettingWarp(this.settings, sgGeneral, "武器匠", VillagerType.武器匠, 1);
    private final VillagerSettingWarp villagerSettingWarp6 = new VillagerSettingWarp(this.settings, sgGeneral, "工具匠", VillagerType.工具匠, 1);
    private final VillagerSettingWarp villagerSettingWarp7 = new VillagerSettingWarp(this.settings, sgGeneral, "其他", VillagerType.石匠, 6);
    private final Setting<Boolean> initBtn = sgGeneral.add(new BoolSetting.Builder()
        .name("初始化")
        .description("使用前需要先初始化, 否则无法使用")
        .defaultValue(false)
        .onChanged(this::init)
        .build());
    private static final int firstTraderTime = 2000;
    private static final int secondTraderTime = 9000;
    private final Map<VillagerType, VillagerSettingWarp> villagerSettingWarpMap;
    private List<VillagerEntityWarp> villagerList;
    private Map<ItemBo, BlockPos> putPosMap;
    private BlockPos moneyPos;
    private BlockPos bookPos;
    private VillagerSettingWarp curVillagerSettingWarp;
    private BlockPos curPutPos;
    private ItemBo curPutVillagerItem;
    private BlockPos curTakePos;
    private Item curTakeItem;
    private List<ItemBo> buyVillagerItemList;
    private VillagerEntityWarp currentVillager;
    private long waitStartTime;
    private int tradeIndex;

    public VillagerTrader() {
        super("A自动村民交易", "自动和村民交易。展示框下2格的盒子卸货、最近的木桶补绿宝石, 展示框放书的箱子补给书");
        this.villagerList = Collections.emptyList();
        this.putPosMap = Collections.emptyMap();
        this.buyVillagerItemList = Collections.emptyList();
        this.tradeIndex = 0;
        this.addStep(Steps.GOTO_PUT_ITEM, this::gotoPutIfNeed);
        this.addStep(Steps.PUT_ITEM, this::put);
        this.addStep(Steps.GOTO_TAKE_ITEM, this::gotoTakeIfNotFull);
        this.addStep(Steps.TAKE_ITEM, this::take);
        this.addStep(Steps.NEXT, this::next);
        this.addStep(Steps.WALKING, this::none);
        this.addStep(Steps.OPEN_TRADE, this::openTrade);
        this.addStep(Steps.EXECUTE_TRADE, this::executeTrade);
        this.addStep(Steps.WAIT, this::waitTrade);
        this.villagerSettingWarpMap = new LinkedHashMap<VillagerType, VillagerSettingWarp>();
        this.villagerSettingWarpMap.put(VillagerType.牧师, this.villagerSettingWarp1);
        this.villagerSettingWarpMap.put(VillagerType.农民, this.villagerSettingWarp2);
        this.villagerSettingWarpMap.put(VillagerType.图书管理员, this.villagerSettingWarp3);
        this.villagerSettingWarpMap.put(VillagerType.盔甲匠, this.villagerSettingWarp4);
        this.villagerSettingWarpMap.put(VillagerType.武器匠, this.villagerSettingWarp5);
        this.villagerSettingWarpMap.put(VillagerType.工具匠, this.villagerSettingWarp6);
    }

    @Override
    protected boolean useQuickStopKeybind() {
        return true;
    }

    @Override
    protected boolean allowQuickStop() {
        return this.step != Steps.WALKING && this.step != Steps.WAIT;
    }

    @Override
    public void onActivate() {
        if (!this.isReady()) {
            this.toggle();
            return;
        }
        GameType currentGameMode = this.mc.gameMode.getPlayerMode();
        if (currentGameMode != GameType.CREATIVE && currentGameMode != GameType.SURVIVAL) {
            return;
        }
        super.onActivate();
        if (this.villagerList.isEmpty()) {
            this.warning("启动前没有初始化, 自动初始化", new Object[0]);
            this.init(false);
            if (this.villagerList.isEmpty()) {
                this.warning("自动初始化失败", new Object[0]);
                this.toggle();
                return;
            }
        }
        this.delayStart();
    }

    private void init(Boolean enable) {
        BlockPos moneyPos;
        BlockPos takeBookPos;
        if (!Boolean.TRUE.equals(enable)) {
            return;
        }
        this.initBtn.set(false);
        this.villagerList.clear();
        List<VillagerEntityWarp> villagerList = this.getVillagerEntity();
        if (villagerList.isEmpty()) {
            this.warning("附近没有合适村民", new Object[0]);
            return;
        }
        Map<ItemBo, BlockPos> putPosMap = HePosUtils.getItemFrameKitPosMap(this.supplyRange.get());
        if (putPosMap.isEmpty()) {
            this.warning("找不到<卸货容器>", new Object[0]);
            return;
        }
        if (this.villagerSettingWarp3.needBuyEnchantment() || this.villagerSettingWarp7.needBuyEnchantment()) {
            takeBookPos = this.getTakeBookPos();
            if (takeBookPos == null) {
                this.warning("找不到<书>补给位置", new Object[0]);
                return;
            }
            this.bookPos = takeBookPos;
        }
        moneyPos = null;
        double nearestBarrelDistance = Double.MAX_VALUE;
        for (BlockEntity blockEntity : Utils.blockEntities()) {
            BlockEntityType<?> blockEntityType = blockEntity.getType();
            double distance = this.mc.player.position().distanceTo(blockEntity.getBlockPos().getCenter());
            if (!BlockEntityType.BARREL.equals(blockEntityType) || !(distance < (double)this.supplyRange.get()) || !(distance < nearestBarrelDistance)) continue;
            moneyPos = blockEntity.getBlockPos();
            nearestBarrelDistance = distance;
        }
        if (moneyPos == null) {
            this.warning("找不到<绿宝石>补给位置", new Object[0]);
            return;
        }
        List<ItemBo> buyVillagerItemList = new ArrayList<ItemBo>();
        this.fillBuyItemList(buyVillagerItemList, this.villagerSettingWarp1);
        this.fillBuyItemList(buyVillagerItemList, this.villagerSettingWarp2);
        this.fillBuyItemList(buyVillagerItemList, this.villagerSettingWarp3);
        this.fillBuyItemList(buyVillagerItemList, this.villagerSettingWarp4);
        this.fillBuyItemList(buyVillagerItemList, this.villagerSettingWarp5);
        this.fillBuyItemList(buyVillagerItemList, this.villagerSettingWarp6);
        this.fillBuyItemList(buyVillagerItemList, this.villagerSettingWarp7);
        if (buyVillagerItemList.isEmpty()) {
            this.warning("未配置<购买物>", new Object[0]);
            return;
        }
        for (ItemBo villagerItem : buyVillagerItemList) {
            if (putPosMap.containsKey(villagerItem)) continue;
            this.warning("缺少卸货容器:" + villagerItem.getName(), new Object[0]);
            return;
        }
        this.villagerList = villagerList;
        this.putPosMap = putPosMap;
        this.moneyPos = moneyPos;
        this.buyVillagerItemList = buyVillagerItemList;
        this.waitStartTime = -1L;
        this.tradeIndex = 0;
        this.curVillagerSettingWarp = this.villagerSettingWarp1;
        this.info("初始化完毕", new Object[0]);
    }

    private BlockPos getTakeBookPos() {
        List<ItemFrame> itemFrameList = this.mc.level.getEntitiesOfClass(ItemFrame.class, this.mc.player.getBoundingBox().inflate((double)this.supplyRange.get()), frame -> frame.getItem().getItem() == Items.BOOK);
        for (ItemFrame itemFrame : itemFrameList) {
            Optional<BlockPos> chestPos = HePosUtils.getOtherChestPos(itemFrame);
            if (!chestPos.isPresent()) continue;
            return chestPos.get();
        }
        return null;
    }

    private void fillBuyItemList(List<ItemBo> buyVillagerItemList, VillagerSettingWarp villagerSettingWarp) {
        if (villagerSettingWarp.isOpen()) {
            for (Item item : villagerSettingWarp.getBuyItem()) {
                buyVillagerItemList.add(new ItemBo(item));
            }
            if (villagerSettingWarp.getType() == VillagerType.图书管理员) {
                for (ResourceKey<Enchantment> enchantment : villagerSettingWarp.getBuyEnchantment()) {
                    buyVillagerItemList.add(new ItemBo(Items.ENCHANTED_BOOK, enchantment));
                }
            }
        }
    }

    private void waitTrade() {
        long waitStartDay = this.waitStartTime / 24000L;
        long waitStartTimeOfDay = this.waitStartTime % 24000L;
        long time = this.mcTime();
        long day = time / 24000L;
        long timeOfDay = time % 24000L;
        boolean ok = false;
        if (waitStartTimeOfDay > 9000L) {
            if (day > waitStartDay && timeOfDay > 2020L) {
                ok = true;
            }
        } else if (waitStartTimeOfDay > 2000L) {
            if (day > waitStartDay || timeOfDay > 9000L) {
                ok = true;
            }
        } else if (day > waitStartDay || timeOfDay > 2020L) {
            ok = true;
        }
        if (ok) {
            this.info("开始交易", new Object[0]);
            this.waitStartTime = -1L;
            this.step = Steps.GOTO_PUT_ITEM;
        } else {
            this.printLog("waitTrade, waitStartDay : {}, day : {}, waitStartTimeOfDay : {}, timeOfDay : {}", waitStartDay, day, waitStartTimeOfDay, timeOfDay);
            this.setDelay();
        }
    }

    private boolean needClean() {
        FindItemResult findItemResult = InvUtils.find(new Item[]{Items.EMERALD});
        int count = findItemResult.found() ? findItemResult.count() : 0;
        AbstractContainerMenu currentScreenHandler = this.mc.player.containerMenu;
        if (currentScreenHandler instanceof MerchantMenu) {
            MerchantMenu screenHandler = (MerchantMenu)currentScreenHandler;
            ItemStack itemStack = screenHandler.getSlot(0).getItem();
            this.printLog("needClear itemStack : " + String.valueOf(itemStack), new Object[0]);
            if (itemStack.getItem() == Items.EMERALD) {
                count += itemStack.getCount();
            }
        }
        if (this.curVillagerSettingWarp.needBuyEnchantment()) {
            int need = Math.min(this.curVillagerSettingWarp.getMaxMoney() * 12, 64);
            if (count < need) {
                return true;
            }
            FindItemResult findBookResult = InvUtils.find(new Item[]{Items.BOOK});
            int bookCount = findBookResult.found() ? findBookResult.count() : 0;
            if (bookCount < 12) {
                return true;
            }
            Inventory playerInventory = this.mc.player.getInventory();
            int emptyQty = 0;
            for (int i = 0; i < 36; ++i) {
                ItemStack itemStack = playerInventory.getItem(i);
                if (!itemStack.isEmpty()) continue;
                ++emptyQty;
            }
            return emptyQty < 12;
        }
        int need = Math.min(this.curVillagerSettingWarp.getMaxMoney() * 12, 64);
        if (count < need) {
            return true;
        }
        Inventory playerInventory = this.mc.player.getInventory();
        for (int i = 0; i < 36; ++i) {
            ItemStack itemStack = playerInventory.getItem(i);
            if (!itemStack.isEmpty()) continue;
            return false;
        }
        return true;
    }

    private void executeTrade() {
        if (!(this.mc.player.containerMenu instanceof MerchantMenu)) {
            this.setDelay();
            this.step = Steps.OPEN_TRADE;
            return;
        }
        MerchantMenu handler = (MerchantMenu)this.mc.player.containerMenu;
        MerchantOffers tradeOfferList = handler.getOffers();
        while (this.tradeIndex < tradeOfferList.size()) {
            MerchantOffer trade = (MerchantOffer)tradeOfferList.get(this.tradeIndex);
            ItemStack sellItemStack = trade.getResult();
            Item sellItemValue = sellItemStack.getItem();
            ItemCost firstBuyItem = trade.getItemCostA();
            if (trade.getMaxUses() > trade.getUses()) {
                ItemStack firstBuyItemStamp = firstBuyItem.itemStack();
                Item firstBuyItemValue = firstBuyItemStamp.getItem();
                boolean need = false;
                if (firstBuyItemValue == Items.EMERALD) {
                    if (this.curVillagerSettingWarp.getBuyItem().contains(sellItemValue)) {
                        need = true;
                    } else if (this.curVillagerSettingWarp.getType() == VillagerType.图书管理员 && sellItemValue == Items.ENCHANTED_BOOK) {
                        ResourceKey<Enchantment> enchantmentOne = EnchantmentUtils.getEnchantmentOne(sellItemStack);
                        need = this.curVillagerSettingWarp.getBuyEnchantment().contains(enchantmentOne);
                    }
                }
                if (need) {
                    int originCount = trade.getBaseCostA().getCount();
                    int sellCount = originCount + trade.getSpecialPriceDiff();
                    if (trade.getDemand() > 0) {
                        sellCount = (int)((float)sellCount + (float)originCount * trade.getPriceMultiplier() * (float)trade.getDemand());
                    }
                    this.printLog("priceMultiplier : " + trade.getPriceMultiplier(), new Object[0]);
                    this.printLog("demandBonus : " + trade.getDemand(), new Object[0]);
                    this.printLog("firstBuyItemCount : " + firstBuyItem.count(), new Object[0]);
                    this.printLog("originCount : " + originCount, new Object[0]);
                    this.printLog("specialPrice : " + trade.getSpecialPriceDiff(), new Object[0]);
                    this.printLog("sellCount : " + sellCount, new Object[0]);
                    if (sellCount > this.curVillagerSettingWarp.getMaxMoney()) {
                        this.warning("价格超过设定上限, 不交易", new Object[0]);
                    } else {
                        if (this.needClean()) {
                            this.delayCloseNext(Steps.GOTO_PUT_ITEM);
                            return;
                        }
                        handler.setSelectionHint(this.tradeIndex);
                        handler.tryMoveItems(this.tradeIndex);
                        Minecraft.getInstance().getConnection().send((Packet)new ServerboundSelectTradePacket(this.tradeIndex));
                        InvUtils.shiftClick().slotId(2);
                        this.setDelay();
                        return;
                    }
                }
            }
            ++this.tradeIndex;
        }
        this.currentVillager.setLastTradeTime(this.mcTime());
        this.tradeIndex = 0;
        this.delayCloseNext(Steps.NEXT);
    }

    private void openTrade() {
        Villager villager = this.currentVillager.getVillager();
        if (villager == null) {
            this.currentVillager.setLastTradeTime(this.mcTime());
            this.next();
            return;
        }
        Vec3 villagerPos = villager.position();
        Vec3 playerPos = this.mc.player.position();
        double distance = villagerPos.distanceTo(playerPos);
        if (distance > (double)this.minDistance.get() + 0.5) {
            this.printLog("distance false");
            this.currentVillager.setLastFailTime(this.mcTime());
            this.next();
            return;
        }
        if (!villager.isAlive()) {
            this.info("村民挂了?", new Object[0]);
            this.currentVillager.setLastTradeTime(this.mcTime());
            this.next();
            return;
        }
        this.printLog("openTrade");
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult((Entity)this.mc.player, (Vec3)playerPos, (Vec3)villagerPos, (AABB)villager.getBoundingBox(), Entity::isPickable, (double)playerPos.distanceToSqr(villagerPos));
        if (entityHitResult == null) {
            if (this.debug.get()) {
                this.info("射线检测不通过", new Object[0]);
            }
            HeRotationUtils.rotate(villager.getEyePosition(), () -> {
                EntityHitResult location = new EntityHitResult(villager, villager.getBoundingBox().getCenter());
                this.mc.gameMode.interact(this.mc.player,villager,location, InteractionHand.MAIN_HAND);
                this.step = Steps.EXECUTE_TRADE;
            });
        } else {
            if (this.debug.get()) {
                this.info("射线检测通过", new Object[0]);
            }
            HeRotationUtils.rotate(entityHitResult.getLocation(), () -> {
                InteractionResult actionResult = this.mc.gameMode.interact((Player)this.mc.player, (Entity)villager, entityHitResult, InteractionHand.MAIN_HAND);
                if (!actionResult.consumesAction()) {
                    EntityHitResult location2 = new EntityHitResult(villager, villager.getBoundingBox().getCenter());
                    this.mc.gameMode.interact(this.mc.player,villager,location2, InteractionHand.MAIN_HAND);
                    this.step = Steps.EXECUTE_TRADE;
                } else {
                    ChatUtils.error("无法打开交易界面，重试中...", (Object[])new Object[0]);
                }
            });
        }
        this.setDelay(this.delay.get() * 2);
    }

    private void next() {
        boolean needPut;
        if (this.needClean() && (needPut = this.tryPut())) {
            return;
        }
        long time = this.mcTime();
        long timeOfDay = time % 24000L;
        long day = time / 24000L;
        this.printLog("day : " + day);
        this.printLog("timeOfDay : " + timeOfDay);
        List<VillagerEntityWarp> validVillagerList = timeOfDay > 9000L ? this.villagerList.stream().filter(warp -> warp.getDay() < day || warp.getTimeOfDay() < 8900L).filter(warp -> warp.getLastTradeTime() < time - 100L).collect(Collectors.toList()) : (timeOfDay > 2020L ? this.villagerList.stream().filter(warp -> warp.getDay() < day || warp.getTimeOfDay() < 2000L).filter(warp -> warp.getLastTradeTime() < time - 100L).collect(Collectors.toList()) : this.villagerList.stream().filter(warp -> warp.getDay() < day && warp.getTimeOfDay() < 8900L).filter(warp -> warp.getLastTradeTime() < time - 100L).collect(Collectors.toList()));
        Map<VillagerType, List<VillagerEntityWarp>> typeListMap = validVillagerList.stream().collect(Collectors.groupingBy(VillagerEntityWarp::getVillagerType));
        VillagerSettingWarp settingWarp = null;
        for (Map.Entry<VillagerType, VillagerSettingWarp> entry : this.villagerSettingWarpMap.entrySet()) {
            if (!typeListMap.containsKey(entry.getKey())) continue;
            validVillagerList = typeListMap.get(entry.getKey());
            settingWarp = entry.getValue();
            break;
        }
        if (settingWarp != null) {
            this.curVillagerSettingWarp = settingWarp;
        } else {
            validVillagerList = typeListMap.getOrDefault(this.villagerSettingWarp7.getType(), Collections.emptyList());
            if (!validVillagerList.isEmpty()) {
                this.curVillagerSettingWarp = this.villagerSettingWarp7;
            }
        }
        VillagerEntityWarp best = null;
        Vec3 playerPos = this.mc.player.position();
        double nearestDistance = Double.MAX_VALUE;
        for (VillagerEntityWarp warp : validVillagerList) {
            this.printLog("warpDay : {}, timeOfDay : {}", warp.getDay(), warp.getTimeOfDay());
            double distance = warp.getOperatePosCenter().distanceTo(playerPos);
            if (!(distance < nearestDistance)) continue;
            best = warp;
            nearestDistance = distance;
        }
        if (best == null) {
            this.info("准备待机", new Object[0]);
            this.waitStartTime = this.mcTime();
            this.step = Steps.GOTO_PUT_ITEM;
            return;
        }
        this.printLog("find next : {}", best.getOperatePosCenter());
        this.currentVillager = best;
        BlockPos targetPos = this.currentVillager.getOperatePos();
        if (!targetPos.closerToCenterThan((Position)this.mc.player.position(), 300.0)) {
            this.warning("寻路距离过远", new Object[0]);
            this.toggle();
            return;
        }
        this.gotoTargetIfNeed(targetPos, 0, Steps.OPEN_TRADE, null);
    }

    private void tryNext() {
        this.step = this.waitStartTime > 0L ? Steps.WAIT : Steps.NEXT;
    }

    private void take() {
        this.openChest(this.curTakePos, (AbstractContainerMenu screenHandler) -> {
            FindItemResult findItemResult = InvUtils.find(new Item[]{this.curTakeItem});
            int needSupplyQty = this.curTakeItem == Items.BOOK ? 1 : this.curVillagerSettingWarp.getSupplyQty();
            if (findItemResult.found() && findItemResult.count() >= 64 * needSupplyQty) {
                HeInvUtils.closeCurScreen();
                if (this.curTakeItem == Items.EMERALD) {
                    this.gotoTakeIfNotFull();
                } else {
                    this.tryNext();
                }
                return;
            }
            ItemStack nextStack = this.nextScreenStack((ItemStack itemStack) -> itemStack.getItem() == this.curTakeItem);
            if (nextStack.isEmpty()) {
                this.warning("无法补给", new Object[0]);
                this.stop();
                this.delayStart();
            } else {
                this.info("拿取:" + Names.get(this.curTakeItem), new Object[0]);
                InvUtils.shiftClick().slotId(this.getCurScreenSlot());
                this.setDelay();
            }
        });
    }

    private void gotoTakeIfNotFull() {
        FindItemResult findItemResult = InvUtils.find(new Item[]{Items.EMERALD});
        if (!findItemResult.found() || findItemResult.count() < 64 * this.curVillagerSettingWarp.getSupplyQty()) {
            this.curTakePos = this.moneyPos;
            this.curTakeItem = Items.EMERALD;
            this.gotoTargetIfNeed(this.moneyPos, this.minDistance.get(), Steps.TAKE_ITEM, "补给绿宝石");
            return;
        }
        boolean needBuyEnchantment = this.curVillagerSettingWarp.needBuyEnchantment();
        if (needBuyEnchantment) {
            FindItemResult findBookResult = InvUtils.find(new Item[]{Items.BOOK});
            if (!findBookResult.found() || findBookResult.count() < 64) {
                this.curTakePos = this.bookPos;
                this.curTakeItem = Items.BOOK;
                this.gotoTargetIfNeed(this.bookPos, this.minDistance.get(), Steps.TAKE_ITEM, "补给书");
                return;
            }
        }
        this.tryNext();
    }

    private boolean tryPut() {
        Vec3 playerPos = this.mc.player.position();
        ItemBo best = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ItemBo buyVillagerItem : this.buyVillagerItemList) {
            ItemStack nextPlayerStack = this.nextPlayerStack(buyVillagerItem::isSameItem);
            if (nextPlayerStack.isEmpty()) continue;
            BlockPos putPos = this.putPosMap.get(buyVillagerItem);
            double distance = putPos.getCenter().distanceTo(playerPos);
            if (!(distance < nearestDistance)) continue;
            best = buyVillagerItem;
            nearestDistance = distance;
        }
        if (best != null) {
            this.curPutPos = this.putPosMap.get(best);
            this.curPutVillagerItem = best;
            this.gotoTargetIfNeed(this.curPutPos, this.minDistance.get() + 1, Steps.PUT_ITEM, "前往卸货");
            return true;
        }
        return false;
    }

    private void put() {
        this.openChest(this.curPutPos, (AbstractContainerMenu inventory) -> {
            ItemStack nextStack = this.nextPlayerStack((ItemStack itemStack) -> this.curPutVillagerItem.isSameItem(itemStack));
            if (nextStack.isEmpty()) {
                FindItemResult findItemResult = InvUtils.findEmpty();
                if (findItemResult.found()) {
                    HeInvUtils.closeCurScreen();
                    this.gotoPutIfNeed();
                } else {
                    HeInvUtils.closeCurScreen();
                    this.warning("无法清空背包", new Object[0]);
                    this.stop();
                    this.delayStart();
                }
            } else {
                InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                this.setDelay();
            }
        });
    }

    private void gotoPutIfNeed() {
        boolean needPut = this.tryPut();
        if (!needPut) {
            this.gotoTakeIfNotFull();
        }
    }

    private List<VillagerEntityWarp> getVillagerEntity() {
        List<VillagerEntityWarp> villagerList = new ArrayList<VillagerEntityWarp>();
        for (Entity entity : this.mc.level.entitiesForRendering()) {
            if (!EntityType.VILLAGER.equals(entity.getType())) continue;
            double y = entity.position().y() - this.mc.player.getY();
            if (!(y >= -2.0) || !(y <= 2.0)) continue;
            Villager villager = (Villager)entity;
            VillagerType currentType = VillagerType.fromEntry(villager.getVillagerData().profession());
            if (currentType == null) continue;
            VillagerSettingWarp villagerSettingWarp = this.villagerSettingWarpMap.getOrDefault(currentType, this.villagerSettingWarp7);
            if (!villagerSettingWarp.isOpen() || currentType != villagerSettingWarp.getType()) continue;
            BlockPos pos = this.getOperatePos(villager, currentType, Direction.EAST);
            if (pos == null) {
                pos = this.getOperatePos(villager, currentType, Direction.SOUTH);
            }
            if (pos == null) {
                pos = this.getOperatePos(villager, currentType, Direction.WEST);
            }
            if (pos == null) {
                pos = this.getOperatePos(villager, currentType, Direction.NORTH);
            }
            if (pos == null) continue;
            villagerList.add(new VillagerEntityWarp(currentType, villager.getUUID(), pos));
        }
        return villagerList;
    }

    private BlockPos getOperatePos(Villager villager, VillagerType currentType, Direction direction) {
        BlockPos blockPos = villager.blockPosition().relative(direction);
        BlockState blockState = this.mc.level.getBlockState(blockPos);
        if (blockState.getBlock().asItem() == currentType.getItem()) {
            BlockPos pos = blockPos.relative(direction);
            BlockPos checkPos = pos.offset(0, 1, 0);
            if (this.mc.level.getBlockState(checkPos).isAir()) {
                return pos;
            }
        }
        return null;
    }

    private void printLog(String string, Object object) {
        if (this.debug.get()) {
            log.info(string, object);
        }
    }

    private void printLog(String string, Object ... objectArray) {
        if (this.debug.get()) {
            log.info(string, objectArray);
        }
    }
}
