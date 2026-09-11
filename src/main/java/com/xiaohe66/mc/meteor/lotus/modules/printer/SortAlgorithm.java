/*
 * Decompiled with CFR 0.152.
 */
package com.xiaohe66.mc.meteor.lotus.modules.printer;

import com.xiaohe66.mc.meteor.lotus.modules.printer.PlaceBlockHelper;
import java.util.Comparator;
import java.util.List;

public enum SortAlgorithm {
    远处 {
        @Override
        public void sort(List<PlaceBlockHelper> blocks) {
            blocks.sort((block1, block2) -> Double.compare(block2.getDistance(), block1.getDistance()));
        }
    },
    近处 {
        @Override
        public void sort(List<PlaceBlockHelper> blocks) {
            blocks.sort(Comparator.comparingDouble(BlockPosWarp::getDistance));
        }
    };

    public abstract void sort(List<PlaceBlockHelper> blocks);
}