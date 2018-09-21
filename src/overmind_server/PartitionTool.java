package overmind_server;
import com.example.overmind.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

public class PartitionTool extends JFrame implements WindowListener {		
	 Node selectedNode = null;	
	 
	 private PopulationsPanel populationsPanel = new PopulationsPanel();
	 private JPanel globalPanel = new JPanel();
	 private JPanel sidePanel = new JPanel();
	 private JPanel commandsPanel = new JPanel();
	 private JPanel infoPanel = new JPanel();
	 private JPanel optionsPanel = new JPanel();
	 private JPanel addPopPanel = new JPanel();
	 private JButton addPopulation = new JButton();	 
	 private JFrame thisFrame;
	 
	 // TODO: We shouldn't have input and output terminals. Instead we should have input and output
	 // populations
	 
	 private boolean addingInputs = false;
	 private boolean addingOutputs = false;
	 private PopulationLabel selectedPop = null;
	 private TerminalLabel selectedTerminal = null;
	 private ArrayList<Population> inputPops = new ArrayList<>(); 
	 private ArrayList<com.example.overmind.Terminal> inputDevs = new ArrayList<>(); 
	 private ArrayList<Population> outputPops = new ArrayList<>(); 
	 private ArrayList<com.example.overmind.Terminal> outputDevs = new ArrayList<>();
	 
	 // Matrix used to store the population so that they can be accessed 
	 // directly based on their positions on the grid
	 private Population[][] popMatrix;
		
	 // Width and depth of the grid
	 private int maxDepth = 0, maxWidth = 0;

	 
	 int open() {
		 thisFrame = this;
		 addingInputs = false;
		 addingOutputs = false;
		 selectedPop = null;
		 selectedTerminal = null;
		 inputPops.clear();
		 inputDevs.clear();
		 commandsPanel.setEnabled(true);
			Component[] components = commandsPanel.getComponents();
			for (int i = 0; i < components.length; i++)
				components[i].setEnabled(true);
		 
		 try {
			 Image img = ImageIO.read(getClass().getResource("/icons/add.png"));
			 addPopulation.setIcon(new ImageIcon(img.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
		 } catch (IOException e) {
			 e.printStackTrace();
		 }
		 
		 addPopulation.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {				
					addPopProcedure();
				}			
			});
		 
		 this.setTitle("Partition Tool");
		 this.setVisible(true);
		 this.selectedNode = VirtualLayerVisualizer.selectedNode;
		 
		 if (selectedNode == null) {
			 System.out.println("Partition Tool: selectedNode is null when it shouldn't");
			 return Constants.ERROR;
		 }
		 
		 populationsPanel.customUpdate();
		 GridBagConstraints popPanelConstr = new GridBagConstraints();
		 popPanelConstr.gridx = 0;
		 popPanelConstr.gridy = 0;
		 
		 sidePanel.setLayout(new GridLayout(3, 1));
		 GridBagConstraints sidePanelConstr = new GridBagConstraints();
		 sidePanelConstr.gridx = 1;
		 sidePanelConstr.gridy = 0;
		 sidePanel.add(commandsPanel);
		 sidePanel.add(infoPanel);
		 sidePanel.add(optionsPanel);
		 
		 commandsPanel.setLayout(new GridLayout(1, 1));
		 commandsPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Commands"),
					BorderFactory.createEmptyBorder(5,5,5,5)));
		 commandsPanel.add(addPopulation);
		 		 
		 infoPanel.setLayout(new GridLayout(3, 1));
		 infoPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Pop/Dev info"),
					BorderFactory.createEmptyBorder(5,5,5,5)));	
		 infoPanel.removeAll();
		 infoPanel.add(new JLabel("Select a pop. or a dev."));
		 
		 optionsPanel.setLayout(new GridLayout(2, 1));
		 optionsPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Command options"),
					BorderFactory.createEmptyBorder(5,5,5,5)));	
		 optionsPanel.removeAll();
		 optionsPanel.add(new JLabel("Select a command"));
		 
		 
		 addPopPanel.setLayout(new GridBagLayout());
			 
		 globalPanel.setLayout(new GridBagLayout());
		 globalPanel.add(populationsPanel, popPanelConstr);
		 globalPanel.add(sidePanel, sidePanelConstr);
		 		 
		 this.addWindowListener(this);
		 this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		 this.setContentPane(globalPanel);
		 //this.setResizable(false);
		 this.revalidate();
		 this.repaint();
		 this.pack();
		 
		 return Constants.SUCCESS;
	 }
	 
	private void addPopProcedure() {
		JButton addInputsButton = new JButton("Add inputs");
		JButton addOutputsButton = new JButton("Add outputs");
		
		if (selectedPop != null) {
			selectedPop.labelIcon.setImage(selectedPop.deselectedImg);
			selectedPop.repaint();
			selectedPop = null;
		}
		
		if (selectedTerminal != null) {
			selectedTerminal.labelIcon.setImage(selectedTerminal.deselectedImg);
			selectedTerminal.repaint();
			selectedTerminal = null;
		}
		
		commandsPanel.setEnabled(false);
		Component[] components = commandsPanel.getComponents();
		for (int i = 0; i < components.length; i++)
			components[i].setEnabled(false);
		
		addInputsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {	
				addingInputs = false;
				addingOutputs = true;
				
				optionsPanel.removeAll();
				optionsPanel.add(new JLabel("Select outputs"));
				optionsPanel.add(addOutputsButton);
				
				thisFrame.revalidate();
				thisFrame.repaint();
				thisFrame.pack();
			}			
		});
		
		addOutputsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addingOutputs = false;
				
				commandsPanel.setEnabled(true);
				Component[] components = commandsPanel.getComponents();
				for (int i = 0; i < components.length; i++)
					components[i].setEnabled(true);
				
				optionsPanel.removeAll();
				if (inputPops.size() == 0 & inputDevs.size() == 0)
					optionsPanel.add(new JLabel("Select at least 1 input"));
				else if (outputPops.size() == 0 & outputDevs.size() == 0)
					optionsPanel.add(new JLabel("Select at least 1 output"));
				else  {
					optionsPanel.add(new JLabel("Population created"));
				
					short numOfDendrites = 0, numOfSynapses = 0, numOfNeurons = 0;
					Population population = new Population(numOfNeurons, numOfDendrites, numOfSynapses);

					ArrayList<Integer> inputs = new ArrayList<>();
					ArrayList<Integer> outputs = new ArrayList<>();
					for (Population inputPop : inputPops) {
						inputPop.outputIndexes.add(population.id);
						numOfDendrites += inputPop.numOfNeurons;
						inputs.add(inputPop.id);
					}
					for (Terminal inputDev : inputDevs) {						
						numOfDendrites += inputDev.numOfNeurons;
						inputs.add(inputDev.id);
					}
					for (Population outputPop : outputPops) {
						outputPop.inputIndexes.add(population.id);
						numOfSynapses += outputPop.numOfNeurons;
						outputs.add(outputPop.id);
					}
					for (Terminal outputDev : outputDevs) {
						numOfSynapses += outputDev.numOfNeurons;
						outputs.add(outputDev.id);
					}
					
					population.numOfDendrites = numOfDendrites;
					population.numOfNeurons = numOfNeurons;
					population.numOfSynapses = numOfSynapses;
					population.inputIndexes = inputs;
					population.outputIndexes = outputs;
					selectedNode.terminal.populations.put(population.id, population);
					populationsPanel.customUpdate();
				}
				
				inputPops.clear();
				inputDevs.clear();
				outputPops.clear();
				outputDevs.clear();
				
				thisFrame.revalidate();
				thisFrame.repaint();
				thisFrame.pack();
			}			
		});
		
		optionsPanel.removeAll();
		optionsPanel.add(new JLabel("Select inputs"));
		optionsPanel.add(addInputsButton);
		
		this.revalidate();
		this.repaint();
		this.pack();
		addingInputs = true;
	}
	 
	void close() {
		this.setVisible(false);
	 }

	@Override
	public void windowOpened(WindowEvent e) {		
	}

	@Override
	public void windowClosing(WindowEvent e) {		
		this.setVisible(false);
	}

	@Override
	public void windowClosed(WindowEvent e) {	
	}

	@Override
	public void windowIconified(WindowEvent e) {		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {		
	}

	@Override
	public void windowActivated(WindowEvent e) {		
	}

	@Override
	public void windowDeactivated(WindowEvent e) {		
	}
	
	/**
	 * This method is used to build the matrix which contains the populations from the respective hash map. 
	 * The matrix structure is useful to organize the populations in a grid that can be visualized, whereas
	 * the hash map is useful for accessing the populations using their IDs
	 * @param populationsMap: The HashMap containing the populations. Their IDs are used as keys
	 */
	
	private void buildPopulationsMatrix(HashMap<Integer, Population> populationsMap) {
		Collection<Population> populations = populationsMap.values();
		
		// Before the order of the matrix is known, a HashMap of arrays is used to store the entries.
		// If a double array were to used immediately, it would need to be updated every time the order 
		// of the matrix was found to be greater than previously known
		HashMap<Integer, ArrayList<Population>> matrixElements = new HashMap<>(); 
		
		/*
		 * The algorithm works by exploring the tree of the populations until a terminal
		 * is found. The depth at which the terminal is found is stored. The tree is explored
		 * both downwards and upwards and then the two depths are sum together. The upwards depth
		 * is the distance from the input terminal and thus represent the layer of the population 
		 */
					
		for (Population population : populations) {
			int depthUpwards = ExploreUpwards(population, 0);
			
			// Layer 0 in unique to the presynaptic terminals
			//depthUpwards = depthUpwards == 0 ? 1 : depthUpwards;
			
			int depthDownwards = ExploreDownwards(population, 0);
			int depth = depthUpwards + depthDownwards;
			
			// Use the layer of population to store it in the hash map
			Integer key = Integer.valueOf(depthUpwards);
			if (!matrixElements.containsKey(key))
				matrixElements.put(key, new ArrayList<Population>());				
			matrixElements.get(key).add(population);
			int width = matrixElements.get(key).size();
			
			// Check if the order of the matrix should be increased based on the new info
			maxDepth = depth > maxDepth ? depth : maxDepth;
			maxWidth = width > maxWidth ? width : maxWidth;
		}
		
		// Create the matrix
		popMatrix = new Population[maxDepth][];
		for(int i = 0; i < maxDepth; i++) {
			// Each array represents a different row of populations
			ArrayList<Population> row = matrixElements.get(Integer.valueOf(i));
			
			if (row != null) {
				popMatrix[i] = new Population[row.size()];
				for (int j = 0; j < row.size(); j++) {
					popMatrix[i][j] = row.get(j);
				}
			}
		}			
		
	}
	
	/**
	 * Recursive method that explore the tree of the populations downwards until 
	 * it's found and output terminal
	 * @param population: The population from which the exploration starts
	 * @param depth: The depth of the current population
	 * @return: The depth of the last known population
	 */
	
	private int ExploreDownwards(Population population, int depth) {
		int newDepth = depth;

		if (population != null) {	
			for (Integer index : population.outputIndexes) {
				int tmpDepth = ExploreDownwards(selectedNode.terminal.populations.get(index), depth);
				newDepth = tmpDepth + 1 > newDepth ? tmpDepth + 1: newDepth;
			}
		} 
		
		return newDepth;		
	}
	
	// Just as before but the exploration is upwards 
	
	private int ExploreUpwards(Population population, int depth) {
		int newDepth = depth;
		
		if (population != null) {	
			for (Integer index : population.inputIndexes) {
				int tmpDepth = ExploreUpwards(selectedNode.terminal.populations.get(index), depth);
				newDepth = tmpDepth + 1 > newDepth ? tmpDepth + 1: newDepth;
			}
		} 
		
		return newDepth;		
	}
	
	/**
	 * Custom class that describes the panel which visualizes how the neural network of a
	 * given device is partitioned into multiple populations. 
	 * 
	 * Populations are organized on a grid based on their pre and postsynaptic connections
	 * @author rodolforocco
	 *
	 */
	
	private class PopulationsPanel extends JLayeredPane {		
		private Font font = new Font("label font", Font.PLAIN, 10);
		
		// Array of coupled of points which are the extremes of the segments
		// that connect the populations
		ArrayList<int[][]> lineEnds = new ArrayList<>();
	
		private static final int ENTRY_SIDE = 64, // Entry is one space of the grid used to display the populations
				ICON_SIDE = 48,
				BORDER_SIDE = ENTRY_SIDE - ICON_SIDE, // Border is the space between the side of entry and that of the icon inside of it	
				ICON_LAYER = 1; 
				
		PopulationsPanel () {
			this.setOpaque(true);
			this.setBackground(Color.white);
			this.setBorder(BorderFactory.createLineBorder(Color.black));
		}
						
		/**
		 * Method which is called whenever the partition tool is opened. The populations are displayed
		 * on a grid, the row represents different layers. Therefore populations on different columns but same row 
		 * belong to the same layer
		 */
		
		void customUpdate () { 
			this.removeAll();
			lineEnds.clear();	
			
			buildPopulationsMatrix(selectedNode.terminal.populations);
			
			/*
			 * Pre-compute the max width of all the rows. There's the chance that the width
			 * of the matrix is smaller of that of the first or last rows, which contain terminal 
			 * nodes and not populations. Therefore maxWidth must be checked against the width of
			 * these rows
			 */
						
			int width = selectedNode.terminal.presynapticTerminals.size() > maxWidth ? 
					selectedNode.terminal.presynapticTerminals.size() : maxWidth;
			width = width > selectedNode.terminal.postsynapticTerminals.size() ?
					width : selectedNode.terminal.postsynapticTerminals.size();
			width *= ENTRY_SIDE;

			this.setPreferredSize(new Dimension(width + BORDER_SIDE, 
					BORDER_SIDE + ENTRY_SIDE * (maxDepth + 1))); 			
			
			/*
			 * Display the presynaptic terminals
			 */
			
			// Offset from the border of the frame			
			int offset = (width - selectedNode.terminal.presynapticTerminals.size() * ENTRY_SIDE) / 2;
			
			// Hash map used to access the input terminals using their IDs
			HashMap<Integer, TerminalLabel> inputTerminalLabels = new HashMap<>();	
			
			// Create the label for the input terminals and put them on the grid
			for (Terminal presynTerminal : selectedNode.terminal.presynapticTerminals) {
				TerminalLabel label = new TerminalLabel(presynTerminal);
				int i = selectedNode.terminal.presynapticTerminals.indexOf(presynTerminal);
				label.setBounds(offset + BORDER_SIDE + i * ENTRY_SIDE, BORDER_SIDE, ICON_SIDE, ICON_SIDE);
				label.setFont(font);
				this.add(label, ICON_LAYER);
				inputTerminalLabels.put(label.terminal.id, label);
			}
			
			/*
			 * Display the populations
			 */
						
			// Map used to access the populations using only their IDs
			HashMap<Integer, PopulationLabel> populationLabels = new HashMap<>(); 
			
			// Iterate over the layers
			for (int i = 0; i < maxDepth; i++) { 			
				int yPos =	BORDER_SIDE + i * ENTRY_SIDE;					

				if (popMatrix[i] != null) {				
					int rowWidth = popMatrix[i].length;		
										
					// Iterate over the populations of a given layer		
					for (int j = 0; j < rowWidth; j++) {
						// If this row is smaller in width than maxWidth, then its entries must be centred. To 
						// do so, calculate the offset from the boder
						offset = (width - rowWidth * ENTRY_SIDE) / 2;				
						
						PopulationLabel label = new PopulationLabel(popMatrix[i][j]);					
						label.setBounds(offset + BORDER_SIDE + j * ENTRY_SIDE, yPos, ICON_SIDE, ICON_SIDE);
												
						//label.setText("(" + j + ", " + i + ")");
						label.setText("" + label.population.id);
						label.setVerticalTextPosition(JLabel.TOP);
						label.setHorizontalTextPosition(JLabel.CENTER);						
						System.out.println(label.population.id);
						
						this.add(label, ICON_LAYER);
												
						populationLabels.put(label.population.id, label);
						
						// Store the starting and the ending points of the segments that connect the current node to
						// its inputs
						for (Integer index : popMatrix[i][j].inputIndexes) {
							JLabel inputLabel;
							
							// If the current population belongs to the first row, then its inputs must be searched
							// for among the input terminals						
							if (i == 1) {
								inputLabel = inputTerminalLabels.get(index);
							}
							else 
								inputLabel = populationLabels.get(index);
													
							int[][] coordinates = new int[2][2];
							coordinates[0][0] = inputLabel.getX();
							coordinates[0][1] = inputLabel.getY();
							coordinates[1][0] = label.getX();
							coordinates[1][1] = label.getY();
							lineEnds.add(coordinates);
						}
						/* [End of for (Integer index ...) */
					 }			
					/* [End of for (int j ..) */
				}
				/* [End of (if popMatrix[i] ...] */
			}
			/* [End of for (int i ...)] */
			
			
			/*
			 * Display the postsynaptic terminals
			 */
			
			offset = (width - selectedNode.terminal.postsynapticTerminals.size() * ENTRY_SIDE) / 2;
			
			// As before for the presynaptic terminals
			for (Terminal postsynTerminal : selectedNode.terminal.postsynapticTerminals) {
				TerminalLabel label = new TerminalLabel(postsynTerminal);
				int i = selectedNode.terminal.postsynapticTerminals.indexOf(postsynTerminal);
				label.setBounds(offset + BORDER_SIDE + i * ENTRY_SIDE, BORDER_SIDE + maxDepth * ENTRY_SIDE, ICON_SIDE, ICON_SIDE);
				label.setFont(font);
				this.add(label, ICON_LAYER);				
				
				for (PopulationLabel inputLabel : populationLabels.values()) {
					if (inputLabel.population.outputIndexes.contains(postsynTerminal.id)) {
						int[][] coordinates = new int[2][2];
						coordinates[0][0] = inputLabel.getX();
						coordinates[0][1] = inputLabel.getY();
						coordinates[1][0] = label.getX();
						coordinates[1][1] = label.getY();
						lineEnds.add(coordinates);
					}
				}
			}		
						
			this.revalidate();
			this.repaint();
		}		
		
		/**
		 * Custom paint method that in addition to the standard operations 
		 * also paints the segments that connect the populations and terminals
		 */
		
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			for (int[][] coordinates : lineEnds) {
				g.drawLine(coordinates[0][0] + 24, coordinates[0][1] + 24, 
						coordinates[1][0] + 24, coordinates[1][1] + 24);
			}
		}
		
	}
	
	private class TerminalLabel extends JLabel implements MouseListener {
		private ImageIcon labelIcon = new ImageIcon();
		private Image selectedImg, deselectedImg;
		private Terminal terminal;
		
		private TerminalLabel (Terminal terminal) {
			this.terminal = terminal;
			try {
				deselectedImg = ImageIO.read(getClass().getResource("/icons/icons8-New Moon.png"));
				selectedImg = ImageIO.read(getClass().getResource("/icons/icons8-0 Percents.png"));
				labelIcon = (new ImageIcon(deselectedImg.getScaledInstance(32, 32, Image.SCALE_SMOOTH)));
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.setIcon(labelIcon);
			this.setText(terminal.ip);
			this.setVerticalTextPosition(JLabel.TOP);
			this.setHorizontalTextPosition(JLabel.CENTER);
			this.addMouseListener(this);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (selectedPop != null) {
				selectedPop.labelIcon = (new ImageIcon(selectedPop.deselectedImg.getScaledInstance(32, 32, Image.SCALE_SMOOTH)));
				selectedPop.setIcon(selectedPop.labelIcon);
				selectedPop.repaint();
				selectedPop = null;
			}
			
			if (selectedTerminal == null || !selectedTerminal.equals(this)) {
				if (selectedTerminal != null) {
					selectedTerminal.labelIcon = (new ImageIcon(deselectedImg.getScaledInstance(32, 32, Image.SCALE_SMOOTH)));
					selectedTerminal.setIcon(selectedTerminal.labelIcon);
					selectedTerminal.repaint();
				}
				
				selectedTerminal = this;
				labelIcon = (new ImageIcon(selectedImg.getScaledInstance(32, 32, Image.SCALE_SMOOTH)));
				infoPanel.removeAll();				
				infoPanel.add(new JLabel("# of neurons " + terminal.numOfNeurons));
				infoPanel.add(new JLabel("# of dendrites " + terminal.numOfDendrites));
				infoPanel.add(new JLabel("# of synapses " + terminal.numOfSynapses));				
				infoPanel.repaint();
				infoPanel.revalidate();
				
				if (addingInputs) {
					if (!inputDevs.contains(terminal) & 
							selectedNode.terminal.presynapticTerminals.contains(terminal)) {
						inputDevs.add(terminal);						
						JLabel label = (JLabel) optionsPanel.getComponent(0);
						int numOfInputs = inputPops.size() + inputDevs.size();
						label.setText("Added " + numOfInputs + " inputs");
						optionsPanel.repaint();
						optionsPanel.revalidate();
					}
				} else if (addingOutputs) {
					if (!outputDevs.contains(terminal) & 
							selectedNode.terminal.postsynapticTerminals.contains(terminal)) {
						outputDevs.add(terminal);						
						JLabel label = (JLabel) optionsPanel.getComponent(0);
						int numOfOutputs = outputPops.size() + outputDevs.size();
						label.setText("Added " + numOfOutputs + " outputs");
						optionsPanel.repaint();
						optionsPanel.revalidate();
					}
				}
			} else if (selectedTerminal.equals(this)) {
				selectedTerminal = null;
				labelIcon = (new ImageIcon(deselectedImg.getScaledInstance(32, 32, Image.SCALE_SMOOTH)));
				infoPanel.removeAll();				
				infoPanel.add(new JLabel("Select a pop. or a dev."));
				infoPanel.repaint();
				infoPanel.revalidate();
			}
			
			this.setIcon(labelIcon);
			this.repaint();
			thisFrame.pack();
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub			
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub			
		}
		
	}
	
	private class PopulationLabel extends JLabel implements MouseListener {
		private ImageIcon labelIcon = new ImageIcon();
		private Image selectedImg, deselectedImg;
		private Population population;
		
		private PopulationLabel (Population population) {
			this.population = population;
			try {
				deselectedImg = ImageIO.read(getClass().getResource("/icons/neuron.png"));
				selectedImg = ImageIO.read(getClass().getResource("/icons/neuron_selected.png"));
				labelIcon = (new ImageIcon(deselectedImg.getScaledInstance(32, 32, Image.SCALE_SMOOTH)));
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.setIcon(labelIcon);
			this.addMouseListener(this);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (selectedTerminal != null) {
				selectedTerminal.labelIcon = (new ImageIcon(selectedTerminal.deselectedImg.getScaledInstance(32, 32, Image.SCALE_SMOOTH)));
				selectedTerminal.setIcon(selectedTerminal.labelIcon);
				selectedTerminal.repaint();
				selectedTerminal = null;
			}
			
			if (selectedPop == null || !selectedPop.equals(this)) {	
				if (selectedPop != null) {
					selectedPop.labelIcon = (new ImageIcon(deselectedImg.getScaledInstance(32, 32, Image.SCALE_SMOOTH)));
					selectedPop.setIcon(selectedPop.labelIcon);
					selectedPop.repaint();
				}
				selectedPop = this;
				labelIcon = (new ImageIcon(selectedImg.getScaledInstance(32, 32, Image.SCALE_SMOOTH)));
				infoPanel.removeAll();				
				infoPanel.add(new JLabel("# of neurons " + population.numOfNeurons));
				infoPanel.add(new JLabel("# of dendrites " + population.numOfDendrites));
				infoPanel.add(new JLabel("# of synapses " + population.numOfSynapses));				
				infoPanel.repaint();
				infoPanel.revalidate();
				
				if (addingInputs) {
					if (!inputPops.contains(population)) {
						inputPops.add(population);						
						JLabel label = (JLabel) optionsPanel.getComponent(0);
						int numOfInputs = inputPops.size() + inputDevs.size();
						label.setText("Added " + numOfInputs + " inputs");
						optionsPanel.repaint();
						optionsPanel.revalidate();
					}
				} else if (addingOutputs) {
					if (!outputPops.contains(population)) {
						outputPops.add(population);						
						JLabel label = (JLabel) optionsPanel.getComponent(0);
						int numOfOutputs = outputPops.size() + outputDevs.size();
						label.setText("Added " + numOfOutputs + " outputs");
						optionsPanel.repaint();
						optionsPanel.revalidate();
					}
				}
			} else if (selectedPop.equals(this)) {
				selectedPop = null;
				labelIcon = (new ImageIcon(deselectedImg.getScaledInstance(32, 32, Image.SCALE_SMOOTH)));
				infoPanel.removeAll();				
				infoPanel.add(new JLabel("Select a pop. or a dev."));
				infoPanel.repaint();
				infoPanel.revalidate();
			}
			
			this.setIcon(labelIcon);
			this.repaint();
			thisFrame.pack();
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
