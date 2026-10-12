package com.oreseer;

import java.util.Optional;
import java.util.OptionalDouble;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Draws everything in one batched draw call after translucent terrain, with the depth test removed
 * so highlights show through walls. Follows the Fabric docs "Rendering in the world" approach.
 */
public final class OreRenderer {
	private static final int BUFFER_BYTES = 4 * 1024 * 1024;
	private static final int QUAD_BUDGET = 60_000;

	private static final RenderPipeline THROUGH_WALLS = RenderPipelines.register(
		RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
			.withLocation(OreSeerClient.id("pipeline/xray_filled"))
			.withDepthStencilState(Optional.empty())
			.build()
	);

	private static final StagedVertexBuffer BUFFER = new StagedVertexBuffer(() -> "OreSeer Buffer", BUFFER_BYTES);

	private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
	private static final Vector3f MODEL_OFFSET = new Vector3f();
	private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

	private OreRenderer() {
	}

	public static void close() {
		BUFFER.close();
	}

	public static void render(LevelRenderContext ctx) {
		Minecraft mc = Minecraft.getInstance();
		OreSeerConfig cfg = OreSeerConfig.get();
		if (!OreScanner.isActive(mc)) {
			return;
		}
		OreScanner.Snapshot snap = OreScanner.snapshot();
		if (snap.hits().isEmpty()) {
			return;
		}

		Vec3 cam = ctx.levelState().cameraRenderState.pos;
		Matrix4fc pose = ctx.poseStack().last().pose();
		float t = (Util.getMillis() % 3_600_000L) / 1000f;

		var format = THROUGH_WALLS.getVertexFormatBinding(0);
		var primitive = THROUGH_WALLS.getPrimitiveTopology();
		boolean quads = "QUADS".equals(String.valueOf(primitive));
		if (!quads) {
			OreSeerClient.LOGGER.error("OreSeer: pipeline topology is {} (expected QUADS), rendering disabled", primitive);
			return;
		}

		StagedVertexBuffer.Draw draw = BUFFER.appendDraw(format, primitive, RenderSystem.getProjectionType().vertexSorting());
		Shapes shapes = new Shapes(BUFFER.getVertexBuilder(draw), pose, QUAD_BUDGET);

		float maxDist = cfg.renderDistance;
		for (OreHit h : snap.hits()) {
			if (shapes.full()) {
				break;
			}
			OreSetting o = cfg.setting(h.type());
			if (!o.enabled) {
				continue;
			}
			float rx = (float) (h.x() - cam.x);
			float ry = (float) (h.y() - cam.y);
			float rz = (float) (h.z() - cam.z);
			float dist = (float) Math.sqrt(sq(rx + 0.5f) + sq(ry + 0.5f) + sq(rz + 0.5f));
			if (dist > maxDist) {
				continue;
			}
			float fade = cfg.distanceFade ? Math.min(1f, (maxDist - dist) / (maxDist * 0.35f)) : 1f;
			float alpha = cfg.opacity * fade;
			OreFx.draw(shapes, h, o, cfg, t, rx, ry, rz, alpha);
			if (h.type() == OreType.ANCIENT_DEBRIS) {
				OreFx.debrisExtras(shapes, h, o, cfg, t, rx, ry, rz, alpha);
			}
		}

		drawTracers(shapes, mc, cfg, snap, cam, t);

		BUFFER.upload();
		var info = BUFFER.getExecuteInfo(draw);
		if (info != null) {
			GpuBufferSlice transform = RenderSystem.getDynamicUniforms()
				.writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
			var target = mc.gameRenderer.mainRenderTarget();
			try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				() -> OreSeerClient.MOD_ID + " xray pass", target.getColorTextureView(),
				Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
				pass.setPipeline(THROUGH_WALLS);
				RenderSystem.bindDefaultUniforms(pass);
				pass.setUniform("DynamicTransforms", transform);
				pass.setVertexBuffer(0, info.vertexBuffer().slice());
				pass.setIndexBuffer(info.indexBuffer(), info.indexType());
				pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
			}
		}
		BUFFER.endFrame();
	}

	private static void drawTracers(Shapes shapes, Minecraft mc, OreSeerConfig cfg, OreScanner.Snapshot snap, Vec3 cam, float t) {
		if (mc.player == null) {
			return;
		}
		Vec3 look = mc.player.getViewVector(1f);
		// start slightly in front of and below the eyes so the line is visible
		float sx = (float) look.x * 1.2f;
		float sy = (float) look.y * 1.2f - 0.35f;
		float sz = (float) look.z * 1.2f;

		if (cfg.debrisTracer && snap.nearestDebris() != null) {
			OreHit d = snap.nearestDebris();
			OreSetting o = cfg.setting(OreType.ANCIENT_DEBRIS);
			float pulse = 0.6f + 0.4f * (float) Math.sin(t * 4f * cfg.pulseSpeed);
			int c = Palette.isRainbow(o.colorIndex) ? Colors.hsv(t * 0.2f, 0.8f, 1f) : Palette.solid(o.colorIndex);
			shapes.ribbon(sx, sy, sz,
				(float) (d.x() + 0.5 - cam.x), (float) (d.y() + 0.5 - cam.y), (float) (d.z() + 0.5 - cam.z),
				0.05f, Colors.mulAlpha(c, 0.0f), Colors.mulAlpha(c, cfg.opacity * pulse));
		}
		if (cfg.tracerAll) {
			int count = 0;
			for (OreHit h : snap.hits()) {
				if (h.type() == OreType.ANCIENT_DEBRIS || count++ > 12) {
					continue;
				}
				int c = Palette.solid(cfg.setting(h.type()).colorIndex);
				shapes.ribbon(sx, sy, sz,
					(float) (h.x() + 0.5 - cam.x), (float) (h.y() + 0.5 - cam.y), (float) (h.z() + 0.5 - cam.z),
					0.025f, Colors.mulAlpha(c, 0f), Colors.mulAlpha(c, cfg.opacity * 0.5f));
			}
		}
	}

	private static float sq(float v) {
		return v * v;
	}
}
