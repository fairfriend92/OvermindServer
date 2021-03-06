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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;

public class PartitionTool extends JFrame implements WindowListener {		
	 public Node selectedNode = null;	
	 
	 private static final boolean DRAW_LINES_ON = true, DRAW_LINES_OFF = false;
	// Entry is one space of the grid used to display the populations.
	 private static final int ENTRY_SIDE = 64, 
				ICON_SIDE = 48,
				// Border is the space between the side of entry and that of the icon inside of it.
				BORDER_SIDE = ENTRY_SIDE - ICON_SIDE, 
				ICON_LAYER = 1; 
	 
	 private PopulationsPanel populationsPanel = new PopulationsPanel();
	 private JPanel globalPanel = new JPanel();
	 private JPanel sidePanel = new JPanel();
	 private JPanel commandsPanel = new JPanel();
	 private JPanel infoPanel = new JPanel();
	 private JPanel optionsPanel = new JPanel();
	 private JPanel addPopPanel = new JPanel();
	 private JButton addPopulation = new JButton();	
	 private JButton removePopulation = new JButton();
	 private JButton commitButton = new JButton();
	 private JToggleButton paintConnections = new JToggleButton();
	 private JFrame thisFrame;
	 
	 private boolean addingInputs = false;
	 private boolean addingOutputs = false;
	 private boolean firstTimeOpened = true;
	 private PopulationLabel selectedPop = null;
	 private TerminalLabel selectedDev = null;
	 private ArrayList<Population> inputPops = new ArrayList<>(); 
	 private ArrayList<com.example.overmind.Terminal> inputDevs = new ArrayList<>(); 
	 private ArrayList<Population> outputPops = new ArrayList<>(); 
	 private ArrayList<com.example.overmind.Terminal> outputDevs = new ArrayList<>();
	 
	 // Matrix used to store the population so that they can be accessed 
	 // directly based on their positions on the grid.
	 private Population[][] popsMatrix;
		
	 // Width and depth of the grid.
	 private int maxDepth = 0, maxWidth = 0;
	 
	 private short numOfDendrites = 0, numOfSynapses = 0, numOfNeurons = 0;
	 
	 int open() {		 
		 this.setVisible(true);
		 this.selectedNode = VirtualLayerVisualizer.selectedNode;
		 
		 if (selectedNode == null) {
			 System.out.println("Partition Tool: selectedNode is null when it shouldn't");
			 return Constants.ERROR;
		 }
		 
		 this.setTitle("Partition Tool");
		 thisFrame = this;
		 addingInputs = false;
		 addingOutputs = false;
		 selectedPop = null;
		 selectedDev = null;
		 inputPops.clear();
		 inputDevs.clear();
		 commandsPanel.setEnabled(true);
			Component[] components = commandsPanel.getComponents();
			for (int i = 0; i < components.length; i++)
				components[i].setEnabled(true);
			 
		 paintConnections.setSelected(true);
		 
		 if (firstTimeOpened) {createComponents();}
		 else {
			 optionsPanel.removeAll();
			 optionsPanel.add(new JLabel("Select a command"));
		 }
		 
		 this.pack();
		 
		 populationsPanel.customUpdate(DRAW_LINES_ON);
		 
		 this.revalidate();
		 this.repaint();
		 this.pack();
		 
		 return Constants.SUCCESS;
	 }
	 
	 void createComponents() {
		 firstTimeOpened = false;
		 
		 try {
			 Image img = ImageIO.read(getClass().getResource("/icons/add.png"));
			 addPopulation.setIcon(new ImageIcon(img.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
		 } catch (IOException e) {
			 e.printStackTrace();
		 }
		 
		 try {
			 Image img = ImageIO.read(getClass().getResource("/icons/remove.png"));
			 removePopulation.setIcon(new ImageIcon(img.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
		 } catch (IOException e) {
			 e.printStackTrace();
		 }
		 
		 try {
			Image img = ImageIO.read(getClass().getResource("/icons/icons8-Paint Bucket.png"));
			paintConnections.setIcon(new ImageIcon(img.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
		 } catch (IOException e) {
			e.printStackTrace();
		 }	
		 
		 try {
			Image img = ImageIO.read(getClass().getResource("/icons/arrow-up.png"));
			commitButton.setIcon(new ImageIcon(img.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
		 } catch (IOException e) {
			e.printStackTrace();
		 }	
		 
		 addPopulation.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {				
					addPopProcedure();
				}			
			});
		 
		 paintConnections.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ev) {
					if(ev.getStateChange()==ItemEvent.SELECTED) {
						populationsPanel.customUpdate(DRAW_LINES_ON);
					}
					else if(ev.getStateChange()==ItemEvent.DESELECTED) {
						populationsPanel.customUpdate(DRAW_LINES_OFF);
					}
				}
			});
		 		 
		 commitButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {				
					VirtualLayerManager.connectNodes(new Node[]{selectedNode});
					optionsPanel.removeAll();
					optionsPanel.add(new JLabel("Node sent"));
					optionsPanel.repaint();
					optionsPanel.revalidate();
				}							
			});		 
		 
		 GridBagConstraints popPanelConstr = new GridBagConstraints();
		 popPanelConstr.gridx = 0;
		 popPanelConstr.gridy = 0;
		 popPanelConstr.fill = GridBagConstraints.BOTH;
		 
		 sidePanel.setLayout(new GridLayout(3, 1));
		 GridBagConstraints sidePanelConstr = new GridBagConstraints();
		 sidePanelConstr.gridx = 1;
		 sidePanelConstr.gridy = 0;
		 sidePanelConstr.fill = GridBagConstraints.BOTH;
		 sidePanel.add(commandsPanel);
		 sidePanel.add(infoPanel);
		 sidePanel.add(optionsPanel);
		 
		 commandsPanel.setLayout(new GridLayout(1, 3));
		 commandsPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Commands"),
					BorderFactory.createEmptyBorder(5,5,5,5)));
		 commandsPanel.add(addPopulation);
		 commandsPanel.add(removePopulation);
		 commandsPanel.add(paintConnections);
		 commandsPanel.add(commitButton);
		 		 
		 infoPanel.setLayout(new GridLayout(4, 1));
		 infoPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Pop/Dev info"),
					BorderFactory.createEmptyBorder(5,5,5,5)));	
		 infoPanel.removeAll();
		 infoPanel.add(new JLabel("Select a pop. or a dev."));
		 
		 optionsPanel.setLayout(new GridLayout(3, 1));
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
	 }
	
	/**
	 * Method that takes the user through the procedure by which it is possible
	 * to add a new population	 
	 */
	 
	private void addPopProcedure() {
		JButton addInputsButton = new JButton("Add inputs");
		JButton addOutputsButton = new JButton("Add outputs");
		JButton addNeuronsButton = new JButton("Add neurons");
		JButton cancelButton = new JButton("Cancel");
		
		/*
		 * The number of neurons that a population may have should not be greater than
		 * the number of neurons that the terminal can have in total. Additionally, if other
		 * populations already exist, their neurons should be accounted for
		 */
		
		short neuronsUsed = 0;
		for (Population pop : selectedNode.terminal.populations) {
			neuronsUsed += pop.numOfNeurons;
		}
		
		// The population can only have a number of connections equal to that 
		// of the terminal itself
		numOfDendrites = numOfSynapses = selectedNode.originalNumOfSynapses;
		
		// Create a spinner to choose the number of neurons given the aforementioned constraint
		SpinnerNumberModel numberModel = 
				new SpinnerNumberModel(1, 1, selectedNode.terminal.numOfNeurons - neuronsUsed, 1);
        JSpinner neuronsSpinner = new JSpinner(numberModel);	
		
        // TODO: Display error message
		if (selectedNode.terminal.numOfNeurons - neuronsUsed == 0) {return;}
		
		if (selectedPop != null) {
			selectedPop.labelIcon.setImage(selectedPop.deselectedImg);
			selectedPop.repaint();
			selectedPop = null;
		}
		
		if (selectedDev != null) {
			selectedDev.labelIcon.setImage(selectedDev.deselectedImg);
			selectedDev.repaint();
			selectedDev = null;
		}
		
		/*
		 * During the procedure the user may not choose another command,
		 * hence the respective panel should be disabled
		 */
		
		commandsPanel.setEnabled(false);
		Component[] components = commandsPanel.getComponents();
		for (int i = 0; i < components.length; i++)
			components[i].setEnabled(false);
		
		// Adding neurons is the first option that the user is given		
		addNeuronsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// If this button has been pressed it's time to move on to the selection of 
				// the inputs
				addingInputs = true;
				
				Integer number = (Integer)neuronsSpinner.getValue();	
				numOfNeurons = (short) number.intValue();
				
				optionsPanel.removeAll();
				optionsPanel.add(new JLabel("Select inputs"));
				optionsPanel.add(addInputsButton);
				optionsPanel.add(cancelButton);
				
				thisFrame.revalidate();
				thisFrame.repaint();
				thisFrame.pack();
			}			
		});
		
		// As a second step the user must selected which populations and which terminals
		// should act as inputs of the new population
		addInputsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {	
				addingInputs = false; // Once the button is pressed we are not selecting inputs anymore
				addingOutputs = true; // Instead we've moved on to selecting the outputs
				
				optionsPanel.removeAll();
				optionsPanel.add(new JLabel("Select outputs"));
				optionsPanel.add(addOutputsButton);
				optionsPanel.add(cancelButton);
				
				thisFrame.revalidate();
				thisFrame.repaint();
				thisFrame.pack();
			}			
		});
		
		// The last step asks the user to select the pop/dev that should act as outputs
		addOutputsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addingOutputs = false;
				
				// Now that the procedure has completed the commands panel may be 
				// enabled once again
				commandsPanel.setEnabled(true);
				Component[] components = commandsPanel.getComponents();
				for (int i = 0; i < components.length; i++)
					components[i].setEnabled(true);
				
				// The options panel is cleared
				optionsPanel.removeAll();
				
				/*
				 * Display an appropriate error message in the options panel if the 
				 * user has not selected either an input or an output, otherwise
				 * proceed to the creation of the population
				 */
				
				if (inputPops.size() == 0 & inputDevs.size() == 0) 
					optionsPanel.add(new JLabel("Select at least 1 input"));
				else if (outputPops.size() == 0 & outputDevs.size() == 0)
					optionsPanel.add(new JLabel("Select at least 1 output"));
				else  {
					optionsPanel.add(new JLabel("Population created"));
				
					Population population = new Population(numOfNeurons, numOfDendrites, numOfSynapses);
				
					ArrayList<Integer> inputs = new ArrayList<>();
					ArrayList<Integer> outputs = new ArrayList<>();
					
					// Add to the ArrayLists of the new populations the IDs of the inputs
					// and the outputs
					for (Population inputPop : inputPops) {
						inputPop.outputIndexes.add(population.id);
						inputs.add(inputPop.id);
					}
					for (Terminal inputDev : inputDevs) {	
						inputs.add(inputDev.id);
					}
					for (Population outputPop : outputPops) {
						outputPop.inputIndexes.add(population.id);
						outputs.add(outputPop.id);
					}
					for (Terminal outputDev : outputDevs) {
						outputs.add(outputDev.id);		
					}
					
					population.inputIndexes = inputs;
					population.outputIndexes = outputs;
					
					selectedNode.terminal.addPopulation(population);
					// This call will automatically trigger the update of the pops matrix.
					populationsPanel.customUpdate(DRAW_LINES_ON); 
				}
				
				inputPops.clear();
				inputDevs.clear();
				outputPops.clear();
				outputDevs.clear();
				
				numOfNeurons = numOfSynapses = numOfDendrites = 0;
				
				thisFrame.revalidate();
				thisFrame.repaint();
				thisFrame.pack();
			}
		});
		
		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				addingInputs = false;
				addingOutputs = false;
				
				inputPops.clear();
				inputDevs.clear();
				outputPops.clear();
				outputDevs.clear();
				
				numOfNeurons = numOfDendrites = numOfSynapses = 0;
				optionsPanel.removeAll();
				optionsPanel.add(new JLabel("Select a command"));
				
				commandsPanel.setEnabled(true);
				Component[] components = commandsPanel.getComponents();
				for (int i = 0; i < components.length; i++)
					components[i].setEnabled(true);
				
				thisFrame.revalidate();
				thisFrame.repaint();
				thisFrame.pack();
			}
			
		});
		
		optionsPanel.removeAll();
		
		// The user is first given the option of choosing how many neurons the population should have
		optionsPanel.add(neuronsSpinner);
		optionsPanel.add(addNeuronsButton);
		optionsPanel.add(cancelButton);
		
		this.revalidate();
		this.repaint();
		this.pack();
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
	 * 
	 * @param populations: The ArrayList containing the populations. 
	 * @param terminal This is an optional parameter. If it's different from null the matrix of this terminal is updated
	 * 		  instead of that of the selected terminal
	 */
	
	public void buildPopulationsMatrix(ArrayList<Population> populations, Terminal terminal) {
		
		// Values which are local to this instance of PartitionTool should be updated only if the populations
		// come from the selected terminal
		boolean updateLocalValues = terminal == null || 
				(selectedNode != null && selectedNode.terminal.id == terminal.id);		
		
		// Reset global depth and width values
		if (updateLocalValues) {
			maxWidth = 0; 
			maxDepth = 0;
		}
		
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
			int depthUpwards = ExploreUpwards(population, 0) - 1;		
			int depthDownwards = ExploreDownwards(population, 0);
			int depth = depthUpwards + depthDownwards;
			
			// Use the layer of population to store it in the hash map
			Integer key = Integer.valueOf(depthUpwards);
			if (!matrixElements.containsKey(key))
				matrixElements.put(key, new ArrayList<Population>());				
			matrixElements.get(key).add(population);
			int width = matrixElements.get(key).size();
			
			// Check if the order of the matrix should be increased based on the new info
			if (updateLocalValues) {
				maxDepth = depth > maxDepth ? depth : maxDepth;
				maxWidth = width > maxWidth ? width : maxWidth;
			}
		}
		
		// Create the matrix
		popsMatrix = new Population[maxDepth][];
		for(int i = 0; i < maxDepth; i++) {
			// Each array represents a different row of populations
			ArrayList<Population> row = matrixElements.get(Integer.valueOf(i));
			
			popsMatrix[i] = new Population[row.size()];
			for (int j = 0; j < row.size(); j++) {
				popsMatrix[i][j] = row.get(j);
			}			
		}		

		if (updateLocalValues) {
			selectedNode.terminal.popsMatrix = popsMatrix;	
			
			if (terminal != null) { terminal.popsMatrix = popsMatrix; }
		} else {
			terminal.popsMatrix = popsMatrix;
		}
	}
	
	/**
	 * Recursive method that explore the tree of the populations downwards until 
	 * it's found an output terminal.
	 * @param population: The population from which the exploration starts.
	 * @param depth: The depth of the current population.
	 * @return: The depth of the last known population.
	 */
	
	private int ExploreDownwards(Population population, int depth) {
		int newDepth = depth;

		if (population != null) {	
			for (Integer index : population.outputIndexes) {
				
				Population pop = null;
				for (Population tmpPop : selectedNode.terminal.populations) 
					if (tmpPop.id == index) {
						pop = tmpPop;
						break;
					}
				assert pop != null;
				
				int tmpDepth = ExploreDownwards(pop, depth);
				newDepth = tmpDepth + 1 > newDepth ? tmpDepth + 1: newDepth;
			}
		} 
		
		return newDepth;		
	}
	
	// Just as before but the exploration is upwards. 
	
	private int ExploreUpwards(Population population, int depth) {
		int newDepth = depth;
		
		if (population != null) {	
			for (Integer index : population.inputIndexes) {
				
				Population pop = null;
				for (Population tmpPop : selectedNode.terminal.populations) 
					if (tmpPop.id == index) {
						pop = tmpPop;
						break;
					}
				assert pop != null;
				
				int tmpDepth = ExploreUpwards(pop, depth);
				newDepth = tmpDepth + 1 > newDepth ? tmpDepth + 1: newDepth;
			}
		} 
		
		return newDepth;		
	}
	
	/**
	 * Custom class that describes the panel which visualizes how the neural network of a
	 * given device is partitioned into multiple populations. 
	 * 
	 * Populations are organized on a grid based on their presynaptic and postsynaptic connections.
	 * @author rodolforocco
	 *
	 */
	
	private class PopulationsPanel extends JLayeredPane {		
		private Font font = new Font("label font", Font.PLAIN, 10);
		private int minHeight = 480;
		
		// Array of coupled of points which are the extremes of the segments
		// that connect the populations
		ArrayList<int[][]> lineEnds = new ArrayList<>();
				
		PopulationsPanel () {
			this.setOpaque(true);
			this.setBackground(Color.white);
			this.setBorder(BorderFactory.createLineBorder(Color.black));
		}
								
		/**
		 * Method which is called whenever the partition tool is opened. The populations are displayed
		 * on a grid, the row represents different layers. Therefore populations on different columns but same row 
		 * belong to the same layer.
		 */
		
		void customUpdate (boolean drawLines) { 
			this.removeAll();
			lineEnds.clear();	
			
			buildPopulationsMatrix(selectedNode.terminal.populations, null);
			
			/*
			 * Precompute the max width of all the rows. There's the chance that the width
			 * of the matrix is smaller of that of the first or last rows, which contain terminal 
			 * nodes and not populations. Therefore maxWidth must be checked against the width of
			 * these rows.
			 */
						
			int width = selectedNode.terminal.presynapticTerminals.size() > maxWidth ? 
					selectedNode.terminal.presynapticTerminals.size() : maxWidth;
			width = width > selectedNode.terminal.postsynapticTerminals.size() ?
					width : selectedNode.terminal.postsynapticTerminals.size();
			width *= ENTRY_SIDE;

			int height = BORDER_SIDE + ENTRY_SIDE * (maxDepth + 2); // The + 2 accounts for the input and the output layers.
			
			minHeight = sidePanel.getHeight() > minHeight ? sidePanel.getHeight() : minHeight; 
			
			int yOffset = 0;
			if (height < minHeight) {
				yOffset = (minHeight - height) / 2;
				height = minHeight;
			}
			
			this.setPreferredSize(new Dimension(width + BORDER_SIDE, height)); 	
			
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
				label.setBounds(offset + BORDER_SIDE + i * ENTRY_SIDE, BORDER_SIDE + yOffset, ICON_SIDE, ICON_SIDE);
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
				int yPos =	BORDER_SIDE + (i + 1) * ENTRY_SIDE + yOffset;					

				if (popsMatrix[i] != null) {				
					int rowWidth = popsMatrix[i].length;		
										
					// Iterate over the populations of a given layer		
					for (int j = 0; j < rowWidth; j++) {
						// If this row is smaller in width than maxWidth, then its entries must be centred. To 
						// do so, calculate the offset from the boder
						offset = (width - rowWidth * ENTRY_SIDE) / 2;				
						
						PopulationLabel label = new PopulationLabel(popsMatrix[i][j]);					
						label.setBounds(offset + BORDER_SIDE + j * ENTRY_SIDE, yPos, ICON_SIDE, ICON_SIDE);
												
						//label.setText("(" + j + ", " + i + ")");
						//label.setText("" + label.population.id);
						label.setVerticalTextPosition(JLabel.TOP);
						label.setHorizontalTextPosition(JLabel.CENTER);						
						//System.out.println(label.population.id);
						
						this.add(label, ICON_LAYER);
												
						populationLabels.put(label.population.id, label);
						
						// Store the starting and the ending points of the segments that connect the current node to
						// its inputs
						for (Integer index : popsMatrix[i][j].inputIndexes) {
							JLabel inputLabel;
							
							// If the input is not among the populations then it must be a terminal	
							// Note: Equals method of Population can also accept Integer type
							if (!selectedNode.terminal.populations.contains(index)) {
								inputLabel = inputTerminalLabels.get(index);
							}
							else 
								inputLabel = populationLabels.get(index);
													
							int[][] coordinates = new int[2][2];
							coordinates[0][0] = inputLabel.getX();
							coordinates[0][1] = inputLabel.getY();
							coordinates[1][0] = label.getX();
							coordinates[1][1] = label.getY();
							if (drawLines)
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
				label.setBounds(offset + BORDER_SIDE + i * ENTRY_SIDE, 
						BORDER_SIDE + (maxDepth + 1) * ENTRY_SIDE + yOffset, // The + 1 accounts for the input layer
						ICON_SIDE, 
						ICON_SIDE);
				label.setFont(font);
				this.add(label, ICON_LAYER);				
				
				for (PopulationLabel inputLabel : populationLabels.values()) {
					if (inputLabel.population.outputIndexes.contains(postsynTerminal.id)) {
						int[][] coordinates = new int[2][2];
						coordinates[0][0] = inputLabel.getX();
						coordinates[0][1] = inputLabel.getY();
						coordinates[1][0] = label.getX();
						coordinates[1][1] = label.getY();
						if (drawLines)
							lineEnds.add(coordinates);
					}
				}
			}		
						
			this.revalidate();
			this.repaint();
		}		
		
		/**
		 * Custom paint method that in addition to the standard operations 
		 * also paints the segments that connect the populations and terminals.
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
	
	/**
	 * Class that represents the label of a terminal. Whenever the label is clicked 
	 * different operations may be performed depending on whether a given command had be given
	 * before, like for instance that of initializing the procedure of adding a population
	 * @author rodolforocco
	 *
	 */
	
	private class TerminalLabel extends JLabel implements MouseListener {
		private ImageIcon labelIcon = new ImageIcon();
		private Image selectedImg, deselectedImg;
		private Terminal terminal;
		
		private TerminalLabel (Terminal terminal) {
			this.terminal = terminal;
			try {
				deselectedImg = ImageIO.read(getClass().getResource("/icons/icons8-New Moon.png"));
				selectedImg = ImageIO.read(getClass().getResource("/icons/icons8-0 Percents.png"));
				labelIcon = (new ImageIcon(deselectedImg.getScaledInstance(ICON_SIDE, ICON_SIDE, Image.SCALE_SMOOTH)));
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.setIcon(labelIcon);
			//this.setText(terminal.ip);
			this.setVerticalTextPosition(JLabel.TOP);
			this.setHorizontalTextPosition(JLabel.CENTER);
			this.addMouseListener(this);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			// If a population is selected it should be deselected
			if (selectedPop != null) {
				selectedPop.labelIcon = 
						(new ImageIcon(selectedPop.deselectedImg.getScaledInstance(ICON_SIDE, ICON_SIDE, Image.SCALE_SMOOTH)));
				selectedPop.setIcon(selectedPop.labelIcon);
				selectedPop.repaint();
				selectedPop = null;
			}
			
			// If no terminal is selected or a terminal other than that represented by this label is selected...
			if (selectedDev == null || !selectedDev.equals(this)) {
				// Deselect the other terminal that may be currently selected
				if (selectedDev != null) {
					selectedDev.labelIcon = 
							(new ImageIcon(deselectedImg.getScaledInstance(ICON_SIDE, ICON_SIDE, Image.SCALE_SMOOTH)));
					selectedDev.setIcon(selectedDev.labelIcon);
					selectedDev.repaint();
				}
				
				// Update the relevant info regarding this terminal
				selectedDev = this;
				labelIcon = (new ImageIcon(selectedImg.getScaledInstance(ICON_SIDE, ICON_SIDE, Image.SCALE_SMOOTH)));
				infoPanel.removeAll();				
				infoPanel.add(new JLabel("# of neurons " + terminal.numOfNeurons));
				infoPanel.add(new JLabel("# of dendrites " + terminal.numOfDendrites));
				infoPanel.add(new JLabel("# of synapses " + terminal.numOfSynapses));
				infoPanel.add(new JLabel("ip " + terminal.ip));
				infoPanel.repaint();
				infoPanel.revalidate();
				
				// If currently inputs are being added to a new population...
				if (addingInputs) {
					// If the terminal of the selected label has not been added to the inputs before...
					if (!inputDevs.contains(terminal)) {
						// If the number of dendrites is sufficient to accommodate this terminal...
						if (numOfDendrites >= terminal.numOfNeurons) {
							numOfDendrites -= terminal.numOfNeurons;
							inputDevs.add(terminal);						
							JLabel label = (JLabel) optionsPanel.getComponent(0);
							int numOfInputs = inputPops.size() + inputDevs.size();
							label.setText("Selected " + numOfInputs + " inputs");
							optionsPanel.repaint();
							optionsPanel.revalidate(); 
						} else { // The dendrites are not enough...
							JLabel label = (JLabel) optionsPanel.getComponent(0);
							label.setText("Insufficient resources");
							optionsPanel.repaint();
							optionsPanel.revalidate();
						}						
					} else { // Ther terminal has already been included among the inputs...
						JLabel label = (JLabel) optionsPanel.getComponent(0);
						label.setText("Input already selected");
						optionsPanel.repaint();
						optionsPanel.revalidate();
					}
				} else if (addingOutputs) { // Like before but now the terminal is being added to the outputs of the new pop...
					if (!outputDevs.contains(terminal)) {
						if (numOfSynapses >= terminal.numOfNeurons) {
							numOfSynapses -= terminal.numOfNeurons;
							outputDevs.add(terminal);						
							JLabel label = (JLabel) optionsPanel.getComponent(0);
							int numOfOutputs = outputPops.size() + outputDevs.size();
							label.setText("Selected " + numOfOutputs + " outputs");
							optionsPanel.repaint();
							optionsPanel.revalidate();
						} else {
							JLabel label = (JLabel) optionsPanel.getComponent(0);
							label.setText("Insufficient resources");
							optionsPanel.repaint();
							optionsPanel.revalidate();
						}
					} else {
						JLabel label = (JLabel) optionsPanel.getComponent(0);
						label.setText("Output already selected");
						optionsPanel.repaint();
						optionsPanel.revalidate();
					}
				}
			} else if (selectedDev.equals(this)) { // The label was already selected and thus must be deselected
				selectedDev = null;
				labelIcon = (new ImageIcon(deselectedImg.getScaledInstance(ICON_SIDE, ICON_SIDE, Image.SCALE_SMOOTH)));
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
	
	/**
	 * This class is identical to TerminalLabel with the obvious exception that the label
	 * represents a Population object rather than a Terminal one. Minor differences follow from
	 * this, but functionality remains the same
	 * @author rodolforocco
	 *
	 */
	
	private class PopulationLabel extends JLabel implements MouseListener {
		private ImageIcon labelIcon = new ImageIcon();
		private Image selectedImg, deselectedImg;
		private Population population;
		
		private PopulationLabel (Population population) {
			this.population = population;
			try {
				deselectedImg = ImageIO.read(getClass().getResource("/icons/neuron.png"));
				selectedImg = ImageIO.read(getClass().getResource("/icons/neuron_selected.png"));	
				Image img = null;
				if (selectedPop != null && selectedPop.population.equals(this.population)) {
					img = selectedImg;
					selectedPop = this;
				} else {
					img = deselectedImg;
				}
				labelIcon = (new ImageIcon(img.getScaledInstance(ICON_SIDE, ICON_SIDE, Image.SCALE_SMOOTH)));
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.setIcon(labelIcon);
			this.addMouseListener(this);
		}
		
		/*
		 * There can't be multiple labels with the same population, therefore the equals method can
		 * be overridden with one that compares the underlying populations, which is useful when the labels
		 * are created afresh and their memory addresses are different from the ones they had before
		 */
		
		@Override 
		public boolean equals(Object obj) {
			if (obj == null || !obj.getClass().equals(this.getClass())) {return false;}
			PopulationLabel compare = (PopulationLabel) obj;
			return this.population.equals(compare.population);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (selectedDev != null) {
				selectedDev.labelIcon = 
						(new ImageIcon(selectedDev.deselectedImg.getScaledInstance(ICON_SIDE, ICON_SIDE, Image.SCALE_SMOOTH)));
				selectedDev.setIcon(selectedDev.labelIcon);
				selectedDev.repaint();
				selectedDev = null;
			}
			
			if (selectedPop == null || !selectedPop.equals(this)) {	
				if (selectedPop != null) {
					selectedPop.labelIcon = (new ImageIcon(deselectedImg.getScaledInstance(ICON_SIDE, ICON_SIDE, Image.SCALE_SMOOTH)));
					selectedPop.setIcon(selectedPop.labelIcon);
					selectedPop.repaint();
				}
				selectedPop = this;
				labelIcon = (new ImageIcon(selectedImg.getScaledInstance(ICON_SIDE, ICON_SIDE, Image.SCALE_SMOOTH)));
				infoPanel.removeAll();				
				infoPanel.add(new JLabel("# of neurons " + population.numOfNeurons));
				infoPanel.add(new JLabel("# of dendrites " + population.numOfDendrites));
				infoPanel.add(new JLabel("# of synapses " + population.numOfSynapses));				
				infoPanel.repaint();
				infoPanel.revalidate();
				
				if (addingInputs) {
					if (!inputPops.contains(population)) {
						
						/*
						 * One difference from TerminalLabel is that we must also check that the resource of 
						 * the input and output populations are sufficient; this condition is always satisfied for
						 * a terminal (otherwise it wouldn't have been connected to the terminal the new population
						 * belongs  to) 
						 */
						
						if (numOfDendrites  >= population.numOfNeurons & 
								population.numOfSynapses >= numOfNeurons) {
							numOfDendrites -= population.numOfNeurons;
							population.numOfSynapses -= numOfNeurons;
							inputPops.add(population);						
							JLabel label = (JLabel) optionsPanel.getComponent(0);
							int numOfInputs = inputPops.size() + inputDevs.size();
							label.setText("Selected " + numOfInputs + " inputs");
							optionsPanel.repaint();
							optionsPanel.revalidate();
						} else {
							JLabel label = (JLabel) optionsPanel.getComponent(0);
							label.setText("Insufficient resources");
							optionsPanel.repaint();
							optionsPanel.revalidate();
						}
					} else {
						JLabel label = (JLabel) optionsPanel.getComponent(0);
						label.setText("Input already selected");
						optionsPanel.repaint();
						optionsPanel.revalidate();
					}
				} else if (addingOutputs) {
					if (!outputPops.contains(population)) {
						if (numOfSynapses >= population.numOfNeurons & 
								population.numOfDendrites >= numOfNeurons) {
							numOfSynapses -= population.numOfNeurons;
							population.numOfDendrites -= numOfNeurons;
							outputPops.add(population);						
							JLabel label = (JLabel) optionsPanel.getComponent(0);
							int numOfOutputs = outputPops.size() + outputDevs.size();
							label.setText("Selected " + numOfOutputs + " outputs");
							optionsPanel.repaint();
							optionsPanel.revalidate();
						} else {
							JLabel label = (JLabel) optionsPanel.getComponent(0);
							label.setText("Insufficient resources");
							optionsPanel.repaint();
							optionsPanel.revalidate();
						}
					} else {
						JLabel label = (JLabel) optionsPanel.getComponent(0);
						label.setText("Output already selected");
						optionsPanel.repaint();
						optionsPanel.revalidate();
					}
				}
			} else if (selectedPop.equals(this)) {
				selectedPop = null;
				labelIcon = (new ImageIcon(deselectedImg.getScaledInstance(ICON_SIDE, ICON_SIDE, Image.SCALE_SMOOTH)));
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
