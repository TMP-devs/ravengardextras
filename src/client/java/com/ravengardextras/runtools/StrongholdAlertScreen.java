package com.ravengardextras.runtools;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Settings for the Stronghold sound alert: on/off, growl loudness, and a Test button. */
public class StrongholdAlertScreen extends Screen {
	private final Screen parent;
	private final RunToolsConfig config;
	private float volume;

	public StrongholdAlertScreen(Screen parent, RunToolsConfig config) {
		super(Component.literal("Stronghold Alert"));
		this.parent = parent;
		this.config = config;
		this.volume = config.strongholdVolume;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int rowY = this.height / 2 - 50;
		int gap = 24;

		this.addRenderableWidget(CycleButton.onOffBuilder(this.config.strongholdAlertEnabled)
				.create(centerX - 75, rowY, 150, 20, Component.literal("Alert"),
						(button, value) -> this.config.strongholdAlertEnabled = value));
		rowY += gap;

		this.addRenderableWidget(new AbstractSliderButton(centerX - 75, rowY, 150, 20,
				volumeLabel(this.volume), this.volume) {
			@Override
			protected void updateMessage() {
				setMessage(volumeLabel((float) this.value));
			}

			@Override
			protected void applyValue() {
				StrongholdAlertScreen.this.volume = (float) this.value;
			}
		});
		rowY += gap;

		this.addRenderableWidget(Button.builder(Component.literal("Test growl"), button -> StrongholdAlert.play(this.volume))
				.bounds(centerX - 75, rowY, 150, 20).build());
		rowY += gap + 8;

		this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> this.save())
				.bounds(centerX - 75, rowY, 72, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> this.onClose())
				.bounds(centerX + 3, rowY, 72, 20).build());
	}

	private static Component volumeLabel(float value) {
		return Component.literal("Volume: " + Math.round(value * 100) + "%");
	}

	private void save() {
		this.config.strongholdVolume = this.volume;
		this.config.save();
		this.onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		int centerX = this.width / 2;
		graphics.text(this.font, this.title, centerX - this.font.width(this.title) / 2, this.height / 2 - 80, 0xFFFFFF);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(this.parent);
	}
}
