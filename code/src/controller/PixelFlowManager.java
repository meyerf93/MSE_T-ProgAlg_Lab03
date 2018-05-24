package controller;

import java.util.Arrays;
import java.util.List;

import controller.PixelFlowRegion.Direction;
import model.Site;
import model.SiteSource;
import model.SiteType;

import ch.icosys.popjava.core.annotation.POPClass;


/**
 * This class contains the whole simulation
 * @author beat
 *
 */
 @POPClass(isDistributable = false)
 public class PixelFlowManager implements IPixelFlowManager{

	private static final int WAVE_SAMPLING = 8;

    private final List<SiteType> siteTypes;
    private final int defaultSiteTypeIndex;
    private final double deltaTimePerIteration; // in seconds
    private final double maxFlow;
    private final float initialTemperature;
    private long iterations = 0;

    private final static int DIV_COL = 1;
    private final static int DIV_ROW = 1;

    private int rows, cols;

    private final PixelFlowRegion [][] regions;

    public PixelFlowManager(
            int rows,
            int cols,
            SiteType[] siteTypesArray,
            SiteType defaultSiteType,
            float initialTemperature){

        String[] hosts = new String("grid61","grid62","grid63","grid64");
        Int[] core = {16,16,16,16};
        this.siteTypes = Arrays.asList(siteTypesArray);
        this.defaultSiteTypeIndex = siteTypes.indexOf(defaultSiteType);
        this.initialTemperature = initialTemperature;
        this.deltaTimePerIteration = computeDeltaTimePerIteration();
        this.maxFlow = computeMaxFlow();

    	this.rows = rows;
    	this.cols = cols;


    	regions = new PixelFlowRegion[DIV_COL][DIV_ROW];
        for(int x = 0; x < regions.length/2; x++) {
        	for(int y = 0; y < regions[x].length/2; y++) {
        		regions[x][y] = new PixelFlowRegion(deltaTimePerIteration, siteTypes.toArray(new SiteType[0]), hosts[0],core[0]);
        	}
        }
        for(int x = 0; x < regions.length/2; x++) {
          for(int y = regions[x].length/2; y < regions[x].length; y++) {
            regions[x][y] = new PixelFlowRegion(deltaTimePerIteration, siteTypes.toArray(new SiteType[0]), hosts[1],core[1]);
          }
        }
        for(int x = regions.length/2; x < regions.length; x++) {
        	for(int y = 0; y < regions[x].length; y++) {
        		regions[x][y] = new PixelFlowRegion(deltaTimePerIteration, siteTypes.toArray(new SiteType[0]), hosts[2],core[2]);
        	}
        }
        for(int x = regions.length/2; x < regions.length; x++) {
        	for(int y = regions[x].lenght/2; y < regions[x].length; y++) {
        		regions[x][y] = new PixelFlowRegion(deltaTimePerIteration, siteTypes.toArray(new SiteType[0]), hosts[3],core[3]);
        	}
        }


        for(int x = 0; x < regions.length; x++) {
        	for(int y = 0; y < regions[x].length; y++) {
        		for(Direction dir : Direction.values()) {
        			PixelFlowRegion neighbour = null;

        			if(y > 0 && dir == Direction.UP) {
        				neighbour = regions[x][y - 1];
        			}else if(x > 0 && dir == Direction.LEFT) {
        				neighbour = regions[x - 1][y];
        			}else if(y + 1 < regions[x].length && dir == Direction.DOWN) {
        				neighbour = regions[x][y + 1];
        			}else if(x + 1 < regions.length && dir == Direction.RIGHT) {
        				neighbour = regions[x + 1][y];
        			}

        			if(neighbour != null) {
                		regions[x][y].setNeighbour(neighbour, dir);
        			}
        		};
        	}
        }

    	initRegions(rows, cols, false);

		reinitializeSites(rows, cols, false);
    }

	private float computeMaxFlow() {
		long highestSourceAmplitude = 0;
		for (SiteType s : siteTypes) {
			if (s instanceof SiteSource) {
				long amplitude = ((SiteSource) s).getAmplitude();
				if (amplitude > highestSourceAmplitude) {
					highestSourceAmplitude = amplitude;
				}
			}
		}
		return highestSourceAmplitude;
	}

	private float computeDeltaTimePerIteration() {
		long highestSourceFrequency = 0;
		for (SiteType s : siteTypes) {
			if (s instanceof SiteSource) {
				long frequency = ((SiteSource) s).getFrequency();
				if (frequency > highestSourceFrequency) {
					highestSourceFrequency = frequency;
				}
			}
		}
		return 1.0f / (highestSourceFrequency * WAVE_SAMPLING);
	}

    private void initRegions(int rows, int cols, boolean copy) {

    	int rowChunk = rows / DIV_ROW;
		int colChunk = cols / DIV_COL;
		this.rows = rows;
		this.cols = cols;

		for(int x = 0; x < regions.length; x++) {
        	for(int y = 0; y < regions[x].length; y++) {
        		int width = colChunk;

        		if(x == regions.length - 1) {
        			width = cols - x * colChunk;
        		}

        		int height = rowChunk;

        		if(y == regions[x].length - 1) {
        			height = rows - y * rowChunk;
        		}

        		regions[x][y].createSites(x * colChunk, y * rowChunk, width, height, cols, rows, copy);
        	}
		}
    }

    public void step() {
		updateFlows();
		updateTemperatures();
		iterations++;
	}

	private void updateFlows() {
		for(int x = 0; x < regions.length; x++) {
        	for(int y = 0; y < regions[x].length; y++) {
        		regions[x][y].prepareFlowUpdate();
        	}
		}

		for(int x = 0; x < regions.length; x++) {
        	for(int y = 0; y < regions[x].length; y++) {
        		regions[x][y].updateFlows(getElapsedTime());
        	}
		}
		for(int x = 0; x < regions.length; x++) {
        	for(int y = 0; y < regions[x].length; y++) {
        		regions[x][y].finishFlowUpdate();
        	}
		}
	}

	private void updateTemperatures() {
		for(int x = 0; x < regions.length; x++) {
        	for(int y = 0; y < regions[x].length; y++) {
        		regions[x][y].updateTemperatures();
        	}
		}
	}

	@Override
	public int getCols() {
		return cols;
	}

	@Override
	public int getRows() {
		return rows;
	}

	@Override
	public void reinitializeSites(int rows, int cols, boolean copy) {
		this.rows = rows;
		this.cols = cols;

		initRegions(rows, cols, copy);

		for(int x = 0; x < regions.length; x++) {
        	for(int y = 0; y < regions[x].length; y++) {
        		regions[x][y].initSites(defaultSiteTypeIndex, initialTemperature, copy);
        	}
		}
	}

	private PixelFlowRegion getRegionForPosition(int row, int col) {
		int rowChunk = rows / DIV_ROW;
		int colChunk = cols / DIV_COL;

		int x = Math.min(col / colChunk, regions.length - 1);
		int y = Math.min(row / rowChunk, regions[0].length - 1);

		return regions[x][y];
	}

	@Override
	public float getTemperatureAtPosition(int row, int col) {
		return getRegionForPosition(row, col).getSite(row, col).getTemperature();
	}

	@Override
	public SiteType getSiteType(int row, int col) {
		return siteTypes.get(getRegionForPosition(row, col).getSite(row, col).getTypeIndex());
	}

    @Override
    public SiteType[][] getAllSiteTypes() {

        SiteType [][] siteTypes = new SiteType[cols][rows];
        int rowChunk = rows / DIV_ROW;
        int colChunk = cols / DIV_COL;


        for(int x = 0; x < regions.length; x++) {
            for(int y = 0; y < regions[x].length; y++) {
                int [][] indexes = regions[x][y].getSiteTypes();

                for(int tempX = 0; tempX < indexes.length; tempX++) {
                    for(int tempY = 0; tempY < indexes[0].length; tempY++) {
                        siteTypes[x * colChunk + tempX][y * rowChunk + tempY] = this.siteTypes.get(indexes[tempX][tempY]);
                    }
                }
            }
        }

        return siteTypes;
    }

	@Override
	public void setSiteType(int row, int col, int typeIndex) {
		getRegionForPosition(row, col).setSiteType(col, row, typeIndex);
	}

	@Override
	public double getDeltaTimePerIteration() {
		return deltaTimePerIteration;
	}

	@Override
	public Site[][] getAllSites() {
		return null;
	}

	@Override
	public double getElapsedTime() {
		return iterations * deltaTimePerIteration;
	}

	@Override
	public double getGlobalFlowAtPosition(int row, int col) {
		Site site = getRegionForPosition(row, col).getSite(row, col);

		SiteType s = siteTypes.get(site.getTypeIndex());

		if (s instanceof SiteSource) {
			return ((SiteSource) s).getValue(getElapsedTime());
		}

		float globalFlowValue = 0.0f;
		for (int i = 0; i < 4; i++) {
			globalFlowValue += site.getFlows()[i];
		}

		return globalFlowValue;
	}

	@Override
    public double[][] getAllFlows() {
        double [][] globalFlows = new double[cols][rows];

        int rowChunk = rows / DIV_ROW;
        int colChunk = cols / DIV_COL;

        for(int x = 0; x < regions.length; x++) {
            for(int y = 0; y < regions[x].length; y++) {
                double [][] flows = regions[x][y].getGlobalFlows(getElapsedTime());

                for(int tempX = 0; tempX < flows.length; tempX++) {
                    for(int tempY = 0; tempY < flows[0].length; tempY++) {
                        globalFlows[x * colChunk + tempX][y * rowChunk + tempY] = flows[tempX][tempY];
                    }
                }

            }
        }

        return globalFlows;
    }

    @Override
    public double getFlowAtPosition(int row, int col, int flow) {
        Site site = getRegionForPosition(row, col).getSite(row, col);

        SiteType s = siteTypes.get(site.getTypeIndex());

        if (s instanceof SiteSource) {
            return ((SiteSource) s).getValue(getElapsedTime());
        }

        return site.getFlows()[3];
    }

	@Override
	public void setNextSiteTypeIndex(int row, int col) {
		int currentSiteTypeIndex = getRegionForPosition(row, col).getSite(row, col).getTypeIndex();
		int nextSiteTypeIndex = (currentSiteTypeIndex + 1) % siteTypes.size();
		getRegionForPosition(row, col).setSiteType(col, row, nextSiteTypeIndex);
	}

	@Override
	public void setSameSiteTypeIndexAsIn(int row, int col, int lastR, int lastC) {
		int lastSiteTypeIndex = getRegionForPosition(lastR, lastC).getSite(lastR, lastC).getTypeIndex();
		getRegionForPosition(row, col).setSiteType(col, row, lastSiteTypeIndex);
	}

	@Override
	public double getMaxFlow() {
		return maxFlow;
	}

	@Override
	public void setSiteTypes(int[][] siteTypes) {
        int rowChunk = rows / DIV_ROW;
        int colChunk = cols / DIV_COL;

        for(int x = 0; x < regions.length; x++) {
            for(int y = 0; y < regions[x].length; y++) {
            	PixelFlowRegion region = regions[x][y];

            	int xStart = region.getX();
            	int yStart = region.getY();
            	int width = region.getWidth();
            	int height = region.getHeight();
            	int [][] regionTypes = new int [width][height];

                for(int tempX = 0; tempX < width; tempX++) {
                    for(int tempY = 0; tempY < height; tempY++) {
                    	regionTypes[tempX][tempY] = siteTypes[xStart + tempX][yStart + tempY];
                    }
                }

                region.setSiteTypes(regionTypes);
            }
        }
	}

}
