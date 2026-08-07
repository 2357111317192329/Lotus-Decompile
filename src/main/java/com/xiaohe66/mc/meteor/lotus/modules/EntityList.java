package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.bo.ItemWarp;
import com.xiaohe66.mc.meteor.lotus.event.LogEvent;
import com.xiaohe66.mc.meteor.lotus.modules.entitylist.DisplaySide;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class EntityList extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Set<EntityType<?>>> entitys = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("主世界实体")).description("仅在主世界显示的实体"))
            .defaultValue(new EntityType[]{EntityType.EXPERIENCE_ORB, EntityType.ZOMBIFIED_PIGLIN})
            .build()
      );
   public final Setting<Set<EntityType<?>>> netherEntitys = this.sgGeneral
      .add(
         ((Builder)((Builder)new Builder().name("下界实体")).description("仅在下界显示的实体"))
            .defaultValue(
               new EntityType[]{
                  EntityType.EXPERIENCE_ORB,
                  EntityType.COW,
                  EntityType.SHEEP,
                  EntityType.PIG,
                  EntityType.HORSE,
                  EntityType.ZOMBIE,
                  EntityType.CREEPER,
                  EntityType.BOGGED,
                  EntityType.HUSK,
                  EntityType.SLIME,
                  EntityType.VILLAGER,
                  EntityType.SPIDER,
                  EntityType.CAVE_SPIDER,
                  EntityType.DROWNED,
                  EntityType.ZOMBIE_VILLAGER
               }
            )
            .build()
      );
   public final Setting<SettingColor> entitysColor = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("实体颜色"))
            .defaultValue(Color.MAGENTA)
            .build()
      );
   public final Setting<Boolean> entityLog = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("实体日志"))
               .defaultValue(false))
            .build()
      );
   private final SettingGroup itemGroup = this.settings.createGroup("物品");
   public final Setting<List<Item>> items1 = this.itemGroup
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder().name("物品1"))
            .defaultValue(
               new Item[]{
                  Items.ELYTRA,
                  Items.SHULKER_BOX,
                  Items.WHITE_SHULKER_BOX,
                  Items.ORANGE_SHULKER_BOX,
                  Items.MAGENTA_SHULKER_BOX,
                  Items.LIGHT_BLUE_SHULKER_BOX,
                  Items.YELLOW_SHULKER_BOX,
                  Items.LIME_SHULKER_BOX,
                  Items.PINK_SHULKER_BOX,
                  Items.GRAY_SHULKER_BOX,
                  Items.LIGHT_GRAY_SHULKER_BOX,
                  Items.CYAN_SHULKER_BOX,
                  Items.PURPLE_SHULKER_BOX,
                  Items.BLUE_SHULKER_BOX,
                  Items.BROWN_SHULKER_BOX,
                  Items.GREEN_SHULKER_BOX,
                  Items.RED_SHULKER_BOX,
                  Items.BLACK_SHULKER_BOX,
                  Items.BUNDLE,
                  Items.WHITE_BUNDLE,
                  Items.ORANGE_BUNDLE,
                  Items.MAGENTA_BUNDLE,
                  Items.LIGHT_BLUE_BUNDLE,
                  Items.YELLOW_BUNDLE,
                  Items.LIME_BUNDLE,
                  Items.PINK_BUNDLE,
                  Items.GRAY_BUNDLE,
                  Items.LIGHT_GRAY_BUNDLE,
                  Items.CYAN_BUNDLE,
                  Items.PURPLE_BUNDLE,
                  Items.BLUE_BUNDLE,
                  Items.BROWN_BUNDLE,
                  Items.GREEN_BUNDLE,
                  Items.RED_BUNDLE,
                  Items.BLACK_BUNDLE,
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
                  Items.NETHERITE_BOOTS
               }
            )
            .build()
      );
   public final Setting<SettingColor> items1Color = this.itemGroup
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("物品1颜色"))
            .defaultValue(Color.RED)
            .build()
      );
   public final Setting<Boolean> item1Log = this.itemGroup
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("物品1日志"))
               .defaultValue(false))
            .build()
      );
   public final Setting<Boolean> sendNotifications = this.itemGroup
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                  .name("物品1通知"))
               .defaultValue(true))
            .build()
      );
   public final Setting<Integer> sendNotificationsCheckSeconds = this.itemGroup
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("通知检测时间"))
               .min(1)
               .sliderMax(5)
               .defaultValue(2))
            .build()
      );
   public final Setting<Integer> sendNotificationsIntervalSeconds = this.itemGroup
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("通知间隔时间"))
               .min(1)
               .sliderMax(10)
               .defaultValue(5))
            .build()
      );
   public final Setting<List<Item>> items2 = this.itemGroup
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder().name("物品2"))
            .build()
      );
   public final Setting<SettingColor> items2Color = this.itemGroup
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("物品2颜色"))
            .defaultValue(Color.CYAN)
            .build()
      );
   public final Setting<SettingColor> itemsColor = this.itemGroup
      .add(
         ((meteordevelopment.meteorclient.settings.ColorSetting.Builder)new meteordevelopment.meteorclient.settings.ColorSetting.Builder().name("物品默认颜色"))
            .defaultValue(Color.YELLOW)
            .build()
      );
   public final Setting<List<Item>> blackList = this.itemGroup
      .add(
         ((meteordevelopment.meteorclient.settings.ItemListSetting.Builder)new meteordevelopment.meteorclient.settings.ItemListSetting.Builder().name("黑名单"))
            .build()
      );
   private final SettingGroup ui = this.settings.createGroup("界面");
   public final Setting<Integer> xOffset = this.ui
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("X偏移"))
               .min(0)
               .sliderMax(2048)
               .defaultValue(20))
            .build()
      );
   public final Setting<Integer> yOffset = this.ui
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("Y偏移"))
               .min(0)
               .sliderMax(2048)
               .defaultValue(500))
            .build()
      );
   public final Setting<DisplaySide> displaySide = this.ui
      .add(
         ((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)((meteordevelopment.meteorclient.settings.EnumSetting.Builder)new meteordevelopment.meteorclient.settings.EnumSetting.Builder()
                     .name("位置"))
                  .description("选择实体列表显示的位置"))
               .defaultValue(DisplaySide.Left))
            .build()
      );
   public final Setting<Integer> lineHeight = this.ui
      .add(
         ((meteordevelopment.meteorclient.settings.IntSetting.Builder)((meteordevelopment.meteorclient.settings.IntSetting.Builder)new meteordevelopment.meteorclient.settings.IntSetting.Builder()
                  .name("行高"))
               .min(0)
               .sliderMax(100)
               .defaultValue(20))
            .build()
      );
   public final Setting<Double> scale = this.ui
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder().name("字体大小"))
            .min(0.0)
            .sliderMax(6.0)
            .defaultValue(1.0)
            .build()
      );
   private long startTime;
   private long prevTime;

   public EntityList() {
      super(Const.CATEGORY, "实体列表", "显示实体列表, 可以按重要程度分组, 如红色为重点关注");
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      if (!Utils.isLoading()) {
         if (this.isActive()) {
            Set<Item> items1Set = new HashSet<>((Collection<? extends Item>)this.items1.get());
            Set<Item> items2Set = new HashSet<>((Collection<? extends Item>)this.items2.get());
            Set<Item> blackListSet = new HashSet<>((Collection<? extends Item>)this.blackList.get());
            Map<Item, ItemWarp> items1Map = new HashMap<>();
            Map<Item, ItemWarp> items2Map = new HashMap<>();
            Map<Item, ItemWarp> itemsMap = new HashMap<>();
            Map<EntityType<?>, Integer> entitysMap = new HashMap<>();
            ResourceKey<Level> registryKey = this.mc.player.level().dimension();

            for (Entity entity : this.mc.level.entitiesForRendering()) {
               if (entity instanceof ItemEntity itemEntity) {
                  ItemStack stack = itemEntity.getItem();
                  Item item = stack.getItem();
                  if (!blackListSet.contains(item)) {
                     Map<Item, ItemWarp> map;
                     if (items1Set.contains(item)) {
                        map = items1Map;
                        if ((Boolean)this.item1Log.get()) {
                           String msg = "检测到物品 : " + Names.get(item);
                           this.postLogEvent(entity, registryKey, entity.position(), msg);
                        }
                     } else if (items2Set.contains(item)) {
                        map = items2Map;
                     } else {
                        map = itemsMap;
                     }

                     ItemWarp itemWarp = map.computeIfAbsent(item, k -> new ItemWarp());
                     itemWarp.setItem(item);
                     itemWarp.setCount(itemWarp.getCount() + stack.getCount());
                     float distanceTo = this.mc.player.distanceTo(entity);
                     if (distanceTo < itemWarp.getMinDistance()) {
                        itemWarp.setMinDistance(distanceTo);
                     }
                  }
               } else {
                  EntityType<?> entityType = entity.getType();
                  Set<EntityType<?>> entityTypes;
                  if (registryKey == ServerLevel.NETHER) {
                     entityTypes = (Set<EntityType<?>>)this.netherEntitys.get();
                  } else if (registryKey == ServerLevel.OVERWORLD) {
                     entityTypes = (Set<EntityType<?>>)this.entitys.get();
                  } else {
                     entityTypes = Collections.emptySet();
                  }

                  if (entityTypes.contains(entityType)) {
                     Integer qty = entitysMap.getOrDefault(entityType, 0);
                     entitysMap.put(entityType, qty + 1);
                     if ((Boolean)this.entityLog.get()) {
                        String msg = "检测到实体 : " + Names.get(entityType);
                        this.postLogEvent(entity, registryKey, entity.position(), msg);
                     }
                  }
               }
            }

            int y = (Integer)this.yOffset.get();
            int screenWidth = this.mc.getWindow().getScreenWidth();
            y = this.draw(items1Map, y, (Color)this.items1Color.get(), screenWidth);
            if ((Boolean)this.sendNotifications.get()) {
               long currentTimeMillis = System.currentTimeMillis();
               if (items1Map.isEmpty()) {
                  this.startTime = 0L;
               } else if (this.startTime == 0L) {
                  this.startTime = currentTimeMillis;
               } else if (currentTimeMillis - this.startTime > (Integer)this.sendNotificationsCheckSeconds.get() * 1000
                  && currentTimeMillis - this.prevTime > (Integer)this.sendNotificationsIntervalSeconds.get() * 1000) {
                  this.info("捡东西啦", new Object[0]);
                  MeteorToast toast = new meteordevelopment.meteorclient.utils.render.MeteorToast.Builder(this.title)
                     .icon(Items.CHEST)
                     .text("捡东西啦~")
                     .build();
                  this.mc.getToastManager().addToast(toast);
                  this.prevTime = currentTimeMillis;
               }
            }

            y = this.draw(items2Map, y, (Color)this.items2Color.get(), screenWidth);
            y = this.draw(itemsMap, y, (Color)this.itemsColor.get(), screenWidth);
            this.draw2(entitysMap, y, (Color)this.entitysColor.get(), screenWidth);
         }
      }
   }

   private void postLogEvent(Entity entity, ResourceKey<Level> worldType, Vec3 pos, String msg) {
      LogEvent logEvent = new LogEvent();
      logEvent.setModuleName(super.name);
      logEvent.setWorldType(worldType);
      logEvent.setPos(new Vec3i((int)pos.x(), (int)pos.y(), (int)pos.z()));
      logEvent.setKey(String.valueOf(entity.getId()));
      logEvent.setMsg(msg);
      MeteorClient.EVENT_BUS.post(logEvent);
   }

   private int draw(Map<Item, ItemWarp> grayMap, int y, Color color, int screenWidth) {
      if (grayMap.isEmpty()) {
         return y;
      }

      TextRenderer textRenderer = TextRenderer.get();

      for (ItemWarp itemWarp : grayMap.values()) {
         String name = Names.get(itemWarp.getItem());
         String text = String.format("%s x%s  (%.1f m)", name, itemWarp.getCount(), itemWarp.getMinDistance());
         textRenderer.begin((Double)this.scale.get());
         int x;
         if (this.displaySide.get() == DisplaySide.Right) {
            int textWidth = (int)textRenderer.getWidth(text);
            x = screenWidth - textWidth - (Integer)this.xOffset.get();
         } else {
            x = (Integer)this.xOffset.get();
         }

         textRenderer.render(text, x, y, color, false);
         textRenderer.end();
         y = (int)(y + ((Integer)this.lineHeight.get()).intValue() * (Double)this.scale.get());
      }

      return y;
   }

   private int draw2(Map<EntityType<?>, Integer> grayMap, int y, Color color, int screenWidth) {
      TextRenderer textRenderer = TextRenderer.get();

      for (Entry<EntityType<?>, Integer> entry : grayMap.entrySet()) {
         EntityType<?> item = entry.getKey();
         Integer count = entry.getValue();
         String name = Names.get(item);
         String text = String.format("%s x%s", name, count);
         textRenderer.begin((Double)this.scale.get());
         int x;
         if (this.displaySide.get() == DisplaySide.Right) {
            int textWidth = (int)textRenderer.getWidth(text);
            x = screenWidth - textWidth - (Integer)this.xOffset.get();
         } else {
            x = (Integer)this.xOffset.get();
         }

         textRenderer.render(text, x, y, color, true);
         textRenderer.end();
         y = (int)(y + ((Integer)this.lineHeight.get()).intValue() * (Double)this.scale.get());
      }

      return y;
   }
}
