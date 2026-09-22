/*
 * Port of Lotus 19.5 aK (slot rect, decompiled bytecode-accurate).
 * A record holding the screen-space rectangle of one stack shown in the
 * pinned preview tooltip:
 *   x/y      – top-left of the slot grid cell (used for hit testing, size cells)
 *   fillX/fillY – top-left of the 16x16 highlight fill (offset inside the cell)
 *   index    – index of the stack inside its container / bundle
 *   stack    – the ItemStack rendered in this cell
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import net.minecraft.item.ItemStack;

public record SlotRect(int x, int y, int size, int fillX, int fillY, int index, ItemStack stack) {
}