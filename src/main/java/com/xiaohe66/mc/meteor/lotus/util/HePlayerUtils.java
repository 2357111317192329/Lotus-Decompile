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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;

public class HePlayerUtils {
    public static void startSneaking() {
        Input playerInput = new Input(false, false, false, false, false, true, false);
        MeteorClient.mc.player.connection.send((Packet)new ServerboundPlayerInputPacket(playerInput));
    }

    public static void stopSneaking() {
        Input playerInput = new Input(false, false, false, false, false, false, false);
        MeteorClient.mc.player.connection.send((Packet)new ServerboundPlayerInputPacket(playerInput));
    }
}
