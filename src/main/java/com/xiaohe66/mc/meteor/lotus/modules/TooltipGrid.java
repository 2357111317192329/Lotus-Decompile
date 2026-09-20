/*
 * Port of Lotus 19.5 aL (tooltip layout state, decompiled bytecode-accurate).
 * Holds the anchor position (x/y = pinned slot), the computed tooltip
 * rectangle (tx/ty/tw/th, filled in by the tooltip positioner) and the list
 * of slot rectangles inside one layer of the pinned preview.
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import java.util.ArrayList;
import java.util.List;

public class TooltipGrid {
    public int x;
    public int y;
    public int tx;
    public int ty;
    public int tw;
    public int th;
    public final List<SlotRect> rects = new ArrayList<>();

    public boolean isPointInTooltip(int mouseX, int mouseY) {
        return mouseX >= this.tx && mouseX < this.tx + this.tw && mouseY >= this.ty && mouseY < this.ty + this.th;
    }
}