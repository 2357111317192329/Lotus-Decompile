/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.MeteorClient
 *  net.minecraft.util.PlayerInput
 *  net.minecraft.network.packet.Packet
 *  net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket
 */
package com.xiaohe66.mc.meteor.lotus.util;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.util.PlayerInput;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;

public class HePlayerUtils {
    public static void startSneaking() {
        PlayerInput playerInput = new PlayerInput(false, false, false, false, false, true, false);
        MeteorClient.mc.player.networkHandler.sendPacket((Packet)new PlayerInputC2SPacket(playerInput));
    }

    public static void stopSneaking() {
        PlayerInput playerInput = new PlayerInput(false, false, false, false, false, false, false);
        MeteorClient.mc.player.networkHandler.sendPacket((Packet)new PlayerInputC2SPacket(playerInput));
    }
}
