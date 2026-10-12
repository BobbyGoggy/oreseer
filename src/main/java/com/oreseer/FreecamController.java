
package com.oreseer;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class FreecamController {
	private static boolean active;

	private static double x;
	private static double y;
	private static double z;

	private static float yaw;
	private static float pitch;

	private FreecamController() {
	}

	public static boolean isActive() {
		return active;
	}

	public static double getX() {
		return x;
	}

	public static double getY() {
		return y;
	}

	public static double getZ() {
		return z;
	}

	public static float getYaw() {
		return yaw;
	}

	public static float getPitch() {
		return pitch;
	}

	public static void toggle(Minecraft mc) {
		if (active) {
			active = false;
			return;
		}

		if (mc.player == null || mc.level == null) {
			return;
		}

		OreSeerConfig config = OreSeerConfig.get();
		if (!config.freecamEnabled) {
			return;
		}

		active = true;

		x = mc.player.getX();
		y = mc.player.getEyeY();
		z = mc.player.getZ();

		yaw = mc.player.getYRot();
		pitch = mc.player.getXRot();
	}

	public static void rotate(double mouseX, double mouseY) {
		if (!active) {
			return;
		}

		yaw += (float) mouseX * 0.15F;
		pitch += (float) mouseY * 0.15F;
		pitch = Math.max(-90.0F, Math.min(90.0F, pitch));
	}

	public static void tick(Minecraft mc, KeyMapping backwardKey) {
		if (!active) {
			return;
		}

		if (mc.player == null || mc.level == null
				|| !OreSeerConfig.get().freecamEnabled) {
			active = false;
			return;
		}

		mc.player.setDeltaMovement(0, 0, 0);

		if (mc.screen != null) {
			return;
		}

		OreSeerConfig config = OreSeerConfig.get();
		double speed = config.freecamSpeed;
		double verticalSpeed = config.freecamVerticalSpeed;

		double radians = Math.toRadians(yaw);
		double forwardX = -Math.sin(radians);
		double forwardZ = Math.cos(radians);
		double rightX = Math.cos(radians);
		double rightZ = Math.sin(radians);

		double forward = 0;
		double strafe = 0;

		if (mc.options.keyUp.isDown()) forward++;
		if (backwardKey.isDown()) forward--;
		if (mc.options.keyRight.isDown()) strafe++;
		if (mc.options.keyLeft.isDown()) strafe--;

		double dx = forwardX * forward + rightX * strafe;
		double dz = forwardZ * forward + rightZ * strafe;

		double length = Math.sqrt(dx * dx + dz * dz);
		if (length > 1.0) {
			dx /= length;
			dz /= length;
		}

		x += dx * speed;
		z += dz * speed;

		if (mc.options.keyJump.isDown()) {
			y += verticalSpeed;
		}
		if (mc.options.keyShift.isDown()) {
			y -= verticalSpeed;
		}
	}
}
