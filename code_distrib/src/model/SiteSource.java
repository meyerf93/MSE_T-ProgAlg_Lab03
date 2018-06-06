package model;

import java.awt.Color;

/**
 * 
 * @author Gisler Christophe
 *
 */
public class SiteSource extends SiteType {

	private final long frequency, amplitude;
	private boolean isConstant;

	public SiteSource(String name, Color color, long frequency, long amplitude) {
		super(name, color);
		this.isConstant = false;
		this.frequency = frequency;
		this.amplitude = amplitude;
	}

	public long getFrequency() {
		return frequency;
	}

	public long getAmplitude() {
		return amplitude;
	}

	/**
	 * 
	 * @param deltaT
	 *            in seconds
	 * @return constant value for the 4 directions if source is constant, otherwise
	 *         amplitude * sin(2 * Pi * frequency * deltaTsec)
	 */
	public float getValue(double deltaTsec) {
		float value = amplitude;
		if (!isConstant) {
			value *= Math.sin(2 * Math.PI * frequency * deltaTsec);
		}
		return value;
	}

}
