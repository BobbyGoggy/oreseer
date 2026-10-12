
package com.oreseer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class OreSeerClient implements ClientModInitializer {
	public static final String MOD_ID = "oreseer";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static KeyMapping openMenu;
	private static KeyMapping toggle;
	private static KeyMapping debrisOnly;
	private static KeyMapping cycleStyle;
	private static KeyMapping freecamToggle;
	private static KeyMapping freecamBackward;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitializeClient() {
		OreSeerConfig.load();

		KeyMapping.Category category =
			KeyMapping.Category.register(id("main"));

		openMenu = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.oreseer.menu",
				InputConstants.Type.KEYBOARD, InputConstants.KEY_O, category));

		toggle = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.oreseer.toggle",
				InputConstants.Type.KEYBOARD, InputConstants.KEY_K, category));

		debrisOnly = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.oreseer.debris_only",
				InputConstants.Type.KEYBOARD, InputConstants.KEY_H, category));

		cycleStyle = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.oreseer.cycle_style",
				InputConstants.Type.KEYBOARD, InputConstants.KEY_J, category));

		freecamToggle = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.oreseer.freecam",
				InputConstants.Type.KEYBOARD, InputConstants.KEY_F6, category));

		freecamBackward = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.oreseer.freecam_backward",
				InputConstants.Type.KEYBOARD, InputConstants.KEY_X, category));

		ClientTickEvents.END_CLIENT_TICK.register(OreSeerClient::onTick);
		LevelRenderEvents.END_MAIN.register(OreRenderer::render);
		HudElementRegistry.attachElementBefore(
			VanillaHudElements.CHAT, id("hud"), OreHud::extract);
		ClientLifecycleEvents.CLIENT_STOPPING.register(
			client -> OreRenderer.close());

		LOGGER.info("OreSeer loaded");
	}

	private static void onTick(Minecraft mc) {
		OreSeerConfig cfg = OreSeerConfig.get();

		while (openMenu.consumeClick()) {
			mc.gui.setScreen(new OreSeerScreen(mc.gui.screen()));
		}

		while (toggle.consumeClick()) {
			cfg.enabled = !cfg.enabled;
			OreSeerConfig.save();
			say(mc, "OreSeer: " + (cfg.enabled ? "ON" : "OFF"));
		}

		while (debrisOnly.consumeClick()) {
			cfg.debrisOnly = !cfg.debrisOnly;
			OreSeerConfig.save();
			say(mc, "OreSeer: Ancient Debris only "
				+ (cfg.debrisOnly ? "ON" : "OFF"));
		}

		while (cycleStyle.consumeClick()) {
			OreSetting setting = cfg.setting(OreType.ANCIENT_DEBRIS);
			setting.style = setting.style.next();
			OreSeerConfig.save();
			say(mc, "Ancient Debris style: " + setting.style.label);
		}

		while (freecamToggle.consumeClick()) {
			if (!cfg.freecamEnabled) {
				say(mc, "Enable Freecam in the OreSeer menu first");
			} else {
				FreecamController.toggle(mc);
				say(mc, "Freecam: "
					+ (FreecamController.isActive() ? "ON" : "OFF"));
			}
		}

		FreecamController.tick(mc, freecamBackward);
		OreScanner.tick(mc);
	}

	private static void say(Minecraft mc, String text) {
		if (mc.player != null) {
			mc.player.sendSystemMessage(Component.literal(text));
		}
	}
}
