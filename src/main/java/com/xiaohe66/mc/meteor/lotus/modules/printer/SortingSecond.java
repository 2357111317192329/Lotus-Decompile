package com.xiaohe66.mc.meteor.lotus.modules.printer;

import java.util.Comparator;
import net.minecraft.util.math.BlockPos;

public enum SortingSecond {
   None(SortAlgorithm.None.algorithm),
   最近的(SortAlgorithm.最近的.algorithm),
   最远的(SortAlgorithm.最远的.algorithm);

   public final Comparator<BlockPos> algorithm;

   SortingSecond(Comparator<BlockPos> algorithm) {
      this.algorithm = algorithm;
   }
}
