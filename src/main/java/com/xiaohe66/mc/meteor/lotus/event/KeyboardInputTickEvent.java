package com.xiaohe66.mc.meteor.lotus.event;

import net.minecraft.util.PlayerInput;

public class KeyboardInputTickEvent extends Event {
   private PlayerInput playerInput;

   public KeyboardInputTickEvent() {
      super(Event.Stage.Post);
   }

   public PlayerInput getPlayerInput() {
      return this.playerInput;
   }

   public void setPlayerInput(PlayerInput playerInput) {
      this.playerInput = playerInput;
   }
}
