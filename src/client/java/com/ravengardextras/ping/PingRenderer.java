package com.ravengardextras.ping;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ravengardextras.RavengardExtrasClient;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Draws each active ping as a camera-facing diamond above the pinged block, plus a
 * "Name (Nm)" label. Both use see-through render paths (the same mechanism as
 * nametags) so they are visible through walls. The diamond's world size grows with
 * distance so it reads at range but never fills the screen up close.
 */
public final class PingRenderer {
	private static final float HALF_WIDTH = 0.35F;
	private static final float HALF_HEIGHT = 0.5F;
	private static final float EDGE = 0.08F;
	/** Diamond center floats this far above the bottom of the pinged block. */
	private static final float HOVER = 1.7F;
	private static final float SCALE_PER_BLOCK = 0.08F;
	private static final long FADE_MILLIS = 1000;

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
		long duration = config.pingDurationSeconds * 1000L;
		List<PingManager.Ping> pings = PingManager.active(now, duration);
		if (pings.isEmpty()) {
			return;
		}

		CameraRenderState camera = context.levelState().cameraRenderState;
		Vec3 cameraPos = camera.pos;
		PoseStack poseStack = context.poseStack();

		// The local player's name is always in the pool so your color is reserved
		// on your own screen even before your first ping.
		List<String> names = new ArrayList<>(pings.size() + 1);
		for (PingManager.Ping ping : pings) {
			names.add(ping.sender());
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null) {
			names.add(minecraft.player.getName().getString());
		}
		Map<String, Integer> colors = PingColors.assign(names);

		for (PingManager.Ping ping : pings) {
			double x = ping.pos().getX() + 0.5 - cameraPos.x;
			double y = ping.pos().getY() + HOVER - cameraPos.y;
			double z = ping.pos().getZ() + 0.5 - cameraPos.z;
			double distance = Math.sqrt(x * x + y * y + z * z);

			float scale = Mth.clamp((float) distance * SCALE_PER_BLOCK, 0.25F, 12.0F);
			long remaining = duration - (now - ping.createdAtMillis());
			if (remaining < FADE_MILLIS) {
				scale *= remaining / (float) FADE_MILLIS;
			}
			if (scale <= 0.0F) {
				continue;
			}

			int color = colors.get(ping.sender().toLowerCase(Locale.ROOT));

			poseStack.pushPose();
			poseStack.translate(x, y, z);
			poseStack.scale(scale, scale, scale);

			// Label below the diamond. submitNameTag applies the camera billboard and its
			// own 0.025 text scale internally, so it gets the pose *before* our billboard
			// rotation; our uniform scale stays on the pose so the label keeps pace with
			// the diamond. The attachment y compensates submitNameTag's +0.5 offset.
			Component label = Component.literal(ping.sender() + " (" + Math.round(distance) + "m)")
					.withColor(color & 0xFFFFFF);
			context.submitNodeCollector().submitNameTag(
					poseStack, new Vec3(0.0, -HALF_HEIGHT - 0.75, 0.0), 0, label,
					true, LightCoordsUtil.FULL_BRIGHT, camera);

			poseStack.mulPose(camera.orientation);
			context.submitNodeCollector().submitCustomGeometry(
					poseStack, RenderTypes.textBackgroundSeeThrough(),
					(pose, buffer) -> drawDiamond(pose, buffer, color));
			poseStack.popPose();
		}
	}

	private static void drawDiamond(PoseStack.Pose pose, VertexConsumer buffer, int color) {
		int fill = (color & 0x00FFFFFF) | 0xB0000000;

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
		buffer.addVertex(pose, x1, y1, 0.0F).setColor(color);
		buffer.addVertex(pose, x2, y2, 0.0F).setColor(color);
		buffer.addVertex(pose, x3, y3, 0.0F).setColor(color);
		buffer.addVertex(pose, x4, y4, 0.0F).setColor(color);
	}
}
