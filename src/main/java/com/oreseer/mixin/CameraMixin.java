
package com.oreseer.mixin;

import com.oreseer.FreecamController;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setPosition(double x, double y, double z);

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Inject(method = "alignWithEntity", at = @At("TAIL"))
	private void orelseer$freecamCamera(float partialTicks, CallbackInfo ci) {
		if (!FreecamController.isActive()) {
			return;
		}

		setPosition(
			FreecamController.getX(),
			FreecamController.getY(),
			FreecamController.getZ()
		);

		setRotation(
			FreecamController.getYaw(),
			FreecamController.getPitch()
		);
	}
}
