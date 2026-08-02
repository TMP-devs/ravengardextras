package com.ravengardextras.dashboard;

/** Feature groupings shown as tabs across the top of the dashboard. */
public enum DashboardTab {
	HIGHLIGHTERS("Highlighters"),
	PARTY("Party"),
	INVENTORY("Inventory");

	public final String label;

	DashboardTab(String label) {
		this.label = label;
	}
}
