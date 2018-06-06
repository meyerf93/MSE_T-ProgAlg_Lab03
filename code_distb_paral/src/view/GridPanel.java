package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import model.Site;
import model.SiteType;
import view.Simulator.ViewMode;
import controller.IPixelFlowManager;

/**
 * 
 * @author Gisler Christophe
 *
 */
public class GridPanel extends JPanel {

	private static final long serialVersionUID = -402693350919620607L;

	private static final Color BACKGROUND_COLOR = Color.LIGHT_GRAY;
	private static final int PADDING = 0;
	private static final float ALPHA_BLENDING_RATIO = 0.4f;
	private static final NumberFormat NF = new DecimalFormat("#.###");

	private final Dimension gridDim;
	private int rows, cols;
	private float squareWidth, squareHeight;
	private IPixelFlowManager pixelFlowManager;
	private int initRow = -1, initCol = -1;
	private ViewMode viewModeIndex = ViewMode.Blank;
	
	private ViewMode lastViewMode = null;
	private BufferedImage drawBuffer = null;

	public GridPanel(Dimension gridDim, IPixelFlowManager pixelFlow) {
		this.gridDim = gridDim;
		setPreferredSize(gridDim);
		this.pixelFlowManager = pixelFlow;
		this.rows = pixelFlow.getRows();
		this.cols = pixelFlow.getCols();
		this.squareWidth = (float) (gridDim.width - (cols + 1) * PADDING) / cols;
		this.squareHeight = (float) (gridDim.height - (rows + 1) * PADDING) / rows;
		addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
				initRow = -1;
				initCol = -1;
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				int row = (int) (e.getY() / (squareHeight + PADDING));
				int col = (int) (e.getX() / (squareWidth + PADDING));
				// System.out.println("Grid position : (" + row + ", " + col + ")");
				//updateModelAfterMouseClic(row, col);
			}
		});
		addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseMoved(MouseEvent e) {
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				int row = (int) (e.getY() / (squareHeight + PADDING));
				int col = (int) (e.getX() / (squareWidth + PADDING));
				updateModelAfterMouseDrag(row, col);
			}
		});

	}

	public void setViewMode(ViewMode viewModeIndex) {
		this.viewModeIndex = viewModeIndex;
	}
	
	public ViewMode getViewMode() {
		return viewModeIndex;
	}

	@Override
	public boolean contains(int x, int y) {
		int row = (int) (y / (squareHeight + PADDING));
		int col = (int) (x / (squareWidth + PADDING));
		
		return super.contains(x, y);
	}

	public void update() {
		this.rows = pixelFlowManager.getRows();
		this.cols = pixelFlowManager.getCols();
		this.squareWidth = (float) (gridDim.width - (cols + 1) * PADDING) / cols;
		this.squareHeight = (float) (gridDim.height - (rows + 1) * PADDING) / rows;
		repaint();
	}

	private void updateModelAfterMouseClic(int row, int col) {
		pixelFlowManager.setNextSiteTypeIndex(row, col);
		repaint();
	}

	private void updateModelAfterMouseDrag(int row, int col) {
		// if (row == initRow && col == initCol) return;
		if (initRow == -1 || initCol == -1) {
			initRow = row;
			initCol = col;
			// pixelFlowManager.setNextSiteType(row, col);
		}
		pixelFlowManager.setSameSiteTypeIndexAsIn(row, col, initRow, initCol);
		repaint();
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		long start = System.currentTimeMillis();
		
		drawBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = (Graphics2D)drawBuffer.getGraphics();

		g2d.setPaint(BACKGROUND_COLOR);
		g2d.fill(new Rectangle2D.Float(0, 0, gridDim.width, gridDim.height));
		
		
		if(viewModeIndex != ViewMode.Blank) {
		    SiteType [][] siteTypes = pixelFlowManager.getAllSiteTypes();
		    
		    double [][] globalFlows = null;
		    if(viewModeIndex == ViewMode.GlobalFlow) {
		        globalFlows = pixelFlowManager.getAllFlows();
		    }
		    
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					SiteType siteType = siteTypes[c][r]; //pixelFlowManager.getSiteType(r, c);
					Color siteTypeInitColor = siteType.getColor();
					switch (viewModeIndex) {
					case GlobalFlow:
						Color globalFlowColor = blend(siteTypeInitColor,
								getColorFromFlowGlobalValue(globalFlows[c][r]),
								ALPHA_BLENDING_RATIO);
						paintSquare(g2d, drawBuffer, globalFlowColor, r, c);
						break;
					case DirectionalFlow:
						Color bottomFlowColor = blend(siteTypeInitColor,
								getWaveColorFromFlowValue(pixelFlowManager.getFlowAtPosition(r, c, Site.BOTTOM)),
								ALPHA_BLENDING_RATIO);
						Color leftFlowColor = blend(siteTypeInitColor,
								getWaveColorFromFlowValue(pixelFlowManager.getFlowAtPosition(r, c, Site.LEFT)),
								ALPHA_BLENDING_RATIO);
						Color topFlowColor = blend(siteTypeInitColor,
								getWaveColorFromFlowValue(pixelFlowManager.getFlowAtPosition(r, c, Site.TOP)),
								ALPHA_BLENDING_RATIO);
						Color rightFlowColor = blend(siteTypeInitColor,
								getWaveColorFromFlowValue(pixelFlowManager.getFlowAtPosition(r, c, Site.RIGHT)),
								ALPHA_BLENDING_RATIO);
						paint4TrianglesInSquare(g2d, bottomFlowColor, leftFlowColor, topFlowColor, rightFlowColor, r, c);
						break;
					case Temperature:
						Color temperatureColor = blend(siteTypeInitColor,
								getColorFromTemperatureValue(pixelFlowManager.getTemperatureAtPosition(r, c)),
								ALPHA_BLENDING_RATIO);
						paintSquare(g2d, drawBuffer, temperatureColor, r, c);
						break;
					case Sites:
						paintSquare(g2d, drawBuffer, siteTypeInitColor, r, c);
						break;
					case Blank:
						break;
					}
				}
			}
		}
		
        ((Graphics2D) g).drawImage(drawBuffer, null, 0, 0);
		
		//System.out.println("Painted in "+(System.currentTimeMillis() - start)+" ms");
	}

	private void paintSquare(Graphics2D g2d, BufferedImage raw, Color color, int row, int col) {
		float x = PADDING + col * (squareWidth + PADDING);
		float y = PADDING + row * (squareHeight + PADDING);
        
		if(squareWidth <= 1 && squareHeight <= 1) {
		    raw.setRGB((int)x, (int)y, color.getRGB());
		}else {
		    g2d.setColor(color);
		    g2d.fillRect((int)x, (int)y, (int)Math.max(Math.ceil(squareWidth), 1), (int)Math.max(1, Math.ceil(squareHeight)));
		}
	}

	private void paint4TrianglesInSquare(Graphics2D g2d, Color bottomColor, Color leftColor, Color topColor,
			Color rightColor, int row, int col) {
		int x0 = Math.round(PADDING + col * (squareWidth + PADDING));
		int y0 = Math.round(PADDING + row * (squareHeight + PADDING));
		Polygon triangle = new Polygon();
		int p1x = x0, p1y = y0 + (int) Math.ceil(squareHeight);
		int p2x = x0, p2y = y0;
		int p3x = x0 + (int) Math.ceil(squareWidth), p3y = y0;
		int p4x = x0 + (int) Math.ceil(squareWidth), p4y = y0 + (int) Math.ceil(squareHeight);
		int p5x = x0 + (int) Math.ceil(squareWidth / 2), p5y = y0 + (int) Math.ceil(squareHeight / 2);

		// Paint triangle BOTTOM
		int[] xs0 = { p1x, p5x, p4x };
		int[] ys0 = { p1y, p5y, p4y };
		triangle = new Polygon(xs0, ys0, xs0.length);
		g2d.setColor(bottomColor);
		g2d.fillPolygon(triangle);

		// Paint triangle LEFT
		int[] xs1 = { p2x, p5x, p1x };
		int[] ys1 = { p2y, p5y, p1y };
		triangle = new Polygon(xs1, ys1, xs1.length);
		g2d.setColor(leftColor);
		g2d.fillPolygon(triangle);

		// Paint triangle TOP
		int[] xs2 = { p2x, p5x, p3x };
		int[] ys2 = { p2y, p5y, p3y };
		triangle = new Polygon(xs2, ys2, xs2.length);
		g2d.setColor(topColor);
		g2d.fillPolygon(triangle);

		// Paint triangle RIGHT
		int[] xs3 = { p4x, p5x, p3x };
		int[] ys3 = { p4y, p5y, p3y };
		triangle = new Polygon(xs3, ys3, xs3.length);
		g2d.setColor(rightColor);
		g2d.fillPolygon(triangle);
	}

	private Color getWaveColorFromFlowValue(double value) {
		/*
		 * Color color = Color.red; for (int i = 0; i < 30; i++) { if (Math.abs(value)
		 * >= (1 - i * 0.033333)) { int quantityRed = 255; int quantityGreen = 255; if
		 * (i < 15) quantityRed = i * 17; else quantityGreen = 255 - (i - 15) * 17;
		 * return new Color(quantityRed, quantityGreen, 0); } } return color;
		 */
		/*
		 * for (int i = 0; i < 30; i++) { if (Math.abs(value) >= (1 - i * 0.033333)) {
		 * int quantityRed = 255; int quantityGreen = 255; if (i < 15) quantityRed = i *
		 * 17; else quantityGreen = 255 - (i - 15) * 17; return new Color(quantityRed,
		 * quantityGreen, 0); } } return null;
		 */
		return getHSVColorFromValue((float) value/* , 0.00001f */,
				(float) pixelFlowManager.getMaxFlow() /* / 10.0f/*0.1f */);
	}

	private Color getColorFromFlowGlobalValue(double value) {
		return getHSVColorFromValue((float) value/* , 0.00001f */,
				(float) pixelFlowManager.getMaxFlow() /* / 10.0f/*0.1f */);
	}

	private Color getColorFromTemperatureValue(double value) {
		return blend(Color.BLUE, Color.RED,
				(float) (0.5 + 0.5 * Math.tanh((value - 50.0) / 25.0))/* (float) value / 100.0f */);
	}

	private static Color getHSVColorFromValue(float value, /* float min, */ float max) {
		/*
		 * if (value == 0.0f) return null; final float hue = (1.0f - (value - min) /
		 * (max - min)) * 2 / 3;
		 */ // * 2 / 3 => return a color between red and blue
		if (value == 0.0f)
			return null;
		final float hue = (1.0f - Math.abs(value) / max) * 2 / 3; // * 2 / 3 => return a color between red and blue
		final float saturation = 1.0f;
		final float luminance = 1.0f;
		final Color color = Color.getHSBColor(hue, saturation, luminance);
		return color;
	}

	private static Color blend(Color c1, Color c2, float ratio) {
		if (c1 == null)
			return c2;
		if (c2 == null)
			return c1;

		if (ratio > 1f)
			ratio = 1f;
		else if (ratio < 0f)
			ratio = 0f;
		float iRatio = 1.0f - ratio;

		int i1 = c1.getRGB();
		int i2 = c2.getRGB();

		int a1 = (i1 >> 24 & 0xff);
		int r1 = ((i1 & 0xff0000) >> 16);
		int g1 = ((i1 & 0xff00) >> 8);
		int b1 = (i1 & 0xff);

		int a2 = (i2 >> 24 & 0xff);
		int r2 = ((i2 & 0xff0000) >> 16);
		int g2 = ((i2 & 0xff00) >> 8);
		int b2 = (i2 & 0xff);

		int a = (int) ((a1 * iRatio) + (a2 * ratio));
		int r = (int) ((r1 * iRatio) + (r2 * ratio));
		int g = (int) ((g1 * iRatio) + (g2 * ratio));
		int b = (int) ((b1 * iRatio) + (b2 * ratio));

		return new Color(a << 24 | r << 16 | g << 8 | b);
	}

	private static Color getRandomColor() {
		float hue = new Random().nextFloat();
		int rgb = Color.HSBtoRGB(hue, 0.9f, 0.9f);
		return new Color(rgb);
	}

}
