
package com.oreseer;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Multi-page settings menu: General / Ancient Debris / Ores / Effects / HUD / Freecam. */
public class OreSeerScreen extends Screen {
	private static final String[] PAGES = {
		"General", "Ancient Debris", "Ores", "Effects", "HUD", "Freecam"
	};

	private static int lastPage = 0;
	private static OreType selectedOre = OreType.DIAMOND;

	private final Screen parent;
	private int page = lastPage;
	private int rowX;
	private int rowY;
	private int rowW;

	public OreSeerScreen(Screen parent) {
		super(Component.literal("OreSeer"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		OreSeerConfig c = OreSeerConfig.get();
		int cx = this.width / 2;

		int tabW = 84;
		int total = PAGES.length * tabW + (PAGES.length - 1) * 4;
		int tx = cx - total / 2;

		for (int i = 0; i < PAGES.length; i++) {
			final int idx = i;
			String label = (i == page ? "> " : "") + PAGES[i];

			this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
				page = idx;
				lastPage = idx;
				this.rebuildWidgets();
			}).bounds(tx + i * (tabW + 4), 20, tabW, 20).build());
		}

		rowW = 250;
		rowX = cx - rowW / 2;
		rowY = 52;

		switch (page) {
			case 0 -> {
				toggle("Highlights", () -> c.enabled, v -> c.enabled = v);
				toggle("Ancient Debris only", () -> c.debrisOnly, v -> c.debrisOnly = v);
				toggle("Hide already visible ores", () -> c.hideExposed, v -> c.hideExposed = v);
				slider("Scan radius", 16, 96, true, () -> c.scanRadius, v -> c.scanRadius = (int) v);
				slider("Vertical range", 8, 96, true, () -> c.verticalRadius, v -> c.verticalRadius = (int) v);
				slider("Scan speed (columns/tick)", 1, 16, true, () -> c.columnsPerTick, v -> c.columnsPerTick = (int) v);
				slider("Max highlights", 20, 300, true, () -> c.maxHighlights, v -> c.maxHighlights = (int) v);
			}

			case 1 -> {
				oreEditor(OreType.ANCIENT_DEBRIS);
				toggle("Light pillar", () -> c.debrisPillar, v -> c.debrisPillar = v);
				toggle("Tracer to nearest", () -> c.debrisTracer, v -> c.debrisTracer = v);
				toggle("Vein strobe (3+ blocks)", () -> c.debrisVeinFlash, v -> c.debrisVeinFlash = v);
			}

			case 2 -> {
				cycle("Ore", () -> selectedOre.label, () -> {
					OreType next = selectedOre;
					do {
						next = OreType.ALL[(next.ordinal() + 1) % OreType.ALL.length];
					} while (next == OreType.ANCIENT_DEBRIS);

					selectedOre = next;
					this.rebuildWidgets();
				});

				oreEditor(selectedOre);
			}

			case 3 -> {
				slider("Opacity", 0.1, 1.0, false, () -> c.opacity, v -> c.opacity = (float) v);
				slider("Animation speed", 0.2, 3.0, false, () -> c.pulseSpeed, v -> c.pulseSpeed = (float) v);
				slider("Render distance", 16, 128, true, () -> c.renderDistance, v -> c.renderDistance = (int) v);
				toggle("Distance fade", () -> c.distanceFade, v -> c.distanceFade = v);
				toggle("Tracers to other ores", () -> c.tracerAll, v -> c.tracerAll = v);
			}

			case 4 -> {
				toggle("Info panel", () -> c.showHud, v -> c.showHud = v);
				toggle("Radar", () -> c.showRadar, v -> c.showRadar = v);
			}

			case 5 -> {
				toggle("Freecam enabled", () -> c.freecamEnabled, v -> c.freecamEnabled = v);
				slider("Flight speed", 0.1, 5.0, false, () -> c.freecamSpeed, v -> c.freecamSpeed = v);
				slider("Vertical speed", 0.1, 5.0, false, () -> c.freecamVerticalSpeed, v -> c.freecamVerticalSpeed = v);
				toggle("Night vision", () -> c.freecamNightVision, v -> c.freecamNightVision = v);
				toggle("Hide ore blocks", () -> c.freecamHideOres, v -> c.freecamHideOres = v);
				toggle("Hide OreSeer visuals", () -> c.freecamHideOreSeer, v -> c.freecamHideOreSeer = v);
			}
		}

		this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
			.bounds(cx - 102, this.height - 28, 100, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("Reset everything"), b -> {
			OreSeerConfig.reset();
			this.rebuildWidgets();
		}).bounds(cx + 2, this.height - 28, 100, 20).build());
	}

	private void oreEditor(OreType type) {
		OreSetting s = OreSeerConfig.get().setting(type);

		toggle(type.label + " enabled", () -> s.enabled, v -> s.enabled = v);
		cycle("Style", () -> s.style.label, () -> s.style = s.style.next());
		cycle("Colour", () -> Palette.NAMES[Palette.clamp(s.colorIndex)],
			() -> s.colorIndex = Palette.clamp(s.colorIndex + 1));
		slider("Size", 0.6, 1.6, false, () -> s.size, v -> s.size = (float) v);
	}

	private void toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
		Button b = Button.builder(
			Component.literal(label + ": " + (get.getAsBoolean() ? "ON" : "OFF")),
			btn -> {
				set.accept(!get.getAsBoolean());
				btn.setMessage(Component.literal(label + ": " + (get.getAsBoolean() ? "ON" : "OFF")));
				OreSeerConfig.save();
			}
		).bounds(rowX, rowY, rowW, 20).build();

		this.addRenderableWidget(b);
		rowY += 24;
	}

	private void cycle(String label, Supplier<String> value, Runnable next) {
		Button b = Button.builder(Component.literal(label + ": " + value.get()), btn -> {
			next.run();
			btn.setMessage(Component.literal(label + ": " + value.get()));
			OreSeerConfig.save();
		}).bounds(rowX, rowY, rowW, 20).build();

		this.addRenderableWidget(b);
		rowY += 24;
	}

	private void slider(
		String label,
		double min,
		double max,
		boolean integer,
		DoubleSupplier get,
		DoubleConsumer set
	) {
		this.addRenderableWidget(
			new Slider(rowX, rowY, rowW, 20, label, min, max, integer, get.getAsDouble(), set)
		);
		rowY += 24;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		String title = "OreSeer";
		graphics.text(
			this.font,
			title,
			this.width / 2 - this.font.width(title) / 2,
			6,
			0xFFFFD34D,
			true
		);
	}

	@Override
	public void onClose() {
		OreSeerConfig.save();
		this.minecraft.gui.setScreen(this.parent);
	}

	private static final class Slider extends AbstractSliderButton {
		private final String label;
		private final double min;
		private final double max;
		private final boolean integer;
		private final DoubleConsumer setter;

		Slider(
			int x,
			int y,
			int w,
			int h,
			String label,
			double min,
			double max,
			boolean integer,
			double current,
			DoubleConsumer setter
		) {
			super(x, y, w, h, Component.empty(), (current - min) / (max - min));
			this.label = label;
			this.min = min;
			this.max = max;
			this.integer = integer;
			this.setter = setter;
			this.updateMessage();
		}

		private double actual() {
			double v = min + this.value * (max - min);
			return integer ? Math.round(v) : Math.round(v * 100.0) / 100.0;
		}

		@Override
		protected void updateMessage() {
			if (label == null) {
				return;
			}

			double v = actual();
			this.setMessage(Component.literal(
				label + ": " + (integer ? String.valueOf((long) v) : String.valueOf(v))
			));
		}

		@Override
		protected void applyValue() {
			setter.accept(actual());
			OreSeerConfig.save();
		}
	}
}
