package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.bo.StorageItem;
import com.xiaohe66.mc.meteor.lotus.bo.StoragePos;
import com.xiaohe66.mc.meteor.lotus.modules.clearup.ClearUpMapping;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.settings.ItemListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.ItemSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.util.Hand;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.hit.BlockHitResult;

public class AutoClearUp extends WalkModule {
   private final Setting<Integer> scanRange = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("扫描范围")).description("检测展示框和箱子的范围")).defaultValue(50)).min(1).sliderMax(100).build());
   private final Setting<Integer> qty = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("操作数")).description("每次操作的物品数量")).min(1).sliderMax(9).defaultValue(9)).build());
   private final Setting<Item> emptyKitItem = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemSetting.Builder)((meteordevelopment.meteorclient.settings.ItemSetting.Builder)((meteordevelopment.meteorclient.settings.ItemSetting.Builder)new meteordevelopment.meteorclient.settings.ItemSetting.Builder()
                     .name("空盒标识"))
                  .description("空盒箱子展示框上的物品"))
               .defaultValue(Items.WHITE_SHULKER_BOX))
            .build()
      );
   private final Setting<Item> takeKitItem = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemSetting.Builder)((meteordevelopment.meteorclient.settings.ItemSetting.Builder)((meteordevelopment.meteorclient.settings.ItemSetting.Builder)new meteordevelopment.meteorclient.settings.ItemSetting.Builder()
                     .name("待整理标识"))
                  .description("待整理箱子展示框上的物品"))
               .defaultValue(Items.SHULKER_BOX))
            .build()
      );
   private final Setting<Item> miscKitItem = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemSetting.Builder)((meteordevelopment.meteorclient.settings.ItemSetting.Builder)((meteordevelopment.meteorclient.settings.ItemSetting.Builder)new meteordevelopment.meteorclient.settings.ItemSetting.Builder()
                     .name("无法整理标识"))
                  .description("无法整理箱子展示框上的物品"))
               .defaultValue(Items.BLUE_SHULKER_BOX))
            .build()
      );
   private final Setting<List<Item>> needItems = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                  .name("提取整盒物品"))
               .description("将这里的物品提取出来打包成盒"))
            .defaultValue(
               new Item[]{
                  Items.ELYTRA,
                  Items.ENCHANTED_GOLDEN_APPLE,
                  Items.GOLDEN_CARROT,
                  Items.EXPERIENCE_BOTTLE,
                  Items.OBSIDIAN,
                  Items.ENDER_CHEST,
                  Items.ENDER_PEARL,
                  Items.FLINT_AND_STEEL,
                  Items.END_CRYSTAL,
                  Items.RESPAWN_ANCHOR,
                  Items.GLOWSTONE,
                  Items.TOTEM_OF_UNDYING,
                  Items.COAL_BLOCK,
                  Items.GOLD_BLOCK,
                  Items.IRON_BLOCK,
                  Items.REDSTONE_BLOCK,
                  Items.EMERALD_BLOCK,
                  Items.DIAMOND_BLOCK,
                  Items.LAPIS_BLOCK,
                  Items.COPPER_BLOCK,
                  Items.QUARTZ_BLOCK,
                  Items.ANCIENT_DEBRIS,
                  Items.NETHERITE_SCRAP,
                  Items.NETHERITE_INGOT,
                  Items.NETHERITE_BLOCK,
                  Items.NETHERITE_SWORD,
                  Items.NETHERITE_AXE,
                  Items.NETHERITE_HOE,
                  Items.NETHERITE_PICKAXE,
                  Items.NETHERITE_SHOVEL,
                  Items.NETHERITE_HELMET,
                  Items.NETHERITE_CHESTPLATE,
                  Items.NETHERITE_LEGGINGS,
                  Items.NETHERITE_BOOTS,
                  Items.DIAMOND_SWORD,
                  Items.DIAMOND_AXE,
                  Items.DIAMOND_HOE,
                  Items.DIAMOND_PICKAXE,
                  Items.DIAMOND_SHOVEL,
                  Items.DIAMOND_HELMET,
                  Items.DIAMOND_CHESTPLATE,
                  Items.DIAMOND_LEGGINGS,
                  Items.DIAMOND_BOOTS
               }
            )
            .build()
      );
   private final Setting<Boolean> initBtn = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("初始化"))
                     .description("使用前需要先初始化，保存所有箱子位置"))
                  .defaultValue(false))
               .onChanged(this::init))
            .build()
      );
   private final Map<Item, ClearUpMapping> clearUpMappings = new HashMap<>();
   private final Map<Item, StoragePos> itemPosMap = new HashMap<>();
   private StoragePos emptyKitPos;
   private StoragePos takeKitPos;
   private StoragePos miscKitPos;
   private final Map<Item, StoragePos> needPosMap = new HashMap<>();
   private final Map<Item, StoragePos> multiItemPosMap = new HashMap<>();
   private final Map<Item, Item> relatedToTargetMap = new HashMap<>();
   private StoragePos curOperationPos;
   private int takeQty = 0;
   private int putKitIndex = -1;

   public AutoClearUp() {
      super("自动分类", "有价值物品打包、散装物品装箱, 需要在lotus简易仓库才可使用。由资深猎人<jiafog199>友情赞助开发");
      this.addStep(Steps.NEXT, this::nextStep);
      this.addStep(Steps.PUT_KIT, this::putKit);
      this.addStep(Steps.PUT_ITEM, this::putItem);
      this.addStep(Steps.TAKE_ITEM, this::takeItem);
      this.addStep(Steps.PLACE_EMPTY_KIT, this::placeEmptyKit);
      this.addStep(Steps.TAKE_EMPTY_KIT, this::takeEmptyKit);
      this.addStep(Steps.PLACE_KIT, this::placeKit);
      this.addStep(Steps.TAKE_KIT, this::takeKit);
      this.addStep(Steps.TAKE_LOOSE_ITEM, this::takeLooseItem);
      this.addStep(Steps.BREAK_KIT, this::breakKit);
      this.initDefaultMappings();
   }

   private void initDefaultMappings() {
      this.clearUpMappings
         .put(
            Items.REDSTONE,
            new ClearUpMapping(
               Items.REDSTONE,
               List.of(
                  Items.REDSTONE,
                  Items.REDSTONE_BLOCK,
                  Items.REPEATER,
                  Items.COMPARATOR,
                  Items.PISTON,
                  Items.STICKY_PISTON,
                  Items.DISPENSER,
                  Items.DROPPER,
                  Items.HOPPER,
                  Items.OBSERVER,
                  Items.REDSTONE_TORCH,
                  Items.REDSTONE_LAMP,
                  Items.NOTE_BLOCK,
                  Items.TARGET,
                  Items.LEVER,
                  Items.TRIPWIRE_HOOK,
                  Items.DAYLIGHT_DETECTOR,
                  Items.LECTERN,
                  Items.CALIBRATED_SCULK_SENSOR,
                  Items.SCULK_SENSOR
               ),
               true
            )
         );
      List<Item> woodItems = new ArrayList<>(
         List.of(
            Items.OAK_PLANKS,
            Items.SPRUCE_PLANKS,
            Items.BIRCH_PLANKS,
            Items.JUNGLE_PLANKS,
            Items.ACACIA_PLANKS,
            Items.DARK_OAK_PLANKS,
            Items.MANGROVE_PLANKS,
            Items.CHERRY_PLANKS,
            Items.BAMBOO_PLANKS,
            Items.CRIMSON_PLANKS,
            Items.WARPED_PLANKS,
            Items.OAK_LOG,
            Items.SPRUCE_LOG,
            Items.BIRCH_LOG,
            Items.JUNGLE_LOG,
            Items.ACACIA_LOG,
            Items.DARK_OAK_LOG,
            Items.MANGROVE_LOG,
            Items.CHERRY_LOG,
            Items.BAMBOO_BLOCK,
            Items.CRIMSON_STEM,
            Items.WARPED_STEM,
            Items.STRIPPED_OAK_LOG,
            Items.STRIPPED_SPRUCE_LOG,
            Items.STRIPPED_BIRCH_LOG,
            Items.STRIPPED_JUNGLE_LOG,
            Items.STRIPPED_ACACIA_LOG,
            Items.STRIPPED_DARK_OAK_LOG,
            Items.STRIPPED_MANGROVE_LOG,
            Items.STRIPPED_CHERRY_LOG,
            Items.STRIPPED_BAMBOO_BLOCK,
            Items.STRIPPED_CRIMSON_STEM,
            Items.STRIPPED_WARPED_STEM,
            Items.OAK_WOOD,
            Items.SPRUCE_WOOD,
            Items.BIRCH_WOOD,
            Items.JUNGLE_WOOD,
            Items.ACACIA_WOOD,
            Items.DARK_OAK_WOOD,
            Items.MANGROVE_WOOD,
            Items.CHERRY_WOOD,
            Items.CRIMSON_HYPHAE,
            Items.WARPED_HYPHAE,
            Items.STRIPPED_OAK_WOOD,
            Items.STRIPPED_SPRUCE_WOOD,
            Items.STRIPPED_BIRCH_WOOD,
            Items.STRIPPED_JUNGLE_WOOD,
            Items.STRIPPED_ACACIA_WOOD,
            Items.STRIPPED_DARK_OAK_WOOD,
            Items.STRIPPED_MANGROVE_WOOD,
            Items.STRIPPED_CHERRY_WOOD,
            Items.STRIPPED_CRIMSON_HYPHAE,
            Items.STRIPPED_WARPED_HYPHAE,
            Items.OAK_SLAB,
            Items.SPRUCE_SLAB,
            Items.BIRCH_SLAB,
            Items.JUNGLE_SLAB,
            Items.ACACIA_SLAB,
            Items.DARK_OAK_SLAB,
            Items.MANGROVE_SLAB,
            Items.CHERRY_SLAB,
            Items.BAMBOO_SLAB,
            Items.CRIMSON_SLAB,
            Items.WARPED_SLAB,
            Items.OAK_STAIRS,
            Items.SPRUCE_STAIRS,
            Items.BIRCH_STAIRS,
            Items.JUNGLE_STAIRS,
            Items.ACACIA_STAIRS,
            Items.DARK_OAK_STAIRS,
            Items.MANGROVE_STAIRS,
            Items.CHERRY_STAIRS,
            Items.BAMBOO_STAIRS,
            Items.CRIMSON_STAIRS,
            Items.WARPED_STAIRS,
            Items.OAK_FENCE,
            Items.SPRUCE_FENCE,
            Items.BIRCH_FENCE,
            Items.JUNGLE_FENCE,
            Items.ACACIA_FENCE,
            Items.DARK_OAK_FENCE,
            Items.MANGROVE_FENCE,
            Items.CHERRY_FENCE,
            Items.BAMBOO_FENCE,
            Items.CRIMSON_FENCE,
            Items.WARPED_FENCE,
            Items.OAK_FENCE_GATE,
            Items.SPRUCE_FENCE_GATE,
            Items.BIRCH_FENCE_GATE,
            Items.JUNGLE_FENCE_GATE,
            Items.ACACIA_FENCE_GATE,
            Items.DARK_OAK_FENCE_GATE,
            Items.MANGROVE_FENCE_GATE,
            Items.CHERRY_FENCE_GATE,
            Items.BAMBOO_FENCE_GATE,
            Items.CRIMSON_FENCE_GATE,
            Items.WARPED_FENCE_GATE,
            Items.OAK_DOOR,
            Items.SPRUCE_DOOR,
            Items.BIRCH_DOOR,
            Items.JUNGLE_DOOR,
            Items.ACACIA_DOOR,
            Items.DARK_OAK_DOOR,
            Items.MANGROVE_DOOR,
            Items.CHERRY_DOOR,
            Items.BAMBOO_DOOR,
            Items.CRIMSON_DOOR,
            Items.WARPED_DOOR,
            Items.OAK_TRAPDOOR,
            Items.SPRUCE_TRAPDOOR,
            Items.BIRCH_TRAPDOOR,
            Items.JUNGLE_TRAPDOOR,
            Items.ACACIA_TRAPDOOR,
            Items.DARK_OAK_TRAPDOOR,
            Items.MANGROVE_TRAPDOOR,
            Items.CHERRY_TRAPDOOR,
            Items.BAMBOO_TRAPDOOR,
            Items.CRIMSON_TRAPDOOR,
            Items.WARPED_TRAPDOOR,
            Items.OAK_PRESSURE_PLATE,
            Items.SPRUCE_PRESSURE_PLATE,
            Items.BIRCH_PRESSURE_PLATE,
            Items.JUNGLE_PRESSURE_PLATE,
            Items.ACACIA_PRESSURE_PLATE,
            Items.DARK_OAK_PRESSURE_PLATE,
            Items.MANGROVE_PRESSURE_PLATE,
            Items.CHERRY_PRESSURE_PLATE,
            Items.BAMBOO_PRESSURE_PLATE,
            Items.CRIMSON_PRESSURE_PLATE,
            Items.WARPED_PRESSURE_PLATE,
            Items.OAK_BUTTON,
            Items.SPRUCE_BUTTON,
            Items.BIRCH_BUTTON,
            Items.JUNGLE_BUTTON,
            Items.ACACIA_BUTTON,
            Items.DARK_OAK_BUTTON,
            Items.MANGROVE_BUTTON,
            Items.CHERRY_BUTTON,
            Items.BAMBOO_BUTTON,
            Items.CRIMSON_BUTTON,
            Items.WARPED_BUTTON,
            Items.STICK,
            Items.CRAFTING_TABLE,
            Items.CHEST,
            Items.BARREL,
            Items.LECTERN,
            Items.COMPOSTER,
            Items.FLETCHING_TABLE,
            Items.LOOM,
            Items.SMITHING_TABLE,
            Items.CARTOGRAPHY_TABLE,
            Items.BOOKSHELF,
            Items.BOWL,
            Items.WOODEN_PICKAXE,
            Items.WOODEN_AXE,
            Items.WOODEN_SHOVEL,
            Items.WOODEN_HOE,
            Items.WOODEN_SWORD,
            Items.OAK_SIGN,
            Items.SPRUCE_SIGN,
            Items.BIRCH_SIGN,
            Items.JUNGLE_SIGN,
            Items.ACACIA_SIGN,
            Items.DARK_OAK_SIGN,
            Items.MANGROVE_SIGN,
            Items.CHERRY_SIGN,
            Items.BAMBOO_SIGN,
            Items.CRIMSON_SIGN,
            Items.WARPED_SIGN,
            Items.OAK_HANGING_SIGN,
            Items.SPRUCE_HANGING_SIGN,
            Items.BIRCH_HANGING_SIGN,
            Items.JUNGLE_HANGING_SIGN,
            Items.ACACIA_HANGING_SIGN,
            Items.DARK_OAK_HANGING_SIGN,
            Items.MANGROVE_HANGING_SIGN,
            Items.CHERRY_HANGING_SIGN,
            Items.BAMBOO_HANGING_SIGN,
            Items.CRIMSON_HANGING_SIGN,
            Items.WARPED_HANGING_SIGN,
            Items.OAK_BOAT,
            Items.SPRUCE_BOAT,
            Items.BIRCH_BOAT,
            Items.JUNGLE_BOAT,
            Items.ACACIA_BOAT,
            Items.DARK_OAK_BOAT,
            Items.MANGROVE_BOAT,
            Items.CHERRY_BOAT,
            Items.BAMBOO_RAFT,
            Items.OAK_CHEST_BOAT,
            Items.SPRUCE_CHEST_BOAT,
            Items.BIRCH_CHEST_BOAT,
            Items.JUNGLE_CHEST_BOAT,
            Items.ACACIA_CHEST_BOAT,
            Items.DARK_OAK_CHEST_BOAT,
            Items.MANGROVE_CHEST_BOAT,
            Items.CHERRY_CHEST_BOAT,
            Items.BAMBOO_CHEST_RAFT
         )
      );
      this.clearUpMappings.put(Items.BIRCH_PLANKS, new ClearUpMapping(Items.BIRCH_PLANKS, woodItems, true));
      this.clearUpMappings
         .put(
            Items.IRON_INGOT,
            new ClearUpMapping(
               Items.IRON_INGOT,
               List.of(
                  Items.RAW_IRON,
                  Items.IRON_NUGGET,
                  Items.IRON_INGOT,
                  Items.IRON_BLOCK,
                  Items.RAW_IRON_BLOCK,
                  Items.RAW_COPPER,
                  Items.COPPER_INGOT,
                  Items.COPPER_BLOCK,
                  Items.RAW_COPPER_BLOCK,
                  Items.RAW_GOLD,
                  Items.GOLD_NUGGET,
                  Items.GOLD_INGOT,
                  Items.GOLD_BLOCK,
                  Items.RAW_GOLD_BLOCK,
                  Items.COAL,
                  Items.CHARCOAL,
                  Items.COAL_BLOCK,
                  Items.REDSTONE,
                  Items.REDSTONE_BLOCK,
                  Items.LAPIS_LAZULI,
                  Items.LAPIS_BLOCK,
                  Items.DIAMOND,
                  Items.DIAMOND_BLOCK,
                  Items.EMERALD,
                  Items.EMERALD_BLOCK,
                  Items.QUARTZ,
                  Items.QUARTZ_BLOCK,
                  Items.AMETHYST_SHARD,
                  Items.AMETHYST_BLOCK,
                  Items.ANCIENT_DEBRIS,
                  Items.NETHERITE_SCRAP,
                  Items.NETHERITE_INGOT,
                  Items.NETHERITE_BLOCK
               ),
               true
            )
         );
      this.clearUpMappings
         .put(
            Items.WHEAT_SEEDS,
            new ClearUpMapping(
               Items.WHEAT_SEEDS,
               List.of(
                  Items.WHEAT,
                  Items.WHEAT_SEEDS,
                  Items.CARROT,
                  Items.POTATO,
                  Items.BEETROOT,
                  Items.BEETROOT_SEEDS,
                  Items.PUMPKIN_SEEDS,
                  Items.MELON_SEEDS,
                  Items.TORCHFLOWER_SEEDS,
                  Items.PITCHER_PLANT,
                  Items.PITCHER_POD,
                  Items.BAMBOO,
                  Items.SUGAR_CANE,
                  Items.CACTUS,
                  Items.VINE,
                  Items.LILY_PAD,
                  Items.KELP,
                  Items.SEAGRASS,
                  Items.SEA_PICKLE,
                  Items.CORNFLOWER,
                  Items.POPPY,
                  Items.DANDELION,
                  Items.BLUE_ORCHID,
                  Items.ALLIUM,
                  Items.AZURE_BLUET,
                  Items.OXEYE_DAISY,
                  Items.LILY_OF_THE_VALLEY,
                  Items.SUNFLOWER,
                  Items.LILAC,
                  Items.ROSE_BUSH,
                  Items.PEONY,
                  Items.TALL_GRASS,
                  Items.FERN,
                  Items.LARGE_FERN,
                  Items.DEAD_BUSH,
                  Items.GLOW_LICHEN,
                  Items.HANGING_ROOTS,
                  Items.OAK_SAPLING,
                  Items.SPRUCE_SAPLING,
                  Items.BIRCH_SAPLING,
                  Items.JUNGLE_SAPLING,
                  Items.ACACIA_SAPLING,
                  Items.DARK_OAK_SAPLING,
                  Items.MANGROVE_PROPAGULE,
                  Items.CHERRY_SAPLING,
                  Items.AZALEA,
                  Items.FLOWERING_AZALEA,
                  Items.BROWN_MUSHROOM,
                  Items.RED_MUSHROOM,
                  Items.CRIMSON_FUNGUS,
                  Items.WARPED_FUNGUS
               ),
               true
            )
         );
      this.clearUpMappings
         .put(
            Items.COBBLESTONE,
            new ClearUpMapping(
               Items.COBBLESTONE,
               List.of(
                  Items.COBBLESTONE,
                  Items.MOSSY_COBBLESTONE,
                  Items.COBBLESTONE_SLAB,
                  Items.COBBLESTONE_STAIRS,
                  Items.COBBLESTONE_WALL,
                  Items.STONE,
                  Items.SMOOTH_STONE,
                  Items.STONE_SLAB,
                  Items.SMOOTH_STONE_SLAB,
                  Items.STONE_BRICKS,
                  Items.MOSSY_STONE_BRICKS,
                  Items.CRACKED_STONE_BRICKS,
                  Items.CHISELED_STONE_BRICKS,
                  Items.STONE_BRICK_SLAB,
                  Items.STONE_BRICK_STAIRS,
                  Items.STONE_BRICK_WALL,
                  Items.ANDESITE,
                  Items.POLISHED_ANDESITE,
                  Items.ANDESITE_SLAB,
                  Items.ANDESITE_STAIRS,
                  Items.ANDESITE_WALL,
                  Items.DIORITE,
                  Items.POLISHED_DIORITE,
                  Items.DIORITE_SLAB,
                  Items.DIORITE_STAIRS,
                  Items.DIORITE_WALL,
                  Items.GRANITE,
                  Items.POLISHED_GRANITE,
                  Items.GRANITE_SLAB,
                  Items.GRANITE_STAIRS,
                  Items.GRANITE_WALL,
                  Items.COBBLED_DEEPSLATE,
                  Items.COBBLED_DEEPSLATE_SLAB,
                  Items.COBBLED_DEEPSLATE_STAIRS,
                  Items.COBBLED_DEEPSLATE_WALL,
                  Items.DEEPSLATE,
                  Items.POLISHED_DEEPSLATE,
                  Items.DEEPSLATE_BRICKS,
                  Items.CRACKED_DEEPSLATE_BRICKS,
                  Items.DEEPSLATE_TILES,
                  Items.CRACKED_DEEPSLATE_TILES,
                  Items.CHISELED_DEEPSLATE,
                  Items.REINFORCED_DEEPSLATE,
                  Items.TUFF,
                  Items.POLISHED_TUFF,
                  Items.TUFF_BRICKS,
                  Items.CHISELED_TUFF,
                  Items.SMOOTH_BASALT,
                  Items.BASALT,
                  Items.POLISHED_BASALT,
                  Items.BLACKSTONE,
                  Items.POLISHED_BLACKSTONE,
                  Items.BLACKSTONE_SLAB,
                  Items.BLACKSTONE_STAIRS,
                  Items.BLACKSTONE_WALL,
                  Items.POLISHED_BLACKSTONE_BRICKS,
                  Items.CRACKED_POLISHED_BLACKSTONE_BRICKS,
                  Items.END_STONE,
                  Items.END_STONE_BRICKS,
                  Items.SANDSTONE,
                  Items.RED_SANDSTONE,
                  Items.CHISELED_SANDSTONE,
                  Items.CUT_SANDSTONE,
                  Items.SMOOTH_SANDSTONE,
                  Items.CHISELED_RED_SANDSTONE,
                  Items.CUT_RED_SANDSTONE,
                  Items.SMOOTH_RED_SANDSTONE
               ),
               true
            )
         );
      this.clearUpMappings
         .put(
            Items.GLASS,
            new ClearUpMapping(
               Items.GLASS,
               List.of(
                  Items.GLASS,
                  Items.TINTED_GLASS,
                  Items.WHITE_STAINED_GLASS,
                  Items.ORANGE_STAINED_GLASS,
                  Items.MAGENTA_STAINED_GLASS,
                  Items.LIGHT_BLUE_STAINED_GLASS,
                  Items.YELLOW_STAINED_GLASS,
                  Items.LIME_STAINED_GLASS,
                  Items.PINK_STAINED_GLASS,
                  Items.GRAY_STAINED_GLASS,
                  Items.LIGHT_GRAY_STAINED_GLASS,
                  Items.CYAN_STAINED_GLASS,
                  Items.PURPLE_STAINED_GLASS,
                  Items.BLUE_STAINED_GLASS,
                  Items.BROWN_STAINED_GLASS,
                  Items.GREEN_STAINED_GLASS,
                  Items.RED_STAINED_GLASS,
                  Items.BLACK_STAINED_GLASS,
                  Items.GLASS_PANE,
                  Items.WHITE_STAINED_GLASS_PANE,
                  Items.ORANGE_STAINED_GLASS_PANE,
                  Items.MAGENTA_STAINED_GLASS_PANE,
                  Items.LIGHT_BLUE_STAINED_GLASS_PANE,
                  Items.YELLOW_STAINED_GLASS_PANE,
                  Items.LIME_STAINED_GLASS_PANE,
                  Items.PINK_STAINED_GLASS_PANE,
                  Items.GRAY_STAINED_GLASS_PANE,
                  Items.LIGHT_GRAY_STAINED_GLASS_PANE,
                  Items.CYAN_STAINED_GLASS_PANE,
                  Items.PURPLE_STAINED_GLASS_PANE,
                  Items.BLUE_STAINED_GLASS_PANE,
                  Items.BROWN_STAINED_GLASS_PANE,
                  Items.GREEN_STAINED_GLASS_PANE,
                  Items.RED_STAINED_GLASS_PANE,
                  Items.BLACK_STAINED_GLASS_PANE,
                  Items.SAND,
                  Items.RED_SAND
               ),
               true
            )
         );
      this.clearUpMappings
         .put(
            Items.TORCH,
            new ClearUpMapping(
               Items.TORCH,
               List.of(
                  Items.TORCH,
                  Items.SOUL_TORCH,
                  Items.REDSTONE_TORCH,
                  Items.LANTERN,
                  Items.SOUL_LANTERN,
                  Items.GLOWSTONE,
                  Items.SEA_LANTERN,
                  Items.JACK_O_LANTERN,
                  Items.CAMPFIRE,
                  Items.SOUL_CAMPFIRE,
                  Items.CANDLE,
                  Items.WHITE_CANDLE,
                  Items.ORANGE_CANDLE,
                  Items.MAGENTA_CANDLE,
                  Items.LIGHT_BLUE_CANDLE,
                  Items.YELLOW_CANDLE,
                  Items.LIME_CANDLE,
                  Items.PINK_CANDLE,
                  Items.GRAY_CANDLE,
                  Items.LIGHT_GRAY_CANDLE,
                  Items.CYAN_CANDLE,
                  Items.PURPLE_CANDLE,
                  Items.BLUE_CANDLE,
                  Items.BROWN_CANDLE,
                  Items.GREEN_CANDLE,
                  Items.RED_CANDLE,
                  Items.BLACK_CANDLE,
                  Items.END_ROD,
                  Items.CRYING_OBSIDIAN,
                  Items.OCHRE_FROGLIGHT,
                  Items.VERDANT_FROGLIGHT,
                  Items.PEARLESCENT_FROGLIGHT,
                  Items.GLOW_LICHEN,
                  Items.SHROOMLIGHT
               ),
               true
            )
         );
      this.clearUpMappings
         .put(
            Items.BONE,
            new ClearUpMapping(
               Items.BONE,
               List.of(
                  Items.BONE,
                  Items.BONE_MEAL,
                  Items.ROTTEN_FLESH,
                  Items.GUNPOWDER,
                  Items.STRING,
                  Items.SPIDER_EYE,
                  Items.ENDER_PEARL,
                  Items.BLAZE_ROD,
                  Items.GHAST_TEAR,
                  Items.SLIME_BALL,
                  Items.MAGMA_CREAM,
                  Items.LEATHER,
                  Items.FEATHER,
                  Items.ARROW,
                  Items.SPECTRAL_ARROW,
                  Items.TIPPED_ARROW,
                  Items.INK_SAC,
                  Items.GLOW_INK_SAC,
                  Items.PRISMARINE_SHARD,
                  Items.PRISMARINE_CRYSTALS,
                  Items.NETHER_STAR,
                  Items.WITHER_SKELETON_SKULL,
                  Items.SHULKER_SHELL,
                  Items.PHANTOM_MEMBRANE,
                  Items.RABBIT_HIDE,
                  Items.RABBIT_FOOT,
                  Items.TURTLE_SCUTE,
                  Items.ARMADILLO_SCUTE,
                  Items.WOLF_ARMOR,
                  Items.HEAVY_CORE,
                  Items.ENDER_EYE,
                  Items.FERMENTED_SPIDER_EYE,
                  Items.BREEZE_ROD,
                  Items.WIND_CHARGE
               ),
               true
            )
         );
   }

   public NbtCompound toTag() {
      NbtCompound tag = super.toTag();
      NbtList list = new NbtList();

      for (ClearUpMapping mapping : this.clearUpMappings.values()) {
         list.add(mapping.toTag());
      }

      tag.put("clearUpMappings", list);
      return tag;
   }

   public Module fromTag(NbtCompound tag) {
      super.fromTag(tag);
      if (tag.contains("clearUpMappings")) {
         NbtList list = tag.getListOrEmpty("clearUpMappings");
         this.clearUpMappings.clear();

         for (NbtElement e : list) {
            if (e.getType() == 10) {
               ClearUpMapping mapping = new ClearUpMapping().fromTag((NbtCompound)e);
               this.clearUpMappings.put(mapping.getTargetItem(), mapping);
            }
         }
      }

      return this;
   }

   public WWidget getWidget(GuiTheme theme) {
      WVerticalList list = theme.verticalList();
      this.fillWidget(theme, list);
      return list;
   }

   private void fillWidget(GuiTheme theme, WVerticalList list) {
      WSection section = (WSection)list.add(theme.section("散装物品分类 (" + this.clearUpMappings.size() + ")")).expandX().widget();
      WTable table = (WTable)section.add(theme.table()).expandX().widget();

      for (ClearUpMapping mapping : new ArrayList<>(this.clearUpMappings.values())) {
         Item oldTarget = mapping.getTargetItem();
         ItemStack targetStack = mapping.getTargetItem().getDefaultStack();
         table.add(theme.item(targetStack));
         WButton changeTarget = (WButton)table.add(theme.button(Names.get(mapping.getTargetItem()))).expandX().widget();
         changeTarget.action = () -> {
            ItemSetting tempSetting = ((meteordevelopment.meteorclient.settings.ItemSetting.Builder)((meteordevelopment.meteorclient.settings.ItemSetting.Builder)((meteordevelopment.meteorclient.settings.ItemSetting.Builder)((meteordevelopment.meteorclient.settings.ItemSetting.Builder)new meteordevelopment.meteorclient.settings.ItemSetting.Builder()
                           .name("target-item"))
                        .description(""))
                     .defaultValue(mapping.getTargetItem()))
                  .onChanged(item -> {
                     this.clearUpMappings.remove(oldTarget);
                     mapping.setTargetItem(item);
                     this.clearUpMappings.put(item, mapping);
                     list.clear();
                     this.fillWidget(theme, list);
                  }))
               .build();
            this.mc.setScreen(new ItemSettingScreen(theme, tempSetting));
         };
         String relatedText = mapping.getRelatedItems().isEmpty() ? "空" : mapping.getRelatedItems().size() + " 个物品";
         WButton editRelated = (WButton)table.add(theme.button(relatedText)).expandX().widget();
         editRelated.action = () -> {
            ItemListSetting tempSetting = ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder()
                        .name("related-items"))
                     .description(""))
                  .defaultValue(mapping.getRelatedItems().toArray(new Item[0]))
                  .onChanged(items -> mapping.setRelatedItems(new HashSet<>(items))))
               .build();
            ItemListSettingScreen screen = new ItemListSettingScreen(theme, tempSetting);
            screen.onClosed(() -> {
               list.clear();
               this.fillWidget(theme, list);
            });
            this.mc.setScreen(screen);
         };
         WCheckbox enabled = (WCheckbox)table.add(theme.checkbox(mapping.isEnabled())).widget();
         enabled.action = () -> mapping.setEnabled(enabled.checked);
         WMinus del = (WMinus)table.add(theme.minus()).widget();
         del.action = () -> {
            this.clearUpMappings.remove(mapping.getTargetItem());
            list.clear();
            this.fillWidget(theme, list);
         };
         table.row();
      }

      WTable controls = (WTable)list.add(theme.table()).expandX().widget();
      WButton add = (WButton)controls.add(theme.button("新增分类")).expandX().widget();
      add.action = () -> {
         this.clearUpMappings.put(Items.AIR, new ClearUpMapping());
         list.clear();
         this.fillWidget(theme, list);
      };
      WButton resetDefaults = (WButton)controls.add(theme.button("恢复默认")).expandX().widget();
      resetDefaults.action = () -> {
         this.clearUpMappings.clear();
         this.initDefaultMappings();
         list.clear();
         this.fillWidget(theme, list);
      };
      controls.row();
   }

   private void init(Boolean enabled) {
      if (Boolean.TRUE.equals(enabled)) {
         this.initBtn.set(false);
         if (this.mc.player != null && this.mc.world != null) {
            this.clear();
            boolean scanPositionsSuccess = this.scanPositions();
            if (!scanPositionsSuccess) {
               this.clear();
            } else {
               for (Item item : this.needItems.get()) {
                  if (this.itemPosMap.containsKey(item)) {
                     this.needPosMap.put(item, this.itemPosMap.get(item));
                  }
               }

               for (ClearUpMapping mapping : this.clearUpMappings.values()) {
                  if (mapping.isEnabled()) {
                     Item targetItem = mapping.getTargetItem();
                     if (this.itemPosMap.containsKey(targetItem)) {
                        this.multiItemPosMap.put(targetItem, this.itemPosMap.get(targetItem));

                        for (Item relatedItem : mapping.getRelatedItems()) {
                           this.relatedToTargetMap.put(relatedItem, targetItem);
                        }
                     }
                  }
               }

               if (this.needPosMap.isEmpty() && this.multiItemPosMap.isEmpty()) {
                  this.warning("未检测到需要的<物品>位置>", new Object[0]);
                  this.clear();
               } else {
                  String itemNames = this.needPosMap.keySet().stream().<CharSequence>map(Names::get).collect(Collectors.joining(","));
                  if (!itemNames.isEmpty()) {
                     this.info("成功识别打包物品: " + itemNames, new Object[0]);
                  }

                  String multiItemNames = this.multiItemPosMap.keySet().stream().<CharSequence>map(Names::get).collect(Collectors.joining(","));
                  if (!multiItemNames.isEmpty()) {
                     this.info("成功识别多物品分类: " + multiItemNames, new Object[0]);
                  }

                  this.info("初始化完毕！", new Object[0]);
               }
            }
         } else {
            this.warning("玩家或世界未加载", new Object[0]);
         }
      }
   }

   @Override
   public void onActivate() {
      super.onActivate();
      if (this.takeKitPos != null && this.emptyKitPos != null && (!this.needPosMap.isEmpty() || !this.multiItemPosMap.isEmpty())) {
         this.nextStep();
      } else {
         this.warning("请先进行初始化", new Object[0]);
         this.toggle();
      }
   }

   private void nextStep() {
      ItemStack nextKit = this.nextPlayerStack(itemStackx -> HeItemUtils.isShulkerBox(itemStackx.getItem()));
      if (nextKit.isEmpty()) {
         List<ItemEntity> shulkerEntities = this.mc
            .world
            .getEntitiesByClass(ItemEntity.class, this.mc.player.getBoundingBox().expand(5.0), e -> HeItemUtils.isShulkerBox(e.getStack().getItem()));
         if (!shulkerEntities.isEmpty()) {
            BlockPos targetPos = shulkerEntities.getFirst().getBlockPos();
            if (targetPos.getY() != this.mc.player.getBlockY()) {
               BlockPos testPos = new BlockPos(targetPos.getX(), this.mc.player.getBlockY(), targetPos.getZ());
               BlockPos canStandPos = HeBlockUtils.getCanStandPos(testPos, 1);
               if (canStandPos != null) {
                  targetPos = canStandPos;
               }
            }

            this.gotoTargetIfNeed(targetPos, 0, Steps.NEXT, "捡kit");
         } else {
            this.step = Steps.PUT_ITEM;
         }
      } else {
         this.putKitIndex = this.getCurPlayerSlot();
         ShulkerBoxReader reader = new ShulkerBoxReader(nextKit);
         if (reader.isEmpty()) {
            this.curOperationPos = this.emptyKitPos;
            this.gotoTargetIfNeed(this.curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放空盒");
         } else {
            if (reader.isSameItem()) {
               Item firstItem = reader.getFirstItem();
               if (this.itemPosMap.containsKey(firstItem)) {
                  this.curOperationPos = this.itemPosMap.get(firstItem);
                  this.gotoTargetIfNeed(this.curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放满盒");
                  return;
               }
            }

            List<ItemStack> condensed = reader.getCondensed();
            boolean need = false;

            for (ItemStack itemStack : condensed) {
               Item item = itemStack.getItem();
               if (this.needPosMap.containsKey(item) || this.relatedToTargetMap.containsKey(item)) {
                  need = true;
                  break;
               }
            }

            if (need) {
               this.curOperationPos = this.takeKitPos;
               this.gotoTargetIfNeed(this.curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放满盒");
               return;
            }

            this.curOperationPos = this.miscKitPos;
            this.gotoTargetIfNeed(this.curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放杂盒");
         }
      }
   }

   private void putKit() {
      if (this.notInOperationRange(this.curOperationPos)) {
         this.gotoBtnPos(this.curOperationPos, "<存kit>距离不够，尝试移动", Steps.PUT_KIT);
      } else {
         ItemStack kitItemStack = this.getItemStack(this.putKitIndex);
         if (!HeItemUtils.isShulkerBox(kitItemStack.getItem())) {
            this.warning("放kit, 但身上没有...", new Object[0]);
            this.step = Steps.NEXT;
         } else {
            this.openChest(this.curOperationPos.getPutPos(), inventory -> {
               FindItemResult emptyResult = InvUtils.findEmpty();
               if (emptyResult.found()) {
                  InvUtils.shiftClick().slot(this.putKitIndex);
                  this.delayCloseNext(Steps.NEXT);
               } else {
                  this.breakStep("<存kit>箱满，无法存放");
               }
            });
         }
      }
   }

   private void takeEmptyKit() {
      if (this.notInOperationRange(this.emptyKitPos)) {
         this.gotoBtnPos(this.emptyKitPos, "<拿空盒>距离不够, 尝试移动", Steps.TAKE_EMPTY_KIT);
      } else {
         this.openChest(this.emptyKitPos.getTakePos(), inventory -> {
            for (int slot = 0; slot < inventory.size(); slot++) {
               ItemStack kitItemStack = inventory.getStack(slot);
               if (HeItemUtils.isShulkerBox(kitItemStack.getItem())) {
                  ShulkerBoxReader reader = new ShulkerBoxReader(kitItemStack);
                  if (reader.isEmpty()) {
                     InvUtils.shiftClick().slotId(slot);
                     this.delayCloseNext(Steps.PLACE_EMPTY_KIT);
                     return;
                  }
               }
            }

            this.info("<空盒箱>缺少盒子", new Object[0]);
            this.toggle();
         });
      }
   }

   private void placeEmptyKit() {
      BlockState emptyKitState = this.mc.world.getBlockState(this.curOperationPos.getKitPos());
      if (emptyKitState.getBlock() instanceof ShulkerBoxBlock) {
         this.step = Steps.PUT_ITEM;
      } else if (!emptyKitState.isAir()) {
         this.breakStep("<位置>被占用: " + Names.get(this.curOperationPos.getItem().getItem()));
      } else {
         FindItemResult findItemResult = HeInvUtils.findShulkerBox(Items.AIR);
         if (!findItemResult.found()) {
            this.step = Steps.TAKE_EMPTY_KIT;
         } else if (!findItemResult.isMainHand()) {
            HeInvUtils.swap(findItemResult.slot(), this.getMainSlot());
            this.setDelay();
         } else if (this.notInOperationRange(this.curOperationPos)) {
            this.gotoTarget(this.curOperationPos.getBtnPos(), 0, Steps.PLACE_EMPTY_KIT);
         } else {
            this.info("放置空盒", new Object[0]);
            HeBlockUtils.place(this.curOperationPos.getKitPos(), this.getMainSlot(), true, Direction.DOWN);
            this.delayNext(Steps.PUT_ITEM);
         }
      }
   }

   private void takeKit() {
      if (this.notInOperationRange(this.takeKitPos)) {
         this.gotoBtnPos(this.takeKitPos, "<拿kit>距离不够，尝试移动", Steps.TAKE_KIT);
      } else {
         this.openChest(this.takeKitPos.getTakePos(), inventory -> {
            ItemStack nextScreenStack = this.nextScreenStack(itemStack -> {
               if (HeItemUtils.isShulkerBox(itemStack.getItem())) {
                  ShulkerBoxReader reader = new ShulkerBoxReader(itemStack);

                  for (ItemStack stack : reader.getCondensed()) {
                     Item item = stack.getItem();
                     if (this.needPosMap.containsKey(item) || this.relatedToTargetMap.containsKey(item)) {
                        return true;
                     }
                  }
               }

               return false;
            });
            if (nextScreenStack.isEmpty()) {
               this.breakStep("分类完毕...");
            } else {
               this.info("拿取kit", new Object[0]);
               InvUtils.shiftClick().slotId(this.getCurScreenSlot());
               this.delayCloseNext(Steps.PLACE_KIT);
            }
         });
      }
   }

   private void placeKit() {
      BlockState finishedKitBlockState = this.mc.world.getBlockState(this.takeKitPos.getKitPos());
      if (!finishedKitBlockState.isAir()) {
         if (finishedKitBlockState.getBlock() instanceof ShulkerBoxBlock) {
            this.step = Steps.TAKE_ITEM;
         } else {
            this.breakStep("<待分类位置>被占用");
         }
      } else {
         ItemStack nextItemStack = this.nextPlayerStack(itemStack -> HeItemUtils.isShulkerBox(itemStack.getItem()));
         if (nextItemStack.isEmpty()) {
            this.step = Steps.TAKE_KIT;
         } else if (this.notInOperationRange(this.takeKitPos)) {
            this.gotoBtnPos(this.takeKitPos, "去拿kit", Steps.PLACE_KIT);
         } else if (!this.swapToMainHand(this.getCurPlayerSlot())) {
            HeBlockUtils.place(this.takeKitPos.getKitPos(), this.getMainSlot(), true, Direction.DOWN);
            this.delayNext(Steps.TAKE_ITEM);
         }
      }
   }

   private void takeLooseItem() {
      if (this.takeQty >= (Integer)this.qty.get()) {
         this.takeQty = 0;
         this.step = Steps.PUT_ITEM;
      } else if (this.notInOperationRange(this.takeKitPos)) {
         this.gotoTarget(this.takeKitPos.getBtnPos(), 0, Steps.TAKE_LOOSE_ITEM);
      } else {
         this.openChest(this.takeKitPos.getTakePos(), inventory -> {
            ItemStack nextLooseItem = this.nextScreenStack(itemStack -> {
               Item item = itemStack.getItem();
               return this.needPosMap.containsKey(item) || this.relatedToTargetMap.containsKey(item);
            });
            if (!nextLooseItem.isEmpty()) {
               this.info("拿取散装物品: " + Names.get(nextLooseItem), new Object[0]);
               InvUtils.shiftClick().slotId(this.getCurScreenSlot());
               this.takeQty++;
               if (this.takeQty >= (Integer)this.qty.get()) {
                  this.delayCloseNext(Steps.PUT_ITEM);
               } else {
                  this.setDelay();
               }
            } else {
               this.delayCloseNext(Steps.TAKE_ITEM);
            }
         });
      }
   }

   private void takeItem() {
      if (this.takeQty >= (Integer)this.qty.get()) {
         this.step = Steps.PUT_ITEM;
      } else {
         BlockState takeKitPosState = this.mc.world.getBlockState(this.takeKitPos.getKitPos());
         if (takeKitPosState.isAir()) {
            this.step = Steps.PLACE_KIT;
         } else if (!(takeKitPosState.getBlock() instanceof ShulkerBoxBlock)) {
            this.breakStep("<kit位置>被占用");
         } else if (this.notInOperationRange(this.takeKitPos)) {
            this.gotoTarget(this.takeKitPos.getBtnPos(), 0, Steps.TAKE_ITEM);
         } else {
            this.openKit(this.takeKitPos.getKitPos(), screenHandler -> {
               ItemStack nextItemStack = this.nextScreenStack(itemStack -> {
                  Item item = itemStack.getItem();
                  return this.needPosMap.containsKey(item) || this.relatedToTargetMap.containsKey(item);
               });
               if (nextItemStack.isEmpty()) {
                  this.info("待分类kit已空，挖掉重放", new Object[0]);
                  this.curOperationPos = this.takeKitPos;
                  this.delayCloseNext(Steps.BREAK_KIT);
               } else {
                  this.info("拿取物品: " + Names.get(nextItemStack), new Object[0]);
                  InvUtils.shiftClick().slotId(this.getCurScreenSlot());
                  this.takeQty++;
                  if (this.takeQty >= (Integer)this.qty.get()) {
                     this.delayCloseNext(Steps.PUT_ITEM);
                  } else {
                     this.setDelay();
                  }
               }
            });
         }
      }
   }

   private void putItem() {
      Set<Item> needItemSet = new HashSet<>();

      for (int i = 0; i < 36; i++) {
         Item item = this.getItemStack(i).getItem();
         if (this.needPosMap.containsKey(item)) {
            needItemSet.add(item);
         }
      }

      if (!needItemSet.isEmpty()) {
         this.putKitItem(needItemSet);
      } else {
         Set<Item> multiItemSet = new HashSet<>();

         for (int i = 0; i < 36; i++) {
            Item item = this.getItemStack(i).getItem();
            if (this.relatedToTargetMap.containsKey(item)) {
               multiItemSet.add(item);
            }
         }

         if (!multiItemSet.isEmpty()) {
            this.putMultiItem(multiItemSet);
         } else {
            this.takeQty = 0;
            this.delayCloseNext(this.multiItemPosMap.isEmpty() ? Steps.TAKE_ITEM : Steps.TAKE_LOOSE_ITEM);
         }
      }
   }

   private void putKitItem(Set<Item> needItemSet) {
      Vec3d playerPos = this.mc.player.getEntityPos();
      double minDistance = Double.MAX_VALUE;
      Item putItem = Items.AIR;

      for (Item item : needItemSet) {
         StoragePos storagePos = this.needPosMap.get(item);
         double newDistance = playerPos.distanceTo(storagePos.getBtnPos().toCenterPos());
         if (newDistance < minDistance) {
            minDistance = newDistance;
            putItem = item;
         }
      }

      StoragePos storagePos = this.needPosMap.get(putItem);
      BlockState putItemKitPos = this.mc.world.getBlockState(storagePos.getKitPos());
      if (putItemKitPos.isAir()) {
         this.curOperationPos = storagePos;
         this.closeScreen();
         this.step = Steps.PLACE_EMPTY_KIT;
      } else if (!(putItemKitPos.getBlock() instanceof ShulkerBoxBlock)) {
         this.closeScreen();
         this.breakStep("<" + Names.get(putItem) + "位置>被占用");
      } else if (this.notInOperationRange(storagePos)) {
         this.closeScreen();
         this.gotoBtnPos(storagePos, "放<" + Names.get(putItem) + ">距离不够, 尝试移动", Steps.PUT_ITEM);
      } else {
         this.curOperationPos = storagePos;
         Item finalPutItem = putItem;
         this.openKit(this.curOperationPos.getKitPos(), screenHandler -> {
            if (this.hasScreenFull()) {
               this.info("kit已满，挖掉存放", new Object[0]);
               this.delayCloseNext(Steps.BREAK_KIT);
            } else {
               this.nextPlayerStack(itemStack -> itemStack.getItem() == finalPutItem);
               InvUtils.shiftClick().slot(this.getCurPlayerSlot());
               if (this.hasScreenFull()) {
                  this.info("kit已满，挖掉存放", new Object[0]);
                  this.delayCloseNext(Steps.BREAK_KIT);
               } else {
                  this.setDelay();
               }
            }
         });
      }
   }

   private void putMultiItem(Set<Item> multiItemSet) {
      Vec3d playerPos = this.mc.player.getEntityPos();
      double minDistance = Double.MAX_VALUE;
      Item putItem = Items.AIR;
      StoragePos targetPos = null;

      for (Item item : multiItemSet) {
         Item targetItem = this.relatedToTargetMap.get(item);
         StoragePos storagePos = this.multiItemPosMap.get(targetItem);
         if (storagePos != null) {
            double newDistance = playerPos.distanceTo(storagePos.getBtnPos().toCenterPos());
            if (newDistance < minDistance) {
               minDistance = newDistance;
               putItem = item;
               targetPos = storagePos;
            }
         }
      }

      if (targetPos == null) {
         this.delayCloseNext(Steps.PUT_ITEM);
      } else {
         this.curOperationPos = targetPos;
         Item finalPutItem = putItem;
         if (this.notInOperationRange(targetPos)) {
            this.closeScreen();
            this.gotoBtnPos(targetPos, "放<" + Names.get(finalPutItem) + ">距离不够, 尝试移动", Steps.PUT_ITEM);
         } else {
            BlockState putPosState = this.mc.world.getBlockState(targetPos.getPutPos());
            if (putPosState.getBlock() != Blocks.CHEST) {
               this.closeScreen();
               this.breakStep("<" + Names.get(finalPutItem) + "位置>箱子不存在");
            } else {
               this.openChest(targetPos.getPutPos(), inventory -> {
                  if (this.isContainerFull()) {
                     this.breakStep("<" + Names.get(finalPutItem) + ">箱子已满");
                  } else {
                     this.nextPlayerStack(itemStack -> itemStack.getItem() == finalPutItem);
                     InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                     this.setDelay();
                  }
               });
            }
         }
      }
   }

   private void breakKit() {
      if (this.notInOperationRange(this.curOperationPos)) {
         this.gotoBtnPos(this.curOperationPos, "<挖盒子>距离不够, 尝试移动", Steps.BREAK_KIT);
      } else {
         Vec3d hitPos = Vec3d.ofCenter(this.curOperationPos.getBtnPos()).add(0.0, -0.5, 0.0);
         BlockHitResult hitResult = new BlockHitResult(hitPos, Direction.UP, this.curOperationPos.getBtnPos(), false);
         PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0);
         this.mc.getNetworkHandler().sendPacket(packet);
         this.step = Steps.NEXT;
         this.setDelay(40);
      }
   }

   private boolean scanPositions() {
      List<ItemFrameEntity> itemFrames = this.mc
         .world
         .getEntitiesByClass(
            ItemFrameEntity.class,
            this.mc.player.getBoundingBox().expand(((Integer)this.scanRange.get()).intValue()),
            framex -> !framex.getHeldItemStack().isEmpty()
         );
      int playerY = this.mc.player.getBlockPos().getY();
      Vec3d playerPos = this.mc.player.getEntityPos();

      for (ItemFrameEntity frame : itemFrames) {
         BlockPos attachedBlockPos = frame.getAttachedBlockPos();
         if (attachedBlockPos.getY() >= playerY && attachedBlockPos.getY() <= playerY + 4) {
            double distance = attachedBlockPos.toCenterPos().distanceTo(playerPos);
            if (!(distance > ((Integer)this.scanRange.get()).intValue())) {
               ItemStack frameHeldItemStack = frame.getHeldItemStack();
               Item item = frameHeldItemStack.getItem();
               StoragePos pos = this.checkAndBuildStoragePos(frame, StorageItem.valueOf(new ItemBo(frameHeldItemStack)));
               if (pos != null) {
                  if (item == this.takeKitItem.get()) {
                     this.takeKitPos = this.keepNearer(pos, this.takeKitPos, playerPos);
                  } else if (item == this.emptyKitItem.get()) {
                     this.emptyKitPos = this.keepNearer(pos, this.emptyKitPos, playerPos);
                  } else if (item == this.miscKitItem.get()) {
                     this.miscKitPos = this.keepNearer(pos, this.miscKitPos, playerPos);
                  } else if (!HeItemUtils.isShulkerBox(item)) {
                     StoragePos oldPos = this.itemPosMap.get(item);
                     StoragePos nearerPos = this.keepNearer(pos, oldPos, playerPos);
                     if (nearerPos == pos) {
                        this.itemPosMap.put(item, pos);
                     }
                  }
               }
            }
         }
      }

      if (this.emptyKitPos == null) {
         this.warning("未检测到<空盒位置>，请在对应展示框放置" + Names.get((Item)this.emptyKitItem.get()), new Object[0]);
         return false;
      } else if (this.takeKitPos == null) {
         this.warning("未检测到<待整理位置>，请在对应展示框放置" + Names.get((Item)this.takeKitItem.get()), new Object[0]);
         return false;
      } else if (this.miscKitPos == null) {
         this.warning("未检测到<无法整理位置>，请在对应展示框放置" + Names.get((Item)this.miscKitItem.get()), new Object[0]);
         return false;
      } else if (this.itemPosMap.isEmpty()) {
         this.warning("未检测到<物品>位置>", new Object[0]);
         return false;
      } else {
         return true;
      }
   }

   private StoragePos keepNearer(StoragePos newPos, StoragePos oldPos, Vec3d playerPos) {
      if (oldPos == null) {
         return newPos;
      }

      double newDist = newPos.getBtnPos().getSquaredDistance(playerPos);
      double oldDist = oldPos.getBtnPos().getSquaredDistance(playerPos);
      return newDist < oldDist ? newPos : oldPos;
   }

   private void clear() {
      this.itemPosMap.clear();
      this.takeKitPos = null;
      this.emptyKitPos = null;
      this.miscKitPos = null;
      this.needPosMap.clear();
      this.multiItemPosMap.clear();
      this.relatedToTargetMap.clear();
      this.curOperationPos = null;
   }

   @Override
   public void onDeactivate() {
      super.onDeactivate();
      this.curOperationPos = null;
      this.takeQty = 0;
      HeInvUtils.closeCurScreen();
   }
}
