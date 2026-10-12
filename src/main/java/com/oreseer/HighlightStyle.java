package com.oreseer;

/** Exotic through-the-wall highlight styles. Each one is drawn procedurally in {@link OreFx}. */
public enum HighlightStyle {
	HEARTBEAT("Heartbeat"),     // lub-dub pulsing core with a flashing halo
	CRYSTAL("Spinning Crystal"),// double rotating octahedron floating over the ore
	AURORA("Aurora Layers"),    // stacked colour-shifting slabs, northern-lights wave
	BEACON("Sky Beacon"),       // tall fading light pillar you can see from far away
	RIPPLE("Sonar Ripples"),    // expanding hollow shells, like sonar pings
	GLITCH("Glitch"),           // flickering displaced slices in cyan/magenta/yellow
	ORBIT("Atom Orbit"),        // little cubes orbiting the ore with comet trails
	PRISM("Prism Frame");       // rainbow wireframe, every edge a different hue

	public final String label;

	HighlightStyle(String label) {
		this.label = label;
	}

	public HighlightStyle next() {
		HighlightStyle[] v = values();
		return v[(ordinal() + 1) % v.length];
	}
}
