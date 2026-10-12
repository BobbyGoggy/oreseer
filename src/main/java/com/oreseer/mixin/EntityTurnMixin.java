
package com.oreseer.mixin;

import com.oreseer.FreecamController;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityTurnMixin {
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void orelseer$freecamLook(
			double mouseX, double mouseY, CallbackInfo ci) {
		if ((Object) this instanceof LocalPlayer
				&& FreecamController.isActive()) {
			FreecamController.rotate(mouseX, mouseY);
			ci.cancel();
		}
	}
}
