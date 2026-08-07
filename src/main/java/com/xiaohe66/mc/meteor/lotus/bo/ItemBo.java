package com.xiaohe66.mc.meteor.lotus.bo;

import com.xiaohe66.mc.meteor.lotus.util.EnchantmentUtils;
import java.util.Objects;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.saveddata.maps.MapId;

public class ItemBo {
   private final Item item;
   private final String name;
   private final ResourceKey<Enchantment> enchantment;
   private final PotionContents potionContents;
   private final Integer mapId;

   public ItemBo(ItemStack itemStack) {
      this(itemStack, false);
   }

   public ItemBo(ItemStack itemStack, boolean haveName) {
      this.item = itemStack.getItem();
      this.name = haveName && itemStack.getCustomName() != null ? itemStack.getCustomName().getString() : null;
      this.enchantment = itemStack.getItem() == Items.ENCHANTED_BOOK ? EnchantmentUtils.getEnchantmentOne(itemStack) : null;
      PotionContents potion = (PotionContents)itemStack.get(DataComponents.POTION_CONTENTS);
      this.potionContents = potion != null && !potion.equals(PotionContents.EMPTY) ? potion : null;
      MapId mapIdComponent = (MapId)itemStack.get(DataComponents.MAP_ID);
      this.mapId = mapIdComponent != null ? mapIdComponent.id() : null;
   }

   public ItemBo(Item item) {
      this.item = item;
      this.name = null;
      this.enchantment = null;
      this.potionContents = null;
      this.mapId = null;
   }

   public ItemBo(Item item, ResourceKey<Enchantment> enchantment) {
      this.item = item;
      this.name = null;
      this.enchantment = enchantment;
      this.potionContents = null;
      this.mapId = null;
   }

   public Item getItem() {
      return this.item;
   }

   public ResourceKey<Enchantment> getEnchantment() {
      return this.enchantment;
   }

   public PotionContents getPotionContents() {
      return this.potionContents;
   }

   public Integer getMapId() {
      return this.mapId;
   }

   public String getName() {
      return this.item == Items.ENCHANTED_BOOK ? Names.get(this.enchantment) : Names.get(this.item);
   }

   public boolean isSameItem(ItemStack itemStack) {
      if (itemStack != null && !itemStack.isEmpty()) {
         Item testItem = itemStack.getItem();
         if (!Objects.equals(this.item, testItem)) {
            return false;
         }

         if (testItem == Items.ENCHANTED_BOOK) {
            ResourceKey<Enchantment> testEnchantment = EnchantmentUtils.getEnchantmentOne(itemStack);
            return Objects.equals(this.enchantment, testEnchantment);
         }

         PotionContents testPotion = (PotionContents)itemStack.get(DataComponents.POTION_CONTENTS);
         PotionContents testPotionNormalized = testPotion != null && !testPotion.equals(PotionContents.EMPTY) ? testPotion : null;
         if (!Objects.equals(this.potionContents, testPotionNormalized)) {
            return false;
         }

         MapId testMapIdComponent = (MapId)itemStack.get(DataComponents.MAP_ID);
         Integer testMapId = testMapIdComponent != null ? testMapIdComponent.id() : null;
         return Objects.equals(this.mapId, testMapId);
      } else {
         return false;
      }
   }

   @Override
   public boolean equals(Object o) {
      if (o != null && this.getClass() == o.getClass()) {
         ItemBo itemBo = (ItemBo)o;
         return Objects.equals(this.item, itemBo.item)
            && Objects.equals(this.name, itemBo.name)
            && Objects.equals(this.enchantment, itemBo.enchantment)
            && Objects.equals(this.potionContents, itemBo.potionContents)
            && Objects.equals(this.mapId, itemBo.mapId);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(this.item);
      result = 31 * result + Objects.hashCode(this.name);
      result = 31 * result + Objects.hashCode(this.enchantment);
      result = 31 * result + Objects.hashCode(this.potionContents);
      return 31 * result + Objects.hashCode(this.mapId);
   }
}
