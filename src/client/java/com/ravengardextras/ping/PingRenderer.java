package com.ravengardextras.ping;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ravengardextras.RavengardExtrasClient;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Draws each active ping as a camera-facing diamond above the pinged block (a flashing
 * red circle for alerts), plus a "Name (Nm)" label. Both use see-through render paths
 * (the same mechanism as nametags) so they are visible through walls. The shape's world
 * size grows with the square root of distance (not linearly) so it stays readable at
 * range without fully cancelling perspective — far pings still look smaller than close
 * ones, giving a sense of depth instead of every ping reading as the same flat size.
 * Marks (permanent) draw as a bold X-cross instead of a diamond so they read apart
 * from temp pings at a glance even at a distance, rather than relying on a subtle
 * hollow-vs-filled difference that's easy to miss.
 */
public final class PingRenderer {
	// Equal width/height so the diamond reads as a "perfect" diamond (a square on its
	// side) rather than an elongated rhombus - the X-cross shares these corners too,
	// so keeping them equal is also what stops the cross from looking stretched tall.
	private static final float HALF_WIDTH = 0.42F;
	private static final float HALF_HEIGHT = 0.42F;
	private static final float EDGE = 0.11F;
	private static final float CROSS_HALF_THICKNESS = 0.11F;
	/** Diamond center floats this far above the bottom of the pinged block (shared with aim-to-clear). */
	public static final float HOVER = 1.7F;
	private static final float SCALE_PER_SQRT_BLOCK = 0.14F;
	private static final float MAX_SCALE = 5.0F;
	private static final long FADE_MILLIS = 1000;
	private static final int ALERT_COLOR = 0xFFFF4040;
	private static final long FLASH_CYCLE_MILLIS = 500;
	private static final int CIRCLE_SEGMENTS = 24;
	private static final float CIRCLE_RADIUS = 0.5F;
	private static final float CIRCLE_EDGE = 0.09F;

	private PingRenderer() {
	}

	public static void register() {
		LevelRenderEvents.COLLECT_SUBMITS.register(PingRenderer::collectSubmits);
	}

	private static void collectSubmits(LevelRenderContext context) {
		PingConfig config = RavengardExtrasClient.PING_CONFIG;
		if (!config.enabled) {
			return;
		}
		long now = System.currentTimeMillis();
		long duration = config.tempPingSeconds * 1000L;
		List<PingManager.Ping> pings = PingManager.active(now, duration);
		if (pings.isEmpty()) {
			return;
		}

		CameraRenderState camera = context.levelState().cameraRenderState;
		Vec3 cameraPos = camera.pos;
		PoseStack poseStack = context.poseStack();

		for (PingManager.Ping ping : pings) {
			double x = ping.pos().getX() + 0.5 - cameraPos.x;
			double y = ping.pos().getY() + HOVER - cameraPos.y;
			double z = ping.pos().getZ() + 0.5 - cameraPos.z;
			double distance = Math.sqrt(x * x + y * y + z * z);

			float scale = Mth.clamp((float) Math.sqrt(distance) * SCALE_PER_SQRT_BLOCK, 0.25F, MAX_SCALE);
			if (!ping.permanent()) {
				long remaining = duration - (now - ping.createdAtMillis());
				if (remaining < FADE_MILLIS) {
					scale *= remaining / (float) FADE_MILLIS;
				}
				if (scale <= 0.0F) {
					continue;
				}
			}

			boolean alert = ping.kind() == PingManager.Kind.ALERT;
			int color = alert ? ALERT_COLOR : PingColors.colorFor(ping.sender());
			// Marks draw hollow (rim only, see drawDiamond's filled flag) so they read
			// apart from temp pings at a glance; alerts flash between full and dim red.
			int shapeColor = color;
			if (alert) {
				float phase = (now % FLASH_CYCLE_MILLIS) / (float) FLASH_CYCLE_MILLIS;
				shapeColor = scaleBrightness(color, 0.4F + 0.6F * (0.5F + 0.5F * Mth.sin(phase * Mth.TWO_PI)));
			}

			poseStack.pushPose();
			poseStack.translate(x, y, z);

			// Label above the shape. submitNameTag applies the camera billboard and its
			// own 0.025 text scale internally, so it gets the pose *before* our billboard
			// rotation. It uses its own scale, clamped so the text never drops below
			// normal nametag size up close (the shape's scale shrinks toward zero).
			// submitNameTag also nudges the label up by its own +0.5, so our offset only
			// needs to add a small gap above the shape, not the shape's full half-height twice.
			float labelScale = Math.max(1.0F, scale);
			String labelText = alert
					? possessive(ping.sender()) + " Alert"
					: ping.label() != null ? ping.label() : ping.sender();
			Component label = Component.literal(labelText + " (" + Math.round(distance) + "m)")
					.withColor(color & 0xFFFFFF);
			poseStack.pushPose();
			poseStack.scale(labelScale, labelScale, labelScale);
			context.submitNodeCollector().submitNameTag(
					poseStack, new Vec3(0.0, (HALF_HEIGHT * scale) / labelScale - 0.2, 0.0), 0, label,
					true, LightCoordsUtil.FULL_BRIGHT, distance * distance, camera);
			poseStack.popPose();

			poseStack.scale(scale, scale, scale);
			poseStack.mulPose(camera.orientation);
			final int finalShapeColor = shapeColor;
			boolean permanent = ping.permanent();
			context.submitNodeCollector().submitCustomGeometry(
					poseStack, RenderTypes.textBackgroundSeeThrough(),
					alert ? (pose, buffer) -> drawCircle(pose, buffer, finalShapeColor)
							: permanent ? (pose, buffer) -> drawCross(pose, buffer, finalShapeColor)
							: (pose, buffer) -> drawDiamond(pose, buffer, finalShapeColor));
			poseStack.popPose();
		}
	}

	/** Scales an ARGB color's RGB channels by factor (0..1), keeping alpha. */
	private static int scaleBrightness(int argb, float factor) {
		int r = (int) (((argb >> 16) & 0xFF) * factor);
		int g = (int) (((argb >> 8) & 0xFF) * factor);
		int b = (int) ((argb & 0xFF) * factor);
		return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
	}

	private static void drawDiamond(PoseStack.Pose pose, VertexConsumer buffer, int color) {
		int fill = (color & 0x00FFFFFF) | 0xE6000000;

		// Center fill, both windings so it can't be culled away.
		quad(pose, buffer, fill,
				0.0F, HALF_HEIGHT, HALF_WIDTH, 0.0F, 0.0F, -HALF_HEIGHT, -HALF_WIDTH, 0.0F);
		quad(pose, buffer, fill,
				-HALF_WIDTH, 0.0F, 0.0F, -HALF_HEIGHT, HALF_WIDTH, 0.0F, 0.0F, HALF_HEIGHT);

		// Solid rim: a slightly larger diamond ring built from four edge quads.
		float ow = HALF_WIDTH + EDGE;
		float oh = HALF_HEIGHT + (EDGE * HALF_HEIGHT / HALF_WIDTH);
		edge(pose, buffer, color, 0.0F, HALF_HEIGHT, HALF_WIDTH, 0.0F, 0.0F, oh, ow, 0.0F);
		edge(pose, buffer, color, HALF_WIDTH, 0.0F, 0.0F, -HALF_HEIGHT, ow, 0.0F, 0.0F, -oh);
		edge(pose, buffer, color, 0.0F, -HALF_HEIGHT, -HALF_WIDTH, 0.0F, 0.0F, -oh, -ow, 0.0F);
		edge(pose, buffer, color, -HALF_WIDTH, 0.0F, 0.0F, HALF_HEIGHT, -ow, 0.0F, 0.0F, oh);
	}

	/** Bold X-cross for marks: two thick diagonal bars, always fully colored (never hollow). */
	private static void drawCross(PoseStack.Pose pose, VertexConsumer buffer, int color) {
		bar(pose, buffer, color, -HALF_WIDTH, -HALF_HEIGHT, HALF_WIDTH, HALF_HEIGHT, CROSS_HALF_THICKNESS);
		bar(pose, buffer, color, -HALF_WIDTH, HALF_HEIGHT, HALF_WIDTH, -HALF_HEIGHT, CROSS_HALF_THICKNESS);
	}

	/** A thick line segment from (ax,ay) to (bx,by), double-sided so it can't be culled away. */
	private static void bar(PoseStack.Pose pose, VertexConsumer buffer, int color,
	                        float ax, float ay, float bx, float by, float halfThickness) {
		float dx = bx - ax;
		float dy = by - ay;
		float len = Mth.sqrt(dx * dx + dy * dy);
		float px = -dy / len * halfThickness;
		float py = dx / len * halfThickness;
		quad(pose, buffer, color, ax + px, ay + py, bx + px, by + py, bx - px, by - py, ax - px, ay - py);
		quad(pose, buffer, color, ax - px, ay - py, bx - px, by - py, bx + px, by + py, ax + px, ay + py);
	}

	/** A ring with a translucent fill, same visual language as the diamond. */
	private static void drawCircle(PoseStack.Pose pose, VertexConsumer buffer, int color) {
		int fill = (color & 0x00FFFFFF) | 0xD0000000;
		float outer = CIRCLE_RADIUS + CIRCLE_EDGE;
		for (int i = 0; i < CIRCLE_SEGMENTS; i++) {
			float a1 = (float) (i * Math.TAU / CIRCLE_SEGMENTS);
			float a2 = (float) ((i + 1) * Math.TAU / CIRCLE_SEGMENTS);
			float ix1 = Mth.cos(a1) * CIRCLE_RADIUS;
			float iy1 = Mth.sin(a1) * CIRCLE_RADIUS;
			float ix2 = Mth.cos(a2) * CIRCLE_RADIUS;
			float iy2 = Mth.sin(a2) * CIRCLE_RADIUS;
			// Fill wedge (quad degenerate at the center), both windings so it can't be culled.
			quad(pose, buffer, fill, 0.0F, 0.0F, ix1, iy1, ix2, iy2, 0.0F, 0.0F);
			quad(pose, buffer, fill, 0.0F, 0.0F, ix2, iy2, ix1, iy1, 0.0F, 0.0F);
			// Solid rim segment, double-sided via edge().
			edge(pose, buffer, color, ix1, iy1, ix2, iy2,
					Mth.cos(a1) * outer, Mth.sin(a1) * outer, Mth.cos(a2) * outer, Mth.sin(a2) * outer);
		}
	}

	/** "Scrolls" -> "Scrolls'", "Bob" -> "Bob's". */
	static String possessive(String name) {
		return name.endsWith("s") || name.endsWith("S") ? name + "'" : name + "'s";
	}

	/** One rim segment: inner edge (i1->i2) to its outer counterpart (o1->o2), double-sided. */
	private static void edge(PoseStack.Pose pose, VertexConsumer buffer, int color,
	                         float i1x, float i1y, float i2x, float i2y,
	                         float o1x, float o1y, float o2x, float o2y) {
		quad(pose, buffer, color, i1x, i1y, i2x, i2y, o2x, o2y, o1x, o1y);
		quad(pose, buffer, color, o1x, o1y, o2x, o2y, i2x, i2y, i1x, i1y);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int color,
	                         float x1, float y1, float x2, float y2,
	                         float x3, float y3, float x4, float y4) {
		buffer.addVertex(pose, x1, y1, 0.0F).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
		buffer.addVertex(pose, x2, y2, 0.0F).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
		buffer.addVertex(pose, x3, y3, 0.0F).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
		buffer.addVertex(pose, x4, y4, 0.0F).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
	}
}
