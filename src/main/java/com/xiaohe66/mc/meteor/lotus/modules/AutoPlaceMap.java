/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.events.render.Render3DEvent
 *  meteordevelopment.meteorclient.renderer.ShapeMode
 *  meteordevelopment.meteorclient.settings.EnumSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.utils.player.FindItemResult
 *  meteordevelopment.meteorclient.utils.player.InvUtils
 *  meteordevelopment.meteorclient.utils.render.color.Color
 *  meteordevelopment.meteorclient.utils.world.BlockUtils
 *  meteordevelopment.orbit.EventHandler
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.minecraft.util.Hand
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.decoration.ItemFrameEntity
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.Direction
 *  net.minecraft.util.math.Box
 *  net.minecraft.util.math.Vec3i
 *  net.minecraft.util.hit.HitResult$Type
 *  net.minecraft.util.hit.BlockHitResult
 *  net.minecraft.client.network.ClientPlayerEntity
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.modules.StepModule;
import com.xiaohe66.mc.meteor.lotus.modules.placemap.PlaceMapPos;

import com.xiaohe66.mc.meteor.lotus.modules.placemap.AutoPlaceOrder;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.util.Hand;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public class AutoPlaceMap extends StepModule {
    private final Setting<AutoPlaceOrder> placeOrder = sgGeneral.add(new EnumSetting.Builder<AutoPlaceOrder>()
        .name("放置顺序")
        .description("放置地图画的顺序")
        .defaultValue(AutoPlaceOrder.先竖后横)
        .build());
    private boolean wasRightClicking;
    private BlockPos blockPos1;
    private BlockPos blockPos2;
    private Direction playerDirection;
    private final List<PlaceMapPos> placePosList;
    private int placeIndex;

    public AutoPlaceMap() {
        super("自动贴画", "开启功能后, 给2个角放置展示框后自动贴画。由<hn2>友情赞助开发");
        this.wasRightClicking = false;
        this.placePosList = new ArrayList<PlaceMapPos>();
        this.addStep(Steps.PREPARE, this::preparePlace);
        this.addStep(Steps.PLACING, this::place);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (this.mc.player == null || !this.isActive() || this.step == Steps.PLACING) {
                return;
            }
            boolean rightClicking = this.mc.options.useKey.isPressed();
            if (rightClicking && !this.wasRightClicking) {
                this.checkItemFramePlacement();
            }
            this.wasRightClicking = rightClicking;
        });
    }

    public void onActivate() {
        this.init();
    }

    private void preparePlace() {
        ItemFrameEntity firstItemFrame = this.getItemFrameAtPosition(this.blockPos1);
        ItemFrameEntity secondItemFrame = this.getItemFrameAtPosition(this.blockPos2);
        if (firstItemFrame == null || secondItemFrame == null) {
            return;
        }
        Direction frameDirection = firstItemFrame.getFacing();
        if (frameDirection != secondItemFrame.getFacing()) {
            this.warning("方向不一致", new Object[0]);
            return;
        }
        this.playerDirection = frameDirection.getOpposite();
        this.placePosList.clear();
        this.readyPlacePos();
        this.readyMap();
        this.setDelay();
        this.placeIndex = 1;
        this.step = Steps.PLACING;
    }

    private void place() {
        int startIndex = this.placeIndex;
        do {
            PlaceMapPos placeMapPos;
            boolean success;
            if (!(success = this.doPlace(placeMapPos = this.placePosList.get(this.placeIndex)))) {
                return;
            }
            ++this.placeIndex;
            if (this.placeIndex < this.placePosList.size()) continue;
            this.placeIndex = 0;
        } while (this.placeIndex != startIndex);
        this.warning("放置完毕", new Object[0]);
        this.toggle();
    }

    /*
     * Enabled aggressive block sorting
     */
    private boolean doPlace(PlaceMapPos placeMapPos) {
        BlockPos placePos = placeMapPos.getBlockPos();
        ItemFrameEntity itemFrame = this.getItemFrameAtPosition(placePos);
        if (itemFrame == null) {
            FindItemResult findItemResult = this.findFrame();
            if (findItemResult.found()) {
                HeInvUtils.swapToSelectedSlot(findItemResult.slot());
                BlockUtils.place((BlockPos)placePos, (FindItemResult)findItemResult, (int)0);
                HeInvUtils.swapToSelectedSlot(findItemResult.slot());
                HeInvUtils.sendCloseScreenPacket();
                this.setDelay();
                return false;
            }
            this.info("缺少<展示框>", new Object[0]);
            this.toggle();
            return false;
        }
        ItemStack heldItemStack = itemFrame.getHeldItemStack();
        if (!heldItemStack.isEmpty()) {
            return true;
        }
        FindItemResult findItemResult = this.findMap(placeMapPos.getName());
        if (!findItemResult.found()) {
            this.info("缺少<地图画>:" + placeMapPos.getName(), new Object[0]);
            this.setDelay();
            return false;
        }
        if (!findItemResult.isHotbar()) {
            HeInvUtils.swap(findItemResult.slot(), 7);
            return false;
        }
        if (findItemResult.getHand() == null) {
            InvUtils.swap((int)findItemResult.slot(), (boolean)false);
            return false;
        }
        this.interactEntity((Entity)itemFrame);
        this.setDelay();
        return true;
    }

    private FindItemResult findMap(String mapName) {
        return InvUtils.find(itemStack -> itemStack.getItem() == Items.FILLED_MAP && itemStack.getCustomName() != null && mapName.equals(itemStack.getCustomName().getString()));
    }

    private FindItemResult findFrame() {
        return InvUtils.find(itemStack -> itemStack.getItem() == Items.ITEM_FRAME || itemStack.getItem() == Items.GLOW_ITEM_FRAME);
    }

    private void checkItemFramePlacement() {
        ClientPlayerEntity player = this.mc.player;
        ItemStack mainHandStack = player.getStackInHand(Hand.MAIN_HAND);
        ItemStack offHandStack = player.getStackInHand(Hand.OFF_HAND);
        boolean hasFrame = mainHandStack.isOf(Items.ITEM_FRAME) || offHandStack.isOf(Items.ITEM_FRAME) || mainHandStack.isOf(Items.GLOW_ITEM_FRAME) || offHandStack.isOf(Items.GLOW_ITEM_FRAME);
        if (!hasFrame) {
            return;
        }
        if (this.mc.crosshairTarget == null || this.mc.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockHitResult blockHit = (BlockHitResult)this.mc.crosshairTarget;
        BlockPos blockPos = blockHit.getBlockPos();
        Direction side = blockHit.getSide();
        BlockPos framePos = blockPos.offset(side);
        if (this.blockPos1 == null) {
            this.blockPos1 = new BlockPos((Vec3i)framePos);
        } else {
            this.blockPos2 = new BlockPos((Vec3i)framePos);
            this.step = Steps.PREPARE;
        }
    }

    private void readyMap() {
        Text customName;
        ItemStack mapStack;
        int i;
        ArrayList<String> mapNames = new ArrayList<String>();
        for (i = 0; i < 36; ++i) {
            mapStack = this.getItemStack(i);
            if (mapStack.getItem() != Items.FILLED_MAP || (customName = mapStack.getCustomName()) == null) continue;
            mapNames.add(customName.getString());
        }
        if (mapNames.size() < this.placePosList.size()) {
            this.info("地图画数量不对", new Object[0]);
            this.toggle();
        } else {
            mapNames.sort(null);
            for (i = 0; i < this.placePosList.size(); ++i) {
                PlaceMapPos placeMapPos = this.placePosList.get(i);
                String mapName = mapNames.get(i);
                placeMapPos.setName(mapName);
            }
        }
    }

    private void readyPlacePos() {
        block29: {
            block31: {
                block30: {
                    block28: {
                        if (this.playerDirection != Direction.NORTH) break block28;
                        if (this.blockPos1.getZ() != this.blockPos2.getZ()) {
                            this.info("不在平面上", new Object[0]);
                            this.toggle();
                            return;
                        }
                        if (this.placeOrder.get() == AutoPlaceOrder.先竖后横) {
                            int endX = this.blockPos2.getX();
                            for (int x = this.blockPos1.getX(); x <= endX; ++x) {
                                int endY = this.blockPos2.getY();
                                for (int y = this.blockPos1.getY(); y >= endY; --y) {
                                    this.placePosList.add(new PlaceMapPos(x, y, this.blockPos1.getZ()));
                                }
                            }
                        } else {
                            int endY = this.blockPos2.getY();
                            for (int y = this.blockPos1.getY(); y >= endY; --y) {
                                int endX = this.blockPos2.getX();
                                for (int x = this.blockPos1.getX(); x <= endX; ++x) {
                                    this.placePosList.add(new PlaceMapPos(x, y, this.blockPos1.getZ()));
                                }
                            }
                        }
                        break block29;
                    }
                    if (this.playerDirection != Direction.SOUTH) break block30;
                    if (this.blockPos1.getZ() != this.blockPos2.getZ()) {
                        this.info("不在平面上", new Object[0]);
                        this.toggle();
                        return;
                    }
                    if (this.placeOrder.get() == AutoPlaceOrder.先竖后横) {
                        int endX = this.blockPos2.getX();
                        for (int x = this.blockPos1.getX(); x >= endX; --x) {
                            int endY = this.blockPos2.getY();
                            for (int y = this.blockPos1.getY(); y >= endY; --y) {
                                this.placePosList.add(new PlaceMapPos(x, y, this.blockPos1.getZ()));
                            }
                        }
                    } else {
                        int endY = this.blockPos2.getY();
                        for (int y = this.blockPos1.getY(); y >= endY; --y) {
                            int endX = this.blockPos2.getX();
                            for (int x = this.blockPos1.getX(); x >= endX; --x) {
                                this.placePosList.add(new PlaceMapPos(x, y, this.blockPos1.getZ()));
                            }
                        }
                    }
                    break block29;
                }
                if (this.playerDirection != Direction.WEST) break block31;
                if (this.blockPos1.getX() != this.blockPos2.getX()) {
                    this.info("不在平面上", new Object[0]);
                    this.toggle();
                    return;
                }
                if (this.placeOrder.get() == AutoPlaceOrder.先竖后横) {
                    int endZ = this.blockPos2.getZ();
                    for (int z = this.blockPos1.getZ(); z >= endZ; --z) {
                        int endY = this.blockPos2.getY();
                        for (int y = this.blockPos1.getY(); y >= endY; --y) {
                            this.placePosList.add(new PlaceMapPos(this.blockPos1.getX(), y, z));
                        }
                    }
                } else {
                    int endY = this.blockPos2.getY();
                    for (int y = this.blockPos1.getY(); y >= endY; --y) {
                        int endZ = this.blockPos2.getZ();
                        for (int z = this.blockPos1.getZ(); z >= endZ; --z) {
                            this.placePosList.add(new PlaceMapPos(this.blockPos1.getX(), y, z));
                        }
                    }
                }
                break block29;
            }
            if (this.playerDirection != Direction.EAST) break block29;
            if (this.blockPos1.getX() != this.blockPos2.getX()) {
                this.info("不在平面上", new Object[0]);
                this.toggle();
                return;
            }
            if (this.placeOrder.get() == AutoPlaceOrder.先竖后横) {
                int endZ = this.blockPos2.getZ();
                for (int z = this.blockPos1.getZ(); z <= endZ; ++z) {
                    int endY = this.blockPos2.getY();
                    for (int y = this.blockPos1.getY(); y >= endY; --y) {
                        this.placePosList.add(new PlaceMapPos(this.blockPos1.getX(), y, z));
                    }
                }
            } else {
                int endY = this.blockPos2.getY();
                for (int y = this.blockPos1.getY(); y >= endY; --y) {
                    int endZ = this.blockPos2.getZ();
                    for (int z = this.blockPos1.getZ(); z <= endZ; ++z) {
                        this.placePosList.add(new PlaceMapPos(this.blockPos1.getX(), y, z));
                    }
                }
            }
        }
    }

    public ItemFrameEntity getItemFrameAtPosition(BlockPos framePos) {
        if (this.mc.world == null) {
            return null;
        }
        Box searchBox = new Box((double)framePos.getX(), (double)framePos.getY(), (double)framePos.getZ(), (double)(framePos.getX() + 1), (double)(framePos.getY() + 1), (double)(framePos.getZ() + 1));
        List itemFrames = this.mc.world.getEntitiesByClass(ItemFrameEntity.class, searchBox, itemFrame -> itemFrame.getBlockPos().equals(framePos));
        return itemFrames.isEmpty() ? null : (ItemFrameEntity)itemFrames.getFirst();
    }

    @EventHandler
    public void onRenderWorld(Render3DEvent event) {
        if (this.placePosList.isEmpty()) {
            if (this.blockPos1 != null) {
                event.renderer.box(this.blockPos1, Color.ORANGE, Color.BLUE, ShapeMode.Lines, 0);
            }
        } else {
            for (PlaceMapPos placeMapPos : this.placePosList) {
                event.renderer.box(placeMapPos.getBlockPos(), Color.ORANGE, Color.BLUE, ShapeMode.Lines, 0);
            }
        }
    }

    private void init() {
        this.blockPos1 = null;
        this.blockPos2 = null;
        this.placePosList.clear();
        this.step = Steps.INITIAL;
    }

    @Override
    public void onDeactivate() {
        this.init();
    }
}
