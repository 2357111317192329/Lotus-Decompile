package com.xiaohe66.mc.meteor.lotus.modules;

import baritone.api.event.listener.AbstractGameEventListener;
import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.modules.villager.VillagerEntityWarp;
import com.xiaohe66.mc.meteor.lotus.modules.villager.VillagerSettingWarp;
import com.xiaohe66.mc.meteor.lotus.modules.villager.VillagerType;
import com.xiaohe66.mc.meteor.lotus.util.EnchantmentUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeRotationUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.settings.BlockPosSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
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
    public final Setting<Integer> minDistance = sgGeneral.add(new IntSetting.Builder()
        .name("操作范围")
        .description("补给、卸货的距离")
        .min(2)
        .sliderMax(3)
        .defaultValue(2)
        .build());
    private final SettingGroup sgUnload = settings.createGroup("卸货点");
    private final SettingGroup sgEmerald = settings.createGroup("绿宝石点");
    private final SettingGroup sgSupply = settings.createGroup("取货点");
    private final Setting<Integer> unloadPointCount = sgUnload.add(new IntSetting.Builder()
        .name("卸货点数量")
        .description("卸货点列的数量, 用于存放购买的物品")
        .min(1)
        .sliderMax(10)
        .defaultValue(1)
        .build());
    private final Setting<Integer> emeraldPointCount = sgEmerald.add(new IntSetting.Builder()
        .name("绿宝石点数量")
        .description("绿宝石点列的数量, 用于存放/取出绿宝石")
        .min(1)
        .sliderMax(10)
        .defaultValue(1)
        .build());
    private final Setting<Integer> supplyPointCount = sgSupply.add(new IntSetting.Builder()
        .name("取货点数量")
        .description("取货点列的数量, 用于存放要卖给村民的物品")
        .min(1)
        .sliderMax(10)
        .defaultValue(1)
        .build());
    private final Setting<Integer> supplyGroups = sgGeneral.add(new IntSetting.Builder()
        .name("补给数量(组)")
        .description("每种物品补给拿取的组数上限(1组=64个)。实际拿取组数不超过 背包空格/(买数量+卖数量+1+附魔书相关) 的向下取整, 保证背包放得下所有交易物")
        .range(1, 27)
        .sliderRange(1, 10)
        .defaultValue(3)
        .build());
    private final Setting<BlockPos>[] unloadPosSettings = this.createPosSettings("卸货点", this.unloadPointCount, sgUnload);
    private final Setting<BlockPos>[] emeraldPosSettings = this.createPosSettings("绿宝石点", this.emeraldPointCount, sgEmerald);
    private final Setting<BlockPos>[] supplyPosSettings = this.createPosSettings("取货点", this.supplyPointCount, sgSupply);
    private final VillagerSettingWarp villagerSettingWarp1 = new VillagerSettingWarp(this.settings, sgGeneral, "牧师", VillagerType.牧师, this::onVillagerSettingChanged);
    private final VillagerSettingWarp villagerSettingWarp2 = new VillagerSettingWarp(this.settings, sgGeneral, "农民", VillagerType.农民, this::onVillagerSettingChanged);
    private final VillagerSettingWarp villagerSettingWarp3 = new VillagerSettingWarp(this.settings, sgGeneral, "图书管理员", VillagerType.图书管理员, this::onVillagerSettingChanged);
    private final VillagerSettingWarp villagerSettingWarp4 = new VillagerSettingWarp(this.settings, sgGeneral, "盔甲匠", VillagerType.盔甲匠, this::onVillagerSettingChanged);
    private final VillagerSettingWarp villagerSettingWarp5 = new VillagerSettingWarp(this.settings, sgGeneral, "武器匠", VillagerType.武器匠, this::onVillagerSettingChanged);
    private final VillagerSettingWarp villagerSettingWarp6 = new VillagerSettingWarp(this.settings, sgGeneral, "工具匠", VillagerType.工具匠, this::onVillagerSettingChanged);
    private final VillagerSettingWarp villagerSettingWarp8 = new VillagerSettingWarp(this.settings, sgGeneral, "渔夫", VillagerType.渔夫, this::onVillagerSettingChanged);
    private final VillagerSettingWarp villagerSettingWarp9 = new VillagerSettingWarp(this.settings, sgGeneral, "制箭师", VillagerType.制箭师, this::onVillagerSettingChanged);
    private final VillagerSettingWarp villagerSettingWarp10 = new VillagerSettingWarp(this.settings, sgGeneral, "皮匠", VillagerType.皮匠, this::onVillagerSettingChanged);
    private final VillagerSettingWarp villagerSettingWarp7 = new VillagerSettingWarp(this.settings, sgGeneral, "其他", VillagerType.石匠, this::onVillagerSettingChanged);
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
    private List<BlockPos> validUnloadPosList;
    private List<BlockPos> validEmeraldPosList;
    private List<BlockPos> validSupplyPosList;
    private List<ItemBo> buyVillagerItemList;
    private List<Item> sellItemList;
    private VillagerSettingWarp curVillagerSettingWarp;
    private VillagerEntityWarp currentVillager;
    private long waitStartTime;
    private int tradeIndex;
    private BlockPos curTakePos;
    private int takePhase;
    private int takePointIndex;
    private BlockPos curPutPos;
    private int putPhase;
    private int putPointIndex;
    private boolean initialized;
    private int executeTradeRetryCount;
    private int tradeOpenRetryCount;
    private boolean roundInsufficient;

    public VillagerTrader() {
        super("A自动村民交易", "自动和村民交易。设置卸货点/绿宝石点/取货点后使用");
        this.villagerList = Collections.emptyList();
        this.validUnloadPosList = Collections.emptyList();
        this.validEmeraldPosList = Collections.emptyList();
        this.validSupplyPosList = Collections.emptyList();
        this.buyVillagerItemList = Collections.emptyList();
        this.sellItemList = Collections.emptyList();
        this.tradeIndex = 0;
        this.initialized = false;
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
        this.villagerSettingWarpMap.put(VillagerType.渔夫, this.villagerSettingWarp8);
        this.villagerSettingWarpMap.put(VillagerType.制箭师, this.villagerSettingWarp9);
        this.villagerSettingWarpMap.put(VillagerType.皮匠, this.villagerSettingWarp10);
    }

    private Setting<BlockPos>[] createPosSettings(String prefix, Setting<Integer> countSetting, SettingGroup group) {
        Setting<BlockPos>[] settings = new Setting[10];
        for (int i = 1; i <= 10; ++i) {
            final int index = i;
            settings[i - 1] = group.add(new BlockPosSetting.Builder()
                .name(prefix + index)
                .description(prefix + "容器位置" + index)
                .defaultValue(new BlockPos(0, 0, 0))
                .visible(() -> countSetting.get() >= index)
                .build());
        }
        return settings;
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
            if (!this.tryInit()) {
                this.warning("自动初始化失败", new Object[0]);
                this.toggle();
                return;
            }
        }
        this.delayStart(Steps.GOTO_TAKE_ITEM);
    }

    private void init(Boolean enable) {
        if (!Boolean.TRUE.equals(enable)) {
            return;
        }
        this.initBtn.set(false);
        this.tryInit();
    }

    private void onVillagerSettingChanged(boolean typeChanged) {
        if (!this.isActive() || !this.isReady()) {
            return;
        }
        if (typeChanged) {
            this.info("交易配置已变更, 尝试自动初始化", new Object[0]);
            if (!this.tryInit()) {
                this.warning("自动初始化失败, 请先手动初始化后再运行", new Object[0]);
                this.toggle();
            }
        } else if (this.initialized) {
            this.updateItemLists();
        }
    }

    private void updateItemLists() {
        this.buyVillagerItemList = this.collectBuyItemList();
        this.sellItemList = this.collectSellItemList();
    }

    private boolean tryInit() {
        this.villagerList.clear();
        List<ItemBo> buyItemList = this.collectBuyItemList();
        List<Item> sellItemList = this.collectSellItemList();
        if (buyItemList.isEmpty() && sellItemList.isEmpty()) {
            this.warning("没有配置任何买/卖物品, 初始化失败", new Object[0]);
            return false;
        }
        List<BlockPos> unloadPosList = this.collectValidPosList(this.unloadPosSettings, this.unloadPointCount.get());
        if (unloadPosList.isEmpty()) {
            this.warning("找不到<卸货点>容器, 初始化失败", new Object[0]);
            return false;
        }
        List<BlockPos> emeraldPosList = this.collectValidPosList(this.emeraldPosSettings, this.emeraldPointCount.get());
        if (emeraldPosList.isEmpty()) {
            this.warning("找不到<绿宝石点>容器, 初始化失败", new Object[0]);
            return false;
        }
        List<BlockPos> supplyPosList = Collections.emptyList();
        if (!sellItemList.isEmpty() || this.needTakeBook()) {
            supplyPosList = this.collectValidPosList(this.supplyPosSettings, this.supplyPointCount.get());
            if (supplyPosList.isEmpty()) {
                this.warning("找不到<取货点>容器, 初始化失败", new Object[0]);
                return false;
            }
        }
        List<VillagerEntityWarp> villagerList = this.getVillagerEntity();
        if (villagerList.isEmpty()) {
            this.warning("附近没有合适村民", new Object[0]);
            return false;
        }
        this.validUnloadPosList = unloadPosList;
        this.validEmeraldPosList = emeraldPosList;
        this.validSupplyPosList = supplyPosList;
        this.villagerList = villagerList;
        this.buyVillagerItemList = buyItemList;
        this.sellItemList = sellItemList;
        this.initialized = true;
        this.info("初始化完成", new Object[0]);
        return true;
    }

    private List<ItemBo> collectBuyItemList() {
        List<ItemBo> buyItemList = new ArrayList<ItemBo>();
        for (VillagerSettingWarp warp : this.getAllSettingWarps()) {
            if (!warp.isOpen()) continue;
            for (Item item : warp.getBuyItem()) {
                buyItemList.add(new ItemBo(item));
            }
            if (warp.getType() == VillagerType.图书管理员) {
                for (ResourceKey<Enchantment> enchantment : warp.getBuyEnchantment()) {
                    buyItemList.add(new ItemBo(Items.ENCHANTED_BOOK, enchantment));
                }
            }
        }
        return buyItemList;
    }

    private List<Item> collectSellItemList() {
        List<Item> sellItemList = new ArrayList<Item>();
        for (VillagerSettingWarp warp : this.getAllSettingWarps()) {
            if (!warp.isOpen()) continue;
            for (Item item : warp.getSellItem()) {
                if (sellItemList.contains(item)) continue;
                sellItemList.add(item);
            }
        }
        return sellItemList;
    }

    private boolean needTakeBook() {
        for (VillagerSettingWarp warp : this.getAllSettingWarps()) {
            if (warp.isOpen() && !warp.getBuyEnchantment().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private List<BlockPos> collectValidPosList(Setting<BlockPos>[] posSettings, int count) {
        List<BlockPos> posList = new ArrayList<BlockPos>();
        for (int i = 0; i < count && i < posSettings.length; ++i) {
            BlockPos pos = posSettings[i].get();
            if (!HeBlockUtils.isContainer(pos)) continue;
            posList.add(pos);
        }
        return posList;
    }

    private List<VillagerSettingWarp> getAllSettingWarps() {
        List<VillagerSettingWarp> list = new ArrayList<VillagerSettingWarp>(this.villagerSettingWarpMap.values());
        list.add(this.villagerSettingWarp7);
        return list;
    }

    private void gotoTakeIfNotFull() {
        this.takePhase = 0;
        this.takePointIndex = 0;
        this.roundInsufficient = false;
        for (VillagerEntityWarp warp : this.villagerList) {
            warp.setTradedThisRound(false);
        }
        this.startTakePhase();
    }

    private void startTakePhase() {
        // 只有卖物品时不需要去绿宝石点取绿宝石(绿宝石由交易获得, 卸货时存入即可)
        if (this.takePhase == 1 && this.buyVillagerItemList.isEmpty()) {
            this.finishTake();
            return;
        }
        List<BlockPos> posList = this.takePhase == 0 ? this.validSupplyPosList : this.validEmeraldPosList;
        if (posList.isEmpty()) {
            this.nextTakePhase();
            return;
        }
        this.takePointIndex = 0;
        if (!this.gotoTakePos(posList)) {
            this.warning("找不到合适的取货点, 停止运行", new Object[0]);
            this.stop();
            this.delayStart();
        }
    }

    private boolean gotoTakePos(List<BlockPos> posList) {
        while (this.takePointIndex < posList.size()) {
            BlockPos containerPos = posList.get(this.takePointIndex);
            BlockPos viewPos = HeBlockUtils.findViewPos(containerPos);
            if (viewPos != null) {
                this.curTakePos = containerPos;
                this.gotoTargetIfNeed(viewPos, 1, Steps.TAKE_ITEM, "前往取货");
                return true;
            }
            ++this.takePointIndex;
        }
        return false;
    }

    private void nextTakePhase() {
        if (this.takePhase == 0) {
            this.takePhase = 1;
            this.startTakePhase();
        } else {
            this.finishTake();
        }
    }

    private void nextTakePoint() {
        List<BlockPos> posList = this.takePhase == 0 ? this.validSupplyPosList : this.validEmeraldPosList;
        ++this.takePointIndex;
        if (!this.gotoTakePos(posList)) {
            this.nextTakePhase();
        }
    }

    private void take() {
        this.openChest(this.curTakePos, (AbstractContainerMenu screenHandler) -> {
            Item needTakeItem = this.findNeedTakeItem();
            if (needTakeItem == null) {
                HeInvUtils.closeCurScreen();
                this.nextTakePhase();
                return;
            }
            ItemStack nextStack = this.nextScreenStack((ItemStack itemStack) -> itemStack.getItem() == needTakeItem);
            if (nextStack.isEmpty()) {
                HeInvUtils.closeCurScreen();
                this.nextTakePoint();
                return;
            }
            this.info("拿取:" + Names.get(needTakeItem), new Object[0]);
            InvUtils.shiftClick().slotId(this.getCurScreenSlot());
            this.setDelay();
        });
    }

    private Item findNeedTakeItem() {
        if (this.takePhase == 0) {
            int targetGroups = this.getTakeGroups();
            for (Item item : this.sellItemList) {
                if (this.getInvCount(item) < targetGroups * item.getDefaultMaxStackSize()) {
                    return item;
                }
            }
            if (this.needTakeBook() && this.getInvCount(Items.BOOK) < 1) {
                return Items.BOOK;
            }
            return null;
        }
        if (this.takePhase == 1) {
            // 綠寶石也是物品之一: 拿取組數同樣受補給數量與背包空間限制
            int targetGroups = this.getTakeGroups();
            if (this.getInvCount(Items.EMERALD) < targetGroups * 64) {
                return Items.EMERALD;
            }
        }
        return null;
    }

    /*
     * 每種物品拿取組數上限 = min(補給數量設定, floor(背包空格m / 分母)):
     * 分母 = n1(買) + n2(賣) + 1(綠寶石) + (有附魔書時 1(書) + n3(附魔書))。
     * 綠寶石1位恆存在: 交易所得/多餘的綠寶石即使沒花光也會佔背包空格;
     * 每種物品若沒花光也會佔空位, 因此分母即為背包需預留的空間。
     */
    private int getTakeGroups() {
        int n1 = 0;
        int n3 = 0;
        for (ItemBo itemBo : this.buyVillagerItemList) {
            if (itemBo.getItem() == Items.ENCHANTED_BOOK) {
                n3++;
            } else {
                n1++;
            }
        }
        int n2 = this.sellItemList.size();
        int m = this.getFreeInvSlots();
        int denominator = n1 + n2;
        // 綠寶石: 交易所得餘額(沒花光)佔背包1格, 恆預留
        denominator++;
        // 書 + 附魔書: 有附魔書交易時需要1本書, 每本附魔書再各佔1格
        if (n3 > 0) {
            denominator += 1 + n3;
        }
        if (denominator <= 0) {
            denominator = 1;
        }
        int groupsBySpace = m / denominator;
        return Math.max(1, Math.min(this.supplyGroups.get().intValue(), groupsBySpace));
    }

    /*
     * 背包剩餘空格數(主背包36格中空的數量)
     */
    private int getFreeInvSlots() {
        int free = 0;
        Inventory inventory = this.mc.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).isEmpty()) {
                free++;
            }
        }
        return free;
    }

    private void finishTake() {
        boolean canTrade = false;
        if (!this.buyVillagerItemList.isEmpty() && this.getInvCount(Items.EMERALD) > 0) {
            canTrade = true;
            if (this.needTakeBook() && this.getInvCount(Items.BOOK) < 1) {
                canTrade = false;
            }
        }
        if (!canTrade && !this.sellItemList.isEmpty()) {
            for (Item item : this.sellItemList) {
                if (this.getInvCount(item) <= 0) continue;
                canTrade = true;
                break;
            }
        }
        if (canTrade) {
            this.tryNext();
        } else {
            this.info("没有足够物品进行交易, 取消本轮, 卸货归还", new Object[0]);
            this.delayCloseNext(Steps.GOTO_PUT_ITEM);
        }
    }

    private int getInvCount(Item item) {
        FindItemResult findItemResult = InvUtils.find(new Item[]{item});
        return findItemResult.found() ? findItemResult.count() : 0;
    }

    private void next() {
        long time = this.mcTime();
        long timeOfDay = time % 24000L;
        long day = time / 24000L;
        this.printLog("day : " + day, new Object[0]);
        this.printLog("timeOfDay : " + timeOfDay, new Object[0]);
        List<VillagerEntityWarp> validVillagerList = timeOfDay > 9000L ? this.villagerList.stream().filter(warp -> warp.getDay() < day || warp.getTimeOfDay() < 8900L).filter(warp -> !warp.isTradedThisRound()).collect(Collectors.toList()) : (timeOfDay > 2020L ? this.villagerList.stream().filter(warp -> warp.getDay() < day || warp.getTimeOfDay() < 2000L).filter(warp -> !warp.isTradedThisRound()).collect(Collectors.toList()) : this.villagerList.stream().filter(warp -> warp.getDay() < day && warp.getTimeOfDay() < 8900L).filter(warp -> !warp.isTradedThisRound()).collect(Collectors.toList()));
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
            // 所有職業的所有交易對都處理完畢
            if (this.roundInsufficient) {
                // 本輪有交易對因物品不足而無法交易: 卸貨補給後即可恢復, 無需等村民補貨, 直接繼續交易
                this.info("物品不足, 卸货补给后继续交易", new Object[0]);
                this.waitStartTime = -1L;
                this.step = Steps.GOTO_PUT_ITEM;
                return;
            }
            // 所有職業的所有交易對都達交易上限或價格超過(無物品不足): 需等村民補貨(刷新交易/降價)
            this.info("准备待机", new Object[0]);
            this.waitStartTime = this.mcTime();
            this.step = Steps.GOTO_PUT_ITEM;
            return;
        }
        this.printLog("find next : {}", best.getOperatePosCenter());
        this.currentVillager = best;
        // 在村民周围搜尋可點擊到村民的站位點(不可到達/視線不通的點會被排除)
        BlockPos targetPos = HeBlockUtils.findVillagerViewPos(best.getVillager(), this.currentVillager.getOperatePos());
        if (targetPos == null) {
            // 找不到合適的站位點: 視同該村民已達交易上限, 嘗試下一個村民, 不卡在此處
            this.info("找不到可点击村民的站位, 跳过该村民", new Object[0]);
            this.currentVillager.setLastTradeTime(this.mcTime());
            this.currentVillager.setTradedThisRound(true);
            this.next();
            return;
        }
        if (!targetPos.closerToCenterThan((Position)this.mc.player.position(), 300.0)) {
            this.warning("寻路距离过远", new Object[0]);
            this.toggle();
            return;
        }
        this.gotoTargetIfNeed(targetPos, 1, Steps.OPEN_TRADE, null);
    }

    private void tryNext() {
        this.step = this.waitStartTime > 0L ? Steps.WAIT : Steps.NEXT;
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
        if (distance > 3.5) {
            this.printLog("distance false", new Object[0]);
            if (this.tradeOpenRetryCount < 5) {
                ++this.tradeOpenRetryCount;
                // 重新搜尋可點擊站位點(避免站在上次選的點卻因村民移動等原因點不到)
                BlockPos retryPos = HeBlockUtils.findVillagerViewPos(villager, this.currentVillager.getOperatePos());
                if (retryPos == null) {
                    this.tradeOpenRetryCount = 0;
                    this.currentVillager.setLastTradeTime(this.mcTime());
                    this.currentVillager.setTradedThisRound(true);
                    this.next();
                    return;
                }
                this.gotoTargetIfNeed(retryPos, 1, Steps.OPEN_TRADE, "距离村民过远, 重新靠近");
            } else {
                this.tradeOpenRetryCount = 0;
                this.currentVillager.setLastTradeTime(this.mcTime());
                this.currentVillager.setLastFailTime(this.mcTime());
                this.currentVillager.setTradedThisRound(true);
                this.next();
            }
            return;
        }
        this.tradeOpenRetryCount = 0;
        if (!villager.isAlive()) {
            this.info("村民挂了?", new Object[0]);
            this.currentVillager.setLastTradeTime(this.mcTime());
            this.next();
            return;
        }
        this.printLog("openTrade", new Object[0]);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult((Entity)this.mc.player, (Vec3)playerPos, (Vec3)villagerPos, (AABB)villager.getBoundingBox(), Entity::isPickable, (double)playerPos.distanceToSqr(villagerPos));
        if (entityHitResult == null) {
            // 射線檢查失敗(站位偏離/村民移動/視線被擋): 視同該村民已達交易上限, 嘗試下一個村民, 不卡在此處
            if (this.debug.get()) {
                this.info("射线检测不通过, 跳过该村民", new Object[0]);
            }
            this.currentVillager.setLastTradeTime(this.mcTime());
            this.currentVillager.setTradedThisRound(true);
            this.next();
            return;
        }
        if (this.debug.get()) {
            this.info("射线检测通过", new Object[0]);
        }
        HeRotationUtils.rotate(entityHitResult.getLocation(), () -> {
            InteractionResult actionResult = this.mc.gameMode.interact((Player)this.mc.player, (Entity)villager, entityHitResult, InteractionHand.MAIN_HAND);
            if (!actionResult.consumesAction()) {
                EntityHitResult location2 = new EntityHitResult(villager, villager.getBoundingBox().getCenter());
                this.mc.gameMode.interact(this.mc.player, villager, location2, InteractionHand.MAIN_HAND);
            }
            this.step = Steps.EXECUTE_TRADE;
        });
        this.setDelay(this.delay.get() * 2);
    }

    private void executeTrade() {
        if (!(this.mc.player.containerMenu instanceof MerchantMenu)) {
            if (this.executeTradeRetryCount >= 10) {
                this.executeTradeRetryCount = 0;
                // 交互後交易界面始終未能打開: 視同該村民無法交易, 跳過並嘗試下一個村民, 避免卡死
                this.printLog("交易界面未能打开, 跳过该村民", new Object[0]);
                this.currentVillager.setLastTradeTime(this.mcTime());
                this.currentVillager.setTradedThisRound(true);
                this.next();
            } else {
                this.executeTradeRetryCount++;
                this.setDelay();
            }
            return;
        }
        this.executeTradeRetryCount = 0;
        MerchantMenu handler = (MerchantMenu)this.mc.player.containerMenu;
        MerchantOffers tradeOfferList = handler.getOffers();
        boolean insufficient = false;
        while (this.tradeIndex < tradeOfferList.size()) {
            MerchantOffer trade = (MerchantOffer)tradeOfferList.get(this.tradeIndex);
            ItemStack sellItemStack = trade.getResult();
            Item sellItemValue = sellItemStack.getItem();
            ItemCost firstBuyItem = trade.getItemCostA();
            ItemStack firstBuyItemStack = firstBuyItem.itemStack();
            Item firstBuyItemValue = firstBuyItemStack.getItem();
            boolean need = false;
            boolean isBuy = firstBuyItemValue == Items.EMERALD;
            int sellCount = 0;
            int priceLimit = 0;
            boolean isBookTrade = false;
            ResourceKey<Enchantment> enchantmentOne = null;
            if (isBuy) {
                if (this.curVillagerSettingWarp.getBuyItem().contains(sellItemValue)) {
                    need = true;
                    priceLimit = this.curVillagerSettingWarp.getBuyPriceLimit(sellItemValue);
                } else if (this.curVillagerSettingWarp.getType() == VillagerType.图书管理员 && sellItemValue == Items.ENCHANTED_BOOK) {
                    enchantmentOne = EnchantmentUtils.getEnchantmentOne(sellItemStack);
                    if (this.curVillagerSettingWarp.getBuyEnchantment().contains(enchantmentOne)) {
                        need = true;
                        priceLimit = this.curVillagerSettingWarp.getEnchantmentPriceLimit(enchantmentOne);
                        isBookTrade = true;
                    }
                }
                if (need) {
                    int originCount = trade.getBaseCostA().getCount();
                    sellCount = originCount + trade.getSpecialPriceDiff();
                    if (trade.getDemand() > 0) {
                        sellCount = (int)((float)sellCount + (float)originCount * trade.getPriceMultiplier() * (float)trade.getDemand());
                    }
                    if (sellCount < 1) {
                        sellCount = 1;
                    }
                    this.printLog("priceMultiplier : " + trade.getPriceMultiplier(), new Object[0]);
                    this.printLog("demandBonus : " + trade.getDemand(), new Object[0]);
                    this.printLog("sellCount : " + sellCount, new Object[0]);
                }
            } else {
                if (sellItemValue == Items.EMERALD && this.curVillagerSettingWarp.getSellItem().contains(firstBuyItemValue)) {
                    need = true;
                    priceLimit = this.curVillagerSettingWarp.getSellPriceLimit(firstBuyItemValue);
                }
                if (need) {
                    sellCount = Math.max(1, firstBuyItem.count());
                    this.printLog("sellItem : " + Names.get(firstBuyItemValue), new Object[0]);
                    this.printLog("sellCount : " + sellCount, new Object[0]);
                }
            }
            if (trade.getMaxUses() <= trade.getUses()) {
                this.tradeIndex++;
                continue;
            }
            if (need) {
                if (isBuy && this.getInvCount(Items.EMERALD) < sellCount) {
                    this.printLog("绿宝石不足, 不交易", new Object[0]);
                    insufficient = true;
                } else if (isBuy && sellCount > priceLimit) {
                    this.printLog("买价格超过上限, 不交易", new Object[0]);
                } else if (!isBuy && this.getInvCount(firstBuyItemValue) < sellCount) {
                    this.printLog("物品不足, 不交易", new Object[0]);
                    insufficient = true;
                } else if (!isBuy && sellCount > priceLimit) {
                    this.printLog("卖价格超过上限, 不交易", new Object[0]);
                } else if (isBookTrade && this.getInvCount(Items.BOOK) < 1) {
                    this.printLog("附魔书交易需要普通书, 背包中无书, 不交易", new Object[0]);
                    insufficient = true;
                } else {
                    handler.setSelectionHint(this.tradeIndex);
                    handler.tryMoveItems(this.tradeIndex);
                    Minecraft.getInstance().getConnection().send((Packet)new ServerboundSelectTradePacket(this.tradeIndex));
                    InvUtils.shiftClick().slotId(2);
                    this.setDelay();
                    return;
                }
            }
            this.tradeIndex++;
        }
        this.currentVillager.setLastTradeTime(this.mcTime());
        this.currentVillager.setTradedThisRound(true);
        // 物品不足(或价格超限)时, 同一职业其他村民同样的交易对基本也无法交易(价格基本相同),
        // 直接跳过整个职业, 下一轮取货补充物资后再来; 只有达到村民交易上限(uses满)时才需要检查同一职业的其他村民
        if (insufficient) {
            // 本輪存在因物品不足而無法交易的交易對 → 記錄, 卸貨補給後應直接繼續交易, 不進 waitTrade
            this.roundInsufficient = true;
            // 物品不足時, 同一職業其他村民同樣的交易對基本也無法交易(價格基本相同),
            // 直接跳過整個職業, 下一輪取貨補充物資後再來; 達上限/漲價則不在此列(漲價是單一村民行為)
            for (VillagerEntityWarp warp : this.villagerList) {
                if (warp.getVillagerType() == this.currentVillager.getVillagerType() && !warp.isTradedThisRound()) {
                    warp.setTradedThisRound(true);
                }
            }
        }
        this.tradeIndex = 0;
        this.delayCloseNext(Steps.NEXT);
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

    private void gotoPutIfNeed() {
        this.putPhase = 0;
        this.putPointIndex = 0;
        this.startPutPhase();
    }

    private void startPutPhase() {
        List<BlockPos> posList = this.getPutPosList(this.putPhase);
        while (posList.isEmpty()) {
            this.nextPutPhase();
            if (this.putPhase > 2) {
                return;
            }
            posList = this.getPutPosList(this.putPhase);
        }
        this.putPointIndex = 0;
        if (!this.gotoPutPos(posList)) {
            this.warning("找不到合适的卸货点, 停止运行", new Object[0]);
            this.stop();
            this.delayStart();
        }
    }

    private boolean gotoPutPos(List<BlockPos> posList) {
        while (this.putPointIndex < posList.size()) {
            BlockPos containerPos = posList.get(this.putPointIndex);
            BlockPos viewPos = HeBlockUtils.findViewPos(containerPos);
            if (viewPos != null) {
                this.curPutPos = containerPos;
                this.gotoTargetIfNeed(viewPos, 1, Steps.PUT_ITEM, "前往卸货");
                return true;
            }
            ++this.putPointIndex;
        }
        return false;
    }

    private List<BlockPos> getPutPosList(int phase) {
        switch (phase) {
            case 0: {
                return this.validSupplyPosList;
            }
            case 1: {
                return this.validEmeraldPosList;
            }
            default: {
                return this.validUnloadPosList;
            }
        }
    }

    private void nextPutPhase() {
        ++this.putPhase;
        if (this.putPhase > 2) {
            this.finishPut();
        } else {
            this.startPutPhase();
        }
    }

    private void nextPutPoint() {
        List<BlockPos> posList = this.getPutPosList(this.putPhase);
        ++this.putPointIndex;
        if (!this.gotoPutPos(posList)) {
            this.nextPutPhase();
        }
    }

    private void put() {
        this.openChest(this.curPutPos, (AbstractContainerMenu screenHandler) -> {
            if (this.isContainerFull()) {
                HeInvUtils.closeCurScreen();
                this.nextPutPoint();
                return;
            }
            ItemStack nextStack = this.findPutStack();
            if (nextStack.isEmpty()) {
                HeInvUtils.closeCurScreen();
                this.nextPutPhase();
                return;
            }
            InvUtils.shiftClick().slot(this.getCurPlayerSlot());
            this.setDelay();
        });
    }

    private ItemStack findPutStack() {
        if (this.putPhase == 0) {
            for (Item item : this.sellItemList) {
                ItemStack stack = this.nextPlayerStack((ItemStack itemStack) -> itemStack.getItem() == item);
                if (stack.isEmpty()) continue;
                return stack;
            }
            if (this.needTakeBook()) {
                ItemStack book = this.nextPlayerStack((ItemStack itemStack) -> itemStack.getItem() == Items.BOOK);
                if (!book.isEmpty()) {
                    return book;
                }
            }
            return ItemStack.EMPTY;
        }
        if (this.putPhase == 1) {
            return this.nextPlayerStack((ItemStack itemStack) -> itemStack.getItem() == Items.EMERALD);
        }
        for (ItemBo itemBo : this.buyVillagerItemList) {
            ItemStack stack = this.nextPlayerStack(itemBo::isSameItem);
            if (stack.isEmpty()) continue;
            return stack;
        }
        for (Item item : this.sellItemList) {
            ItemStack stack = this.nextPlayerStack((ItemStack itemStack) -> itemStack.getItem() == item);
            if (stack.isEmpty()) continue;
            return stack;
        }
        return this.nextPlayerStack((ItemStack itemStack) -> itemStack.getItem() == Items.EMERALD);
    }

    private void finishPut() {
        if (this.hasRemainTargetItem()) {
            this.warning("卸货点已满, 背包中仍有交易物品, 停止运行", new Object[0]);
            this.stop();
            this.delayStart();
            return;
        }
        this.gotoTakeIfNotFull();
    }

    private boolean hasRemainTargetItem() {
        for (Item item : this.sellItemList) {
            if (this.getInvCount(item) > 0) {
                return true;
            }
        }
        if (this.getInvCount(Items.EMERALD) > 0) {
            return true;
        }
        for (ItemBo itemBo : this.buyVillagerItemList) {
            ItemStack stack = this.nextPlayerStack(itemBo::isSameItem);
            if (stack.isEmpty()) continue;
            return true;
        }
        if (this.needTakeBook() && this.getInvCount(Items.BOOK) > 0) {
            return true;
        }
        return false;
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
