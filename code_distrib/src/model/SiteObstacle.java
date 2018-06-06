package model;

import java.awt.Color;


/**
 * 
 * @author Gisler Christophe
 *
 */
public class SiteObstacle extends SiteType {

    protected final float beta, gamma;
    protected final double[][] betaMatrix, gammaMatrix;
    protected final float energyNeededToIncreaseByOneDegree;
    protected final float energyLossFactor;
    
    public SiteObstacle(String name, Color color, float beta, float gamma, float energyNeededToIncreaseByOneDegree) {
	super(name, color);
	this.beta = beta;
	this.gamma = gamma;
	this.betaMatrix = getBetaMatrix(beta);
	this.gammaMatrix = getGammaMatrix(gamma);
	this.energyNeededToIncreaseByOneDegree = energyNeededToIncreaseByOneDegree;
	this.energyLossFactor = 1.0f - (gamma * gamma) - (beta * gamma) - (beta * beta);
    }
    
    public double getBeta() {
	return beta;
    }
    
    public double getGamma() {
	return gamma;
    }
    
    public float getEnergyNeededToIncreaseByOneDegree() {
	return energyNeededToIncreaseByOneDegree;
    }
    
    public float getEnergyLossFactor() {
	return energyLossFactor;
    }
    
    public double[][] getBetaMatrix() {
	return this.betaMatrix;
    }
    
    public double[][] getGammaMatrix() {
	return this.gammaMatrix;
    }
    
    protected static double[][] getGammaMatrix(float gamma) {
	double[][] gammaMatrix = {{ 0.0, -1.0,  0.0,  0.0 },
				 { -1.0,  0.0,  0.0,  0.0 },
				 {  0.0,  0.0,  0.0, -1.0 },
				 {  0.0,  0.0, -1.0,  0.0 }};
	for (int i = 0; i < gammaMatrix.length; i++) {
	    for (int j = 0; j < gammaMatrix[0].length; j++) {
		gammaMatrix[i][j] *= gamma; 
	    }
	}
	return gammaMatrix;
    }

    protected static double[][] getBetaMatrix(float beta) {
	double[][] betaMatrix = {{  1.0, -1.0,  1.0,  1.0 },
				 { -1.0,  1.0,  1.0,  1.0 },
		   		 {  1.0,  1.0,  1.0, -1.0 },
		   		 {  1.0,  1.0, -1.0,  1.0 }};
	for (int i = 0; i < betaMatrix.length; i++) {
	    for (int j = 0; j < betaMatrix[0].length; j++) {
		betaMatrix[i][j] *= beta / 2.0; 
	    }
	}
	return betaMatrix;
    }
    
}
