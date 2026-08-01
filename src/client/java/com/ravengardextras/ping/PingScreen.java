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
	private EditBox holdBox;
	private String errorMessage = "";

	public PingScreen(Screen parent, PingConfig config) {
		super(Component.literal("Party Ping"));
		this.parent = parent;
		this.config = config;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;

		this.addRenderableWidget(CycleButton.onOffBuilder(this.config.enabled)
				.create(centerX - 75, this.height / 2 - 88, 150, 20, Component.literal("Pings"),
						(button, value) -> this.config.enabled = value));

		this.commandBox = new EditBox(this.font, centerX - 75, this.height / 2 - 52, 150, 20,
				Component.literal("Party chat command"));
		this.commandBox.setMaxLength(128);
		this.commandBox.setValue(this.config.partyCommand);
		this.addRenderableWidget(this.commandBox);

		this.durationBox = new EditBox(this.font, centerX - 75, this.height / 2 - 12, 150, 20,
				Component.literal("Temporary ping duration (seconds)"));
		this.durationBox.setValue(Integer.toString(this.config.tempPingSeconds));
		this.addRenderableWidget(this.durationBox);

		this.holdBox = new EditBox(this.font, centerX - 75, this.height / 2 + 28, 150, 20,
				Component.literal("Hold-to-clear duration (seconds)"));
		this.holdBox.setValue(Integer.toString(this.config.clearAllHoldSeconds));
		this.addRenderableWidget(this.holdBox);

		this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> this.save())
				.bounds(centerX - 75, this.height / 2 + 76, 72, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> this.onClose())
				.bounds(centerX + 3, this.height / 2 + 76, 72, 20).build());
	}

	private void save() {
		int duration;
		int hold;
		try {
			duration = Integer.parseInt(this.durationBox.getValue().trim());
			hold = Integer.parseInt(this.holdBox.getValue().trim());
		} catch (NumberFormatException e) {
			this.errorMessage = "Durations must be whole numbers of seconds";
			return;
		}

		this.config.partyCommand = this.commandBox.getValue();
		this.config.tempPingSeconds = duration;
		this.config.clearAllHoldSeconds = hold;
		this.config.save();
		this.onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int centerX = this.width / 2;
		graphics.text(this.font, this.title, centerX - this.font.width(this.title) / 2, this.height / 2 - 104, 0xFFFFFFFF);

		graphics.text(this.font, "Party chat command (blank = show only to yourself)",
				centerX - 75, this.commandBox.getY() - 12, 0xFFAAAAAA);
		graphics.text(this.font, "Temp ping duration (marks never expire)",
				centerX - 75, this.durationBox.getY() - 12, 0xFFAAAAAA);
		graphics.text(this.font, "Hold mark key this long to clear all marks",
				centerX - 75, this.holdBox.getY() - 12, 0xFFAAAAAA);

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null) {
			String name = minecraft.player.getName().getString();
			int color = PingColors.preview(name);
			String label = "Your ping color:";
			int labelWidth = this.font.width(label);
			int swatchY = this.holdBox.getY() + 28;
			graphics.text(this.font, label, centerX - (labelWidth + 14) / 2, swatchY, 0xFFAAAAAA);
			int swatchX = centerX + (labelWidth + 14) / 2 - 10;
			graphics.fill(swatchX, swatchY - 1, swatchX + 10, swatchY + 9, 0xFFFFFFFF);
			graphics.fill(swatchX + 1, swatchY, swatchX + 9, swatchY + 8, color);
		}

		if (!this.errorMessage.isEmpty()) {
			graphics.text(this.font, this.errorMessage, centerX - this.font.width(this.errorMessage) / 2,
					this.height / 2 + 102, 0xFFFF5555);
		}
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(this.parent);
	}
}
