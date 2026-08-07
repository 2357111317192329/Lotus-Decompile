package com.xiaohe66.mc.meteor.lotus.util;

import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class Const {
   public static final String NAME = "xiaohe66-meteor-lotus";
   public static final Category CATEGORY = new Category("Lotus", ()-> DisplayItemUtils.toStack(Items.LILY_PAD));
}
