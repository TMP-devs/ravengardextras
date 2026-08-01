package com.ravengardextras.ping;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Party Ping feature screen: master toggle, the party chat command pings are sent
 * through, and how long a ping stays up. Also previews the color your own pings
 * get (derived from your name, same on every teammate's screen).
 */
public class PingScreen extends Screen {
	private final Screen parent;
	private final PingConfig config;

	private EditBox commandBox;
	private EditBox durationBox;
	private String errorMessage = "";

	public PingScreen(Screen parent, PingConfig config) {
		super(Component.literal("Party Ping"));
		this.parent = parent;
		this.config = config;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int rowY = this.height / 2 - 80;
		int gap = 38;

		this.addRenderableWidget(CycleButton.onOffBuilder(this.config.enabled)
				.create(centerX - 75, rowY, 150, 20, Component.literal("Pings"),
						(button, value) -> this.config.enabled = value));
		rowY += 30;

		this.commandBox = new EditBox(this.font, centerX - 75, rowY + 12, 150, 20,
				Component.literal("Party chat command"));
		this.commandBox.setValue(this.config.partyCommand);
		this.addRenderableWidget(this.commandBox);
		rowY += gap + 12;

		this.durationBox = new EditBox(this.font, centerX - 75, rowY + 12, 150, 20,
				Component.literal("Ping duration (seconds)"));
		this.durationBox.setValue(Integer.toString(this.config.pingDurationSeconds));
		this.addRenderableWidget(this.durationBox);
		rowY += gap + 18;

		this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> this.save())
				.bounds(centerX - 75, rowY, 72, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> this.onClose())
				.bounds(centerX + 3, rowY, 72, 20).build());
	}

	private void save() {
		int duration;
		try {
			duration = Integer.parseInt(this.durationBox.getValue().trim());
		} catch (NumberFormatException e) {
			this.errorMessage = "Duration must be a whole number of seconds";
			return;
		}

		this.config.partyCommand = this.commandBox.getValue();
		this.config.pingDurationSeconds = duration;
		this.config.save();
		this.onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int centerX = this.width / 2;
		graphics.text(this.font, this.title, centerX - this.font.width(this.title) / 2, this.height / 2 - 104, 0xFFFFFF);

		graphics.text(this.font, "Party chat command (blank = show only to yourself)",
				centerX - 75, this.commandBox.getY() - 12, 0xAAAAAA);
		graphics.text(this.font, "Ping duration in seconds",
				centerX - 75, this.durationBox.getY() - 12, 0xAAAAAA);

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null) {
			String name = minecraft.player.getName().getString();
			java.util.List<String> names = new java.util.ArrayList<>();
			for (PingManager.Ping ping : PingManager.active(System.currentTimeMillis(), this.config.pingDurationSeconds * 1000L)) {
				names.add(ping.sender());
			}
			names.add(name);
			int color = PingColors.assign(names).get(name.toLowerCase(java.util.Locale.ROOT));
			String label = "Your ping color:";
			int labelWidth = this.font.width(label);
			int swatchY = this.durationBox.getY() + 30;
			graphics.text(this.font, label, centerX - (labelWidth + 14) / 2, swatchY, 0xAAAAAA);
			int swatchX = centerX + (labelWidth + 14) / 2 - 10;
			graphics.fill(swatchX, swatchY - 1, swatchX + 10, swatchY + 9, 0xFFFFFFFF);
			graphics.fill(swatchX + 1, swatchY, swatchX + 9, swatchY + 8, color);
		}

		if (!this.errorMessage.isEmpty()) {
			graphics.text(this.font, this.errorMessage, centerX - this.font.width(this.errorMessage) / 2,
					this.height / 2 + 92, 0xFF5555);
		}
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(this.parent);
	}
}
