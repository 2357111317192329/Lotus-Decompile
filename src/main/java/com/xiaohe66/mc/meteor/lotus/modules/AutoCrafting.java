/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.events.world.TickEvent$Pre
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.EnumSetting$Builder
 *  meteordevelopment.meteorclient.settings.ItemListSetting$Builder
 *  meteordevelopment.meteorclient.settings.KeybindSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.utils.misc.Keybind
 *  meteordevelopment.meteorclient.utils.misc.Names
 *  meteordevelopment.meteorclient.utils.player.InvUtils
 *  meteordevelopment.orbit.EventHandler
 *  net.minecraft.recipe.display.RecipeDisplay
 *  net.minecraft.recipe.RecipeDisplayEntry
 *  net.minecraft.recipe.NetworkRecipeId
 *  net.minecraft.recipe.display.SlotDisplayContexts
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.screen.ScreenHandler
 *  net.minecraft.screen.slot.SlotActionType
 *  net.minecraft.screen.CraftingScreenHandler
 *  net.minecraft.screen.slot.Slot
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.world.World
 *  net.minecraft.client.recipebook.ClientRecipeBook
 *  net.minecraft.screen.SmithingScreenHandler
 *  net.minecraft.client.gui.screen.recipebook.RecipeResultCollection
 *  net.minecraft.client.gui.screen.recipebook.RecipeResultCollection$RecipeFilterMode
 *  net.minecraft.item.equipment.trim.ArmorTrim
 *  net.minecraft.component.type.FireworksComponent
 *  net.minecraft.component.DataComponentTypes
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.BaseModule;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoCrafting
extends BaseModule {
    private static final Logger log = LoggerFactory.getLogger(AutoCrafting.class);
    private static final Map<Item, Item> kitColorMap;
    private final Setting<List<Item>> craftingItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("合成物品")
        .description("需要合成的物品")
        .defaultValue(List.of())
        .build());
    private final Setting<Keybind> keybind1 = sgGeneral.add(new KeybindSetting.Builder()
        .name("合成键")
        .description("合成键")
        .defaultValue(Keybind.fromKey(67))
        .action(() -> this.keydown(false))
        .build());
    private final Setting<Keybind> keybind2 = sgGeneral.add(new KeybindSetting.Builder()
        .name("丢出合成键")
        .description("丢出合成键")
        .defaultValue(Keybind.fromKey(86))
        .action(() -> this.keydown(true))
        .build());
    private final Setting<FireworkSpeed> fireworkSpeed = sgGeneral.add(new EnumSetting.Builder<FireworkSpeed>()
        .name("烟花速度")
        .description("合成烟花时使用的火药数量（决定飞行速度）")
        .defaultValue(FireworkSpeed.三速)
        .build());
    private final Setting<Boolean> closeScreen = sgGeneral.add(new BoolSetting.Builder()
        .name("关闭界面")
        .description("在合成完毕后关闭界面")
        .defaultValue(true)
        .build());
    private static final long KIT_PLACE_TIMEOUT_MS = 1000L;
    private Item needItem;
    private boolean doing;
    private boolean isDrop;
    private boolean kitPlacedPending;
    private long kitPlacedTime;
    private int needGunpowderCount;
    private Item smithingItem1;
    private Item smithingItem2;
    private Item smithingItem3;
    private ArmorTrim smithingTrim;

    public AutoCrafting() {
        super("S自动合成", "必须在工作台或锻造台使用。工作台可自动识别配方或手动选择合成物品；锻造台可识别已放置的配方并循环合成。", 3);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!this.checkAndDecrement()) {
            return;
        }
        if (!this.doing) {
            return;
        }
        AbstractContainerMenu screenHandler = this.mc.player.containerMenu;
        if (screenHandler instanceof CraftingMenu) {
            CraftingMenu craftingScreenHandler = (CraftingMenu)screenHandler;
            if (this.craftingItems.get().isEmpty()) {
                return;
            }
            this.doCrafting(craftingScreenHandler);
        } else {
            screenHandler = this.mc.player.containerMenu;
            if (screenHandler instanceof SmithingMenu) {
                SmithingMenu smithingScreenHandler = (SmithingMenu)screenHandler;
                if (this.smithingItem1 == null && this.smithingItem2 == null && this.smithingItem3 == null) {
                    this.finish();
                    return;
                }
                this.doSmithing(smithingScreenHandler);
            } else {
                this.finish();
            }
        }
    }

    private void doCrafting(CraftingMenu craftingScreenHandler) {
        if (this.needItem == Items.FIREWORK_ROCKET) {
            this.doFireworkCrafting(craftingScreenHandler);
            return;
        }
        if (HeItemUtils.isShulkerBox(this.needItem)) {
            this.margeKit(craftingScreenHandler);
            return;
        }
        Map<Item, RecipeDisplayId> map = this.getRecipeIdMap();
        if (map.containsKey(this.needItem)) {
            RecipeDisplayId networkRecipeId = map.get(this.needItem);
            this.mc.gameMode.handlePlaceRecipe(craftingScreenHandler.containerId, networkRecipeId, true);
            ContainerInput actionType = this.isDrop ? ContainerInput.THROW : ContainerInput.QUICK_MOVE;
            this.mc.gameMode.handleContainerInput(craftingScreenHandler.containerId, 0, 1, actionType, (Player)this.mc.player);
            this.setDelay();
            return;
        }
        this.finish();
    }

    private void doSmithing(SmithingMenu smithingScreenHandler) {
        ItemStack outputStack = smithingScreenHandler.getSlot(3).getItem();
        if (!outputStack.isEmpty()) {
            ArmorTrim armorTrim = (ArmorTrim)outputStack.get(DataComponents.TRIM);
            if (armorTrim != null) {
                this.smithingTrim = armorTrim;
            }
            if (this.isDrop) {
                InvUtils.drop().slotId(3);
            } else {
                InvUtils.shiftClick().slotId(3);
            }
        } else if (smithingScreenHandler.getSlot(0).getItem().isEmpty() && this.smithingItem1 != null && this.smithingItem1 != Items.AIR) {
            ItemStack nextStack = this.nextPlayerStack(itemStack -> itemStack.getItem() == this.smithingItem1);
            if (nextStack.isEmpty()) {
                this.finish();
                return;
            }
            InvUtils.shiftClick().slot(this.getCurPlayerSlot());
        } else if (smithingScreenHandler.getSlot(1).getItem().isEmpty() && this.smithingItem2 != null && this.smithingItem2 != Items.AIR) {
            ItemStack nextStack = this.nextPlayerStack(itemStack -> {
                if (itemStack.getItem() != this.smithingItem2) {
                    return false;
                }
                if (this.smithingTrim != null) {
                    ArmorTrim armorTrim = (ArmorTrim)itemStack.get(DataComponents.TRIM);
                    return !this.smithingTrim.equals((Object)armorTrim);
                }
                return true;
            });
            if (nextStack.isEmpty()) {
                this.finish();
                return;
            }
            InvUtils.shiftClick().slot(this.getCurPlayerSlot());
        } else if (smithingScreenHandler.getSlot(2).getItem().isEmpty() && this.smithingItem3 != null && this.smithingItem3 != Items.AIR) {
            ItemStack nextStack = this.nextPlayerStack(itemStack -> itemStack.getItem() == this.smithingItem3);
            if (nextStack.isEmpty()) {
                this.finish();
                return;
            }
            InvUtils.shiftClick().slot(this.getCurPlayerSlot());
        }
        this.setDelay();
    }

    private void margeKit(CraftingMenu craftingScreenHandler) {
        ItemStack outItemStack = craftingScreenHandler.getResultSlot().getItem();
        if (outItemStack.getItem() == this.needItem) {
            this.kitPlacedPending = false;
            if (this.isDrop) {
                InvUtils.drop().slotId(0);
            } else {
                InvUtils.shiftClick().slotId(0);
            }
            this.setDelay();
            return;
        }
        Item colorItem = kitColorMap.get(this.needItem);
        boolean needKit = true;
        boolean needColor = true;
        for (int i = 1; i <= 9; ++i) {
            Item item = craftingScreenHandler.getSlot(i).getItem().getItem();
            if (HeItemUtils.isShulkerBox(item)) {
                needKit = false;
            }
            if (item != colorItem) continue;
            needColor = false;
        }
        if (needKit) {
            ItemStack nextStack;
            if (this.kitPlacedPending) {
                if (System.currentTimeMillis() - this.kitPlacedTime < 1000L) {
                    this.setDelay();
                    return;
                }
                this.kitPlacedPending = false;
            }
            if ((nextStack = this.nextPlayerStack((ItemStack itemStack) -> HeItemUtils.isShulkerBox(itemStack.getItem()) && itemStack.getItem() != this.needItem)).isEmpty()) {
                this.finish();
            } else {
                InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                this.kitPlacedPending = true;
                this.kitPlacedTime = System.currentTimeMillis();
                this.setDelay();
            }
        } else if (needColor) {
            this.kitPlacedPending = false;
            ItemStack nextStack = this.nextPlayerStack((ItemStack itemStack) -> itemStack.getItem() == colorItem);
            if (nextStack.isEmpty()) {
                this.finish();
            } else {
                InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                this.setDelay();
            }
        } else {
            this.setDelay();
        }
    }

    private int getFireworkFlightDuration(ItemStack stack) {
        if (stack.getItem() != Items.FIREWORK_ROCKET) {
            return -1;
        }
        Fireworks component = (Fireworks)stack.get(DataComponents.FIREWORKS);
        if (component == null) {
            return -1;
        }
        return component.flightDuration();
    }

    private void doFireworkCrafting(CraftingMenu craftingScreenHandler) {
        ItemStack outputStack = craftingScreenHandler.getResultSlot().getItem();
        if (outputStack.getItem() == Items.FIREWORK_ROCKET && this.getFireworkFlightDuration(outputStack) == this.needGunpowderCount) {
            if (this.isDrop) {
                InvUtils.drop().slotId(0);
            } else {
                InvUtils.shiftClick().slotId(0);
            }
            this.setDelay();
            return;
        }
        int paperCount = 0;
        int gunpowderCount = 0;
        for (int i = 1; i <= 9; ++i) {
            ItemStack stack = craftingScreenHandler.getSlot(i).getItem();
            if (stack.getItem() == Items.PAPER) {
                ++paperCount;
                continue;
            }
            if (stack.getItem() == Items.GUNPOWDER) {
                ++gunpowderCount;
                continue;
            }
            if (stack.isEmpty()) continue;
            this.warning("工作台上存在错误的材料", new Object[0]);
            this.finish();
            return;
        }
        if (paperCount <= 0) {
            ItemStack paperStack = this.nextPlayerStack((ItemStack s) -> s.getItem() == Items.PAPER);
            if (paperStack.isEmpty()) {
                this.finish();
            } else {
                InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                this.setDelay();
            }
        } else if (gunpowderCount > this.needGunpowderCount) {
            for (int i = 1; i <= 9; ++i) {
                if (craftingScreenHandler.getSlot(i).getItem().getItem() != Items.GUNPOWDER) continue;
                InvUtils.shiftClick().slotId(i);
                this.setDelay();
                return;
            }
        } else if (gunpowderCount < this.needGunpowderCount) {
            ItemStack nextStack = this.nextPlayerStack((ItemStack s) -> s.getItem() == Items.GUNPOWDER);
            if (nextStack.isEmpty()) {
                this.finish();
            } else {
                InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                this.setDelay();
            }
        } else {
            this.setDelay();
        }
    }

    private void keydown(boolean isDrop) {
        if (!this.isReady()) {
            return;
        }
        AbstractContainerMenu screenHandler = this.mc.player.containerMenu;
        if (screenHandler instanceof CraftingMenu) {
            CraftingMenu craftingScreenHandler = (CraftingMenu)screenHandler;
            if (!isDrop && !InvUtils.find(ItemStack::isEmpty, (int)0, (int)35).found()) {
                this.info("需要至少留一个空位", new Object[0]);
                return;
            }
            Item needItem = Items.AIR;
            Slot outputSlot = craftingScreenHandler.getResultSlot();
            ItemStack outItemStack = outputSlot.getItem();
            if (!outItemStack.isEmpty()) {
                if (!this.craftingItems.get().contains(outItemStack.getItem())) {
                    this.craftingItems.get().add(outItemStack.getItem());
                }
                needItem = outItemStack.getItem();
            }
            if (needItem == Items.AIR) {
                Map<Item, RecipeDisplayId> recipeIdMap = this.getRecipeIdMap();
                for (Map.Entry<Item, RecipeDisplayId> entry : recipeIdMap.entrySet()) {
                    Item item = entry.getKey();
                    if (!this.craftingItems.get().contains(item)) continue;
                    needItem = item;
                    if (!kitColorMap.containsKey(item)) continue;
                    break;
                }
            }
            if (needItem == Items.AIR) {
                return;
            }
            if (needItem == Items.FIREWORK_ROCKET) {
                this.needGunpowderCount = this.fireworkSpeed.get().getGunpowderCount();
            }
            this.needItem = needItem;
            this.doing = true;
            this.isDrop = isDrop;
            this.kitPlacedPending = false;
            this.info("合成物品: " + Names.get(needItem), new Object[0]);
        } else {
            AbstractContainerMenu smithingHandler = this.mc.player.containerMenu;
            if (smithingHandler instanceof SmithingMenu) {
                SmithingMenu smithingScreenHandler = (SmithingMenu)smithingHandler;
                if (!isDrop && !InvUtils.find(ItemStack::isEmpty, (int)0, (int)35).found()) {
                    this.info("需要至少留一个空位", new Object[0]);
                    return;
                }
                ItemStack baseStack = smithingScreenHandler.getSlot(0).getItem();
                ItemStack additionStack = smithingScreenHandler.getSlot(1).getItem();
                ItemStack templateStack = smithingScreenHandler.getSlot(2).getItem();
                ItemStack outputStack = smithingScreenHandler.getSlot(3).getItem();
                if (outputStack.isEmpty() && (baseStack.isEmpty() || additionStack.isEmpty() || templateStack.isEmpty())) {
                    return;
                }
                if (!outputStack.isEmpty()) {
                    this.smithingTrim = (ArmorTrim)outputStack.get(DataComponents.TRIM);
                }
                this.smithingItem1 = baseStack.isEmpty() ? null : baseStack.getItem();
                this.smithingItem2 = additionStack.isEmpty() ? null : additionStack.getItem();
                this.smithingItem3 = templateStack.isEmpty() ? null : templateStack.getItem();
                this.needItem = outputStack.isEmpty() ? Items.AIR : outputStack.getItem();
                this.doing = true;
                this.isDrop = isDrop;
                this.kitPlacedPending = false;
                this.info("锻造台合成: " + (this.needItem == Items.AIR ? "已识别配方" : Names.get(this.needItem)), new Object[0]);
            } else {
                this.doing = false;
            }
        }
    }

    private void closeCurScreenIfNeed() {
        if (this.closeScreen.get()) {
            HeInvUtils.closeCurScreen();
        }
    }

    private void finish() {
        this.info("合成结束", new Object[0]);
        this.doing = false;
        this.smithingItem1 = null;
        this.smithingItem2 = null;
        this.smithingItem3 = null;
        this.smithingTrim = null;
        this.closeCurScreenIfNeed();
    }

    private Map<Item, RecipeDisplayId> getRecipeIdMap() {
        HashMap<Item, RecipeDisplayId> recipeIdMap = new HashMap<Item, RecipeDisplayId>();
        ClientRecipeBook recipeBook = this.mc.player.getRecipeBook();
        for (RecipeCollection recipeResultCollection : recipeBook.getCollections()) {
            List<RecipeDisplayEntry> recipes = recipeResultCollection.getSelectedRecipes(RecipeCollection.CraftableStatus.CRAFTABLE);
            for (RecipeDisplayEntry recipe : recipes) {
                RecipeDisplay recipeDisplay = recipe.display();
                List<ItemStack> resultStacks = recipeDisplay.result().resolveForStacks(SlotDisplayContext.fromLevel((Level)this.mc.level));
                for (ItemStack resultStack : resultStacks) {
                    Item item = resultStack.getItem();
                    if (!this.craftingItems.get().contains(item)) continue;
                    recipeIdMap.put(item, recipe.id());
                }
            }
        }
        return recipeIdMap;
    }

    static {
        HashMap<Item, Item> hashMap = new HashMap<Item, Item>();
        hashMap.put(Items.WHITE_SHULKER_BOX, Items.WHITE_DYE);
        hashMap.put(Items.ORANGE_SHULKER_BOX, Items.ORANGE_DYE);
        hashMap.put(Items.MAGENTA_SHULKER_BOX, Items.MAGENTA_DYE);
        hashMap.put(Items.LIGHT_BLUE_SHULKER_BOX, Items.LIGHT_BLUE_DYE);
        hashMap.put(Items.YELLOW_SHULKER_BOX, Items.YELLOW_DYE);
        hashMap.put(Items.LIME_SHULKER_BOX, Items.LIME_DYE);
        hashMap.put(Items.PINK_SHULKER_BOX, Items.PINK_DYE);
        hashMap.put(Items.GRAY_SHULKER_BOX, Items.GRAY_DYE);
        hashMap.put(Items.LIGHT_GRAY_SHULKER_BOX, Items.LIGHT_GRAY_DYE);
        hashMap.put(Items.CYAN_SHULKER_BOX, Items.CYAN_DYE);
        hashMap.put(Items.PURPLE_SHULKER_BOX, Items.PURPLE_DYE);
        hashMap.put(Items.BLUE_SHULKER_BOX, Items.BLUE_DYE);
        hashMap.put(Items.BROWN_SHULKER_BOX, Items.BROWN_DYE);
        hashMap.put(Items.GREEN_SHULKER_BOX, Items.GREEN_DYE);
        hashMap.put(Items.RED_SHULKER_BOX, Items.RED_DYE);
        hashMap.put(Items.BLACK_SHULKER_BOX, Items.BLACK_DYE);
        kitColorMap = Collections.unmodifiableMap(hashMap);
    }

    public static enum FireworkSpeed {
        一速,
        二速,
        三速;

        public int getGunpowderCount() {
            return this.ordinal() + 1;
        }
    }
}
