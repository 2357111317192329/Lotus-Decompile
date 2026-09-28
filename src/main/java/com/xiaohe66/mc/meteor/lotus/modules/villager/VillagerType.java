/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.Item
 *  net.minecraft.item.Items
 *  net.minecraft.village.VillagerProfession
 *  net.minecraft.registry.RegistryKey
 *  net.minecraft.registry.entry.RegistryEntry
 */
package com.xiaohe66.mc.meteor.lotus.modules.villager;

import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;

public enum VillagerType {
    盔甲匠(Items.BLAST_FURNACE, (ResourceKey<VillagerProfession>)VillagerProfession.ARMORER, Set.of(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS), List.of(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS)),
    屠夫(Items.SMOKER, (ResourceKey<VillagerProfession>)VillagerProfession.BUTCHER, Set.of(Items.RABBIT_STEW, Items.COOKED_CHICKEN, Items.COOKED_PORKCHOP), List.of(Items.COOKED_PORKCHOP)),
    制图师(Items.CARTOGRAPHY_TABLE, (ResourceKey<VillagerProfession>)VillagerProfession.CARTOGRAPHER, Set.of(Items.MAP, Items.ITEM_FRAME, Items.BANNER.pick(DyeColor.WHITE), Items.BANNER.pick(DyeColor.ORANGE), Items.BANNER.pick(DyeColor.YELLOW), Items.BANNER.pick(DyeColor.LIGHT_GRAY), Items.BANNER.pick(DyeColor.RED), Items.BANNER.pick(DyeColor.BROWN), Items.BANNER.pick(DyeColor.MAGENTA), Items.BANNER.pick(DyeColor.LIGHT_BLUE), Items.BANNER.pick(DyeColor.LIME), Items.BANNER.pick(DyeColor.PINK), Items.BANNER.pick(DyeColor.GRAY), Items.BANNER.pick(DyeColor.CYAN), Items.BANNER.pick(DyeColor.PURPLE), Items.BANNER.pick(DyeColor.BLUE), Items.BANNER.pick(DyeColor.GREEN), Items.BANNER.pick(DyeColor.BLACK)), List.of(Items.MAP)),
    牧师(Items.BREWING_STAND, (ResourceKey<VillagerProfession>)VillagerProfession.CLERIC, Set.of(Items.REDSTONE, Items.LAPIS_LAZULI, Items.GLOWSTONE, Items.ENDER_PEARL, Items.EXPERIENCE_BOTTLE), List.of(Items.EXPERIENCE_BOTTLE, Items.ENDER_PEARL)),
    农民(Items.COMPOSTER, (ResourceKey<VillagerProfession>)VillagerProfession.FARMER, Set.of(Items.BREAD, Items.PUMPKIN_PIE, Items.APPLE, Items.COOKIE, Items.GOLDEN_CARROT), List.of(Items.GOLDEN_CARROT)),
    图书管理员(Items.LECTERN, (ResourceKey<VillagerProfession>)VillagerProfession.LIBRARIAN, Set.of(Items.BOOKSHELF, Items.LANTERN, Items.GLASS, Items.COMPASS, Items.NAME_TAG), List.of(Items.GLASS)),
    石匠(Items.STONECUTTER, (ResourceKey<VillagerProfession>)VillagerProfession.MASON, Set.of(Items.BRICK, Items.CHISELED_STONE_BRICKS, Items.DRIPSTONE_BLOCK, Items.POLISHED_ANDESITE, Items.POLISHED_DIORITE, Items.POLISHED_GRANITE, Items.TERRACOTTA, Items.DYED_TERRACOTTA.pick(DyeColor.WHITE), Items.DYED_TERRACOTTA.pick(DyeColor.ORANGE), Items.DYED_TERRACOTTA.pick(DyeColor.YELLOW), Items.DYED_TERRACOTTA.pick(DyeColor.LIGHT_GRAY), Items.DYED_TERRACOTTA.pick(DyeColor.RED), Items.DYED_TERRACOTTA.pick(DyeColor.BROWN), Items.DYED_TERRACOTTA.pick(DyeColor.MAGENTA), Items.DYED_TERRACOTTA.pick(DyeColor.LIGHT_BLUE), Items.DYED_TERRACOTTA.pick(DyeColor.LIME), Items.DYED_TERRACOTTA.pick(DyeColor.PINK), Items.DYED_TERRACOTTA.pick(DyeColor.GRAY), Items.DYED_TERRACOTTA.pick(DyeColor.CYAN), Items.DYED_TERRACOTTA.pick(DyeColor.PURPLE), Items.DYED_TERRACOTTA.pick(DyeColor.BLUE), Items.DYED_TERRACOTTA.pick(DyeColor.GREEN), Items.DYED_TERRACOTTA.pick(DyeColor.BLACK), Items.GLAZED_TERRACOTTA.pick(DyeColor.WHITE), Items.GLAZED_TERRACOTTA.pick(DyeColor.ORANGE), Items.GLAZED_TERRACOTTA.pick(DyeColor.YELLOW), Items.GLAZED_TERRACOTTA.pick(DyeColor.LIGHT_GRAY), Items.GLAZED_TERRACOTTA.pick(DyeColor.RED), Items.GLAZED_TERRACOTTA.pick(DyeColor.BROWN), Items.GLAZED_TERRACOTTA.pick(DyeColor.MAGENTA), Items.GLAZED_TERRACOTTA.pick(DyeColor.LIGHT_BLUE), Items.GLAZED_TERRACOTTA.pick(DyeColor.LIME), Items.GLAZED_TERRACOTTA.pick(DyeColor.PINK), Items.GLAZED_TERRACOTTA.pick(DyeColor.GRAY), Items.GLAZED_TERRACOTTA.pick(DyeColor.CYAN), Items.GLAZED_TERRACOTTA.pick(DyeColor.PURPLE), Items.GLAZED_TERRACOTTA.pick(DyeColor.BLUE), Items.GLAZED_TERRACOTTA.pick(DyeColor.GREEN), Items.GLAZED_TERRACOTTA.pick(DyeColor.BLACK), Items.QUARTZ_PILLAR, Items.QUARTZ_BLOCK), List.of()),
    牧羊人(Items.LOOM, (ResourceKey<VillagerProfession>)VillagerProfession.SHEPHERD, Set.of(Items.SHEARS, Items.WOOL.pick(DyeColor.WHITE), Items.WOOL.pick(DyeColor.ORANGE), Items.WOOL.pick(DyeColor.MAGENTA), Items.WOOL.pick(DyeColor.LIGHT_BLUE), Items.WOOL.pick(DyeColor.YELLOW), Items.WOOL.pick(DyeColor.LIME), Items.WOOL.pick(DyeColor.PINK), Items.WOOL.pick(DyeColor.GRAY), Items.WOOL.pick(DyeColor.LIGHT_GRAY), Items.WOOL.pick(DyeColor.CYAN), Items.WOOL.pick(DyeColor.PURPLE), Items.WOOL.pick(DyeColor.BLUE), Items.WOOL.pick(DyeColor.BROWN), Items.WOOL.pick(DyeColor.GREEN), Items.WOOL.pick(DyeColor.RED), Items.WOOL.pick(DyeColor.BLACK), Items.CARPET.pick(DyeColor.WHITE), Items.CARPET.pick(DyeColor.ORANGE), Items.CARPET.pick(DyeColor.MAGENTA), Items.CARPET.pick(DyeColor.LIGHT_BLUE), Items.CARPET.pick(DyeColor.YELLOW), Items.CARPET.pick(DyeColor.LIME), Items.CARPET.pick(DyeColor.PINK), Items.CARPET.pick(DyeColor.GRAY), Items.CARPET.pick(DyeColor.LIGHT_GRAY), Items.CARPET.pick(DyeColor.CYAN), Items.CARPET.pick(DyeColor.PURPLE), Items.CARPET.pick(DyeColor.BLUE), Items.CARPET.pick(DyeColor.BROWN), Items.CARPET.pick(DyeColor.GREEN), Items.CARPET.pick(DyeColor.RED), Items.CARPET.pick(DyeColor.BLACK), Items.BED.pick(DyeColor.WHITE), Items.BED.pick(DyeColor.ORANGE), Items.BED.pick(DyeColor.MAGENTA), Items.BED.pick(DyeColor.LIGHT_BLUE), Items.BED.pick(DyeColor.YELLOW), Items.BED.pick(DyeColor.LIME), Items.BED.pick(DyeColor.PINK), Items.BED.pick(DyeColor.GRAY), Items.BED.pick(DyeColor.LIGHT_GRAY), Items.BED.pick(DyeColor.CYAN), Items.BED.pick(DyeColor.PURPLE), Items.BED.pick(DyeColor.BLUE), Items.BED.pick(DyeColor.BROWN), Items.BED.pick(DyeColor.GREEN), Items.BED.pick(DyeColor.RED), Items.BED.pick(DyeColor.BLACK), Items.BANNER.pick(DyeColor.WHITE), Items.BANNER.pick(DyeColor.ORANGE), Items.BANNER.pick(DyeColor.YELLOW), Items.BANNER.pick(DyeColor.LIGHT_GRAY), Items.BANNER.pick(DyeColor.RED), Items.BANNER.pick(DyeColor.BROWN), Items.BANNER.pick(DyeColor.MAGENTA), Items.BANNER.pick(DyeColor.LIGHT_BLUE), Items.BANNER.pick(DyeColor.LIME), Items.BANNER.pick(DyeColor.PINK), Items.BANNER.pick(DyeColor.GRAY), Items.BANNER.pick(DyeColor.CYAN), Items.BANNER.pick(DyeColor.PURPLE), Items.BANNER.pick(DyeColor.BLUE), Items.BANNER.pick(DyeColor.GREEN), Items.BANNER.pick(DyeColor.BLACK), Items.PAINTING), List.of(Items.WOOL.pick(DyeColor.WHITE))),
    工具匠(Items.SMITHING_TABLE, (ResourceKey<VillagerProfession>)VillagerProfession.TOOLSMITH, Set.of(Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_PICKAXE), List.of(Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_PICKAXE)),
    武器匠(Items.GRINDSTONE, (ResourceKey<VillagerProfession>)VillagerProfession.WEAPONSMITH, Set.of(Items.DIAMOND_AXE, Items.DIAMOND_SWORD), List.of(Items.DIAMOND_AXE, Items.DIAMOND_SWORD));

    private final Item item;
    private final ResourceKey<VillagerProfession> profession;
    private final Set<Item> canVillagerItemSet;
    private final List<Item> defaultVillagerItemList;

    VillagerType(Item item, ResourceKey<VillagerProfession> profession, Set<Item> canVillagerItemSet, List<Item> defaultVillagerItemList) {
        this.item = item;
        this.profession = profession;
        this.canVillagerItemSet = canVillagerItemSet;
        this.defaultVillagerItemList = defaultVillagerItemList;
    }

    public Item getItem() {
        return this.item;
    }

    public ResourceKey<VillagerProfession> getProfession() {
        return this.profession;
    }

    public Set<Item> getCanVillagerItemSet() {
        return this.canVillagerItemSet;
    }

    public List<Item> getDefaultVillagerItemList() {
        return this.defaultVillagerItemList;
    }

    public static VillagerType fromEntry(Holder<VillagerProfession> registryEntry) {
        for (VillagerType type : VillagerType.values()) {
            if (!registryEntry.is(type.getProfession())) continue;
            return type;
        }
        return null;
    }
}
