package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import controller.IPixelFlowManager;
import controller.PixelFlowManager;
import model.Site;
import model.SiteObstacle;
import model.SiteSource;
import model.SiteType;

/**
 * 
 * @author Gisler Christophe
 *
 */
public class Simulator extends JFrame {

	private final static String DEFAULT_GRID_FILE = "default.csv";

	private static final int MAX_GRID_SIZE = 10000;

	public static enum ViewMode {
		GlobalFlow("Global flow"), DirectionalFlow("Directional flows"), Temperature("Temperature"), Sites(
				"Sites"), Blank("Nothing");

		private final String name;

		private ViewMode(String name) {
			this.name = name;
		}

		@Override
        public String toString() {
			return name;
		}
	}

	private static final long serialVersionUID = -1552617090672901307L;
	private final Dimension SCREEN_DIM;
	private final static int PADDING = 4, COMMAND_PANEL_HEIGHT = 27;
	private final static int MIN_FPS = 1, MAX_FPS = 200;
	private int fps = 200, maxSimulTime = 30;
	private final static String SEP = ",", SEP2 = "_";

	private IPixelFlowManager pixelFlowManager;
	private GridPanel gridPanel;
	private boolean isRunning = false;
	private JButton runButton;
	private JLabel fpsLabel, simulationTimeLabel;
	private JSpinner gridWidthSpinner, gridHeightSpinner, simulationMaxTimeSpinner;
	private SimulatorWorker simulatorWorker;
	private Thread simulationThread;

	public Simulator() {
	    SCREEN_DIM = setupScreenSize();
		// Create Pixel Flow manager
		createPixelFlowManager();

		// Initialize GUI
		createGUI();

		// Load default Pixel Flow grid from file if exist
		loadDefaultGrid();
	}
	
	/**
	 * Recover the correct screensize.
	 * Why this is done in the first place is unclear, but this is required to work
	 * in a multi screen environment
	 */
	private Dimension setupScreenSize(){
	    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    GraphicsDevice[] gs = ge.getScreenDevices();
	    
	    int minWidth = Integer.MAX_VALUE;
	    int minHeight = Integer.MAX_VALUE;
	    for(GraphicsDevice curGs : gs)
	    {
	          DisplayMode dm = curGs.getDisplayMode();
	          
	          minWidth = Math.min(dm.getWidth(), minWidth);
	          minHeight = Math.min(dm.getHeight(), minHeight);
	    }
	    
	    return new Dimension(minWidth, minHeight - 83); //83 is a magic value from the previous code. Parts of the display are cutoff otherwise
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new Simulator();
			}
		});
	}

	private void createPixelFlowManager() {
		List<SiteType> siteTypes = new ArrayList<>();

		// TODO Here are defined the various types of sites of the system.

		/*
		 * *****************************************************************************
		 * ************
		 */
		/*
		 * For a given site type, beta^2 + beta * gamma + gamma^2 <= 1 (and < 1 <=>
		 * calorific loss)
		 */
		/*
		 * *****************************************************************************
		 * ************
		 */

		// Site of type "air": all incoming energy is spread <=> beta = 1.0 and gamma =
		// 0.0
		SiteType air = new SiteObstacle("Air", Color.WHITE, 1.0f, 0.0f, Float.MAX_VALUE);

		// Site of type "wall": all incoming energy is reflected <=> beta = 1.0 and
		// gamma = 0.0
		SiteType wall = new SiteObstacle("Wall", Color.DARK_GRAY, 0.0f, 1.0f, Float.MAX_VALUE);

		// Site of type "water": a part of the incoming energy is lost in heat: beta^2 +
		// beta * gamma + gamma^2 < 1
		SiteType water = new SiteObstacle("Water", Color.BLUE, 0.93f, 0.0f, 4.2f);

		// Site of type "source": yields the energy
		SiteType source = new SiteSource("Source", Color.MAGENTA, (long) 24.5, 40);

		/*
		 * *****************************************************************************
		 * ***********************
		 */
		/*
		 * A sinusoidal source (like micro-ondes) have a frequency and an amplitude:
		 * freq = 2.45GHz = 2.45E9Hz
		 */
		/*
		 * Micro-ondes have a frequency of 2.45 GHz = 2.45E9 Hz, but for simulation
		 * reasons that you must
		 */
		/*
		 * understand and explain, have been set to 24.5Hz. The amplitude has been
		 * trivially set to 40.
		 */
		/*
		 * *****************************************************************************
		 * ***********************
		 */

		siteTypes.add(air);
		siteTypes.add(wall);
		siteTypes.add(water);
		siteTypes.add(source);
		this.pixelFlowManager = new PixelFlowManager(/* 154, 300 */215, 418, siteTypes.toArray(new SiteType[0]), air, 21.0f);
				//new PixelFlowManager(/* 154, 300 */215, 418, siteTypes.toArray(new SiteType[0]), air, 21.0f);
	}

	private void createGUI() {
		setTitle("Pixel Flow (Lattice Boltzmann) Simulator");
		setPreferredSize(SCREEN_DIM);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		JMenuBar menu = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenuItem loadGridConfigMenuItem = new JMenuItem("Load Grid Configuration...");
		loadGridConfigMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser(".");
				fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fc.setDialogTitle("Load Config File");
				int returnVal = fc.showOpenDialog(gridPanel);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					loadGridConfigFromFile(fc.getSelectedFile());
				}
			}
		});
		fileMenu.add(loadGridConfigMenuItem);
		JMenuItem saveGridConfigMenuItem = new JMenuItem("Save Grid Configuration...");
		saveGridConfigMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveGridConfig();
			}
		});
		fileMenu.add(saveGridConfigMenuItem);
		fileMenu.add(new JSeparator());
		JMenuItem saveGridValuesMenuItem = new JMenuItem("Save Grid Values...");
		saveGridValuesMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveGridValues();
			}
		});
		fileMenu.add(saveGridValuesMenuItem);
		fileMenu.add(new JSeparator());
		JMenuItem quitMenuItem = new JMenuItem("Quit");
		quitMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
				System.exit(0);
			}
		});
		fileMenu.add(quitMenuItem);
		menu.add(fileMenu);
		add(menu, BorderLayout.PAGE_START);
		gridPanel = new GridPanel(new Dimension(SCREEN_DIM.width - PADDING * 2,
				SCREEN_DIM.height - COMMAND_PANEL_HEIGHT * 3 - PADDING * 4), pixelFlowManager);
		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
		p.add(gridPanel);
		add(p, BorderLayout.CENTER);

		JPanel commandPanel = new JPanel();
		gridWidthSpinner = new JSpinner(new SpinnerNumberModel(pixelFlowManager.getCols(), 10, MAX_GRID_SIZE, 1));
		gridWidthSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int cols = (Integer) ((JSpinner) e.getSource()).getValue();
				pixelFlowManager.reinitializeSites(pixelFlowManager.getRows(), cols, true);
				gridPanel.update();
			}
		});
		commandPanel.add(gridWidthSpinner);
		gridHeightSpinner = new JSpinner(new SpinnerNumberModel(pixelFlowManager.getRows(), 10, MAX_GRID_SIZE, 1));
		gridHeightSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int rows = (Integer) ((JSpinner) e.getSource()).getValue();
				pixelFlowManager.reinitializeSites(rows, pixelFlowManager.getCols(), true);
				gridPanel.update();
			}
		});
		commandPanel.add(gridHeightSpinner);
		JButton resetGridButton = new JButton("Reset Grid");
		resetGridButton.setToolTipText("Reset all grid squares to the default type of site");
		resetGridButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				resetGrid();
			}
		});
		commandPanel.add(resetGridButton);
		JButton stepButton = new JButton("Step");
		stepButton.setToolTipText("Run the pixel flow simulator step by step");
		stepButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				step();
			}
		});
		commandPanel.add(stepButton);
		runButton = new JButton("Run");
		runButton.setToolTipText("Run the pixel flow simulator");
		runButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!isRunning)
					startSimulation();
				else
					stopSimulation();
			}
		});
		commandPanel.add(runButton);
		simulationTimeLabel = new JLabel("Simulation duration: 0s");
		simulationTimeLabel.setToolTipText(
				"Elapsed simulation time (" + pixelFlowManager.getDeltaTimePerIteration() + " sec./iteration)");
		commandPanel.add(simulationTimeLabel);
		JCheckBox maxTimeCB = new JCheckBox("Max. duration:");
		maxTimeCB.setToolTipText("Set maximum duration in seconds");
		simulationMaxTimeSpinner = new JSpinner(new SpinnerNumberModel(maxSimulTime, 1, 999, 1));
		simulationMaxTimeSpinner.setToolTipText("Set the maximum duration (in seconds) for the simulation");
		simulationMaxTimeSpinner.setEnabled(false);
		simulationMaxTimeSpinner.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				maxSimulTime = (Integer) ((JSpinner) e.getSource()).getValue();
			}
		});
		maxTimeCB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				simulationMaxTimeSpinner.setEnabled(((JCheckBox) e.getSource()).isSelected());
			}
		});
		commandPanel.add(maxTimeCB);
		commandPanel.add(simulationMaxTimeSpinner);
		JSlider fpsSlider = new JSlider(JSlider.HORIZONTAL, MIN_FPS, MAX_FPS, fps);
		fpsSlider.setToolTipText("Set the number of frames per second while the simulator is running");
		fpsSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				// if (!source.getValueIsAdjusting()) {
				fps = source.getValue();
				fpsLabel.setText(fps + " FPS");
				// }
			}
		});
		commandPanel.add(fpsSlider);
		fpsLabel = new JLabel(fps + " FPS");
		fpsLabel.setToolTipText("Frames per second");
		commandPanel.add(fpsLabel);
		JButton resetFlowButton = new JButton("Reset Flow");
		resetFlowButton.setToolTipText("Reset the flow spreading over the grid");
		resetFlowButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				resetSites();
			}
		});
		final JComboBox<ViewMode> viewModeCB = new JComboBox<>(ViewMode.values());
		viewModeCB.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED) {
					setViewMode((ViewMode) viewModeCB.getSelectedItem());
				}
			}
		});
		
		viewModeCB.setSelectedItem(ViewMode.Blank);
		
		commandPanel.add(viewModeCB);
		commandPanel.add(resetFlowButton);
		add(commandPanel, BorderLayout.PAGE_END);

		pack();
		setResizable(true);
		setVisible(true);
		setLocationRelativeTo(null);
	}

	private void resetSites() {
		pixelFlowManager.reinitializeSites(pixelFlowManager.getRows(), pixelFlowManager.getCols(), true);
		gridPanel.repaint();
	}

	private void setViewMode(ViewMode viewModeIndex) {
		if(gridPanel.getViewMode() != viewModeIndex) {
			gridPanel.setViewMode(viewModeIndex);
			gridPanel.repaint();
		}
	}

	private void resetGrid() {
		pixelFlowManager.reinitializeSites(pixelFlowManager.getRows(), pixelFlowManager.getCols(), false);
		gridPanel.repaint();
	}

	private void saveGridConfig() {
		JFileChooser fc = new JFileChooser();
		fc.setSelectedFile(new File("Grid Config.csv"));
		int returnVal = fc.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			// Write grid file
			try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(fc.getSelectedFile()),
					"UTF-8");
					BufferedWriter bufWriter = new BufferedWriter(fileWriter);
					PrintWriter printWriter = new PrintWriter(bufWriter, true)) {
				Site[][] sites = pixelFlowManager.getAllSites();
				printWriter.println(sites.length + SEP + sites[0].length);
				for (int row = 0; row < sites.length; row++) {
					String line = String.valueOf(sites[row][0].getTypeIndex());
					for (int col = 1; col < sites[0].length; col++) {
						line += SEP + sites[row][col].getTypeIndex();
					}
					printWriter.println(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * Save each grid cell values in CSV file with the following format:
	 * GlobalFlow_TopFlow_RighFlow_BottomFlow_LeftFlow_Temperature
	 */
	private void saveGridValues() {
		JFileChooser fc = new JFileChooser();
		fc.setSelectedFile(new File("Grid Values.csv"));
		int returnVal = fc.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			// Write grid file
			try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(fc.getSelectedFile()),
					"UTF-8");
					BufferedWriter bufWriter = new BufferedWriter(fileWriter);
					PrintWriter printWriter = new PrintWriter(bufWriter, true)) {
				printWriter.println(
						"# Format of each grid cell: GlobalFlow_TopFlow_RighFlow_BottomFlow_LeftFlow_Temperature");
				for (int row = 0; row < pixelFlowManager.getRows(); row++) {
					String line = "";
					for (int col = 0; col < pixelFlowManager.getCols(); col++) {
						line += pixelFlowManager.getGlobalFlowAtPosition(row, col);
						line += SEP2 + pixelFlowManager.getFlowAtPosition(row, col, Site.TOP);
						line += SEP2 + pixelFlowManager.getFlowAtPosition(row, col, Site.RIGHT);
						line += SEP2 + pixelFlowManager.getFlowAtPosition(row, col, Site.BOTTOM);
						line += SEP2 + pixelFlowManager.getFlowAtPosition(row, col, Site.LEFT);
						line += SEP2 + pixelFlowManager.getTemperatureAtPosition(row, col);
						if (col < pixelFlowManager.getCols() - 1)
							line += SEP;
					}
					printWriter.println(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadGridConfigFromFile(File file) {
		try (Scanner scanner = new Scanner(file)) {
			String[] cellularAutomatonDim = scanner.nextLine().split(SEP);
			int rows = Integer.parseInt(cellularAutomatonDim[0]);
			int cols = Integer.parseInt(cellularAutomatonDim[1]);
			pixelFlowManager.reinitializeSites(rows, cols, false);
			int row = 0;
			
			int [][] siteTypes = new int[cols][rows]; 
			
			while (scanner.hasNextLine()) {
				String[] rowValues = scanner.nextLine().split(SEP);
				for (int col = 0; col < rowValues.length; col++) {
					int typeIndex = Integer.parseInt(rowValues[col]);
					//pixelFlowManager.setSiteType(row, col, typeIndex );
					siteTypes[col][row] = typeIndex;
				}
				row++;
			}
			
			pixelFlowManager.setSiteTypes(siteTypes);
			if (gridPanel != null) {
				gridPanel.update();
				gridWidthSpinner.setValue(cols);
				gridHeightSpinner.setValue(rows);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void loadDefaultGrid() {
		File defaultGridFile = new File(DEFAULT_GRID_FILE);
		if (defaultGridFile.exists())
			loadGridConfigFromFile(defaultGridFile);
	}

	private void step() {
		pixelFlowManager.step();
		gridPanel.repaint();
	}

	public void startSimulation() {
		isRunning = true;
		runButton.setText("Stop");
		simulatorWorker = new SimulatorWorker();
		simulationThread = new Thread(simulatorWorker);
		simulationThread.start();
	}

	public void stopSimulation() {
		isRunning = false;
		runButton.setText("Run");
	}

	/**
	 * Convert the elapsed time to a formatted string: "XhYmZs"
	 * 
	 * @param elapsedTime
	 * @return string formatted as "X Days Y Hours Z Minutes A Seconds".
	 */
	private static String getFormattedDuration(double elapsedTime) {
		if (elapsedTime < 0.0) {
			throw new IllegalArgumentException("Duration must be greater than zero!");
		}
		long millis = (long) (1000.0 * elapsedTime);
		long hours = TimeUnit.MILLISECONDS.toHours(millis);
		millis -= TimeUnit.HOURS.toMillis(hours);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
		millis -= TimeUnit.MINUTES.toMillis(minutes);
		long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
		String result = "";
		if (hours > 0) {
			result += hours + "h";
		}
		if (minutes > 0) {
			result += minutes + "m";
		}
		result += seconds + "s";
		return result;
	}

	private class SimulatorWorker implements Runnable {

		@Override
		public void run() {
			while (isRunning) {
				try {
					long t0 = System.currentTimeMillis();
					
					pixelFlowManager.step();
					
					final long dt = System.currentTimeMillis() - t0;
					
					System.out.println("Calculated step in "+(dt)+" ms");
					
					final long sleepTime = Math.max((long) (1000.0 / fps) - dt, 0);
					
					SwingUtilities.invokeLater(new Runnable() {
						
						@Override
						public void run() {
							gridPanel.repaint();
							
							if (sleepTime == 0) {
								fpsLabel.setText((long) (1000.0 / dt) + " FPS");
							}
							
							simulationTimeLabel
									.setText("Simulation duration: " + getFormattedDuration(pixelFlowManager.getElapsedTime()));
							if (simulationMaxTimeSpinner.isEnabled() && pixelFlowManager.getElapsedTime() >= maxSimulTime) {
								stopSimulation();
							}
						}
					});
					
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
				}
			}
		}

	}

}
