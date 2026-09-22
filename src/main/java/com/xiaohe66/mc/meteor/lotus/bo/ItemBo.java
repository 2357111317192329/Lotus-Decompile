/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.utils.misc.Names
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.component.type.PotionContentsComponent
 *  net.minecraft.enchantment.Enchantment
 *  net.minecraft.registry.RegistryKey
 *  net.minecraft.component.type.MapIdComponent
 *  net.minecraft.component.DataComponentTypes
 */
package com.xiaohe66.mc.meteor.lotus.bo;

import com.xiaohe66.mc.meteor.lotus.util.EnchantmentUtils;
import java.util.Objects;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;

public class ItemBo {
    private final Item item;
    private final String name;
    private final RegistryKey<Enchantment> enchantment;
    private final PotionContentsComponent potionContents;
    private final Integer mapId;

    public ItemBo(ItemStack itemStack) {
        this(itemStack, false);
    }

    public ItemBo(ItemStack itemStack, boolean haveName) {
        this.item = itemStack.getItem();
        this.name = haveName && itemStack.getCustomName() != null ? itemStack.getCustomName().getString() : null;
        this.enchantment = itemStack.getItem() == Items.ENCHANTED_BOOK ? EnchantmentUtils.getEnchantmentOne(itemStack) : null;
        PotionContentsComponent potion = (PotionContentsComponent)itemStack.get(DataComponentTypes.POTION_CONTENTS);
        this.potionContents = potion != null && !potion.equals(PotionContentsComponent.DEFAULT) ? potion : null;
        MapIdComponent mapIdComponent = (MapIdComponent)itemStack.get(DataComponentTypes.MAP_ID);
        this.mapId = mapIdComponent != null ? Integer.valueOf(mapIdComponent.id()) : null;
    }

    public ItemBo(Item item) {
        this.item = item;
        this.name = null;
        this.enchantment = null;
        this.potionContents = null;
        this.mapId = null;
    }

    public static ItemBo of(Item item) {
        return new ItemBo(item);
    }

    public ItemBo(Item item, RegistryKey<Enchantment> enchantment) {
        this.item = item;
        this.name = null;
        this.enchantment = enchantment;
        this.potionContents = null;
        this.mapId = null;
    }

    public Item getItem() {
        return this.item;
    }

    public RegistryKey<Enchantment> getEnchantment() {
        return this.enchantment;
    }

    public PotionContentsComponent getPotionContents() {
        return this.potionContents;
    }

    public Integer getMapId() {
        return this.mapId;
    }

    public String getName() {
        return this.item == Items.ENCHANTED_BOOK ? Names.get(this.enchantment) : Names.get((Item)this.item);
    }

    public boolean isSameItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        Item testItem = itemStack.getItem();
        if (!Objects.equals(this.item, testItem)) {
            return false;
        }
        if (testItem == Items.ENCHANTED_BOOK) {
            RegistryKey<Enchantment> testEnchantment = EnchantmentUtils.getEnchantmentOne(itemStack);
            return Objects.equals(this.enchantment, testEnchantment);
        }
        PotionContentsComponent testPotion = (PotionContentsComponent)itemStack.get(DataComponentTypes.POTION_CONTENTS);
        PotionContentsComponent testPotionNormalized = testPotion != null && !testPotion.equals(PotionContentsComponent.DEFAULT) ? testPotion : null;
        if (!Objects.equals(this.potionContents, testPotionNormalized)) {
            return false;
        }
        MapIdComponent testMapIdComponent = (MapIdComponent)itemStack.get(DataComponentTypes.MAP_ID);
        Integer testMapId = testMapIdComponent != null ? Integer.valueOf(testMapIdComponent.id()) : null;
        return Objects.equals(this.mapId, testMapId);
    }

    public int getMaxStackSize() {
        return this.item.getMaxCount() * 27;
    }

    public boolean equals(Object o) {
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        ItemBo itemBo = (ItemBo)o;
        return Objects.equals(this.item, itemBo.item) && Objects.equals(this.name, itemBo.name) && Objects.equals(this.enchantment, itemBo.enchantment) && Objects.equals(this.potionContents, itemBo.potionContents) && Objects.equals(this.mapId, itemBo.mapId);
    }

    public int hashCode() {
        int result = Objects.hashCode(this.item);
        result = 31 * result + Objects.hashCode(this.name);
        result = 31 * result + Objects.hashCode(this.enchantment);
        result = 31 * result + Objects.hashCode(this.potionContents);
        return 31 * result + Objects.hashCode(this.mapId);
    }
}
