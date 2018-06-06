package controller;

import model.Site;
import model.SiteType;

public interface IPixelFlowManager {

	/**
	 * Runs one simulation step
	 */
	void step();
	
	/**
	 * Returns the flow of all sites on the grid
	 * @return
	 */
	public double [][] getAllFlows();

	/**
	 * Reset all sites. If the copyPreviousType parameter is set, the previous site types are kept intact
	 * @param rows
	 * @param cols
	 * @param b
	 */
	void reinitializeSites(int rows, int cols, boolean copyPreviousType);
	
	/**
	 * Returns number of columns of the grid
	 * @return
	 */
	int getCols();

	/**
	 * Returns the number of rows of the grid
	 * @return
	 */
	int getRows();

	/**
	 * Returns all sites of the grid
	 * @return
	 */
	Site[][] getAllSites();
	
	/**
	 * Returns the type of all sites on the grid
	 * @return
	 */
	SiteType [][] getAllSiteTypes();

	float getTemperatureAtPosition(int row, int col);

	SiteType getSiteType(int row, int col);

	double getDeltaTimePerIteration();

	double getElapsedTime();

	public double getGlobalFlowAtPosition(int row, int col);

	public double getFlowAtPosition(int row, int col, int flow);

	void setNextSiteTypeIndex(int row, int col);

	void setSameSiteTypeIndexAsIn(int row, int col, int initRow, int initCol);

	double getMaxFlow();

	void setSiteType(int row, int col, int typeIndex);

	void setSiteTypes(int[][] siteTypes);

}
