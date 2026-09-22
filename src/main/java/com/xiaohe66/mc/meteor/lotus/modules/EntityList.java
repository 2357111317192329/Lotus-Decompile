/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.events.render.Render2DEvent
 *  meteordevelopment.meteorclient.renderer.text.TextRenderer
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.ColorSetting$Builder
 *  meteordevelopment.meteorclient.settings.DoubleSetting$Builder
 *  meteordevelopment.meteorclient.settings.EntityTypeListSetting$Builder
 *  meteordevelopment.meteorclient.settings.EnumSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.ItemListSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.systems.config.Config
 *  meteordevelopment.meteorclient.systems.friends.Friends
 *  meteordevelopment.meteorclient.systems.modules.Module
 *  meteordevelopment.meteorclient.utils.Utils
 *  meteordevelopment.meteorclient.utils.misc.Names
 *  meteordevelopment.meteorclient.utils.render.MeteorToast
 *  meteordevelopment.meteorclient.utils.render.MeteorToast$Builder
 *  meteordevelopment.meteorclient.utils.render.color.Color
 *  meteordevelopment.meteorclient.utils.render.color.SettingColor
 *  meteordevelopment.orbit.EventHandler
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityType
 *  net.minecraft.entity.ItemEntity
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.server.world.ServerWorld
 *  net.minecraft.client.toast.Toast
 *  net.minecraft.registry.RegistryKey
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.util.Const;
import com.xiaohe66.mc.meteor.lotus.util.HePosUtils;
import com.xiaohe66.mc.meteor.lotus.modules.entitylist.DisplaySide;

import com.xiaohe66.mc.meteor.lotus.bo.ItemWarp;
import com.xiaohe66.mc.meteor.lotus.bo.PlayerWarp;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.toast.Toast;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class EntityList
extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Set<EntityType<?>>> entitys = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("主世界实体")
        .description("仅在主世界显示的实体")
        .defaultValue(new EntityType[]{EntityType.PLAYER, EntityType.EXPERIENCE_ORB, EntityType.ZOMBIFIED_PIGLIN})
        .build());
    public final Setting<Set<EntityType<?>>> netherEntitys = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("下界实体")
        .description("仅在下界显示的实体")
        .defaultValue(new EntityType[]{EntityType.PLAYER, EntityType.EXPERIENCE_ORB, EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.HORSE, EntityType.ZOMBIE, EntityType.CREEPER, EntityType.BOGGED, EntityType.HUSK, EntityType.SLIME, EntityType.VILLAGER, EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER})
        .build());
    public final Setting<Set<EntityType<?>>> endEntitys = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("末地实体")
        .description("仅在末地显示的实体")
        .defaultValue(new EntityType[]{EntityType.PLAYER, EntityType.EXPERIENCE_ORB, EntityType.VILLAGER})
        .build());
    public final Setting<SettingColor> entitysColor = sgGeneral.add(new ColorSetting.Builder()
        .name("实体颜色")
        .defaultValue(Color.MAGENTA)
        .build());
    public final Setting<SettingColor> netheritePlayerColor = sgGeneral.add(new ColorSetting.Builder()
        .name("合金玩家颜色")
        .defaultValue(Color.MAGENTA)
        .build());
    public final Setting<SettingColor> diamondPlayerColor = sgGeneral.add(new ColorSetting.Builder()
        .name("钻石玩家颜色")
        .defaultValue(new Color(0, 255, 255))
        .build());
    public final Setting<SettingColor> otherPlayerColor = sgGeneral.add(new ColorSetting.Builder()
        .name("其他玩家颜色")
        .defaultValue(Color.ORANGE)
        .build());
    public final Setting<Integer> playerLimit = sgGeneral.add(new IntSetting.Builder()
        .name("玩家数量限制")
        .min(1)
        .sliderMax(64)
        .defaultValue(10)
        .build());
    private final SettingGroup itemGroup = settings.createGroup("物品");
    public final Setting<List<Item>> items1 = itemGroup.add(new ItemListSetting.Builder()
        .name("重点关注物品")
        .defaultValue(new Item[]{Items.ELYTRA, Items.SHULKER_BOX, Items.WHITE_SHULKER_BOX, Items.ORANGE_SHULKER_BOX, Items.MAGENTA_SHULKER_BOX, Items.LIGHT_BLUE_SHULKER_BOX, Items.YELLOW_SHULKER_BOX, Items.LIME_SHULKER_BOX, Items.PINK_SHULKER_BOX, Items.GRAY_SHULKER_BOX, Items.LIGHT_GRAY_SHULKER_BOX, Items.CYAN_SHULKER_BOX, Items.PURPLE_SHULKER_BOX, Items.BLUE_SHULKER_BOX, Items.BROWN_SHULKER_BOX, Items.GREEN_SHULKER_BOX, Items.RED_SHULKER_BOX, Items.BLACK_SHULKER_BOX, Items.BUNDLE, Items.WHITE_BUNDLE, Items.ORANGE_BUNDLE, Items.MAGENTA_BUNDLE, Items.LIGHT_BLUE_BUNDLE, Items.YELLOW_BUNDLE, Items.LIME_BUNDLE, Items.PINK_BUNDLE, Items.GRAY_BUNDLE, Items.LIGHT_GRAY_BUNDLE, Items.CYAN_BUNDLE, Items.PURPLE_BUNDLE, Items.BLUE_BUNDLE, Items.BROWN_BUNDLE, Items.GREEN_BUNDLE, Items.RED_BUNDLE, Items.BLACK_BUNDLE, Items.ANCIENT_DEBRIS, Items.NETHERITE_SCRAP, Items.NETHERITE_INGOT, Items.NETHERITE_BLOCK, Items.NETHERITE_SWORD, Items.NETHERITE_AXE, Items.NETHERITE_HOE, Items.NETHERITE_PICKAXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS, Items.NETHERITE_SPEAR, Items.END_CRYSTAL, Items.ENCHANTED_GOLDEN_APPLE, Items.MACE, Items.HEAVY_CORE, Items.CREEPER_HEAD, Items.ZOMBIE_HEAD, Items.SKELETON_SKULL, Items.WITHER_SKELETON_SKULL, Items.PLAYER_HEAD, Items.PIGLIN_HEAD, Items.DRAGON_HEAD})
        .onChanged(this::setItems1)
        .build());
    public final Setting<SettingColor> items1Color = itemGroup.add(new ColorSetting.Builder()
        .name("重点关注物品颜色")
        .defaultValue(Color.RED)
        .build());
    public final Setting<Boolean> sendNotifications = itemGroup.add(new BoolSetting.Builder()
        .name("重点关注物品通知")
        .defaultValue(true)
        .build());
    public final Setting<Integer> sendNotificationsCheckSeconds = itemGroup.add(new IntSetting.Builder()
        .name("通知检测时间")
        .min(1)
        .sliderMax(5)
        .defaultValue(2)
        .build());
    public final Setting<Integer> sendNotificationsIntervalSeconds = itemGroup.add(new IntSetting.Builder()
        .name("通知间隔时间")
        .min(1)
        .sliderMax(10)
        .defaultValue(5)
        .build());
    public final Setting<List<Item>> items2;
    public final Setting<SettingColor> items2Color = itemGroup.add(new ColorSetting.Builder()
        .name("关注物品颜色")
        .defaultValue(Color.CYAN)
        .build());
    public final Setting<SettingColor> itemsColor = itemGroup.add(new ColorSetting.Builder()
        .name("其他物品颜色")
        .defaultValue(Color.YELLOW)
        .build());
    public final Setting<Integer> otherItemsLimit = itemGroup.add(new IntSetting.Builder()
        .name("其他物品数量限制")
        .min(5)
        .sliderMax(30)
        .defaultValue(15)
        .build());
    public final Setting<List<Item>> blackList = itemGroup.add(new ItemListSetting.Builder()
        .name("黑名单")
        .onChanged(this::setBlackList)
        .build());
    private final SettingGroup ui = settings.createGroup("界面");
    public final Setting<Integer> xOffset = ui.add(new IntSetting.Builder()
        .name("X偏移")
        .min(0)
        .sliderMax(2048)
        .defaultValue(20)
        .build());
    public final Setting<Integer> yOffset = ui.add(new IntSetting.Builder()
        .name("Y偏移")
        .min(0)
        .sliderMax(2048)
        .defaultValue(500)
        .build());
    public final Setting<DisplaySide> displaySide = ui.add(new EnumSetting.Builder<DisplaySide>()
        .name("位置")
        .description("选择实体列表显示的位置")
        .defaultValue(DisplaySide.Left)
        .build());
    public final Setting<Integer> lineHeight = ui.add(new IntSetting.Builder()
        .name("行高")
        .min(0)
        .sliderMax(100)
        .defaultValue(20)
        .build());
    public final Setting<Double> scale = ui.add(new DoubleSetting.Builder()
        .name("字体大小")
        .min(0.0)
        .sliderMax(6.0)
        .defaultValue(1.0)
        .build());
    private final SettingGroup renderGroup = settings.createGroup("渲染");
    public final Setting<Boolean> renderImportantItems = renderGroup.add(new BoolSetting.Builder()
        .name("重点关注物品渲染")
        .description("渲染重点关注物品的边框和连线")
        .defaultValue(true)
        .build());
    public final Setting<ShapeMode> renderMode = renderGroup.add(new EnumSetting.Builder<ShapeMode>()
        .name("渲染模式")
        .description("重点关注物品的渲染模式")
        .defaultValue(ShapeMode.Both)
        .visible(() -> this.renderImportantItems.get())
        .build());
    public final Setting<Double> fillOpacity = renderGroup.add(new DoubleSetting.Builder()
        .name("填充不透明度")
        .description("重点关注物品边框内的填充不透明度")
        .defaultValue(0.3)
        .min(0.0)
        .sliderMax(1.0)
        .visible(() -> this.renderImportantItems.get())
        .build());
    public final Setting<Boolean> connectionLine = renderGroup.add(new BoolSetting.Builder()
        .name("连接线")
        .description("从屏幕中心到重点关注物品的连线")
        .defaultValue(true)
        .visible(() -> this.renderImportantItems.get())
        .build());
    private final Set<Item> items1Set;
    private final Set<Item> items2Set;
    private final Set<Item> blackListSet;
    private final List<PlayerWarp> nonFriendPlayers;
    private final List<PlayerWarp> friendPlayers;
    private final Map<Item, ItemWarp> items1Map;
    private final Map<Item, ItemWarp> items2Map;
    private final Map<Item, ItemWarp> itemsMap;
    private final Map<EntityType<?>, Integer> entitysMap;
    private final Color lineRenderColor = new Color();
    private final Color fillRenderColor = new Color();
    private boolean renderFlag;
    private long startTime;
    private long prevTime;

    public EntityList() {
        super(Const.CATEGORY, "V实体列表", "显示实体列表, 可以按重要程度分组, 如红色为重点关注");
        this.items2 = this.itemGroup.add(new ItemListSetting.Builder().name("关注物品").defaultValue(new Item[]{Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_HOE, Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Items.COAL_ORE, Items.DEEPSLATE_COAL_ORE, Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE, Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE, Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE, Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE, Items.LAPIS_ORE, Items.DEEPSLATE_LAPIS_ORE, Items.DIAMOND_ORE, Items.DEEPSLATE_DIAMOND_ORE, Items.EMERALD_ORE, Items.DEEPSLATE_EMERALD_ORE, Items.NETHER_QUARTZ_ORE, Items.NETHER_GOLD_ORE, Items.RAW_IRON, Items.RAW_COPPER, Items.RAW_GOLD, Items.COAL, Items.IRON_INGOT, Items.COPPER_INGOT, Items.GOLD_INGOT, Items.REDSTONE, Items.LAPIS_LAZULI, Items.DIAMOND, Items.EMERALD, Items.QUARTZ, Items.AMETHYST_SHARD, Items.COAL_BLOCK, Items.IRON_BLOCK, Items.COPPER_BLOCK, Items.GOLD_BLOCK, Items.REDSTONE_BLOCK, Items.LAPIS_BLOCK, Items.DIAMOND_BLOCK, Items.EMERALD_BLOCK, Items.QUARTZ_BLOCK, Items.AMETHYST_BLOCK, Items.SHULKER_SHELL, Items.ENDER_PEARL, Items.ENDER_CHEST, Items.MUSIC_DISC_13, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_BLOCKS, Items.MUSIC_DISC_CHIRP, Items.MUSIC_DISC_FAR, Items.MUSIC_DISC_MALL, Items.MUSIC_DISC_MELLOHI, Items.MUSIC_DISC_STAL, Items.MUSIC_DISC_STRAD, Items.MUSIC_DISC_WARD, Items.MUSIC_DISC_11, Items.MUSIC_DISC_WAIT, Items.MUSIC_DISC_OTHERSIDE, Items.MUSIC_DISC_5, Items.MUSIC_DISC_PIGSTEP, Items.MUSIC_DISC_RELIC, Items.ENDER_EYE, Items.END_ROD, Items.SNIFFER_EGG, Items.SEA_LANTERN, Items.VERDANT_FROGLIGHT, Items.OCHRE_FROGLIGHT, Items.PEARLESCENT_FROGLIGHT, Items.SHROOMLIGHT, Items.BEACON, Items.TNT, Items.SLIME_BALL, Items.SLIME_BLOCK, Items.DRAGON_BREATH, Items.GOLDEN_CARROT, Items.GHAST_TEAR, Items.BLAZE_ROD, Items.BREEZE_ROD, Items.NETHER_STAR, Items.GOLDEN_APPLE, Items.PORKCHOP, Items.COOKED_PORKCHOP, Items.BEEF, Items.COOKED_BEEF, Items.WIND_CHARGE, Items.GOAT_HORN, Items.RABBIT_FOOT, Items.OMINOUS_TRIAL_KEY, Items.TRIAL_KEY, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, Items.HONEY_BLOCK, Items.TOTEM_OF_UNDYING, Items.OMINOUS_BOTTLE, Items.FIREWORK_ROCKET, Items.PUMPKIN_PIE, Items.CHORUS_FRUIT}).onChanged(this::setItems2).build());
        this.items1Set = new HashSet<Item>((Collection)this.items1.get());
        this.items2Set = new HashSet<Item>((Collection)this.items2.get());
        this.blackListSet = new HashSet<Item>((Collection)this.blackList.get());
        this.nonFriendPlayers = new ArrayList<PlayerWarp>();
        this.friendPlayers = new ArrayList<PlayerWarp>();
        this.items1Map = new HashMap<Item, ItemWarp>();
        this.items2Map = new HashMap<Item, ItemWarp>();
        this.itemsMap = new HashMap<Item, ItemWarp>();
        this.entitysMap = new HashMap();
    }

    public void onActivate() {
        this.renderFlag = true;
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (Utils.isLoading()) {
            return;
        }
        if (!this.isActive()) {
            return;
        }
        if (!this.renderFlag) {
            return;
        }
        this.renderFlag = false;
        this.clearLists();
        RegistryKey worldKey = this.mc.player.getEntityWorld().getRegistryKey();
        Set displayEntities;
        if (worldKey == ServerWorld.NETHER) {
            displayEntities = this.netherEntitys.get();
        } else if (worldKey == ServerWorld.OVERWORLD) {
            displayEntities = this.entitys.get();
        } else {
            displayEntities = this.endEntitys.get();
        }
        Vec3d cameraPos = HePosUtils.getCameraPos();
        for (Entity entity : this.mc.world.getEntities()) {
            if (entity instanceof ItemEntity) {
                ItemEntity itemEntity = (ItemEntity)entity;
                this.handleItemEntity(entity, cameraPos, itemEntity);
                continue;
            }
            this.handleEntity(entity, cameraPos, displayEntities);
        }
        double yPos = this.yOffset.get();
        yPos = this.drawPlayers(this.nonFriendPlayers, null, yPos, this.playerLimit.get());
        if (this.nonFriendPlayers.size() < this.playerLimit.get()) {
            int remaining = this.playerLimit.get() - this.nonFriendPlayers.size();
            yPos = this.drawPlayers(this.friendPlayers, Config.get().friendColor.get().copy(), yPos, remaining);
        }
        yPos = this.drawItems(this.items1Map, (Color)this.items1Color.get(), yPos);
        yPos = this.drawItems(this.items2Map, (Color)this.items2Color.get(), yPos);
        yPos = this.drawItems(this.itemsMap, (Color)this.itemsColor.get(), yPos);
        this.drawEntities(this.entitysMap, (Color)this.entitysColor.get(), yPos);
        this.checkNotifications();
        this.renderFlag = true;
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (Utils.isLoading() || !this.renderImportantItems.get()) {
            return;
        }
        SettingColor color = this.items1Color.get();
        this.lineRenderColor.set(color);
        this.fillRenderColor.set(color).a((int)((double)color.a * this.fillOpacity.get()));
        for (Entity entity : this.mc.world.getEntities()) {
            if (!(entity instanceof ItemEntity)) continue;
            ItemEntity itemEntity = (ItemEntity)entity;
            Item item = itemEntity.getStack().getItem();
            if (!this.items1Set.contains(item) || this.blackListSet.contains(item)) continue;
            double dx = MathHelper.lerp((double)event.tickDelta, entity.lastRenderX, entity.getX()) - entity.getX();
            double dy = MathHelper.lerp((double)event.tickDelta, entity.lastRenderY, entity.getY()) - entity.getY();
            double dz = MathHelper.lerp((double)event.tickDelta, entity.lastRenderZ, entity.getZ()) - entity.getZ();
            Box box = entity.getBoundingBox();
            event.renderer.box(dx + box.minX, dy + box.minY, dz + box.minZ, dx + box.maxX, dy + box.maxY, dz + box.maxZ, this.fillRenderColor, this.lineRenderColor, this.renderMode.get(), 0);
            if (this.connectionLine.get() && !this.mc.options.hudHidden) {
                double height = box.maxY - box.minY;
                event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, entity.getX() + dx, entity.getY() + dy + height / 2.0, entity.getZ() + dz, this.lineRenderColor);
            }
        }
    }

    private void handleEntity(Entity entity, Vec3d cameraPos, Set<EntityType<?>> displayEntities) {
        EntityType entityType = entity.getType();
        if (!displayEntities.contains(entityType)) {
            return;
        }
        if (entityType == EntityType.PLAYER) {
            if (entity != this.mc.player && this.nonFriendPlayers.size() + this.friendPlayers.size() < this.playerLimit.get()) {
                PlayerEntity playerEntity = (PlayerEntity)entity;
                String playerName = playerEntity.getName().getString();
                Item armorItem = this.getArmorType(playerEntity);
                float distance = (float)cameraPos.distanceTo(entity.getEntityPos());
                PlayerWarp playerWarp = new PlayerWarp(playerName, armorItem, distance);
                boolean isFriend = Friends.get().get(playerName) != null;
                if (isFriend) {
                    this.friendPlayers.add(playerWarp);
                } else {
                    this.nonFriendPlayers.add(playerWarp);
                }
            }
        } else if (this.entitysMap.containsKey(entityType) || this.entitysMap.size() < this.otherItemsLimit.get()) {
            Integer count = this.entitysMap.getOrDefault(entityType, 0);
            this.entitysMap.put(entityType, count + 1);
        }
    }

    private Item getArmorType(PlayerEntity playerEntity) {
        Item head = playerEntity.getEquippedStack(EquipmentSlot.HEAD).getItem();
        Item chest = playerEntity.getEquippedStack(EquipmentSlot.CHEST).getItem();
        Item legs = playerEntity.getEquippedStack(EquipmentSlot.LEGS).getItem();
        Item feet = playerEntity.getEquippedStack(EquipmentSlot.FEET).getItem();
        if (HeItemUtils.allAir(head, chest, legs, feet)) {
            return Items.AIR;
        }
        if (!HeItemUtils.isNetheriteArmor(head) && !HeItemUtils.isNetheriteArmor(chest) && !HeItemUtils.isNetheriteArmor(legs) && !HeItemUtils.isNetheriteArmor(feet)) {
            return !HeItemUtils.isDiamondArmor(head) && !HeItemUtils.isDiamondArmor(chest) && !HeItemUtils.isDiamondArmor(legs) && !HeItemUtils.isDiamondArmor(feet) ? Items.DIAMOND : Items.DIAMOND_BLOCK;
        }
        return Items.NETHERITE_BLOCK;
    }

    private void handleItemEntity(Entity entity, Vec3d cameraPos, ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getStack();
        Item item = stack.getItem();
        if (this.blackListSet.contains(item)) {
            return;
        }
        Map<Item, ItemWarp> targetMap = this.items1Set.contains(item) ? this.items1Map : (this.items2Set.contains(item) ? this.items2Map : this.itemsMap);
        ItemWarp itemWarp = targetMap.computeIfAbsent(item, k -> new ItemWarp());
        itemWarp.setItem(item);
        itemWarp.setCount(itemWarp.getCount() + stack.getCount());
        float distance = (float)cameraPos.distanceTo(entity.getEntityPos());
        if (distance < itemWarp.getMinDistance()) {
            itemWarp.setMinDistance(distance);
        }
    }

    private void checkNotifications() {
        if (!this.sendNotifications.get()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (this.items1Map.isEmpty()) {
            this.startTime = 0L;
        } else if (this.startTime == 0L) {
            this.startTime = now;
        } else if (now - this.startTime > (long)this.sendNotificationsCheckSeconds.get() * 1000L && now - this.prevTime > (long)this.sendNotificationsIntervalSeconds.get() * 1000L) {
            this.info("捡东西啦", new Object[0]);
            MeteorToast meteorToast = new MeteorToast.Builder(this.title).icon(Items.CHEST).text("捡东西啦~").build();
            this.mc.getToastManager().add((Toast)meteorToast);
            this.prevTime = now;
        }
    }

    private double drawPlayers(List<PlayerWarp> players, Color color, double yPos, int limit) {
        if (players.isEmpty()) {
            return yPos;
        }
        players.sort((a, b) -> Float.compare(a.getDistance(), b.getDistance()));
        int count = Math.min(players.size(), limit);
        for (int i = 0; i < count; ++i) {
            PlayerWarp playerWarp = players.get(i);
            Item item = playerWarp.getItem();
            String tag;
            Color lineColor;
            if (item == Items.NETHERITE_BLOCK) {
                tag = "合金甲";
                lineColor = this.netheritePlayerColor.get();
            } else if (item == Items.DIAMOND_BLOCK) {
                tag = "钻石甲";
                lineColor = this.diamondPlayerColor.get();
            } else if (item == Items.AIR) {
                tag = "裸奔";
                lineColor = Color.WHITE;
            } else {
                tag = "着甲";
                lineColor = this.otherPlayerColor.get();
            }
            String lineText = String.format("[%s] %s %.1fm", tag, playerWarp.getName(), Float.valueOf(playerWarp.getDistance()));
            this.drawText(lineText, color != null ? color : lineColor, yPos);
            yPos += (double)this.lineHeight.get() * this.scale.get();
        }
        return yPos;
    }

    private double drawItems(Map<Item, ItemWarp> map, Color color, double yPos) {
        if (map.isEmpty()) {
            return yPos;
        }
        for (ItemWarp itemWarp : map.values()) {
            String itemName = Names.get(itemWarp.getItem());
            String lineText = String.format("%s x%s  (%.1f m)", itemName, itemWarp.getCount(), Float.valueOf(itemWarp.getMinDistance()));
            this.drawText(lineText, color, yPos);
            yPos += (double)this.lineHeight.get() * this.scale.get();
        }
        return yPos;
    }

    private double drawEntities(Map<EntityType<?>, Integer> map, Color color, double yPos) {
        if (map.isEmpty()) {
            return yPos;
        }
        for (Map.Entry<EntityType<?>, Integer> entry : map.entrySet()) {
            String entityName = Names.get(entry.getKey());
            String lineText = String.format("%s x%s", entityName, entry.getValue());
            this.drawText(lineText, color, yPos);
            yPos += (double)this.lineHeight.get() * this.scale.get();
        }
        return yPos;
    }

    private void drawText(String text, Color color, double yPos) {
        int xPos;
        TextRenderer textRenderer = TextRenderer.get();
        textRenderer.begin(this.scale.get());
        if (this.displaySide.get() == DisplaySide.Right) {
            int textWidth = (int)textRenderer.getWidth(text);
            xPos = this.mc.getWindow().getWidth() - textWidth - this.xOffset.get();
        } else {
            xPos = this.xOffset.get();
        }
        textRenderer.render(text, (double)xPos, yPos, color, false);
        textRenderer.end();
    }

    private void clearLists() {
        this.nonFriendPlayers.clear();
        this.friendPlayers.clear();
        this.items1Map.clear();
        this.items2Map.clear();
        this.itemsMap.clear();
        this.entitysMap.clear();
    }

    private void setItems1(List<Item> list) {
        this.items1Set.clear();
        this.items1Set.addAll(list);
    }

    private void setItems2(List<Item> list) {
        this.items2Set.clear();
        this.items2Set.addAll((Collection)this.items2.get());
    }

    private void setBlackList(List<Item> list) {
        this.blackListSet.clear();
        this.blackListSet.addAll((Collection)this.blackList.get());
    }
}
