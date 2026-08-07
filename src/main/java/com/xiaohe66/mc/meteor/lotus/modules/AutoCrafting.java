package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.ItemListSetting.Builder;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection.CraftableStatus;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoCrafting extends BaseModule {
   private static final Logger log = LoggerFactory.getLogger(AutoCrafting.class);
   private static final Map<Item, Item> kitColorMap;
   private final Setting<List<Item>> craftingItems = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("合成物品")).description("需要合成的物品")).defaultValue(List.of())).build());
   private final Setting<Keybind> keybind1 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                     .name("合成键"))
                  .description("合成键"))
               .defaultValue(Keybind.fromKey(67)))
            .action(() -> this.keydown(false))
            .build()
      );
   private final Setting<Keybind> keybind2 = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)((meteordevelopment.meteorclient.settings.KeybindSetting.Builder)new meteordevelopment.meteorclient.settings.KeybindSetting.Builder()
                     .name("丢出合成键"))
                  .description("丢出合成键"))
               .defaultValue(Keybind.fromKey(86)))
            .action(() -> this.keydown(true))
            .build()
      );
   private final Setting<AutoCrafting.FireworkSpeed> fireworkSpeed = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("烟花速度"))
                  .description("合成烟花时使用的火药数量（决定飞行速度）"))
               .defaultValue(AutoCrafting.FireworkSpeed.三速))
            .build()
      );
   private final Setting<Boolean> closeScreen = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("关闭界面"))
                  .description("在合成完毕后关闭界面"))
               .defaultValue(true))
            .build()
      );
   private static final long KIT_PLACE_TIMEOUT_MS = 1000L;
   private Item needItem;
   private boolean doing;
   private boolean isDrop;
   private boolean kitPlacedPending;
   private long kitPlacedTime;
   private int needGunpowderCount;

   public AutoCrafting() {
      super("自动合成", "必须在工作台使用, 可自动识别工作台上的配方, 或者手动选择合成物品。（计划未来支持锻造台）", 3);
   }

   @EventHandler
   private void onTick(Pre event) {
      if (this.checkAndDecrement()) {
         if (this.doing && !((List)this.craftingItems.get()).isEmpty()) {
            if (this.mc.player.containerMenu instanceof CraftingMenu craftingScreenHandler) {
               this.doCrafting(craftingScreenHandler);
            } else {
               this.finish();
            }
         }
      }
   }

   private void doCrafting(CraftingMenu craftingScreenHandler) {
      if (this.needItem == Items.FIREWORK_ROCKET) {
         this.doFireworkCrafting(craftingScreenHandler);
      } else if (HeItemUtils.isShulkerBox(this.needItem)) {
         this.margeKit(craftingScreenHandler);
      } else {
         Map<Item, RecipeDisplayId> recipeIdMap = this.getRecipeIdMap();
         if (recipeIdMap.containsKey(this.needItem)) {
            RecipeDisplayId networkRecipeId = recipeIdMap.get(this.needItem);
            this.mc.gameMode.handlePlaceRecipe(craftingScreenHandler.containerId, networkRecipeId, true);
            ContainerInput actionType = this.isDrop ? ContainerInput.THROW : ContainerInput.QUICK_MOVE;
            this.mc.gameMode.handleContainerInput(craftingScreenHandler.containerId, 0, 1, actionType, this.mc.player);
            this.setDelay();
         } else {
            this.finish();
         }
      }
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
      } else {
         Item colorItem = kitColorMap.get(this.needItem);
         boolean needKit = true;
         boolean needColor = true;

         for (int i = 1; i <= 9; i++) {
            Item item = craftingScreenHandler.getSlot(i).getItem().getItem();
            if (HeItemUtils.isShulkerBox(item)) {
               needKit = false;
            }

            if (item == colorItem) {
               needColor = false;
            }
         }

         if (needKit) {
            if (this.kitPlacedPending) {
               if (System.currentTimeMillis() - this.kitPlacedTime < 1000L) {
                  this.setDelay();
                  return;
               }

               this.kitPlacedPending = false;
            }

            ItemStack nextStack = this.nextPlayerStack(
               itemStack -> HeItemUtils.isShulkerBox(itemStack.getItem()) && itemStack.getItem() != this.needItem
            );
            if (nextStack.isEmpty()) {
               this.finish();
            } else {
               InvUtils.shiftClick().slot(this.getCurPlayerSlot());
               this.kitPlacedPending = true;
               this.kitPlacedTime = System.currentTimeMillis();
               this.setDelay();
            }
         } else if (needColor) {
            this.kitPlacedPending = false;
            ItemStack nextStack = this.nextPlayerStack(itemStack -> itemStack.getItem() == colorItem);
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
   }

   private int getFireworkFlightDuration(ItemStack stack) {
      if (stack.getItem() != Items.FIREWORK_ROCKET) {
         return -1;
      }

      Fireworks component = (Fireworks)stack.get(DataComponents.FIREWORKS);
      return component == null ? -1 : component.flightDuration();
   }

   private void doFireworkCrafting(CraftingMenu craftingScreenHandler) {
      ItemStack outputStack = craftingScreenHandler.getResultSlot().getItem();
      if (outputStack.getItem() == Items.FIREWORK_ROCKET) {
         int flightDuration = this.getFireworkFlightDuration(outputStack);
         if (flightDuration == this.needGunpowderCount) {
            if (this.isDrop) {
               InvUtils.drop().slotId(0);
            } else {
               InvUtils.shiftClick().slotId(0);
            }

            this.setDelay();
            return;
         }
      }

      int paperCount = 0;
      int gunpowderCount = 0;

      for (int i = 1; i <= 9; i++) {
         ItemStack stack = craftingScreenHandler.getSlot(i).getItem();
         if (stack.getItem() == Items.PAPER) {
            paperCount++;
         } else if (stack.getItem() == Items.GUNPOWDER) {
            gunpowderCount++;
         } else if (!stack.isEmpty()) {
            this.warning("工作台上存在错误的材料", new Object[0]);
            this.finish();
            return;
         }
      }

      if (paperCount <= 0) {
         ItemStack paperStack = this.nextPlayerStack(s -> s.getItem() == Items.PAPER);
         if (paperStack.isEmpty()) {
            this.info("沒有紙了");
            this.finish();
         } else {
            InvUtils.shiftClick().slot(this.getCurPlayerSlot());
            this.setDelay();
         }
      } else if (gunpowderCount > this.needGunpowderCount) {
         for (int i = 1; i <= 9; i++) {
            if (craftingScreenHandler.getSlot(i).getItem().getItem() == Items.GUNPOWDER) {
               InvUtils.shiftClick().slotId(i);
               this.setDelay();
               return;
            }
         }
      } else if (gunpowderCount < this.needGunpowderCount) {
         ItemStack nextStack = this.nextPlayerStack(s -> s.getItem() == Items.GUNPOWDER);
         if (nextStack.isEmpty()) {
            this.info("沒有火藥了(2速煙火至少要2組，3速煙火至少要3組)");
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
      if (this.isReady()) {
         if (this.mc.player.containerMenu instanceof CraftingMenu craftingScreenHandler) {
            if (!isDrop) {
               FindItemResult emptyResult = InvUtils.find(ItemStack::isEmpty, 0, 35);
               if (!emptyResult.found()) {
                  this.info("需要至少留一个空位", new Object[0]);
                  return;
               }
            }

            Item needItem = Items.AIR;
            Slot outputSlot = craftingScreenHandler.getResultSlot();
            ItemStack outItemStack = outputSlot.getItem();
            if (!outItemStack.isEmpty()) {
               if (!((List)this.craftingItems.get()).contains(outItemStack.getItem())) {
                  ((List)this.craftingItems.get()).add(outItemStack.getItem());
               }

               needItem = outItemStack.getItem();
            }

            if (needItem == Items.AIR) {
               Map<Item, RecipeDisplayId> recipeIdMap = this.getRecipeIdMap();

               for (Entry<Item, RecipeDisplayId> entry : recipeIdMap.entrySet()) {
                  Item item = entry.getKey();
                  if (((List)this.craftingItems.get()).contains(item)) {
                     needItem = item;
                     if (kitColorMap.containsKey(item)) {
                        break;
                     }
                  }
               }
            }

            if (needItem == Items.AIR) {
               return;
            }

            if (needItem == Items.FIREWORK_ROCKET) {
               this.needGunpowderCount = ((AutoCrafting.FireworkSpeed)this.fireworkSpeed.get()).getGunpowderCount();
            }

            this.needItem = needItem;
            this.doing = true;
            this.isDrop = isDrop;
            this.kitPlacedPending = false;
            this.info("合成物品: " + Names.get(needItem), new Object[0]);
         } else {
            this.doing = false;
         }
      }
   }

   private void closeCurScreenIfNeed() {
      if ((Boolean)this.closeScreen.get()) {
         HeInvUtils.closeCurScreen();
      }
   }

   private void finish() {
      this.info("合成结束", new Object[0]);
      this.doing = false;
      this.closeCurScreenIfNeed();
   }

   private Map<Item, RecipeDisplayId> getRecipeIdMap() {
      Map<Item, RecipeDisplayId> recipeIdMap = new HashMap<>();
      ClientRecipeBook recipeBook = this.mc.player.getRecipeBook();

      for (RecipeCollection recipeResultCollection : recipeBook.getCollections()) {
         for (RecipeDisplayEntry recipe : recipeResultCollection.getSelectedRecipes(CraftableStatus.CRAFTABLE)) {
            RecipeDisplay recipeDisplay = recipe.display();

            for (ItemStack resultStack : recipeDisplay.result().resolveForStacks(SlotDisplayContext.fromLevel(this.mc.level))) {
               Item item = resultStack.getItem();
               if (((List)this.craftingItems.get()).contains(item)) {
                  recipeIdMap.put(item, recipe.id());
               }
            }
         }
      }

      return recipeIdMap;
   }

   static {
      Map<Item, Item> map = new HashMap<>();
      map.put(Items.WHITE_SHULKER_BOX, Items.WHITE_DYE);
      map.put(Items.ORANGE_SHULKER_BOX, Items.ORANGE_DYE);
      map.put(Items.MAGENTA_SHULKER_BOX, Items.MAGENTA_DYE);
      map.put(Items.LIGHT_BLUE_SHULKER_BOX, Items.LIGHT_BLUE_DYE);
      map.put(Items.YELLOW_SHULKER_BOX, Items.YELLOW_DYE);
      map.put(Items.LIME_SHULKER_BOX, Items.LIME_DYE);
      map.put(Items.PINK_SHULKER_BOX, Items.PINK_DYE);
      map.put(Items.GRAY_SHULKER_BOX, Items.GRAY_DYE);
      map.put(Items.LIGHT_GRAY_SHULKER_BOX, Items.LIGHT_GRAY_DYE);
      map.put(Items.CYAN_SHULKER_BOX, Items.CYAN_DYE);
      map.put(Items.PURPLE_SHULKER_BOX, Items.PURPLE_DYE);
      map.put(Items.BLUE_SHULKER_BOX, Items.BLUE_DYE);
      map.put(Items.BROWN_SHULKER_BOX, Items.BROWN_DYE);
      map.put(Items.GREEN_SHULKER_BOX, Items.GREEN_DYE);
      map.put(Items.RED_SHULKER_BOX, Items.RED_DYE);
      map.put(Items.BLACK_SHULKER_BOX, Items.BLACK_DYE);
      kitColorMap = Collections.unmodifiableMap(map);
   }

   public enum FireworkSpeed {
      一速,
      二速,
      三速;

      public int getGunpowderCount() {
         return this.ordinal() + 1;
      }
   }
}
