
package com.oreseer.mixin;

import com.oreseer.FreecamController;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	@Inject(method = "applyInput", at = @At("HEAD"))
	private void orelseer$freezePlayer(CallbackInfo ci) {
		if (!FreecamController.isActive()) {
			return;
		}

		LocalPlayer player = (LocalPlayer) (Object) this;
		player.input.keyPresses = Input.EMPTY;
	}
}
