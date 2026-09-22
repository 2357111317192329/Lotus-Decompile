package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.bo.StoragePos;
import com.xiaohe66.mc.meteor.lotus.modules.step.Step;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeRotationUtils;
import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import java.util.Set;
import java.util.function.Predicate;

public abstract class WarehouseModule extends WalkModule {
    public static final Set<Item> PICKAXES = Set.of(Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);

    public WarehouseModule(String title, String description) {
        super(title, description);
    }

    protected void putKit(StoragePos pos) {
        ShulkerBoxReader emptyBox = this.findShulkerInPlayer(ShulkerBoxReader::isEmpty);
        if (emptyBox == null) {
            this.closeNext(Steps.NEXT);
        } else if (this.notInOperationRange(pos)) {
            this.gotoBtnPos(pos, "<存kit>距离不够，尝试移动", Steps.PUT_EMPTY_KIT);
        } else {
            this.openChest(pos.getPutPos(), containerMenu -> {
                if (this.isContainerFull()) {
                    this.breakStep("<存kit>箱满，无法存放");
                } else {
                    InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                    this.delayCloseNext(Steps.NEXT);
                }
            });
        }
    }

    protected void takeEmptyKit(StoragePos pos, int count) {
        int emptyBoxCount = 0;
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = this.getItemStack(i);
            if (HeItemUtils.isShulkerBox(stack.getItem())) {
                ShulkerBoxReader reader = new ShulkerBoxReader(stack);
                if (reader.isEmpty()) {
                    ++emptyBoxCount;
                }
            }
        }
        if (emptyBoxCount >= count) {
            this.closeNext(Steps.PLACE_EMPTY_KIT);
        } else if (this.notInOperationRange(pos)) {
            this.gotoBtnPos(pos, "<拿空盒>距离不够，尝试移动", Steps.TAKE_EMPTY_KIT);
        } else {
            this.openChest(pos.getTakePos(), containerMenu -> {
                ShulkerBoxReader emptyBox = this.findShulkerInScreen(ShulkerBoxReader::isEmpty);
                if (emptyBox != null) {
                    InvUtils.shiftClick().slotId(this.getCurScreenSlot());
                    this.setDelay();
                } else {
                    this.error("<空盒箱>缺少空盒子", new Object[0]);
                    this.toggle();
                }
            });
        }
    }

    protected void placeEmptyKit(StoragePos pos) {
        BlockState state = this.mc.world.getBlockState(pos.getKitPos());
        if (state.getBlock() instanceof ShulkerBoxBlock) {
            this.step = Steps.PUT_ITEM;
        } else if (!state.isAir()) {
            this.breakStep("<位置>被占用: " + Names.get(pos.getItem().getItem()));
        } else {
            ShulkerBoxReader emptyBox = this.findShulkerInPlayer(ShulkerBoxReader::isEmpty);
            if (emptyBox == null) {
                this.step = Steps.TAKE_EMPTY_KIT;
            } else if (this.notInOperationRange(pos)) {
                this.gotoTarget(pos.getBtnPos(), 0, Steps.PLACE_EMPTY_KIT);
            } else {
                int curSlot = this.getCurPlayerSlot();
                HeInvUtils.withItemInHand(curSlot, () -> HeBlockUtils.clickAdjacentBlock(pos.getKitPos()));
                this.delayNext(Steps.PUT_ITEM);
            }
        }
    }

    protected void placeKit(StoragePos pos, Predicate<ShulkerBoxReader> predicate) {
        BlockState state = this.mc.world.getBlockState(pos.getKitPos());
        if (state.getBlock() instanceof ShulkerBoxBlock) {
            this.step = Steps.TAKE_ITEM;
        } else if (!state.isAir()) {
            this.breakStep("<位置>被占用: " + Names.get(pos.getItem().getItem()));
        } else {
            ShulkerBoxReader kit = this.findShulkerInPlayer(predicate);
            if (kit == null) {
                this.step = Steps.TAKE_KIT;
            } else if (this.notInOperationRange(pos)) {
                this.gotoBtnPos(pos, "去拿kit", Steps.PLACE_KIT);
            } else {
                int curSlot = this.getCurPlayerSlot();
                HeInvUtils.withItemInHand(curSlot, () -> HeBlockUtils.clickAdjacentBlock(pos.getKitPos()));
                this.delayNext(Steps.TAKE_ITEM);
            }
        }
    }

    protected void putKit(StoragePos pos, Predicate<ShulkerBoxReader> predicate) {
        if (this.notInOperationRange(pos)) {
            this.gotoBtnPos(pos, "<存kit>距离不够，尝试移动", Steps.PUT_KIT);
        } else {
            ShulkerBoxReader kit = this.findShulkerInPlayer(predicate);
            if (kit == null) {
                this.warning("放kit, 但身上没有...", new Object[0]);
                this.step = Steps.NEXT;
            } else {
                this.openChest(pos.getPutPos(), containerMenu -> {
                    if (this.isContainerFull()) {
                        this.error("<存kit>箱满，无法存放", new Object[0]);
                        this.toggle();
                    } else {
                        InvUtils.shiftClick().slot(this.getCurPlayerSlot());
                        this.delayCloseNext(Steps.NEXT);
                    }
                });
            }
        }
    }

    protected void takeKit(StoragePos pos, Predicate<ShulkerBoxReader> predicate) {
        if (this.notInOperationRange(pos)) {
            this.gotoBtnPos(pos, "<拿kit>距离不够，尝试移动", Steps.TAKE_KIT);
        } else {
            this.openChest(pos.getTakePos(), containerMenu -> {
                ShulkerBoxReader kit = this.findShulkerInScreen(predicate);
                if (kit == null) {
                    this.error("库存不足", new Object[0]);
                    this.toggle();
                } else {
                    InvUtils.shiftClick().slotId(this.getCurScreenSlot());
                    this.delayCloseNext(Steps.PLACE_KIT);
                }
            });
        }
    }

    protected void breakKit(StoragePos pos) {
        if (this.notInOperationRange(pos)) {
            this.gotoBtnPos(pos, "<挖盒子>距离不够, 尝试移动", Steps.BREAK_KIT);
        } else {
            BlockPos kitPos = pos.getKitPos();
            BlockState state = this.mc.world.getBlockState(kitPos);
            if (!HeItemUtils.isShulkerBox(state.getBlock().asItem())) {
                this.step = Steps.NEXT;
                this.setDelay(20);
            } else {
                this.swapToMainHand(stack -> PICKAXES.contains(stack.getItem()));
                HeRotationUtils.rotate(kitPos, () -> BlockUtils.breakBlock(kitPos, true));
            }
        }
    }
}
