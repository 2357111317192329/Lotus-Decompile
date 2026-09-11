/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.events.render.Render3DEvent
 *  meteordevelopment.meteorclient.settings.BlockPosSetting$Builder
 *  meteordevelopment.meteorclient.settings.BoolSetting$Builder
 *  meteordevelopment.meteorclient.settings.DoubleSetting$Builder
 *  meteordevelopment.meteorclient.settings.EnumSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.utils.player.InvUtils
 *  meteordevelopment.meteorclient.utils.render.color.Color
 *  meteordevelopment.orbit.EventHandler
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EquipmentSlot
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.world.GameMode
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.network.packet.Packet
 *  net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
 *  net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket$Mode
 *  net.minecraft.client.network.ServerInfo
 */
package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.LotusUtils;
import com.xiaohe66.mc.meteor.lotus.modules.StepModule;
import com.xiaohe66.mc.meteor.lotus.modules.spiral.MosquitoCoilData;
import com.xiaohe66.mc.meteor.lotus.modules.spiral.MosquitoCoilStateManager;
import com.xiaohe66.mc.meteor.lotus.modules.spiral.Pitch40Controller;
import com.xiaohe66.mc.meteor.lotus.modules.spiral.SpiralNavigator;

import com.xiaohe66.mc.meteor.lotus.util.TaskUtils;
import java.util.concurrent.TimeUnit;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.BlockPosSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

public class MosquitoCoilScan extends StepModule {
    public final Setting<BlockPos> center = sgGeneral.add(new BlockPosSetting.Builder()
        .name("中心坐标")
        .description("螺旋线中心点，仅使用 X 和 Z 坐标，Y 坐标无效")
        .defaultValue(new BlockPos(0, 0, 0))
        .build());
    public final Setting<Integer> spiralSpacing = sgGeneral.add(new IntSetting.Builder()
        .name("螺距")
        .description("每完整旋转一圈半径增加的量（区块）")
        .defaultValue(64)
        .min(10)
        .sliderMax(640)
        .build());
    public final Setting<Integer> heightRange = sgGeneral.add(new IntSetting.Builder()
        .name("高度范围")
        .description("飞行高度范围（上限 - 范围 = 下限）")
        .defaultValue(40)
        .min(40)
        .sliderMax(80)
        .build());
    public final Setting<Integer> minHeight = sgGeneral.add(new IntSetting.Builder()
        .name("高度下限")
        .description("允许下降的最低高度，低于此时会用烟花恢复高度")
        .defaultValue(320)
        .min(1)
        .sliderMax(1024)
        .build());
    public final Setting<Boolean> clockwise = sgGeneral.add(new BoolSetting.Builder()
        .name("顺时针")
        .description("true=顺时针，false=逆时针")
        .defaultValue(true)
        .build());
    public final Setting<Boolean> autoReplaceElytra = sgGeneral.add(new BoolSetting.Builder()
        .name("自动更换鞘翅")
        .description("鞘翅耐久过低时自动从背包更换")
        .defaultValue(true)
        .build());
    public final Setting<Boolean> autoFly = sgGeneral.add(new BoolSetting.Builder()
        .name("自动飞行")
        .description("开启后才会执行 Pitch40 自动飞行控制")
        .defaultValue(true)
        .build());
    public final Setting<Integer> maxRenderLength = sgGeneral.add(new IntSetting.Builder()
        .name("最大渲染长度")
        .description("螺旋线最多渲染多少长度（块），0=无限制。超出后只保留当前位置附近")
        .defaultValue(2048)
        .min(512)
        .sliderMax(4096)
        .build());
    public final Setting<MosquitoCoilScanMode> serverIdentifierMode = sgGeneral.add(new EnumSetting.Builder<MosquitoCoilScanMode>()
        .name("服务器标识方式")
        .description("保存数据时, 文件夹的命名方式")
        .defaultValue(MosquitoCoilScanMode.服务器地址)
        .build());
    private final SettingGroup sgControl = settings.createGroup("控制");
    public final Setting<Double> turnSensitivity = sgControl.add(new DoubleSetting.Builder()
        .name("转向灵敏度")
        .description("每 tick 最大转向角度，值越大转弯越灵敏（°/tick）")
        .defaultValue(10.0)
        .min(0.0)
        .sliderMax(180.0)
        .build());
    public final Setting<Double> lookaheadDistance = sgControl.add(new DoubleSetting.Builder()
        .name("前瞻距离")
        .description("飞行路径跟踪的提前距离（块），值越大飞行越平滑")
        .defaultValue(6.0)
        .min(1.0)
        .sliderMax(32.0)
        .build());
    private static final int UNUSED_CONSTANT = 2;
    private final SpiralNavigator navigator;
    private final Pitch40Controller pitchController;
    private MosquitoCoilData scanData;
    private boolean started;
    private long lastElytraReplaceTime;
    private long lastGlidePacketTime;
    private long lastSaveTime;

    public MosquitoCoilScan() {
        super("螺旋扫图", "以阿基米德螺旋线方式自动使用Pitch40鞘翅飞行扫描地图。核心算法源于<Wandelion>的 MilkyAddon 中的 SpiralFlight。哞~");
        this.lastSaveTime = 0L;
        this.navigator = new SpiralNavigator(this);
        this.pitchController = new Pitch40Controller(this);
        this.addStep(Steps.MOVEMENT, this::onMovement);
        this.addStep(Steps.FLY, this::onFly);
    }

    public void onActivate() {
        if (!this.isReady()) {
            return;
        }
        LotusUtils.enableFreeLook();
        this.delayedStart();
    }

    protected void delayedStart() {
        this.stopTask();
        GameType gameMode = this.mc.gameMode.getPlayerMode();
        if (gameMode == GameType.SPECTATOR || gameMode == GameType.ADVENTURE) {
            return;
        }
        this.delayTask = TaskUtils.run(() -> {
            this.delayTask = null;
            if (this.started) {
                this.warning("启动", new Object[0]);
                this.step = Steps.MOVEMENT;
            } else {
                this.warning("延迟启动", new Object[0]);
                this.delayedStart();
            }
        }, 1000L, TimeUnit.MILLISECONDS);
    }

    private void onMovement() {
        boolean falling;
        if (!this.autoFly.get()) {
            this.warning("已开启无动力模式", new Object[0]);
            this.startFlying();
            return;
        }
        ItemStack chestStack = this.mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestStack.getItem() != Items.ELYTRA) {
            return;
        }
        if (this.mc.player.onGround()) {
            this.mc.options.keyJump.setDown(true);
            return;
        }
        this.mc.options.keyJump.setDown(false);
        if (!this.mc.player.isFallFlying()) {
            if (this.mc.player.fallDistance <= 0.0) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now - this.lastGlidePacketTime > 500L) {
                this.lastGlidePacketTime = now;
                this.mc.getConnection().send((Packet)new ServerboundPlayerCommandPacket((Entity)this.mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }
            return;
        }
        this.mc.player.setXRot(-80.0f);
        boolean falling2 = falling = this.mc.player.getDeltaMovement().y < 1.0;
        if (falling) {
            if (this.mc.player.getY() < (double)(this.minHeight.get() + this.heightRange.get())) {
                this.pitchController.launchFirework();
            } else {
                this.lastSaveTime = System.currentTimeMillis();
                if (this.mc.player.isFallFlying()) {
                    this.startFlying();
                    this.warning("起飞完毕", new Object[0]);
                }
            }
        }
    }

    private void startFlying() {
        BlockPos centerPos;
        this.navigator.reset(this.center.get().getX(), this.center.get().getZ(), this.spiralSpacing.get() * 16, this.clockwise.get());
        if (this.scanData != null && this.scanData.centerX == (centerPos = this.center.get()).getX() && this.scanData.centerZ == centerPos.getZ()) {
            double angle = this.navigator.getAngle(this.mc.player.getX(), this.mc.player.getZ());
            this.navigator.setCurrentAngle(angle);
            this.warning("恢复上次扫图进度（θ=%.2f，上次进度 θ=%.2f）", new Object[]{angle, this.scanData.angle});
        }
        this.pitchController.start();
        this.step = Steps.FLY;
    }

    private void onFly() {
        if (this.autoFly.get()) {
            if (!this.mc.player.isFallFlying()) {
                this.step = Steps.MOVEMENT;
                return;
            }
            this.pitchController.tick();
            this.setForwardKey(true);
            this.isElytraUsable();
        }
        this.navigator.update();
        this.autoSave();
    }

    private void loadSavedData() {
        String dimensionId;
        String serverIdentifier = this.getServerIdentifier();
        MosquitoCoilData scanData = MosquitoCoilStateManager.load(serverIdentifier, dimensionId = this.getDimensionId());
        if (scanData != null) {
            this.center.set(new BlockPos(scanData.centerX, 0, scanData.centerZ));
        }
        this.scanData = scanData;
    }

    public void start() {
        this.loadSavedData();
        this.started = true;
    }

    public void stop() {
        this.started = false;
        if (this.isActive()) {
            this.saveData();
        }
    }

    private boolean isElytraUsable() {
        ItemStack chestStack = this.mc.player.getItemBySlot(EquipmentSlot.CHEST);
        boolean needReplace = chestStack.getItem() != Items.ELYTRA || chestStack.getMaxDamage() - chestStack.getDamageValue() <= 2;
        long now = System.currentTimeMillis();
        if (needReplace && now - this.lastElytraReplaceTime > 1000L) {
            ItemStack replacementElytra = this.nextPlayerStack(itemStack -> itemStack.getItem() == Items.ELYTRA && itemStack.getMaxDamage() - itemStack.getDamageValue() > 2);
            if (replacementElytra.isEmpty()) {
                this.warning("背包中的鞘翅耐久用光了！", new Object[0]);
            } else if (this.autoReplaceElytra.get()) {
                InvUtils.move().from(this.getCurPlayerSlot()).toArmor(2);
                this.lastElytraReplaceTime = now;
                return false;
            }
        }
        return true;
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        double endAngle;
        double startAngle;
        double angleStep;
        if (!this.isReady()) {
            return;
        }
        double circumference = (double)(this.spiralSpacing.get() * 16) / (Math.PI * 2);
        boolean clockwise = this.clockwise.get();
        double centerX = this.navigator.getCenterX();
        double centerZ = this.navigator.getCenterZ();
        double renderY = this.pitchController.getHeightLimit() - (double)this.heightRange.get() / 2.0;
        double currentAngle = this.navigator.getCurrentAngle();
        double stepSize = 0.05;
        angleStep = clockwise ? stepSize : -stepSize;
        if (this.maxRenderLength.get() > 0) {
            double maxLength = this.maxRenderLength.get();
            double startFraction = maxLength * 0.25;
            double endFraction = maxLength * 0.75;
            int startDirection = clockwise ? -1 : 1;
            startAngle = this.getAngleAtDistance(currentAngle, startDirection, startFraction, circumference, centerX, centerZ, clockwise);
            startAngle = clockwise ? Math.max(0.0, startAngle) : Math.min(0.0, startAngle);
            int endDirection = clockwise ? 1 : -1;
            endAngle = this.getAngleAtDistance(currentAngle, endDirection, endFraction, circumference, centerX, centerZ, clockwise);
        } else {
            endAngle = currentAngle + (clockwise ? Math.PI * 4 : Math.PI * -4);
            startAngle = 0.0;
        }
        double[] startPoint = SpiralNavigator.spiralPoint(startAngle, circumference, centerX, centerZ, clockwise);
        double prevX = startPoint[0];
        double prevZ = startPoint[1];
        double angle = startAngle + angleStep;
        while (clockwise ? angle <= endAngle : angle >= endAngle) {
            double[] point = SpiralNavigator.spiralPoint(angle, circumference, centerX, centerZ, clockwise);
            double x = point[0];
            double z = point[1];
            boolean isPastCurrent = clockwise ? angle <= currentAngle : angle >= currentAngle;
            Color color = isPastCurrent ? new Color(0, 255, 0, 180) : new Color(255, 255, 255, 180);
            event.renderer.line(prevX, renderY, prevZ, x, renderY, z, color);
            prevX = x;
            prevZ = z;
            angle += angleStep;
        }
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        this.setForwardKey(false);
        LotusUtils.disableFreeLook();
        this.saveData();
    }

    private void setForwardKey(boolean pressed) {
        if (this.mc.options != null && this.mc.options.keyUp != null) {
            this.mc.options.keyUp.setDown(pressed);
        }
    }

    private double getAngleAtDistance(double startAngle, int direction, double maxDistance, double circumference, double centerX, double centerZ, boolean clockwise) {
        double dx;
        double dz;
        if (maxDistance <= 0.0) {
            return startAngle;
        }
        double stepSize = 0.05;
        double angle = startAngle;
        double[] prevPoint = SpiralNavigator.spiralPoint(angle, circumference, centerX, centerZ, clockwise);
        for (double distance = 0.0; distance < maxDistance; distance += Math.sqrt(dz * dz + dx * dx)) {
            angle += (double)direction * stepSize;
            if (clockwise && angle < 0.0) {
                return 0.0;
            }
            if (!clockwise && angle > 0.0) {
                return 0.0;
            }
            double[] point = SpiralNavigator.spiralPoint(angle, circumference, centerX, centerZ, clockwise);
            dx = point[0] - prevPoint[0];
            dz = point[1] - prevPoint[1];
            prevPoint = point;
        }
        return angle;
    }

    private void autoSave() {
        long now = System.currentTimeMillis();
        if (now - this.lastSaveTime >= 300000L) {
            this.saveData();
            this.lastSaveTime = now;
        }
    }

    private void saveData() {
        String serverIdentifier = this.getServerIdentifier();
        String dimensionId = this.getDimensionId();
        if (dimensionId.isEmpty() || serverIdentifier.isEmpty()) {
            return;
        }
        if (this.scanData == null) {
            this.scanData = new MosquitoCoilData();
        }
        this.scanData.centerX = this.center.get().getX();
        this.scanData.centerZ = this.center.get().getZ();
        this.scanData.angle = this.navigator.getCurrentAngle();
        this.scanData.savedAt = System.currentTimeMillis();
        this.scanData.dimensionId = dimensionId;
        MosquitoCoilStateManager.save(serverIdentifier, dimensionId, this.scanData);
    }

    private String getDimensionId() {
        if (this.mc.level == null) {
            return "";
        }
        return this.mc.level.dimension().identifier().toString();
    }

    private String getServerIdentifier() {
        ServerData serverInfo = this.mc.getCurrentServer();
        if (serverInfo != null) {
            if (this.serverIdentifierMode.get() == MosquitoCoilScanMode.服务器地址) {
                String regex = "[\\\\/:*?\"<>|]";
                return serverInfo.ip.replaceAll(regex, "_");
            }
            return serverInfo.name;
        }
        if (this.mc.getSingleplayerServer() != null) {
            return this.mc.getSingleplayerServer().getWorldData().getLevelName();
        }
        return "other";
    }

    public static enum MosquitoCoilScanMode {
        服务器地址,
        服务器名称;
    }
}
