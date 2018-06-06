package model;

import java.io.Serializable;
import java.util.List;

/**
 * 
 * @author Gisler Christophe
 *
 */
public class Site implements Serializable{

    public static final int TOP = 0;
    public static final int BOTTOM = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;
    
    private int typeIndex; 
    private double[] flows;
    private float temperature;
    
    public Site(int typeIndex, float initialTemperature) {
    	this.typeIndex = typeIndex;
    	this.flows = new double[4];
    	this.temperature = initialTemperature;
    }

    public double[] getFlows() {
        return flows;
    }

    public void setFlows(double[] flows) {
        this.flows = flows;
    }

    public void setFlow(int index, double flow) {
        this.flows[index] = flow;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public void addTemperature(float deltaTemperature) {
        this.temperature += deltaTemperature;
    }

    public int getTypeIndex() {
        return typeIndex;
    }
    
    public void setTypeIndex(int typeIndex) {
	this.typeIndex = typeIndex;
    }

    public void resetFlows() {
		for (int i = 0; i < flows.length; i++) { 
		    flows[i] = 0;
		}
    }
    
}
