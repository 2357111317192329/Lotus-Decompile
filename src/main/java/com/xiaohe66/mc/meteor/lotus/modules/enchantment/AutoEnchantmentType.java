package com.xiaohe66.mc.meteor.lotus.modules.enchantment;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public enum AutoEnchantmentType {
   自定义(),
   精准工具(Enchantments.EFFICIENCY, Enchantments.UNBREAKING, Enchantments.MENDING, Enchantments.SILK_TOUCH),
   时运工具(Enchantments.FORTUNE, Enchantments.UNBREAKING, Enchantments.MENDING, Enchantments.EFFICIENCY),
   生电剑(Enchantments.SWEEPING_EDGE, Enchantments.LOOTING, Enchantments.UNBREAKING, Enchantments.SHARPNESS, Enchantments.MENDING),
   击退剑(
      Enchantments.SWEEPING_EDGE,
      Enchantments.LOOTING,
      Enchantments.SHARPNESS,
      Enchantments.FIRE_ASPECT,
      Enchantments.UNBREAKING,
      Enchantments.KNOCKBACK,
      Enchantments.MENDING
   ),
   头盔(Enchantments.RESPIRATION, Enchantments.PROTECTION, Enchantments.AQUA_AFFINITY, Enchantments.UNBREAKING, Enchantments.MENDING),
   胸甲(Enchantments.PROTECTION, Enchantments.UNBREAKING, Enchantments.MENDING),
   荆棘胸甲(Enchantments.THORNS, Enchantments.UNBREAKING, Enchantments.MENDING, Enchantments.PROTECTION),
   裤子(Enchantments.PROTECTION, Enchantments.UNBREAKING, Enchantments.MENDING),
   爆炸裤子(Enchantments.BLAST_PROTECTION, Enchantments.UNBREAKING, Enchantments.MENDING),
   鞋子(Enchantments.DEPTH_STRIDER, Enchantments.PROTECTION, Enchantments.UNBREAKING, Enchantments.FEATHER_FALLING, Enchantments.MENDING),
   爆炸鞋子(Enchantments.BLAST_PROTECTION, Enchantments.DEPTH_STRIDER, Enchantments.UNBREAKING, Enchantments.FEATHER_FALLING, Enchantments.MENDING),
   鞘翅(Enchantments.UNBREAKING, Enchantments.MENDING),
   三叉戟(Enchantments.IMPALING, Enchantments.CHANNELING, Enchantments.LOYALTY, Enchantments.UNBREAKING, Enchantments.MENDING),
   无限弓(Enchantments.POWER, Enchantments.INFINITY, Enchantments.UNBREAKING);

   private final Set<ResourceKey<Enchantment>> enchantments;

   @SafeVarargs
   AutoEnchantmentType(ResourceKey<Enchantment>... enchantments) {
      if (enchantments != null && enchantments.length > 0) {
         this.enchantments = Arrays.stream(enchantments).filter(Objects::nonNull).collect(Collectors.toSet());
      } else {
         this.enchantments = Arrays.stream(enchantments).filter(Objects::nonNull).collect(Collectors.toSet());
         //this.enchantments = Collections.emptySet();
      }
   }

   public Set<ResourceKey<Enchantment>> getAll() {
      return this.enchantments;
   }
}
