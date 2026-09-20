/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.EnchantmentListSetting$Builder
 *  meteordevelopment.meteorclient.settings.EnumSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.ItemSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.settings.StringSetting$Builder
 *  meteordevelopment.meteorclient.utils.misc.Names
 *  meteordevelopment.meteorclient.utils.player.FindItemResult
 *  meteordevelopment.meteorclient.utils.player.InvUtils
 *  meteordevelopment.meteorclient.utils.player.Rotations
 *  meteordevelopment.meteorclient.utils.world.BlockUtils
 *  net.minecraft.entity.decoration.ItemFrameEntity
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.screen.ScreenHandler
 *  net.minecraft.screen.AnvilScreenHandler
 *  net.minecraft.screen.PlayerScreenHandler
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.enchantment.Enchantment
 *  net.minecraft.block.Blocks
 *  net.minecraft.block.Block
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.text.Text
 *  net.minecraft.network.packet.Packet
 *  net.minecraft.block.BlockState
 *  net.minecraft.network.packet.c2s.play.RenameItemC2SPacket
 *  net.minecraft.client.gui.widget.TextFieldWidget
 *  net.minecraft.screen.GrindstoneScreenHandler
 *  net.minecraft.client.gui.screen.ingame.AnvilScreen
 *  net.minecraft.registry.RegistryKey
 *  org.apache.commons.lang3.StringUtils
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.EnchantmentUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.HePosUtils;
import com.xiaohe66.mc.meteor.lotus.util.LotusUtils;
import com.xiaohe66.mc.meteor.lotus.modules.StepModule;
import com.xiaohe66.mc.meteor.lotus.modules.enchantment.AutoEnchantmentType;
import com.xiaohe66.mc.meteor.lotus.modules.enchantment.AutoEnchantmentXpSource;

import com.xiaohe66.mc.meteor.lotus.util.enchantment.EnchantmentNode;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.impl.EnchantmentBookNode;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.impl.EnchantmentEquipNode;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.impl.EnchantmentMargeNode;
import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoEnchantment extends StepModule {
    private static final Logger log = LoggerFactory.getLogger(AutoEnchantment.class);
    private static final int USE_STATE = -2;
    private static final int TAKE_BOOK_STATE = -3;
    private static final Set<Item> ENCHANTABLE_ITEMS = Set.of(Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.SHEARS, Items.FLINT_AND_STEEL, Items.DIAMOND_SWORD, Items.DIAMOND_SPEAR, Items.BOW, Items.CROSSBOW, Items.TRIDENT, Items.MACE, Items.DIAMOND_HELMET, Items.TURTLE_HELMET, Items.DIAMOND_CHESTPLATE, Items.ELYTRA, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Items.FISHING_ROD, Items.BOOK, Items.ENCHANTED_BOOK);
    public final Setting<AutoEnchantmentXpSource> xpSource = sgGeneral.add(new EnumSetting.Builder<AutoEnchantmentXpSource>()
        .name("经验来源")
        .description("缺少经验时经验的来源类型")
        .defaultValue(AutoEnchantmentXpSource.杀戮光环)
        .build());
    public final Setting<Integer> dropXpDelay = sgGeneral.add(new IntSetting.Builder()
        .name("丢经验延迟")
        .description("是否给附魔后的物品改名")
        .sliderRange(0, 100)
        .defaultValue(5)
        .visible(() -> this.xpSource.get() == AutoEnchantmentXpSource.XP)
        .build());
    public final Setting<Boolean> rename = sgGeneral.add(new BoolSetting.Builder()
        .name("改名")
        .description("是否给附魔后的物品改名")
        .defaultValue(true)
        .build());
    public final Setting<String> newName = sgGeneral.add(new StringSetting.Builder()
        .name("新名称")
        .description("给附魔后的物品设置的新名称")
        .defaultValue("lotus打造的神兵")
        .build());
    public final Setting<Boolean> onlyRename = sgGeneral.add(new BoolSetting.Builder()
        .name("不附魔仅改名")
        .description("不做附魔仅做改名")
        .defaultValue(false)
        .onChanged(enabled -> {
            if (enabled) {
                this.rename.set(true);
            }
        })
        .build());
    public final Setting<Item> targetItem = sgGeneral.add(new ItemSetting.Builder()
        .name("物品")
        .description("要附魔的物品")
        .defaultValue(Items.DIAMOND_PICKAXE)
        .filter(ENCHANTABLE_ITEMS::contains)
        .visible(() -> this.onlyRename.get() == false)
        .build());
    public final Setting<AutoEnchantmentType> presetType = sgGeneral.add(new EnumSetting.Builder<AutoEnchantmentType>()
        .name("预设")
        .description("选择预设配置，选择后会自动更新物品和附魔选项")
        .defaultValue(AutoEnchantmentType.自定义)
        .onChanged(this::onPresetChanged)
        .visible(() -> this.onlyRename.get() == false)
        .build());
    public final Setting<Set<ResourceKey<Enchantment>>> selectedEnchantments = sgGeneral.add(new EnchantmentListSetting.Builder()
        .name("附魔")
        .description("选择要附魔的附魔类型，选择预设时会自动更新")
        .visible(() -> this.onlyRename.get() == false)
        .build());
    public final Setting<Integer> pauseTime = sgGeneral.add(new IntSetting.Builder()
        .name("暂停时间")
        .sliderRange(0, 100)
        .defaultValue(20)
        .visible(() -> this.onlyRename.get() == false)
        .build());
    private AnvilMenu anvilLevelHandler;
    private final Map<ResourceKey<Enchantment>, BlockPos> enchantmentPosMap;
    private BlockPos supplyPos;
    private BlockPos grindPos;
    private BlockPos anvilPos;
    private BlockPos putPos;
    private final LinkedList<ResourceKey<Enchantment>> needTakeBook;
    private int needLevel;
    private int index1;
    private int index2;
    private ItemStack itemStack1;
    private Item onlyRenameItem;
    private long lastRenameTime;

    public AutoEnchantment() {
        super("A自动附魔", "自动附魔(祛魔)和改名, 附魔需要搭配附魔平台使用（但仅改名时可以随处使用）。快捷栏需要拿一些铁砧。");
        this.enchantmentPosMap = new HashMap<ResourceKey<Enchantment>, BlockPos>();
        this.needTakeBook = new LinkedList();
        this.onlyRenameItem = Items.AIR;
        this.addStep(Steps.NEXT, this::next);
        this.addStep(Steps.TAKE_ITEM, this::takeEquip);
        this.addStep(Steps.GRIND, this::grind);
        this.addStep(Steps.TAKE_BOOK, this::takeBook);
        this.addStep(Steps.PLACE, this::placeAnvil);
        this.addStep(Steps.USE, this::useAnvil);
        this.addStep(Steps.LEVEL, this::level);
        this.addStep(Steps.PUT_ITEM, this::put);
    }

    @Override
    protected boolean useQuickStopKeybind() {
        return true;
    }

    public void onActivate() {
        if (!this.isReady()) {
            return;
        }
        this.anvilLevelHandler = new AnvilMenu(-1, this.mc.player.getInventory());
        if (this.rename.get() && StringUtils.isBlank(this.newName.get())) {
            this.warning("改名时<新名称>不能为空", new Object[0]);
            this.toggle();
            return;
        }
        if (this.onlyRename.get()) {
            if (!this.rename.get()) {
                this.warning("<仅改名>时需要勾选<改名>", new Object[0]);
                this.toggle();
                return;
            }
            ItemStack onlyRenameItemStack = this.getItemStack(this.getMainSlot());
            if (onlyRenameItemStack.isEmpty()) {
                this.warning("主手需要拿着改名的物品", new Object[0]);
                this.toggle();
                return;
            }
            List<BlockPos> spherePosList = HeBlockUtils.listPosInSphere(4, 2, this.mc.player.blockPosition());
            for (BlockPos blockPos : spherePosList) {
                BlockState blockState = this.mc.level.getBlockState(blockPos);
                Block block = blockState.getBlock();
                if (block != Blocks.ANVIL && block != Blocks.CHIPPED_ANVIL && block != Blocks.DAMAGED_ANVIL) continue;
                this.anvilPos = blockPos;
                this.onlyRenameItem = onlyRenameItemStack.getItem();
                this.info("开始改名:" + Names.get(this.onlyRenameItem), new Object[0]);
                this.delayNext(Steps.NEXT);
                return;
            }
            this.warning("身边没有铁砧", new Object[0]);
            this.toggle();
        } else {
            this.enchantmentPosMap.clear();
            this.needTakeBook.clear();
            this.supplyPos = null;
            this.grindPos = null;
            this.anvilPos = null;
            this.putPos = null;
            List<ItemFrame> itemFrames = this.mc.level.getEntitiesOfClass(ItemFrame.class, this.mc.player.getBoundingBox().inflate(6.0), frame -> {
                Item item = frame.getItem().getItem();
                return item == Items.REDSTONE || item == Items.REDSTONE_BLOCK;
            });
            for (ItemFrame itemFrame : itemFrames) {
                ItemStack heldStack = itemFrame.getItem();
                if (heldStack.isEmpty()) continue;
                if (heldStack.getItem() == Items.REDSTONE) {
                    Optional<BlockPos> supplyPosOptional = HePosUtils.getOtherChestPos(itemFrame);
                    supplyPosOptional.ifPresent(pos -> {
                        this.supplyPos = pos;
                    });
                    continue;
                }
                if (heldStack.getItem() != Items.REDSTONE_BLOCK) continue;
                Optional<BlockPos> putPosOptional = HePosUtils.getOtherChestPos(itemFrame);
                putPosOptional.ifPresent(pos -> {
                    this.putPos = pos;
                });
            }
            List<BlockPos> spherePosList = HeBlockUtils.listPosInSphere(5, 3, this.mc.player.blockPosition());
            Iterator<BlockPos> iterator = spherePosList.iterator();
            while (iterator.hasNext()) {
                BlockPos blockPos = (BlockPos)iterator.next();
                BlockState blockState = this.mc.level.getBlockState(blockPos);
                Block block = blockState.getBlock();
                if (block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL) {
                    this.anvilPos = blockPos;
                    continue;
                }
                if (block != Blocks.GRINDSTONE) continue;
                this.grindPos = blockPos;
            }
            if (this.supplyPos == null) {
                this.warning("未识别到输入位置", new Object[0]);
                this.toggle();
                return;
            }
            if (this.putPos == null) {
                this.warning("未识别到输出位置", new Object[0]);
                this.toggle();
                return;
            }
            if (this.anvilPos == null) {
                this.warning("未识别到铁砧", new Object[0]);
                this.toggle();
                return;
            }
            if (this.grindPos == null) {
                this.warning("未识别到砂轮", new Object[0]);
                this.toggle();
                return;
            }
            Map<ItemBo, BlockPos> kitPosMap = HePosUtils.getItemFrameKitPosMap(6);
            if (kitPosMap.isEmpty()) {
                this.warning("未识别到附魔书", new Object[0]);
                this.toggle();
                return;
            }
            for (Map.Entry entry : kitPosMap.entrySet()) {
                this.enchantmentPosMap.put(((ItemBo)entry.getKey()).getEnchantment(), (BlockPos)entry.getValue());
            }
            this.delayNext(Steps.NEXT);
        }
    }

    private void onPresetChanged(AutoEnchantmentType newType) {
        if (!newType.getAll().equals(this.selectedEnchantments.get())) {
            this.selectedEnchantments.set(newType.getAll());
        }
    }

    private void put() {
        this.openChest(this.putPos, (AbstractContainerMenu screenHandler) -> {
            ItemStack nextDoneStack = this.nextDoneStack();
            if (!nextDoneStack.isEmpty()) {
                this.info("卸货", new Object[0]);
                InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                this.setDelay();
                return;
            }
            this.delayCloseNext(Steps.NEXT);
            this.setCloseScreenAfterDelay(this.pauseTime.get());
        });
    }

    private ItemStack nextDoneStack() {
        Item target = this.targetItem.get();
        return this.nextPlayerStack((ItemStack itemStack) -> {
            if (itemStack.getItem() == this.targetItem.get()) {
                Set<ResourceKey<Enchantment>> enchantments = EnchantmentUtils.getEnchantment(itemStack);
                return enchantments.equals(this.selectedEnchantments.get());
            }
            return false;
        });
    }

    private void level() {
        if (this.xpSource.get() == AutoEnchantmentXpSource.XP) {
            if (this.needLevel <= this.mc.player.experienceLevel) {
                this.delayNext(Steps.NEXT);
                return;
            }
            FindItemResult findItemResult = InvUtils.findInHotbar(new Item[]{Items.EXPERIENCE_BOTTLE});
            if (findItemResult.found()) {
                if (findItemResult.getHand() == null) {
                    InvUtils.swap(findItemResult.slot(), false);
                } else {
                    Rotations.rotate(this.mc.player.getYRot(), 90.0, () -> this.mc.gameMode.useItem(this.mc.player, findItemResult.getHand()));
                }
                this.setDelay(this.dropXpDelay.get());
            } else {
                FindItemResult bottleResult = InvUtils.find(itemStack -> itemStack.getItem() == Items.EXPERIENCE_BOTTLE, 0, 36);
                if (bottleResult.found()) {
                    HeInvUtils.swapMainHand(bottleResult.slot());
                    this.setDelay();
                } else {
                    this.warning("缺少XP", new Object[0]);
                    this.toggle();
                }
            }
            return;
        }
        if (this.needLevel > this.mc.player.experienceLevel) {
            LotusUtils.enableKillAura();
            this.setDelay();
        } else {
            LotusUtils.disableKillAura();
            this.delayNext(Steps.NEXT);
        }
    }

    private void placeAnvil() {
        FindItemResult findItemResult = InvUtils.find(new Item[]{Items.ANVIL});
        if (!findItemResult.found()) {
            this.warning("缺少铁砧", new Object[0]);
            this.toggle();
            return;
        }
        if (!BlockUtils.canPlace(this.anvilPos, true)) {
            this.warning("无法放置铁砧", new Object[0]);
            this.toggle();
            return;
        }
        LotusUtils.disableKillAura();
        if (InvUtils.testInMainHand(new Item[]{Items.ANVIL})) {
            this.info("放置铁砧", new Object[0]);
            HeBlockUtils.place(this.anvilPos, findItemResult.slot(), true, Direction.DOWN);
            this.delayNext(Steps.USE);
        } else {
            HeInvUtils.swapMainHand(findItemResult.slot());
            this.setDelay();
        }
    }

    private void useAnvil() {
        int levelCost;
        ItemStack firstItemStack = this.getItemStack(this.index1);
        ItemStack secondItemStack = ItemStack.EMPTY;
        if (!this.onlyRename.get()) {
            secondItemStack = this.getItemStack(this.index2);
        }
        String newName = null;
        if (this.rename.get()) {
            newName = this.newName.get();
        }
        if ((levelCost = this.getLevelCost(firstItemStack, secondItemStack, newName)) > this.mc.player.experienceLevel) {
            this.needLevel = levelCost;
            this.delayCloseNext(Steps.LEVEL);
            return;
        }
        AbstractContainerMenu handler = this.mc.player.containerMenu;
        if (!(handler instanceof AnvilMenu)) {
            if (this.mc.level.getBlockState(this.anvilPos).isAir()) {
                this.info("补放铁砧", new Object[0]);
                this.delayNext(Steps.PLACE);
                return;
            }
            this.rotateAndOpen(this.anvilPos);
            this.setDelay();
            return;
        }
        AnvilMenu screenHandler = (AnvilMenu)handler;
        if (screenHandler.getSlot(0).getItem() == ItemStack.EMPTY) {
            if (this.getItemStack(this.index1).getItem() != this.itemStack1.getItem()) {
                this.delayCloseNext(Steps.NEXT);
                return;
            }
            InvUtils.shiftClick().slot(this.index1);
            this.setDelay();
        } else if (!this.onlyRename.get() && screenHandler.getSlot(1).getItem() == ItemStack.EMPTY) {
            if (this.getItemStack(this.index2).getItem() != secondItemStack.getItem()) {
                this.delayCloseNext(Steps.NEXT);
                return;
            }
            InvUtils.shiftClick().slot(this.index2);
            this.setDelay();
        } else {
            Component customName;
            ItemStack resultStack = screenHandler.getSlot(2).getItem();
            if (this.rename.get() && resultStack.getItem() != Items.ENCHANTED_BOOK && ((customName = resultStack.getCustomName()) == null || !Objects.equals(newName, customName.getString()))) {
                long now = System.currentTimeMillis();
                if (now - this.lastRenameTime < 1000L) {
                    this.warning("改名超时...", new Object[0]);
                    this.setDelay();
                    return;
                }
                this.lastRenameTime = now;
                this.info("改名", new Object[0]);
                screenHandler.setItemName(newName);
                EditBox nameField = ((AnvilScreen)this.mc.screen).name;
                nameField.setValue(newName);
                nameField.moveCursorToEnd(false);
                this.mc.player.connection.send((Packet)new ServerboundRenameItemPacket(newName));
                this.setDelay();
                return;
            }
            if (screenHandler.getCost() > this.mc.player.experienceLevel) {
                this.needLevel = screenHandler.getCost();
                this.delayCloseNext(Steps.LEVEL);
            } else {
                InvUtils.shiftClick().slotId(2);
                this.lastRenameTime = 0L;
                if (this.onlyRename.get()) {
                    this.delayNext(Steps.NEXT);
                } else {
                    this.delayCloseNext(Steps.NEXT);
                }
            }
        }
    }

    private void grind() {
        AbstractContainerMenu handler = this.mc.player.containerMenu;
        if (!(handler instanceof GrindstoneMenu)) {
            if (this.mc.level.getBlockState(this.grindPos).getBlock() != Blocks.GRINDSTONE) {
                this.breakStep("砂轮位置错误");
                return;
            }
            this.rotateAndOpen(this.grindPos);
            this.setDelay();
            return;
        }
        GrindstoneMenu screenHandler = (GrindstoneMenu)handler;
        if (screenHandler.getSlot(0).getItem() == ItemStack.EMPTY) {
            if (this.getItemStack(this.index1).getItem() != this.itemStack1.getItem()) {
                this.delayCloseNext(Steps.NEXT);
                return;
            }
            InvUtils.shiftClick().slot(this.index1);
            this.setDelay();
        } else {
            this.info("祛魔！", new Object[0]);
            InvUtils.shiftClick().slotId(2);
            this.delayCloseNext(Steps.NEXT);
        }
    }

    private void takeEquip() {
        Item target = this.targetItem.get();
        this.openChest(this.supplyPos, (AbstractContainerMenu screenHandler) -> {
            ItemStack nextItemStack = this.nextScreenStack((ItemStack itemStack) -> itemStack.getItem() == target);
            if (nextItemStack.isEmpty()) {
                this.warning("无法补给<" + Names.get(target) + ">", new Object[0]);
                this.setDelay();
                return;
            }
            int slotId = this.getCurScreenSlot();
            InvUtils.shiftClick().slotId(slotId);
            this.info("拿取装备", new Object[0]);
            this.delayCloseNext(Steps.NEXT);
        });
    }

    private void takeBook() {
        if (this.needTakeBook.isEmpty()) {
            this.delayCloseNext(Steps.NEXT);
            return;
        }
        ResourceKey<Enchantment> needBook = this.needTakeBook.getFirst();
        BlockPos kitPos = this.enchantmentPosMap.get(needBook);
        if (this.mc.player.containerMenu instanceof InventoryMenu) {
            if (kitPos == null) {
                this.warning("找不到<" + Names.get(needBook) + ">容器", new Object[0]);
                this.toggle();
                return;
            }
            if (!HeItemUtils.isShulkerBox(this.mc.level.getBlockState(kitPos).getBlock().asItem())) {
                this.warning("找不到<" + Names.get(needBook) + ">容器", new Object[0]);
                this.setDelay();
                return;
            }
        }
        this.openChest(kitPos, (AbstractContainerMenu screenHandler) -> {
            ItemStack nextBookStack = this.nextNeedBook(needBook);
            if (nextBookStack.isEmpty()) {
                this.warning("无法拿取<" + Names.get(needBook) + ">", new Object[0]);
                this.setDelay();
                return;
            }
            this.info("拿取<" + Names.get(needBook) + ">", new Object[0]);
            int slotId = this.getCurScreenSlot();
            InvUtils.shiftClick().slotId(slotId);
            this.needTakeBook.poll();
            if (this.needTakeBook.isEmpty()) {
                this.delayCloseNext(Steps.NEXT);
            } else {
                this.delayCloseNext(Steps.TAKE_BOOK);
            }
        });
    }

    private ItemStack nextNeedBook(ResourceKey<Enchantment> needBook) {
        return this.nextScreenStack((ItemStack itemStack) -> {
            if (itemStack.getItem() == Items.ENCHANTED_BOOK) {
                Set<ResourceKey<Enchantment>> enchantments = EnchantmentUtils.getEnchantment(itemStack);
                return enchantments.contains(needBook);
            }
            return false;
        });
    }

    private void next() {
        if (this.onlyRename.get()) {
            ItemStack nextStack = this.nextPlayerStack((ItemStack itemStack) -> {
                if (itemStack.getItem() == this.onlyRenameItem) {
                    Component customName = itemStack.getCustomName();
                    boolean alreadyNamed = customName != null && this.newName.get().equals(customName.getString());
                    return !alreadyNamed;
                }
                return false;
            });
            if (nextStack.isEmpty()) {
                this.breakStep("改名完毕");
                this.closeScreen();
                this.toggle();
            } else {
                this.setIndex1(this.getCurPlayerSlot());
                this.delayNext(Steps.USE);
            }
            return;
        }
        ItemStack equipItemStack = this.nextPlayerStack((ItemStack itemStack) -> itemStack.getItem() == this.targetItem.get());
        if (equipItemStack.isEmpty()) {
            this.delayNext(Steps.TAKE_ITEM);
            return;
        }
        Set targetEnchantments = this.selectedEnchantments.get();
        Set itemEnchantments = EnchantmentUtils.getEnchantment(equipItemStack);
        if (!targetEnchantments.containsAll(itemEnchantments)) {
            this.setIndex1(this.getCurPlayerSlot());
            this.delayNext(Steps.GRIND);
            return;
        }
        Set<ResourceKey<Enchantment>> itemAllEnchantments = EnchantmentUtils.getEnchantment(equipItemStack, true);
        if (itemAllEnchantments.containsAll(targetEnchantments)) {
            this.delayNext(Steps.PUT_ITEM);
            return;
        }
        HashSet<ResourceKey<Enchantment>> missingEnchantments = new HashSet<ResourceKey<Enchantment>>(targetEnchantments);
        missingEnchantments.removeAll(itemAllEnchantments);
        EnchantmentMargeNode margeNode = EnchantmentUtils.bestStepSimple(equipItemStack, missingEnchantments);
        this.getSlotOrMarge(margeNode);
    }

    private int getSlotOrMarge(EnchantmentNode node) {
        if (node instanceof EnchantmentMargeNode) {
            EnchantmentMargeNode margeNode = (EnchantmentMargeNode)node;
            if (margeNode.getLeft() instanceof EnchantmentEquipNode) {
                int rightSlot = this.getSlotOrMarge(margeNode.getRight());
                if (rightSlot >= 0) {
                    int playerSlot = this.getCurPlayerSlot();
                    this.setIndex1(playerSlot);
                    this.setIndex2(rightSlot);
                    this.step = Steps.USE;
                    return -2;
                }
                if (rightSlot == -3) {
                    this.step = Steps.TAKE_BOOK;
                    return -3;
                }
                if (rightSlot == -2) {
                    return -2;
                }
                this.error("意外的状态(equip), right :" + rightSlot, new Object[0]);
                return rightSlot;
            }
            int bookSlot = HeInvUtils.findBookSlot(margeNode.getAllEnchantmentKey());
            if (bookSlot >= 0) {
                return bookSlot;
            }
            int leftSlot = this.getSlotOrMarge(margeNode.getLeft());
            if (leftSlot == -2) {
                return -2;
            }
            int rightSlot = this.getSlotOrMarge(margeNode.getRight());
            if (rightSlot == -2) {
                return -2;
            }
            if (leftSlot == -3 || rightSlot == -3) {
                this.step = Steps.TAKE_BOOK;
                return -3;
            }
            if (leftSlot < 0) {
                this.error("意外的状态(book), left :" + leftSlot, new Object[0]);
                return leftSlot;
            }
            if (rightSlot < 0) {
                this.error("意外的状态(book), right :" + rightSlot, new Object[0]);
                return rightSlot;
            }
            this.setIndex1(leftSlot);
            this.setIndex2(rightSlot);
            this.step = Steps.USE;
            return -2;
        }
        if (node instanceof EnchantmentBookNode) {
            EnchantmentBookNode bookNode = (EnchantmentBookNode)node;
            int bookSlot = HeInvUtils.findBookSlot(bookNode.getEnchantmentKey());
            if (bookSlot >= 0) {
                return bookSlot;
            }
            this.needTakeBook.add(bookNode.getEnchantmentKey());
            return -3;
        }
        throw new IllegalStateException("不可能的情况");
    }

    private void setIndex1(int slot) {
        this.index1 = slot;
        this.itemStack1 = this.getItemStack(slot);
    }

    private void setIndex2(int slot) {
        this.index2 = slot;
    }

    private int getLevelCost(ItemStack firstStack, ItemStack secondStack, String newName) {
        this.anvilLevelHandler.getSlot(0).setByPlayer(firstStack);
        if (!secondStack.isEmpty()) {
            this.anvilLevelHandler.getSlot(1).setByPlayer(secondStack);
        }
        if (newName != null) {
            this.anvilLevelHandler.setItemName(newName);
        }
        this.anvilLevelHandler.createResult();
        return this.anvilLevelHandler.getCost();
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        LotusUtils.disableKillAura();
    }
}
