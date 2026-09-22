/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.util.math.MathHelper
 */
package com.xiaohe66.mc.meteor.lotus.modules.spiral;

import com.xiaohe66.mc.meteor.lotus.modules.MosquitoCoilScan;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class SpiralNavigator {
    private static final double STEP_LENGTH = 1.5;
    private static final double STOP_DISTANCE = 8.0;
    private static final double HALF_STEP = 0.5;
    private static final double TURN_STEP = 3.0;
    private static final int MAX_SAMPLES = 192;
    private static final double SEARCH_RANGE = 0.8;
    private static final int REFINE_ROUNDS = 3;
    private static final double MIN_TURN = 1.0E-4;
    private static final double MAX_TURN = 0.25;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final MosquitoCoilScan scan;
    private double centerX;
    private double centerZ;
    private double currentAngle;
    private boolean clockwise;
    private double radius;

    public SpiralNavigator(MosquitoCoilScan scan) {
        this.scan = scan;
    }

    public void reset(double centerX, double centerZ, double radius, boolean clockwise) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.clockwise = clockwise;
        this.currentAngle = 0.0;
    }

    public double getAngle(double x, double z) {
        double circumference = this.radius / (Math.PI * 2);
        double direction = this.clockwise ? 1.0 : -1.0;
        double dx = x - this.centerX;
        double dz = z - this.centerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double maxAngle = Math.max(distance / circumference, Math.abs(this.currentAngle)) + Math.PI * 4;
        maxAngle = Math.min(maxAngle, 5000.0);
        double bestAngle = 0.0;
        double bestDistance = Double.MAX_VALUE;
        double angleStep = 0.5;
        int iterations = (int)(maxAngle / angleStep) + 100;
        for (int i = 0; i <= iterations; ++i) {
            double angle = direction * (double)i * angleStep;
            double squaredDistance = SpiralNavigator.squaredDistanceTo(angle, circumference, this.centerX, this.centerZ, x, z, this.clockwise);
            if (!(squaredDistance < bestDistance)) continue;
            bestDistance = squaredDistance;
            bestAngle = angle;
        }
        double[] refined = this.refineAngle(bestAngle, circumference, this.centerX, this.centerZ, x, z, this.clockwise, angleStep * 2.0, 192, 3);
        return refined[0];
    }

    public void update() {
        double playerX = this.mc.player.getX();
        double playerZ = this.mc.player.getZ();
        double circumference = this.radius / (Math.PI * 2);
        boolean clockwise = this.clockwise;
        double lookahead = this.scan.lookaheadDistance.get();
        double dirAngle = clockwise ? this.currentAngle : -this.currentAngle;
        double arcLength = Math.max(0.0, circumference * dirAngle);
        double turnRate = STEP_LENGTH / Math.max(1.0E-6, Math.sqrt(arcLength * arcLength + circumference * circumference));
        turnRate = SpiralNavigator.clampAbs(turnRate, MIN_TURN, MAX_TURN);
        double turn = clockwise ? turnRate : -turnRate;
        double[] refined = this.refineAngle(this.currentAngle, circumference, this.centerX, this.centerZ, playerX, playerZ, clockwise, SEARCH_RANGE, MAX_SAMPLES, REFINE_ROUNDS);
        double bestAngle = refined[0];
        double bestDistance = refined[1];
        double maxChange = TURN_STEP * Math.abs(turn);
        double newAngle = this.currentAngle = SpiralNavigator.clampAngle(this.currentAngle, bestAngle, maxChange, clockwise);
        if (bestDistance <= STOP_DISTANCE) {
            newAngle += (clockwise ? 1.0 : -1.0) * Math.abs(turn) * HALF_STEP;
        }
        double predictedAngle = SpiralNavigator.predictAngle(newAngle, lookahead, circumference, clockwise);
        double[] target = SpiralNavigator.spiralPoint(predictedAngle, circumference, this.centerX, this.centerZ, clockwise);
        double targetYaw = Math.toDegrees(Math.atan2(target[1] - playerZ, target[0] - playerX)) - 90.0;
        this.rotateYaw((float)targetYaw, this.scan.turnSensitivity.get().floatValue());
    }

    private void rotateYaw(float targetYaw, float maxTurn) {
        float currentYaw = this.mc.player.getYaw();
        float deltaYaw = SpiralNavigator.wrapDegrees(targetYaw - currentYaw);
        if (maxTurn <= 0.0f) {
            this.mc.player.setYaw(currentYaw + deltaYaw);
            return;
        }
        float clampedDelta = MathHelper.clamp((float)deltaYaw, (float)(-maxTurn), (float)maxTurn);
        this.mc.player.setYaw(currentYaw + clampedDelta);
    }

    private static float wrapDegrees(double degrees) {
        float wrapped;
        for (wrapped = (float)degrees; wrapped <= -180.0f; wrapped += 360.0f) {
        }
        while (wrapped > 180.0f) {
            wrapped -= 360.0f;
        }
        return wrapped;
    }

    private static double predictAngle(double angle, double maxDistance, double radius, boolean clockwise) {
        double distance = 0.0;
        double newAngle = angle;
        int iterations = 0;
        while (distance < maxDistance && iterations++ < 4096) {
            double dirAngle = clockwise ? newAngle : -newAngle;
            double arcLength = Math.max(0.0, radius * dirAngle);
            double turnRate = STEP_LENGTH / Math.max(1.0E-6, Math.sqrt(arcLength * arcLength + radius * radius));
            turnRate = SpiralNavigator.clampAbs(turnRate, MIN_TURN, MAX_TURN);
            double turnStep = clockwise ? turnRate : -turnRate;
            distance += STEP_LENGTH;
            newAngle += turnStep;
        }
        return newAngle;
    }

    private double[] refineAngle(double startAngle, double radius, double centerX, double centerZ, double targetX, double targetZ, boolean clockwise, double searchRange, int samples, int refineRounds) {
        double bestAngle = startAngle;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < samples; ++i) {
            double angle = startAngle + ((double)i / (double)(samples - 1) - 0.5) * 2.0 * searchRange;
            double squaredDistance = SpiralNavigator.squaredDistanceTo(angle, radius, centerX, centerZ, targetX, targetZ, clockwise);
            if (!(squaredDistance < bestDistance)) continue;
            bestDistance = squaredDistance;
            bestAngle = angle;
        }
        double lo = bestAngle - searchRange / 6.0;
        double hi = bestAngle + searchRange / 6.0;
        for (int i = 0; i < refineRounds; ++i) {
            double mid1 = lo + (hi - lo) / 3.0;
            double mid2 = hi - (hi - lo) / 3.0;
            double dist1 = SpiralNavigator.squaredDistanceTo(mid1, radius, centerX, centerZ, targetX, targetZ, clockwise);
            double dist2 = SpiralNavigator.squaredDistanceTo(mid2, radius, centerX, centerZ, targetX, targetZ, clockwise);
            if (dist1 < dist2) {
                hi = mid2;
                bestAngle = mid1;
                bestDistance = dist1;
                continue;
            }
            lo = mid1;
            bestAngle = mid2;
            bestDistance = dist2;
        }
        return new double[]{bestAngle, Math.sqrt(bestDistance)};
    }

    private static double squaredDistanceTo(double angle, double radius, double centerX, double centerZ, double targetX, double targetZ, boolean clockwise) {
        double[] point = SpiralNavigator.spiralPoint(angle, radius, centerX, centerZ, clockwise);
        double dx = point[0] - targetX;
        double dz = point[1] - targetZ;
        return dx * dx + dz * dz;
    }

    private static double clampAngle(double current, double target, double maxChange, boolean clockwise) {
        if (clockwise) {
            if (target < current) {
                target = current;
            }
            double change = Math.min(target - current, maxChange);
            if (change < 0.0) {
                change = 0.0;
            }
            return current + change;
        }
        if (target > current) {
            target = current;
        }
        double change = Math.min(current - target, maxChange);
        if (change < 0.0) {
            change = 0.0;
        }
        return current - change;
    }

    public static double[] spiralPoint(double angle, double radius, double centerX, double centerZ, boolean clockwise) {
        double dirAngle = clockwise ? angle : -angle;
        double r = Math.max(0.0, radius * dirAngle);
        double x = centerX + r * Math.cos(angle);
        double z = centerZ + r * Math.sin(angle);
        return new double[]{x, z};
    }

    private static double clampAbs(double value, double min, double max) {
        double sign = Math.signum(value == 0.0 ? 1.0 : value);
        double abs = Math.abs(value);
        if (abs < min) {
            abs = min;
        }
        if (abs > max) {
            abs = max;
        }
        return sign * abs;
    }

    public double getCurrentAngle() {
        return this.currentAngle;
    }

    public void setCurrentAngle(double currentAngle) {
        this.currentAngle = currentAngle;
    }

    public double getCenterX() {
        return this.centerX;
    }

    public double getCenterZ() {
        return this.centerZ;
    }

    public boolean isClockwise() {
        return this.clockwise;
    }

    public double getRadius() {
        return this.radius;
    }
}
