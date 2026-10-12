package com.oreseer;

import java.util.function.IntUnaryOperator;

import org.joml.Matrix4fc;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Tiny geometry toolkit that emits quads (4 vertices, position + colour) into the staged buffer.
 * All coordinates are camera-relative. Faces are not culled, so winding does not matter.
 */
public final class Shapes {
	private final VertexConsumer vc;
	private final Matrix4fc pose;
	private final int limit;
	private int quads;

	public Shapes(VertexConsumer vc, Matrix4fc pose, int limit) {
		this.vc = vc;
		this.pose = pose;
		this.limit = limit;
	}

	public int quadCount() {
		return quads;
	}

	public boolean full() {
		return quads >= limit;
	}

	private void v(float x, float y, float z, int c) {
		vc.addVertex(pose, x, y, z).setColor(c);
	}

	public void quadG(float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, float x4, float y4, float z4,
			int c1, int c2, int c3, int c4) {
		if (full()) {
			return;
		}
		v(x1, y1, z1, c1);
		v(x2, y2, z2, c2);
		v(x3, y3, z3, c3);
		v(x4, y4, z4, c4);
		quads++;
	}

	public void quad(float x1, float y1, float z1, float x2, float y2, float z2,
			float x3, float y3, float z3, float x4, float y4, float z4, int c) {
		quadG(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, c, c, c, c);
	}

	/** Triangle, emitted as a quad with a repeated vertex. */
	public void tri(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, int c) {
		quad(x1, y1, z1, x2, y2, z2, x3, y3, z3, x3, y3, z3, c);
	}

	public void box(float x0, float y0, float z0, float x1, float y1, float z1, int c) {
		int side = Colors.shade(c, 0.82f);
		int side2 = Colors.shade(c, 0.92f);
		int bottom = Colors.shade(c, 0.62f);
		quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, bottom);
		quad(x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, c);
		quad(x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, side);
		quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, side);
		quad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, side2);
		quad(x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, side2);
	}

	public void cube(float cx, float cy, float cz, float half, int c) {
		box(cx - half, cy - half, cz - half, cx + half, cy + half, cz + half, c);
	}

	/** Wireframe made of 12 thin boxes. {@code edgeColor} maps the edge index (0..11) to a colour. */
	public void frame(float x0, float y0, float z0, float x1, float y1, float z1, float th, IntUnaryOperator edgeColor) {
		int i = 0;
		for (int yi = 0; yi < 2; yi++) {
			for (int zi = 0; zi < 2; zi++) {
				float y = yi == 0 ? y0 : y1 - th;
				float z = zi == 0 ? z0 : z1 - th;
				box(x0, y, z, x1, y + th, z + th, edgeColor.applyAsInt(i++));
			}
		}
		for (int xi = 0; xi < 2; xi++) {
			for (int zi = 0; zi < 2; zi++) {
				float x = xi == 0 ? x0 : x1 - th;
				float z = zi == 0 ? z0 : z1 - th;
				box(x, y0, z, x + th, y1, z + th, edgeColor.applyAsInt(i++));
			}
		}
		for (int xi = 0; xi < 2; xi++) {
			for (int yi = 0; yi < 2; yi++) {
				float x = xi == 0 ? x0 : x1 - th;
				float y = yi == 0 ? y0 : y1 - th;
				box(x, y, z0, x + th, y + th, z1, edgeColor.applyAsInt(i++));
			}
		}
	}

	public void frame(float cx, float cy, float cz, float half, float th, int c) {
		frame(cx - half, cy - half, cz - half, cx + half, cy + half, cz + half, th, i -> c);
	}

	/** Faceted octahedron ("diamond") rotated around Y by {@code yaw}. */
	public void octahedron(float cx, float cy, float cz, float r, float tall, float yaw, int c) {
		float[] ex = new float[4];
		float[] ez = new float[4];
		for (int k = 0; k < 4; k++) {
			float a = yaw + k * (float) (Math.PI / 2);
			ex[k] = cx + (float) Math.cos(a) * r;
			ez[k] = cz + (float) Math.sin(a) * r;
		}
		float top = cy + r * tall;
		float bot = cy - r * tall;
		for (int k = 0; k < 4; k++) {
			int n = (k + 1) % 4;
			tri(cx, top, cz, ex[k], cy, ez[k], ex[n], cy, ez[n], Colors.shade(c, 1.0f - 0.12f * k));
			tri(cx, bot, cz, ex[n], cy, ez[n], ex[k], cy, ez[k], Colors.shade(c, 0.7f - 0.08f * k));
		}
	}

	/** Vertical square pillar whose colour fades from {@code cBottom} to {@code cTop}. */
	public void pillar(float cx, float yBottom, float cz, float height, float half, int cBottom, int cTop) {
		float y0 = yBottom;
		float y1 = yBottom + height;
		float x0 = cx - half;
		float x1 = cx + half;
		float z0 = cz - half;
		float z1 = cz + half;
		quadG(x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, cBottom, cBottom, cTop, cTop);
		quadG(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, cBottom, cBottom, cTop, cTop);
		quadG(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, cBottom, cBottom, cTop, cTop);
		quadG(x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, cBottom, cBottom, cTop, cTop);
	}

	/** Camera-facing ribbon from a to b (tracer line). */
	public void ribbon(float ax, float ay, float az, float bx, float by, float bz, float width, int cA, int cB) {
		float dx = bx - ax;
		float dy = by - ay;
		float dz = bz - az;
		float mx = (ax + bx) * 0.5f;
		float my = (ay + by) * 0.5f;
		float mz = (az + bz) * 0.5f;
		// side = d x m (m points from camera to the ribbon centre)
		float sx = dy * mz - dz * my;
		float sy = dz * mx - dx * mz;
		float sz = dx * my - dy * mx;
		float len = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
		if (len < 1e-5f) {
			return;
		}
		float k = width * 0.5f / len;
		sx *= k;
		sy *= k;
		sz *= k;
		quadG(ax - sx, ay - sy, az - sz, ax + sx, ay + sy, az + sz, bx + sx, by + sy, bz + sz, bx - sx, by - sy, bz - sz, cA, cA, cB, cB);
	}
}
