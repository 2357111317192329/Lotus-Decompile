package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.modules.enchantment.AutoEnchantmentType;
import com.xiaohe66.mc.meteor.lotus.modules.enchantment.AutoEnchantmentXpSource;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.EnchantmentUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.HePosUtils;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.EnchantmentNode;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.impl.EnchantmentBookNode;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.impl.EnchantmentEquipNode;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.impl.EnchantmentMargeNode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
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
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.decoration.ItemFrame;
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
   private static final Set<Item> ENCHANTABLE_ITEMS = Set.of(
      Items.DIAMOND_PICKAXE,
      Items.DIAMOND_AXE,
      Items.DIAMOND_SHOVEL,
      Items.DIAMOND_HOE,
      Items.SHEARS,
      Items.FLINT_AND_STEEL,
      Items.DIAMOND_SWORD,
      Items.BOW,
      Items.CROSSBOW,
      Items.TRIDENT,
      Items.MACE,
      Items.DIAMOND_HELMET,
      Items.TURTLE_HELMET,
      Items.DIAMOND_CHESTPLATE,
      Items.ELYTRA,
      Items.DIAMOND_LEGGINGS,
      Items.DIAMOND_BOOTS,
      Items.FISHING_ROD,
      Items.BOOK,
      Items.ENCHANTED_BOOK
   );
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<AutoEnchantmentXpSource> xpSource = this.sgGeneral.add(new EnumSetting.Builder<AutoEnchantmentXpSource>()
      .name("经验来源")
      .description("缺少经验时经验的来源类型")
      .defaultValue(AutoEnchantmentXpSource.杀戮光环)
      .build()
   );
   public final Setting<Integer> dropXpDelay = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                        .name("丢经验延迟"))
                     .description("是否给附魔后的物品改名"))
                  .sliderRange(0, 100)
                  .defaultValue(5))
               .visible(() -> this.xpSource.get() == AutoEnchantmentXpSource.XP))
            .build()
      );
   public final Setting<Boolean> rename = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("改名"))
                  .description("是否给附魔后的物品改名"))
               .defaultValue(true))
            .build()
      );
   public final Setting<String> newName = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)((meteordevelopment.meteorclient.settings.StringSetting.Builder)new meteordevelopment.meteorclient.settings.StringSetting.Builder()
                     .name("新名称"))
                  .description("给附魔后的物品设置的新名称"))
               .defaultValue("lotus打造的神兵"))
            .build()
      );
   public final Setting<Boolean> onlyRename = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("不附魔仅改名"))
                     .description("不做附魔仅做改名"))
                  .defaultValue(false))
               .onChanged(b -> {
                  if (b) {
                     this.rename.set(true);
                  }
               }))
            .build()
      );
   public final Setting<Item> targetItem = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemSetting.Builder)((meteordevelopment.meteorclient.settings.ItemSetting.Builder)((meteordevelopment.meteorclient.settings.ItemSetting.Builder)((meteordevelopment.meteorclient.settings.ItemSetting.Builder)new meteordevelopment.meteorclient.settings.ItemSetting.Builder()
                        .name("物品"))
                     .description("要附魔的物品"))
                  .defaultValue(Items.DIAMOND_PICKAXE))
               .filter(ENCHANTABLE_ITEMS::contains)
               .visible(() -> !(Boolean)this.onlyRename.get()))
            .build()
      );
   public final Setting<AutoEnchantmentType> presetType = this.sgGeneral.add(new EnumSetting.Builder<AutoEnchantmentType>()
        .name("预设")
        .description("选择预设配置，选择后会自动更新物品和附魔选项")
        .defaultValue(AutoEnchantmentType.自定义)
        .onChanged(this::onPresetChanged)
        .visible(() -> !onlyRename.get())
        .build()
   );
   public final Setting<Set<ResourceKey<Enchantment>>> selectedEnchantments = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.EnchantmentListSetting.Builder)((meteordevelopment.meteorclient.settings.EnchantmentListSetting.Builder)((meteordevelopment.meteorclient.settings.EnchantmentListSetting.Builder)new meteordevelopment.meteorclient.settings.EnchantmentListSetting.Builder()
                     .name("附魔"))
                  .description("选择要附魔的附魔类型，选择预设时会自动更新"))
               .visible(() -> !(Boolean)this.onlyRename.get()))
            .build()
      );
   public final Setting<Integer> pauseTime = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                     .name("暂停时间"))
                  .sliderRange(0, 100)
                  .defaultValue(20))
               .visible(() -> !(Boolean)this.onlyRename.get()))
            .build()
      );
   private AnvilMenu anvilLevelHandler;
   private final Map<ResourceKey<Enchantment>, BlockPos> enchantmentPosMap = new HashMap<>();
   private BlockPos supplyPos;
   private BlockPos grindPos;
   private BlockPos anvilPos;
   private BlockPos putPos;
   private final LinkedList<ResourceKey<Enchantment>> needTakeBook = new LinkedList<>();
   private int needLevel;
   private int index1;
   private int index2;
   private ItemStack itemStack1;
   private ItemStack itemStack2;
   private Item onlyRenameItem = Items.AIR;
   private int lastSlot = -1;

   public AutoEnchantment() {
      super("自动附魔", "自动附魔(祛魔)和改名, 附魔需要搭配附魔平台使用（但仅改名时可以随处使用）。快捷栏需要拿一些铁砧。");
      this.addStep(Steps.NEXT, this::next);
      this.addStep(Steps.TAKE_ITEM, this::takeEquip);
      this.addStep(Steps.GRIND, this::grind);
      this.addStep(Steps.TAKE_BOOK, this::takeBook);
      this.addStep(Steps.PLACE, this::placeAnvil);
      this.addStep(Steps.USE, this::useAnvil);
      this.addStep(Steps.LEVEL, this::level);
      this.addStep(Steps.PUT_ITEM, this::put);
   }

   public void onActivate() {
      if (this.isReady()) {
         this.anvilLevelHandler = new AnvilMenu(-1, this.mc.player.getInventory());
         if ((Boolean)this.rename.get() && StringUtils.isBlank((CharSequence)this.newName.get())) {
            this.warning("改名时<新名称>不能为空", new Object[0]);
            this.toggle();
         } else {
            if (!(Boolean)this.onlyRename.get()) {
               this.enchantmentPosMap.clear();
               this.needTakeBook.clear();
               this.supplyPos = null;
               this.grindPos = null;
               this.anvilPos = null;
               this.putPos = null;

               for (ItemFrame itemFrame : this.mc.level.getEntitiesOfClass(ItemFrame.class, this.mc.player.getBoundingBox().inflate(6.0), frame -> {
                  Item item = frame.getItem().getItem();
                  return item == Items.REDSTONE || item == Items.REDSTONE_BLOCK;
               })) {
                  ItemStack itemStack = itemFrame.getItem();
                  if (!itemStack.isEmpty()) {
                     if (itemStack.getItem() == Items.REDSTONE) {
                        Optional<BlockPos> chestPos = HePosUtils.getOtherChestPos(itemFrame);
                        chestPos.ifPresent(blockPosx -> this.supplyPos = blockPosx);
                     } else if (itemStack.getItem() == Items.REDSTONE_BLOCK) {
                        Optional<BlockPos> chestPos = HePosUtils.getOtherChestPos(itemFrame);
                        chestPos.ifPresent(blockPosx -> this.putPos = blockPosx);
                     }
                  }
               }

               for (BlockPos blockPos : HeBlockUtils.listPosInSphere(5, 2, this.mc.player.blockPosition())) {
                  BlockState blockState = this.mc.level.getBlockState(blockPos);
                  Block block = blockState.getBlock();
                  if (block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL) {
                     this.anvilPos = blockPos;
                  } else if (block == Blocks.GRINDSTONE) {
                     this.grindPos = blockPos;
                  }
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

               Map<ItemBo, BlockPos> takePosMap = HePosUtils.getItemFrameKitPosMap(6);
               if (takePosMap.isEmpty()) {
                  this.warning("未识别到附魔书", new Object[0]);
                  this.toggle();
                  return;
               }

               for (Entry<ItemBo, BlockPos> entry : takePosMap.entrySet()) {
                  this.enchantmentPosMap.put(entry.getKey().getEnchantment(), entry.getValue());
               }

               this.delayNext(Steps.NEXT);
            } else {
               if (!(Boolean)this.rename.get()) {
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

               for (BlockPos blockPos : HeBlockUtils.listPosInSphere(4, 2, this.mc.player.blockPosition())) {
                  BlockState blockState = this.mc.level.getBlockState(blockPos);
                  Block block = blockState.getBlock();
                  if (block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL) {
                     this.anvilPos = blockPos;
                     this.onlyRenameItem = onlyRenameItemStack.getItem();
                     this.info("开始改名:" + Names.get(this.onlyRenameItem), new Object[0]);
                     this.delayNext(Steps.NEXT);
                     return;
                  }
               }

               this.warning("身边没有铁砧", new Object[0]);
               this.toggle();
            }
         }
      }
   }

   private void onPresetChanged(AutoEnchantmentType newType) {
      if (!newType.getAll().equals(this.selectedEnchantments.get())) {
         this.selectedEnchantments.set(newType.getAll());
      }
   }

   private void put() {
      this.openChest(this.putPos, inventory -> {
         ItemStack nextDoneStack = this.nextDoneStack();
         if (!nextDoneStack.isEmpty()) {
            this.info("卸货", new Object[0]);
            InvUtils.shiftClick().slot(this.getCurPlayerSlot());
            this.setDelay();
         } else {
            this.delayCloseNext(Steps.NEXT);
            this.setCloseScreenAfterDelay((Integer)this.pauseTime.get());
         }
      });
   }

   private ItemStack nextDoneStack() {
      Item target = (Item)this.targetItem.get();
      return this.nextPlayerStack(itemStack -> {
         if (itemStack.getItem() == this.targetItem.get()) {
            Set<ResourceKey<Enchantment>> enchantmentSet = EnchantmentUtils.getEnchantment(itemStack);
            return enchantmentSet.equals(this.selectedEnchantments.get());
         } else {
            return false;
         }
      });
   }

   private void level() {
      if (this.xpSource.get() == AutoEnchantmentXpSource.XP) {
         if (this.needLevel <= this.mc.player.experienceLevel) {
            this.delayNext(Steps.NEXT);
         } else {
            FindItemResult xpResult = InvUtils.findInHotbar(new Item[]{Items.EXPERIENCE_BOTTLE});
            if (xpResult.found()) {
               if (xpResult.getHand() == null) {
                  InvUtils.swap(xpResult.slot(), false);
               } else {
                  Rotations.rotate(this.mc.player.getYRot(), 90.0, () -> this.mc.gameMode.useItem(this.mc.player, xpResult.getHand()));
               }

               this.setDelay((Integer)this.dropXpDelay.get());
            } else {
               FindItemResult xpResult2 = InvUtils.find(itemStack -> itemStack.getItem() == Items.EXPERIENCE_BOTTLE, 0, 36);
               if (xpResult2.found()) {
                  HeInvUtils.swapMainHand(xpResult2.slot());
                  this.setDelay();
               } else {
                  this.warning("缺少XP", new Object[0]);
                  this.disable();
                  this.sendToggledMsg();
               }
            }
         }
      } else {
         KillAura killAura = (KillAura)Modules.get().get(KillAura.class);
         if (this.needLevel > this.mc.player.experienceLevel) {
            if (!killAura.isActive()) {
               this.info("开启杀戮", new Object[0]);
               killAura.toggle();
            }

            this.setDelay();
         } else {
            if (killAura.isActive()) {
               killAura.toggle();
            }

            this.delayNext(Steps.NEXT);
         }
      }
   }

   private void placeAnvil() {
      FindItemResult anvilResult = InvUtils.find(new Item[]{Items.ANVIL});
      if (!anvilResult.found()) {
         this.warning("缺少铁砧", new Object[0]);
         this.disable();
         this.sendToggledMsg();
      } else if (!BlockUtils.canPlace(this.anvilPos, true)) {
         this.warning("无法放置铁砧", new Object[0]);
         this.disable();
         this.sendToggledMsg();
      } else {
         if (InvUtils.testInMainHand(new Item[]{Items.ANVIL})) {
            this.info("放置铁砧", new Object[0]);
            HeBlockUtils.place(this.anvilPos, anvilResult.slot(), true, Direction.DOWN);
            this.delayNext(Steps.USE);
         } else {
            HeInvUtils.swapMainHand(anvilResult.slot());
            this.setDelay();
         }
      }
   }

   private void useAnvil() {
      ItemStack itemStack1 = this.getItemStack(this.index1);
      ItemStack itemStack2 = ItemStack.EMPTY;
      if (!(Boolean)this.onlyRename.get()) {
         itemStack2 = this.getItemStack(this.index2);
      }

      String newNameValue = null;
      if ((Boolean)this.rename.get()) {
         newNameValue = (String)this.newName.get();
      }

      int needLevel = this.getLevelCost(itemStack1, itemStack2, newNameValue);
      if (needLevel > this.mc.player.experienceLevel) {
         this.needLevel = needLevel;
         this.delayCloseNext(Steps.LEVEL);
      } else if (!(this.mc.player.containerMenu instanceof AnvilMenu screenHandler)) {
         if (this.mc.level.getBlockState(this.anvilPos).isAir()) {
            this.info("补放铁砧", new Object[0]);
            this.delayNext(Steps.PLACE);
         } else {
            this.rotateAndOpen(this.anvilPos);
            this.setDelay();
         }
      } else {
         if (screenHandler.getSlot(0).getItem() == ItemStack.EMPTY) {
            if (this.getItemStack(this.index1).getItem() != this.itemStack1.getItem()) {
               this.delayCloseNext(Steps.NEXT);
               return;
            }

            InvUtils.shiftClick().slot(this.index1);
            this.setDelay();
         } else if (!(Boolean)this.onlyRename.get() && screenHandler.getSlot(1).getItem() == ItemStack.EMPTY) {
            if (this.getItemStack(this.index2).getItem() != itemStack2.getItem()) {
               this.delayCloseNext(Steps.NEXT);
               return;
            }

            InvUtils.shiftClick().slot(this.index2);
            this.setDelay();
         } else {
            if ((Boolean)this.rename.get()) {
               ItemStack itemStack = screenHandler.getSlot(2).getItem();
               if (itemStack.getItem() != Items.ENCHANTED_BOOK) {
                  Component customName = itemStack.getCustomName();
                  if (customName == null || !newNameValue.equals(customName.getString())) {
                     this.info("改名", new Object[0]);
                     screenHandler.setItemName(newNameValue);
                     EditBox textFieldWidget = ((AnvilScreen)this.mc.screen).name;
                     textFieldWidget.setValue(newNameValue);
                     textFieldWidget.moveCursorToEnd(false);
                     this.mc.player.connection.send(new ServerboundRenameItemPacket(newNameValue));
                     this.setDelay();
                     return;
                  }
               }
            }

            if (screenHandler.getCost() > this.mc.player.experienceLevel) {
               this.needLevel = screenHandler.getCost();
               this.delayCloseNext(Steps.LEVEL);
            } else {
               InvUtils.shiftClick().slotId(2);
               if ((Boolean)this.onlyRename.get()) {
                  this.delayNext(Steps.NEXT);
               } else {
                  this.delayCloseNext(Steps.NEXT);
               }
            }
         }
      }
   }

   private void grind() {
      if (this.mc.player.containerMenu instanceof GrindstoneMenu screenHandler) {
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
      } else if (this.mc.level.getBlockState(this.grindPos).getBlock() != Blocks.GRINDSTONE) {
         this.breakStep("砂轮位置错误");
      } else {
         this.rotateAndOpen(this.grindPos);
         this.setDelay();
      }
   }

   private void takeEquip() {
      Item target = (Item)this.targetItem.get();
      this.openChest(this.supplyPos, inventory -> {
         ItemStack nextItemStack = this.nextScreenStack(itemStack -> itemStack.getItem() == target);
         if (nextItemStack.isEmpty()) {
            this.warning("无法补给<" + Names.get(target) + ">", new Object[0]);
            this.disable();
            this.sendToggledMsg();
         } else {
            int curScreenSlot = this.getCurScreenSlot();
            InvUtils.shiftClick().slotId(curScreenSlot);
            this.info("拿取装备", new Object[0]);
            this.delayCloseNext(Steps.NEXT, () -> this.lastSlot == -1);
         }
      });
   }

   private void takeBook() {
      if (this.needTakeBook.isEmpty()) {
         this.delayCloseNext(Steps.NEXT);
      } else {
         ResourceKey<Enchantment> needBook = this.needTakeBook.getFirst();
         BlockPos kitPos = this.enchantmentPosMap.get(needBook);
         if (this.mc.player.containerMenu instanceof InventoryMenu) {
            if (kitPos == null) {
               this.warning("找不到<" + Names.get(needBook) + ">容器", new Object[0]);
               this.disable();
               this.sendToggledMsg();
               return;
            }

            if (!HeItemUtils.isShulkerBox(this.mc.level.getBlockState(kitPos).getBlock().asItem())) {
               this.warning("找不到<" + Names.get(needBook) + ">容器", new Object[0]);
               this.setDelay();
               return;
            }
         }

         this.openKit(kitPos, Direction.UP, screenHandler -> {
            ItemStack nextScreenStack = this.nextNeedBook(needBook);
            if (nextScreenStack.isEmpty()) {
               this.warning("无法拿取<" + Names.get(needBook) + ">", new Object[0]);
               this.setDelay();
            } else {
               this.info("拿取<" + Names.get(needBook) + ">", new Object[0]);
               int bookSlot = this.getCurScreenSlot();
               InvUtils.shiftClick().slotId(bookSlot);
               this.needTakeBook.poll();
               if (this.needTakeBook.isEmpty()) {
                  this.delayCloseNext(Steps.NEXT, () -> this.lastSlot == -1);
               } else {
                  this.delayCloseNext(Steps.TAKE_BOOK);
               }
            }
         });
      }
   }

   private ItemStack nextNeedBook(ResourceKey<Enchantment> needBook) {
      return this.nextScreenStack(itemStack -> {
         if (itemStack.getItem() == Items.ENCHANTED_BOOK) {
            Set<ResourceKey<Enchantment>> bookEnchantMentSet = EnchantmentUtils.getEnchantment(itemStack);
            return bookEnchantMentSet.contains(needBook);
         } else {
            return false;
         }
      });
   }

   private void next() {
      if ((Boolean)this.onlyRename.get()) {
         ItemStack nextStack = this.nextPlayerStack(itemStack -> {
            if (itemStack.getItem() != this.onlyRenameItem) {
               return false;
            }

            Component customName = itemStack.getCustomName();
            boolean done = customName != null && ((String)this.newName.get()).equals(customName.getString());
            return !done;
         });
         if (nextStack.isEmpty()) {
            this.breakStep("改名完毕");
            this.closeScreen();
            this.disable();
            this.sendToggledMsg();
         } else {
            this.setIndex1(this.getCurPlayerSlot());
            this.delayNext(Steps.USE);
         }
      } else {
         ItemStack equipItemStack = this.nextPlayerStack(itemStack -> itemStack.getItem() == this.targetItem.get());
         if (equipItemStack.isEmpty()) {
            this.delayNext(Steps.TAKE_ITEM);
         } else {
            Set<ResourceKey<Enchantment>> targetEnchants = (Set<ResourceKey<Enchantment>>)this.selectedEnchantments.get();
            Set<ResourceKey<Enchantment>> enchantmentSet = EnchantmentUtils.getEnchantment(equipItemStack);
            if (!targetEnchants.containsAll(enchantmentSet)) {
               this.setIndex1(this.getCurPlayerSlot());
               this.delayNext(Steps.GRIND);
            } else {
               Set<ResourceKey<Enchantment>> haveEnchantmentSet = EnchantmentUtils.getEnchantment(equipItemStack, true);
               if (haveEnchantmentSet.containsAll(targetEnchants)) {
                  this.delayNext(Steps.PUT_ITEM);
               } else {
                  Set<ResourceKey<Enchantment>> missEnchantmentSet = new HashSet<>(targetEnchants);
                  missEnchantmentSet.removeAll(haveEnchantmentSet);
                  EnchantmentMargeNode margeNode = EnchantmentUtils.bestStepSimple(equipItemStack, missEnchantmentSet);
                  this.getSlotOrMarge(margeNode);
               }
            }
         }
      }
   }

   private int getSlotOrMarge(EnchantmentNode node) {
      if (node instanceof EnchantmentMargeNode margeNode) {
         if (margeNode.getLeft() instanceof EnchantmentEquipNode) {
            int rightSlot = this.getSlotOrMarge(margeNode.getRight());
            if (rightSlot >= 0) {
               int leftSlot = this.getCurPlayerSlot();
               this.setIndex1(leftSlot);
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
         } else {
            int bookSlot = HeInvUtils.findBookSlot(margeNode.getAllEnchantmentKey());
            if (bookSlot >= 0) {
               return bookSlot;
            } else {
               int leftSlot = this.getSlotOrMarge(margeNode.getLeft());
               if (leftSlot == -2) {
                  return -2;
               } else {
                  int rightSlot = this.getSlotOrMarge(margeNode.getRight());
                  if (rightSlot == -2) {
                     return -2;
                  } else if (leftSlot == -3 || rightSlot == -3) {
                     this.step = Steps.TAKE_BOOK;
                     return -3;
                  } else if (leftSlot < 0) {
                     this.error("意外的状态(book), left :" + leftSlot, new Object[0]);
                     return leftSlot;
                  } else if (rightSlot < 0) {
                     this.error("意外的状态(book), right :" + rightSlot, new Object[0]);
                     return rightSlot;
                  } else {
                     this.setIndex1(leftSlot);
                     this.setIndex2(rightSlot);
                     this.step = Steps.USE;
                     return -2;
                  }
               }
            }
         }
      } else if (node instanceof EnchantmentBookNode bookNode) {
         int slot = HeInvUtils.findBookSlot(bookNode.getEnchantmentKey());
         if (slot >= 0) {
            return slot;
         }

         this.needTakeBook.add(bookNode.getEnchantmentKey());
         return -3;
      } else {
         throw new IllegalStateException("不可能的情况");
      }
   }

   private void setIndex1(int slot) {
      this.index1 = slot;
      this.itemStack1 = this.getItemStack(slot);
   }

   private void setIndex2(int slot) {
      this.index2 = slot;
      this.itemStack2 = this.getItemStack(slot);
   }

   private int getLevelCost(ItemStack itemStack, ItemStack itemStack2, String newName) {
      this.anvilLevelHandler.getSlot(0).setByPlayer(itemStack);
      if (!itemStack2.isEmpty()) {
         this.anvilLevelHandler.getSlot(1).setByPlayer(itemStack2);
      }

      if (newName != null) {
         this.anvilLevelHandler.setItemName(newName);
      }

      this.anvilLevelHandler.createResult();
      return this.anvilLevelHandler.getCost();
   }

   public void onDeactivate() {
      if(!this.isActive()){
          this.sendToggledMsg();
      }
      
      this.step = Steps.NONE;
      KillAura killAura = (KillAura)Modules.get().get(KillAura.class);
      if (killAura.isActive()) {
         killAura.toggle();
      }
   }
}
