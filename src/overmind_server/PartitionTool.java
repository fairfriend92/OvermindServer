package overmind_server;
import com.example.overmind.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

public class PartitionTool extends JFrame implements WindowListener {	
	 Node selectedNode = null;	
	 
	 private PopulationsPanel populationsPanel = new PopulationsPanel();
	 
	 int open() {
		 this.setTitle("Partition Tool");
		 this.setVisible(true);
		 this.selectedNode = VirtualLayerVisualizer.selectedNode;
		 
		 if (selectedNode == null) {
			 System.out.println("Partition Tool: selectedNode is null when it shouldn't");
			 return Constants.ERROR;
		 }
		 
		 populationsPanel.customUpdate();
		 		 
		 this.addWindowListener(this);
		 this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		 this.setContentPane(populationsPanel);
		 //this.setResizable(false);
		 this.revalidate();
		 this.repaint();
		 this.pack();
		 
		 return Constants.SUCCESS;
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
	 * Custom class that describes the panel which visualizes how the neural network of a
	 * given device is partitioned into multiple populations
	 * @author rodolforocco
	 *
	 */
	
	private class PopulationsPanel extends JLayeredPane {		
		private Font font = new Font("label font", Font.PLAIN, 10);
		
		ArrayList<int[][]> lineEnds = new ArrayList<>();
	
		private final int ENTRY_SIDE = 64, // Entry is one space of the grid used to display the populations
				ICON_SIDE = 48,
				BORDER_SIDE = ENTRY_SIDE - ICON_SIDE; // Border is the space between the side of entry and that of the icon inside of it	
		
		private Population[][] popMatrix;
		private int maxDepth = 0, maxWidth = 0;
		
		PopulationsPanel () {
			this.setOpaque(true);
			this.setBackground(Color.white);
			this.setBorder(BorderFactory.createLineBorder(Color.black));
		}
		
		/**
		 * Method which is called whenever the partition tool is opened. The populations are displayed
		 * on a grid, the row represents different layers. Therefore populations on different columns but same row 
		 * belong to the same layer
		 * @param numOfPopulations: The number of columns (TODO: Each layer can have a different number of columns)
		 * @param numOfLayers: The number of rows
		 */
		
		void customUpdate () { 
			this.removeAll();
			lineEnds.clear();	
			buildPopulationsMatrix(selectedNode.terminal.populations);
			
			/*
			 * Pre-compute the max width of all the rows
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
			
			HashMap<Integer, TerminalLabel> inputTerminalLabels = new HashMap<>();	
			
			for (Terminal presynTerminal : selectedNode.terminal.presynapticTerminals) {
				TerminalLabel label = new TerminalLabel(presynTerminal);
				int i = selectedNode.terminal.presynapticTerminals.indexOf(presynTerminal);
				label.setBounds(offset + BORDER_SIDE + i * ENTRY_SIDE, BORDER_SIDE, ICON_SIDE, ICON_SIDE);
				label.setFont(font);
				this.add(label);
				inputTerminalLabels.put(label.terminal.id, label);
			}
			
			/*
			 * Display the populations
			 */
						
			HashMap<Integer, PopulationLabel> populationLabels = new HashMap<>(); 
			
			// Iterate over the layers
			for (int j = 1; j < maxDepth; j++) {				
				int yPos =	BORDER_SIDE + j * ENTRY_SIDE;					

				int rowWidth = popMatrix[j].length;		

				// Iterate over the populations of a given layer		
				for (int i = 0; i < rowWidth; i++) {
					offset = (width - rowWidth * ENTRY_SIDE) / 2;					
					PopulationLabel label = new PopulationLabel(popMatrix[j][i]);					
					label.setBounds(offset + BORDER_SIDE + i * ENTRY_SIDE, yPos, ICON_SIDE, ICON_SIDE); 
					this.add(label);
					populationLabels.put(label.population.id, label);
					
					for (Integer index : popMatrix[j][i].inputIndexes) {
						JLabel inputLabel;
						
						if (j == 1) {
							inputLabel = inputTerminalLabels.get(index);
						}
						else 
							inputLabel = populationLabels.get(index.intValue());
												
						int[][] coordinates = new int[2][2];
						coordinates[0][0] = inputLabel.getX();
						coordinates[0][1] = inputLabel.getY();
						coordinates[1][0] = label.getX();
						coordinates[1][1] = label.getY();
						lineEnds.add(coordinates);
					}
				 }				
			}
			
			
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
				this.add(label);				
				
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
		
		private void buildPopulationsMatrix(HashMap<Integer, Population> populationsMap) {
			Collection<Population> populations = populationsMap.values();
			HashMap<Integer, ArrayList<Population>> matrixElements = new HashMap<>(); 
						
			for (Population population : populations) {
				int depthUpwards = ExploreUpwards(population, 0);
				int depthDownwards = ExploreDownwards(population, 0);
				int depth = depthUpwards + depthDownwards;
				Integer key = Integer.valueOf(depthUpwards);
				if (!matrixElements.containsKey(key))
					matrixElements.put(key, new ArrayList<Population>());
				matrixElements.get(key).add(population);
				int width = matrixElements.get(key).size();
				maxDepth = depth > maxDepth ? depth : maxDepth;
				maxWidth = width > maxWidth ? width : maxWidth;
			}
			
			popMatrix = new Population[maxDepth][maxWidth];
			for(int i = 1; i < maxDepth; i++) {
				ArrayList<Population> row = matrixElements.get(Integer.valueOf(i));
				for (int j = 0; j < row.size(); j++) {
					popMatrix[i][j] = row.get(j);
				}
			}			
			
		}
		
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
		
		private int ExploreUpwards(Population population, int depth) {
			int newDepth = depth;
			
			if (population != null) {	
				for (Integer index : population.inputIndexes) {
					int tmpDepth = ExploreDownwards(selectedNode.terminal.populations.get(index), depth);
					newDepth = tmpDepth + 1 > newDepth ? tmpDepth + 1: newDepth;
				}
			} 
			
			return newDepth;		
		}
		
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
		private Terminal terminal;
		
		private TerminalLabel (Terminal terminal) {
			this.terminal = terminal;
			try {
				Image img = ImageIO.read(getClass().getResource("/icons/icons8-New Moon.png"));
				labelIcon = (new ImageIcon(img.getScaledInstance(32, 32, Image.SCALE_SMOOTH)));
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.setIcon(labelIcon);
			this.setText(terminal.ip);
			this.setVerticalTextPosition(JLabel.TOP);
			this.setHorizontalTextPosition(JLabel.CENTER);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
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
		private Population population;
		
		private PopulationLabel (Population population) {
			this.population = population;
			try {
				Image img = ImageIO.read(getClass().getResource("/icons/neuron.png"));
				labelIcon = (new ImageIcon(img.getScaledInstance(32, 32, Image.SCALE_SMOOTH)));
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.setIcon(labelIcon);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub
			
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
