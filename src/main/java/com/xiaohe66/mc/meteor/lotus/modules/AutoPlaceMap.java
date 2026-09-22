/*
 * Decompiled with CFR 0.152.
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.placemap.AutoPlaceOrder;
import com.xiaohe66.mc.meteor.lotus.modules.placemap.PlaceMapPos;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.NumberNameComparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

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
        super("M自动贴画", "开启功能后, 给2个角放置展示框后自动贴画。支持从收纳袋取画, 超出手长范围的位置会等靠近后再贴。由<hn2>友情赞助开发");
        this.wasRightClicking = false;
        this.placePosList = new ArrayList<PlaceMapPos>();
        this.addStep(Steps.CHECK, this::preparePlace);
        this.addStep(Steps.PLACE, this::place);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (this.mc.player == null || !this.isActive() || this.step == Steps.PLACE) {
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
        this.step = Steps.PLACE;
    }

    private void place() {
        HashMap<String, Integer> needCount = new HashMap<String, Integer>();
        for (PlaceMapPos placeMapPos : this.placePosList) {
            ItemFrameEntity itemFrame = this.getItemFrameAtPosition(placeMapPos.getBlockPos());
            if (itemFrame == null || itemFrame.getHeldItemStack().isEmpty()) {
                needCount.merge(placeMapPos.getName(), 1, Integer::sum);
            }
        }
        if (needCount.isEmpty()) {
            this.warning("放置完毕", new Object[0]);
            this.toggle();
            return;
        }
        Set<String> missingNames = this.subtractInventoryCounts(needCount);
        if (missingNames.isEmpty() || !this.moveMissingToInventory(missingNames)) {
            PlaceMapPos mainHandPos = null;
            ItemFrameEntity mainHandFrame = null;
            PlaceMapPos hotbarPos = null;
            ItemFrameEntity hotbarFrame = null;
            PlaceMapPos otherPos = null;
            ItemFrameEntity otherFrame = null;
            int size = this.placePosList.size();
            for (int i = 0; i < size; ++i) {
                PlaceMapPos placeMapPos = this.placePosList.get(this.placeIndex);
                ++this.placeIndex;
                if (this.placeIndex >= size) {
                    this.placeIndex = 0;
                }
                ItemFrameEntity itemFrame = this.getItemFrameAtPosition(placeMapPos.getBlockPos());
                if ((itemFrame == null || itemFrame.getHeldItemStack().isEmpty()) && !this.isOutOfReach(placeMapPos.getBlockPos(), itemFrame)) {
                    if (itemFrame == null) {
                        if (otherPos == null) {
                            otherPos = placeMapPos;
                        }
                    } else {
                        FindItemResult mapResult = this.findMap(placeMapPos.getName());
                        if (mapResult.found()) {
                            if (mapResult.isMainHand()) {
                                mainHandPos = placeMapPos;
                                mainHandFrame = itemFrame;
                                break;
                            }
                            if (mapResult.isHotbar()) {
                                if (hotbarPos == null) {
                                    hotbarPos = placeMapPos;
                                    hotbarFrame = itemFrame;
                                }
                            } else if (otherPos == null) {
                                otherPos = placeMapPos;
                                otherFrame = itemFrame;
                            }
                        }
                    }
                }
            }
            if (mainHandPos != null) {
                this.doPlace(mainHandPos, mainHandFrame);
            } else if (hotbarPos != null) {
                this.doPlace(hotbarPos, hotbarFrame);
            } else if (otherPos != null) {
                this.doPlace(otherPos, otherFrame);
            } else if (!missingNames.isEmpty() && this.findBundleSlotWithAny(missingNames) == -1) {
                this.info("缺少<地图画>, 身上和收纳袋中都没有: %s", new Object[]{missingNames});
            } else {
                this.info("剩余位置超出手长范围, 等待玩家靠近", new Object[0]);
                this.setDelay(20);
            }
        }
    }

    private Set<String> subtractInventoryCounts(Map<String, Integer> needCount) {
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = this.getItemStack(i);
            Text customName = stack.getCustomName();
            if (stack.getItem() == Items.FILLED_MAP && customName != null) {
                needCount.merge(customName.getString(), -stack.getCount(), Integer::sum);
            }
        }
        HashSet<String> missingNames = new HashSet<String>();
        needCount.forEach((name, count) -> {
            if (count > 0) {
                missingNames.add(name);
            }
        });
        return missingNames;
    }

    private void doPlace(PlaceMapPos placeMapPos, ItemFrameEntity itemFrame) {
        if (itemFrame == null) {
            FindItemResult frameResult = this.findFrame();
            if (frameResult.found()) {
                HeInvUtils.withItemInHand(frameResult.slot(), () -> BlockUtils.place(placeMapPos.getBlockPos(), frameResult, 0));
                this.setDelay();
            } else {
                this.info("缺少<展示框>", new Object[0]);
                this.toggle();
            }
        } else {
            FindItemResult mapResult = this.findMap(placeMapPos.getName());
            if (mapResult.found()) {
                if (mapResult.isHotbar()) {
                    if (mapResult.getHand() == null) {
                        InvUtils.swap(mapResult.slot(), false);
                    } else {
                        this.interactEntity((Entity)itemFrame);
                        this.setDelay();
                    }
                } else {
                    HeInvUtils.swap(mapResult.slot(), 7);
                }
            } else {
                this.info("缺少<地图画>:" + placeMapPos.getName(), new Object[0]);
                this.setDelay();
            }
        }
    }

    private boolean moveMissingToInventory(Set<String> missingNames) {
        if (!(this.mc.player.currentScreenHandler instanceof PlayerScreenHandler)) {
            HeInvUtils.closeCurScreen();
            this.setDelay();
            return true;
        }
        int bundleSlot = this.findBundleSlotWithAny(missingNames);
        if (bundleSlot == -1) {
            return false;
        }
        int emptySlot = this.getFirstEmptySlot();
        if (emptySlot == -1) {
            return false;
        }
        HeInvUtils.stopSprinting();
        HeInvUtils.moveOneFromSlot(SlotUtils.indexToId(bundleSlot), SlotUtils.indexToId(emptySlot));
        HeInvUtils.startSprinting();
        this.setDelay();
        return true;
    }

    private int findBundleSlotWithAny(Set<String> names) {
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = this.getItemStack(i);
            if (!(stack.getItem() instanceof BundleItem)) continue;
            BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (contents == null) continue;
            for (ItemStack bundleStack : contents.stream().toList()) {
                Text customName = bundleStack.getCustomName();
                if (bundleStack.getItem() == Items.FILLED_MAP && customName != null && names.contains(customName.getString())) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isOutOfReach(BlockPos pos, ItemFrameEntity itemFrame) {
        double range = itemFrame == null ? this.mc.player.getBlockInteractionRange() : this.mc.player.getEntityInteractionRange();
        return this.mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) > range * range;
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
            this.step = Steps.CHECK;
        }
    }

    private void readyMap() {
        int emptyFrameCount = 0;
        for (PlaceMapPos placeMapPos : this.placePosList) {
            ItemFrameEntity itemFrame = this.getItemFrameAtPosition(placeMapPos.getBlockPos());
            if (itemFrame != null && !itemFrame.getHeldItemStack().isEmpty()) {
                placeMapPos.setDone(true);
            } else {
                ++emptyFrameCount;
            }
        }
        ArrayList<String> mapNames = new ArrayList<String>();
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = this.getItemStack(i);
            if (stack.getItem() == Items.FILLED_MAP) {
                this.addMapNames(mapNames, stack);
                continue;
            }
            if (!(stack.getItem() instanceof BundleItem)) continue;
            BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (contents == null) continue;
            for (ItemStack bundleStack : contents.stream().toList()) {
                if (bundleStack.getItem() != Items.FILLED_MAP) continue;
                this.addMapNames(mapNames, bundleStack);
            }
        }
        if (mapNames.size() < emptyFrameCount) {
            this.info("地图画数量不对, 还需要[%s]张, 身上和收纳袋中只有[%s]张", new Object[]{emptyFrameCount, mapNames.size()});
            this.toggle();
        } else {
            mapNames.sort(NumberNameComparator.INSTANCE);
            int index = 0;
            for (PlaceMapPos placeMapPos : this.placePosList) {
                if (placeMapPos.isDone()) continue;
                placeMapPos.setName(mapNames.get(index));
                ++index;
            }
        }
    }

    private void addMapNames(List<String> mapNames, ItemStack stack) {
        Text customName = stack.getCustomName();
        if (customName == null) {
            return;
        }
        for (int i = 0; i < stack.getCount(); ++i) {
            mapNames.add(customName.getString());
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
        this.step = Steps.NONE;
    }

    @Override
    public void onDeactivate() {
        this.init();
    }
}