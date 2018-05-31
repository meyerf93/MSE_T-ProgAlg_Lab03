package controller;

import java.util.HashMap;
import java.util.Map;

import model.Site;
import model.SiteObstacle;
import model.SiteSource;
import model.SiteType;

import java.util.concurrent.Semaphore;

import ch.icosys.popjava.core.PopJava;
import ch.icosys.popjava.core.annotation.POPAsyncConc;
import ch.icosys.popjava.core.annotation.POPSyncConc;
import ch.icosys.popjava.core.annotation.POPAsyncSeq;
import ch.icosys.popjava.core.annotation.POPClass;
import ch.icosys.popjava.core.annotation.POPConfig;
import ch.icosys.popjava.core.annotation.POPConfig.Type;
import ch.icosys.popjava.core.annotation.POPObjectDescription;
import ch.icosys.popjava.core.annotation.POPParameter;
import ch.icosys.popjava.core.annotation.POPParameter.Direction;
import ch.icosys.popjava.core.annotation.POPSyncMutex;
import ch.icosys.popjava.core.annotation.POPSyncSeq;

/**
 * This class is responsible to simulate a particular region of the simulation grid.
 * @author beat
 *
 */
@POPClass
public class PixelFlowRegion {

	public static enum Direction{
		UP(3),
		DOWN(2),
		LEFT(1),
		RIGHT(0);

		private final int index;

		Direction(int index){
			this.index = index;
		}
	}

	private final SiteType [] siteTypes;
    private Site[][] sites;
    private Site[][] tempSites;
    private int x, y;
    private final double deltaTimePerIteration;

    private int globalRows, globalCols;
	private int cores;

    private double [][] neighbourFlows = new double[Direction.values().length][];

    private Map<Direction, PixelFlowRegion> neighbours = new HashMap<>();

    public PixelFlowRegion(){
        sites = null;
        deltaTimePerIteration = 0;
        siteTypes = null;
    }

	@POPObjectDescription(jvmParameters = "-XX:+UseG1GC -Xmx5500m -XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=12")
    public PixelFlowRegion(final double deltaTimePerIteration, SiteType [] siteTypes, @POPConfig(Type.URL) String url, int cores) {
    	this.deltaTimePerIteration = deltaTimePerIteration;
    	this.siteTypes = siteTypes;
		this.cores = cores;
    }

	@POPSyncConc
    public void createSites(int x, int y, int width, int height, int globalCols, int globalRows, boolean copy) {
    	this.x = x;
    	this.y = y;

    	this.globalRows = globalRows;
    	this.globalCols = globalCols;

    	if(sites == null || !copy) {
    		this.sites = new Site[width][height];
    	}else if(sites.length != width || sites[0].length != height){
    		Site [][] newSites = new Site[width][height];

    		for(int xLoop = 0; xLoop < sites.length && xLoop < width; xLoop++) {
            	for(int yLoop = 0; yLoop < sites[x].length && yLoop < height; yLoop++) {
            		newSites[xLoop][yLoop] = sites[xLoop][yLoop];
            	}
    		}

    		this.sites = newSites;
    	}

    	neighbourFlows[Direction.UP.index] = new double[width];
    	neighbourFlows[Direction.DOWN.index] = new double[width];
    	neighbourFlows[Direction.LEFT.index] = new double[height];
    	neighbourFlows[Direction.RIGHT.index] = new double[height];
    }

    private PixelFlowRegion fake;

	@POPSyncConc
    public void setNeighbour(PixelFlowRegion neighbour, Direction dir) {
        fake = neighbour;//TODO: This is a POPJava workaround as otherwise the connection is closed to neighbour after function ends
    	neighbours.put(dir, neighbour);
    }

	@POPSyncConc
    public void initSites(int defaultSiteTypeIndex, float initialTemperature, boolean copy) {
    	for(int x = 0; x < sites.length; x++) {
        	for(int y = 0; y < sites[x].length; y++) {
        		if(copy && sites[x][y] != null) {
        			sites[x][y].resetFlows();
        			sites[x][y].setTemperature(initialTemperature);
        		}else {
        			sites[x][y] = new Site(defaultSiteTypeIndex, initialTemperature);
        		}
        	}
    	}
    }

	@POPSyncSeq
    public Site getSite(int row, int col) {
    	return sites[col - x][row - y];
    }

	@POPSyncConc
    public void setSiteType(int col, int row, int type) {
    	sites[col - x][row - y].setTypeIndex(type);
    }

	@POPSyncConc
    public void setFlow(int col, int row, int flowIndex, double value) {
    	tempSites[col - x][row - y].getFlows()[flowIndex] = value;
    }

	@POPSyncSeq
    public Site[][] getAllSites(){
        return sites;
    }

	@POPSyncSeq
    public int[][] getSiteTypes() {
        int [][] types = new int[sites.length][sites[0].length];

        for(int x = 0; x < types.length; x++) {
            for(int y = 0; y < types[0].length; y++) {
                types[x][y] = sites[x][y].getTypeIndex();
            }
        }

        return types;
    }

	@POPSyncConc
	public void prepareFlowUpdate() {
    	//Create copy of current sites
    	tempSites = new Site[sites.length][sites[0].length];
		for (int row = 0; row < tempSites.length; row++) {
			for (int col = 0; col < tempSites[0].length; col++) {
				tempSites[row][col] = new Site(sites[row][col].getTypeIndex(), sites[row][col].getTemperature());
			}
		}
	}

	@POPAsyncConc
    public void updateFlows(double elapsedTime) {

			PixelFlowRegion me = (PixelFlowRegion) PopJava.getThis(this);

    	for (int x = 0; x < sites.length; x++) {
			for (int y = 0; y < sites[0].length; y++) {
				SiteType siteTypeXY = getSiteType(x, y);
				if (siteTypeXY instanceof SiteSource) {
					float sourceValue = ((SiteSource) siteTypeXY).getValue(elapsedTime);

					me.updateTempSite(x + 1, y, Direction.RIGHT, sourceValue);
					me.updateTempSite(x - 1, y, Direction.LEFT, sourceValue);
					me.updateTempSite(x, y + 1, Direction.DOWN, sourceValue);
					me.updateTempSite(x, y - 1, Direction.UP, sourceValue);

				} else {
					SiteObstacle so = (SiteObstacle) siteTypeXY;
					double[] gammaFlowPartXY = multiply(so.getGammaMatrix(), sites[x][y].getFlows());
					double[] betaFlowPartXY = multiply(so.getBetaMatrix(), sites[x][y].getFlows());

					me.updateTempSite(x + 1, y, Direction.RIGHT, gammaFlowPartXY[Direction.RIGHT.index] + betaFlowPartXY[Direction.RIGHT.index]);
					me.updateTempSite(x - 1, y, Direction.LEFT, gammaFlowPartXY[Direction.LEFT.index] + betaFlowPartXY[Direction.LEFT.index]);
					me.updateTempSite(x, y + 1, Direction.DOWN, gammaFlowPartXY[Direction.DOWN.index] + betaFlowPartXY[Direction.DOWN.index]);
					me.updateTempSite(x, y - 1, Direction.UP, gammaFlowPartXY[Direction.UP.index] + betaFlowPartXY[Direction.UP.index]);
				}
			}
		}

    	sendFlowBuffers();
    }

	@POPSyncSeq
    private void sendFlowBuffers() {
    	for(Direction dir : Direction.values()) {
    		if(neighbours.containsKey(dir)) {
    			int xTarget = x;
    			int yTarget = y;

    			Direction flowDir = null;
    			if(dir == Direction.UP) {
    				yTarget--;
    				flowDir = Direction.DOWN;
    			}else if(dir == Direction.DOWN) {
    				yTarget += sites[0].length;
    				flowDir = Direction.UP;
    			}else if(dir == Direction.LEFT) {
    				xTarget--;
    				flowDir = Direction.RIGHT;
    			}else if(dir == Direction.RIGHT) {
    				xTarget += sites.length;
    				flowDir = Direction.LEFT;
    			}

    			try {
    			    neighbours.get(dir).setFlows(neighbourFlows[dir.index], xTarget, yTarget, dir);
    			}catch(Throwable e) {
    			    e.printStackTrace();
    			    System.exit(0);
    			}
    		}
    	}
    }

	@POPSyncConc
    public void setFlows(double [] flows, int xTarget, int yTarget, Direction dir) {
    	for(int i = 0; i < flows.length; i++) {
    		int col;
    		int row;

        	if(dir == Direction.UP || dir == Direction.DOWN) {
        		col = xTarget + i;
        		row = yTarget ;
        	}else {
        		col = xTarget ;
        		row = yTarget + i;
        	}

        	setFlow(col, row, dir.index, flows[i]);
    	}
    }

	@POPSyncSeq
    public void updateTempSite(int x, int y, Direction flowDir, double value) {
    	//We are in our own grid
    	if(x >= 0 && y >= 0 && x < sites.length && y < sites[0].length) {
    		tempSites[x][y].getFlows()[flowDir.index] = value;
    	}else {
    		int realX = x + this.x;
    		int realY = y + this.y;

    		if(realX >= 0 && realY >= 0 && realX < globalCols && realY < globalRows) {
    			if(x < 0) {
    				neighbourFlows[Direction.LEFT.index][y] = value;
    			}else if(y < 0) {
    				neighbourFlows[Direction.UP.index][x] = value;
    			}else if(x >= sites.length) {
    				neighbourFlows[Direction.RIGHT.index][y] = value;
    			}else if(y >= sites[0].length) {
    				neighbourFlows[Direction.DOWN.index][x] = value;
    			}
    		}
    	}
    }

	@POPSyncConc
	public void finishFlowUpdate() {
		this.sites = tempSites;
	}

	@POPSyncConc
    public void updateTemperatures() {
        for (int x = 0; x < sites.length; x++) {
            for (int y = 0; y < sites[0].length; y++) {
                SiteType s = getSiteType(x, y);

                // If site s is a source return by default the initial temperature of the system
                // (because here we don't care with the temperature of a source site)
                if (s instanceof SiteSource) {
                    continue;
                }

                // Else the site is not a source, therefore is an obstacle and has a beta and a
                // gamma value
                SiteObstacle so = (SiteObstacle) s;

                // First retrieve the flows in the 4 directions at the point
                double f0 = sites[x][y].getFlows()[0];
                double f1 = sites[x][y].getFlows()[1];
                double f2 = sites[x][y].getFlows()[2];
                double f3 = sites[x][y].getFlows()[3];

                float factor = so.getEnergyLossFactor(); // 1.0 - gamma^2 - gamma * beta - beta^2
                float energy = (float) (factor * deltaTimePerIteration * (f0 * f0 + f1 * f1 + f2 * f2 + f3 * f3)); // [J]
                float deltaTemperature = (float) energy / so.getEnergyNeededToIncreaseByOneDegree();

                sites[x][y].addTemperature(deltaTemperature);
            }
        }

    }
	@POPSyncConc
    private SiteType getSiteType(int row, int col) {
		return siteTypes[sites[row][col].getTypeIndex()];
	}

	@POPSyncConc
	private static double[] multiply(double[][] matrix, double[] vector) {
		double[] result = new double[matrix.length];

		// Potential omp
		for (int i = 0; i < result.length; i++) {
			for (int j = 0; j < vector.length; j++) {
				result[i] += matrix[i][j] * vector[j];
			}
		}
		return result;
	}
		@POPSyncSeq
    public double[][] getGlobalFlows(double elapsedTime) {
        double [][] flows = new double[sites.length][sites[0].length];

        for(int x = 0; x < flows.length; x++) {
            for(int y = 0; y < flows[0].length; y++) {
                flows[x][y] = getGlobalFlow(sites[x][y], elapsedTime);
            }
        }

        return flows;
    }
		@POPSyncSeq
    public double getGlobalFlowAtPosition(int row, int col, double elapsedTime) {
        Site site = getSite(row, col);

        return getGlobalFlow(site, elapsedTime);
    }

    private double getGlobalFlow(Site site, double elapsedTime) {
        SiteType s = siteTypes[site.getTypeIndex()];

        if (s instanceof SiteSource) {
            return ((SiteSource) s).getValue(elapsedTime);
        }

        float globalFlowValue = 0.0f;
        for (int i = 0; i < 4; i++) {
            globalFlowValue += site.getFlows()[i];
        }

        return globalFlowValue;
    }

    /**
     * Get width of this region
     * @return
     */
		@POPSyncSeq
    public int getWidth() {
    	return sites.length;
    }

    /**
     * Get height of this region
     * @return
     */
		@POPSyncSeq
    public int getHeight() {
    	return sites[0].length;
    }

    /**
     * X position of this region on the global grid
     * @return
     */
		@POPSyncSeq
    public int getX() {
    	return x;
    }

    /**
     * Y position of this region on the global grid
     * @return
     */
		@POPSyncSeq
    public int getY() {
    	return y;
    }

    /**
     * Set the types of all region types
     * @param regionTypes
     */
		@POPSyncSeq
		public void setSiteTypes(int[][] regionTypes) {
    	for(int x = 0; x < regionTypes.length; x++) {
            for(int y = 0; y < regionTypes[0].length; y++) {
            	sites[x][y].setTypeIndex(regionTypes[x][y]);
            }
    	}
		}
}
