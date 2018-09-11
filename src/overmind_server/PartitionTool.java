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
		 
		 int numOfPopulations = selectedNode.terminal.populations.size();
		 int numOfLayers = 1; // TODO: When we'll have multiple layers on the same terminal, the population array will be a matrix 
		 					  // and numOfLayers will be equal to the y order
		 
		 populationsPanel.customUpdate(numOfPopulations, numOfLayers);
		 		 
		 this.addWindowListener(this);
		 this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		 this.setContentPane(populationsPanel);
		 this.setResizable(false);
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
	
		private final int ENTRY_SIDE = 64, // Entry is one space of the grid used to display the populations
				ICON_SIDE = 48,
				BORDER_SIDE = ENTRY_SIDE - ICON_SIDE; // Border is the space between the side of entry and that of the icon inside of it							
		
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
		
		void customUpdate (int numOfPopulations, int numOfLayers) { 
			this.removeAll();	
			
			/*
			 * Pre-compute the max width of all the rows
			 */
						
			int width = selectedNode.terminal.presynapticTerminals.size() > numOfPopulations ? 
					selectedNode.terminal.presynapticTerminals.size() : numOfPopulations;
			width = width > selectedNode.terminal.postsynapticTerminals.size() ?
					width : selectedNode.terminal.postsynapticTerminals.size();
			width *= ENTRY_SIDE;

			this.setPreferredSize(new Dimension(width + BORDER_SIDE, 
					BORDER_SIDE + ENTRY_SIDE * (numOfLayers + 2))); // The +2 accounts for the two rows used to display the pre and postsynaptic terminals			

			ArrayList<JLabel> rowOfLabels = new ArrayList<JLabel>();
			
			/*
			 * Display the presynaptic terminals
			 */
			
			// Offset from the border of the frame
			int offset = (width - selectedNode.terminal.presynapticTerminals.size() * ENTRY_SIDE) / 2;
			
			for (Terminal presynTerminal : selectedNode.terminal.presynapticTerminals) {
				TerminalLabel label = new TerminalLabel(presynTerminal);
				int i = selectedNode.terminal.presynapticTerminals.indexOf(presynTerminal);
				label.setBounds(offset + BORDER_SIDE + i * ENTRY_SIDE, BORDER_SIDE, ICON_SIDE, ICON_SIDE);
				label.setFont(font);
				rowOfLabels.add(label);
				this.add(label);
			}
			
			/*
			 * Display the populations
			 */
			
			// Iterate over the layers
			for (int j = 0; j < numOfLayers; j++) {
				
				// If is either the input or the output layer, leave room for, respectively
				// the presynaptic or the postsynaptic terminals
				int yPos = j == 0 | j == numOfLayers - 1 ?
						BORDER_SIDE + (j + 1) * ENTRY_SIDE :
							BORDER_SIDE + j * ENTRY_SIDE;	
				
				// Iterate over the populations of a given layer		
				for (int i = 0; i < numOfPopulations; i++) {
							
					//offset = (width - numOfPopulations * ENTRY_SIDE) / 2;
					
					PopulationLabel label = new PopulationLabel(selectedNode.terminal.populations.get(i));					
					label.setBounds(offset + BORDER_SIDE + i * ENTRY_SIDE, yPos, ICON_SIDE, ICON_SIDE); 
					this.add(label);
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
				label.setBounds(offset + BORDER_SIDE + i * ENTRY_SIDE, BORDER_SIDE + (numOfLayers + 1) * ENTRY_SIDE, ICON_SIDE, ICON_SIDE);
				label.setFont(font);
				this.add(label);
			}		
			
			this.revalidate();
			this.repaint();
		}			
		
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
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
			try {
				Image img = ImageIO.read(getClass().getResource("/icons/neuron.png"));
				labelIcon = (new ImageIcon(img.getScaledInstance(32, 32, Image.SCALE_SMOOTH)));
				this.population = population;
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
