import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;

public class VirtualLayerVisualizer extends Thread{
	
	static JFrame VLVFrame = new JFrame();
	public LayeredPaneVL layeredPaneVL = new LayeredPaneVL();
	static boolean cutLinkFlag = false;
	static boolean createLinkFlag = false;
	public JLabelVL[] editableNodeLabels = {null, null};
	
	private JToggleButton showTerminalFrame = new JToggleButton();
		
	private boolean shutdown = false;
	private int editingNode = 0;
	static public Node selectedNode;
	private JLabel selectedNodeLabel;
	private HashMap<Integer, JLabelVL> nodeIconsTable = new HashMap<>();
	private JPanel infoPanel = new JPanel();
	private JPanel logPanel = new JPanel();
	private JPanel weightsPanel = new JPanel();

	public boolean allowBidirectionalConn = false;
	
	JToggleButton cutLink = new JToggleButton();
	JToggleButton createLink = new JToggleButton();	
	
	@Override
	public void run () {
		super.run();
		
		String frameTitle = new String("Virtual Layer Visualizer");					

		JScrollPane scrollPane = new JScrollPane(layeredPaneVL.connPanel);
		
		JPanel totalPanel = new JPanel();
		JPanel containerPanel = new JPanel();
		JPanel commandsPanelTotal = new JPanel();
		JPanel commandsPanel = new JPanel();
		JPanel colorsLegend = new JPanel();
		
		JButton resizeX = new JButton();
		JButton resizeY = new JButton();		
		JToggleButton paintAll = new JToggleButton();		
		JCheckBox bidirectionalConn = new JCheckBox("Bidirectional conn.", false);
		bidirectionalConn.setEnabled(false);
		showTerminalFrame.setEnabled(false);
			
		/* Load icons for the buttons of the commands panel */
		
		try {
			Image img = ImageIO.read(getClass().getResource("/icons/icons8-Cut.png"));
			cutLink.setIcon(new ImageIcon(img.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			Image img = ImageIO.read(getClass().getResource("/icons/icons8-Pencil.png"));
			createLink.setIcon(new ImageIcon(img.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			Image img = ImageIO.read(getClass().getResource("/icons/icons8-Info.png"));
			showTerminalFrame.setIcon(new ImageIcon(img.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
		try {
			Image img = ImageIO.read(getClass().getResource("/icons/icons8-Width.png"));
			resizeX.setIcon(new ImageIcon(img.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
		try {
			Image img = ImageIO.read(getClass().getResource("/icons/icons8-Height.png"));
			resizeY.setIcon(new ImageIcon(img.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
		try {
			Image img = ImageIO.read(getClass().getResource("/icons/icons8-Paint Bucket.png"));
			paintAll.setIcon(new ImageIcon(img.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
		/* Action events for the buttons of the commands panel */
		
		cutLink.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				if(ev.getStateChange()==ItemEvent.SELECTED) {
					
					// The flag editingNode indicates that we are currently editing a node
					editingNode = 1;
					
					// Update the info of the log panel
					logPanel.removeAll();
					logPanel.add(new JLabel("Please select 1st node. Selection order is considered in case of bidirectional connection"));
					logPanel.revalidate();
					
					// When using a tool the other one can't be used at the same time
					createLink.setEnabled(false);
					
					// Set the flag that tells which of the two tools is being used
					cutLinkFlag = true;
					
				}
				else if(ev.getStateChange()==ItemEvent.DESELECTED) {
					
					// If an error message is displayed the log panel must not be cleared.
					// Therefore proceed only if editingNode != 0
					if (editingNode != 0) {
						logPanel.removeAll();
						logPanel.add(new JLabel("Use the [scissor] & [pencil] tools to cut and create links"));
						logPanel.revalidate();
						editingNode = 0;
					}
					
					// Restore the state of the tool button and the relative flag
					createLink.setEnabled(true);
					cutLinkFlag = false;
					
				}
			}
		});
		
		
		// Create link is similar to cut link
		createLink.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				if(ev.getStateChange()==ItemEvent.SELECTED) {
					editingNode = 1;
					logPanel.removeAll();
					logPanel.add(new JLabel("Please select 1st node. Selection order determines which node is input"));
					logPanel.revalidate();
					cutLink.setEnabled(false);
					createLinkFlag = true;
					bidirectionalConn.setEnabled(true);
				}
				else if(ev.getStateChange()==ItemEvent.DESELECTED) {
					if (editingNode != 0) {
						logPanel.removeAll();
						logPanel.add(new JLabel("Use the [scissor] & [pencil] tools to cut and create links"));
						logPanel.revalidate();
						editingNode = 0;
					}
					cutLink.setEnabled(true);
					createLinkFlag = false;
					bidirectionalConn.setEnabled(false);
				}
			}
		});
		
		showTerminalFrame.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				if(ev.getStateChange()==ItemEvent.SELECTED && selectedNode != null)
					selectedNode.terminalFrame.frame.setVisible(true);
				else if(ev.getStateChange()==ItemEvent.DESELECTED && selectedNode != null)
					selectedNode.terminalFrame.frame.setVisible(false);
			}
		});
		
		resizeX.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Component[] childComp = layeredPaneVL.VLPanel.getComponents();
				int maxX = 640, maxY = (int) layeredPaneVL.VLPanel.getSize().getHeight();
				for (int i = 0; i < childComp.length; i++) {
					maxX = maxX > childComp[i].getX() ? maxX : childComp[i].getX() + (int)childComp[i].getSize().getWidth();
				}
				layeredPaneVL.setPreferredSize(new Dimension(maxX, maxY));
				layeredPaneVL.connPanel.revalidate();
				layeredPaneVL.connPanel.repaint();
			}
		});		

		resizeY.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Component[] childComp = layeredPaneVL.VLPanel.getComponents();
				int maxX = (int) layeredPaneVL.VLPanel.getSize().getWidth(), maxY = 480;
				for (int i = 0; i < childComp.length; i++) {
					maxY = maxY > childComp[i].getY() ? maxY : childComp[i].getY() + (int)childComp[i].getSize().getHeight();	
				}
				layeredPaneVL.setPreferredSize(new Dimension(maxX, maxY));
				layeredPaneVL.connPanel.revalidate();
				layeredPaneVL.connPanel.repaint();
			}
		});	
		
		paintAll.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				if(ev.getStateChange()==ItemEvent.SELECTED)	{				
					layeredPaneVL.connPanel.paintAll = true; 
					layeredPaneVL.connPanel.repaint();
					}
				else if(ev.getStateChange()==ItemEvent.DESELECTED) {
					layeredPaneVL.connPanel.paintAll = false;
					layeredPaneVL.connPanel.repaint();				
				}
			}
		});		
				
		/* Commands panel */
		
		commandsPanel.setLayout(new GridLayout(2,3));		
		commandsPanel.add(cutLink);
		commandsPanel.add(createLink);
		commandsPanel.add(resizeX);
		commandsPanel.add(resizeY);
		commandsPanel.add(showTerminalFrame);
		commandsPanel.add(paintAll);
		
		/* Commands panel total */
		
		commandsPanelTotal.setAlignmentX(Component.LEFT_ALIGNMENT);
		commandsPanelTotal.setLayout(new BoxLayout(commandsPanelTotal, BoxLayout.Y_AXIS));		
		commandsPanelTotal.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Commands"),
				BorderFactory.createEmptyBorder(5,5,5,5)));
		commandsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		commandsPanelTotal.add(commandsPanel);
		commandsPanelTotal.add(Box.createRigidArea(new Dimension(0,5)));		
		bidirectionalConn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {							
				JCheckBox cb = (JCheckBox) e.getSource();
		        if (cb.isSelected()) {
		        	allowBidirectionalConn = true;
		        } else {
		        	allowBidirectionalConn = false;
		        }
			}
		});	
		commandsPanelTotal.add(bidirectionalConn);

		/* Info panel */
		
		infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		infoPanel.setLayout(new GridLayout(3,0));
		infoPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Node info"),
					BorderFactory.createEmptyBorder(5,5,5,5)));
		infoPanel.add(new JLabel("Select a node"));
		
		/* Colors legend panel */
		
		colorsLegend.setAlignmentX(Component.LEFT_ALIGNMENT);
		colorsLegend.setLayout(new GridLayout(3,0));
		colorsLegend.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Colors legend"),
				BorderFactory.createEmptyBorder(5,5,5,5)));
		JLabel preConn = new JLabel("Presynaptic connections");
		preConn.setForeground(Color.red);
		JLabel postConn = new JLabel("Postsynaptic connections");
		postConn.setForeground(Color.blue);
		JLabel biConn = new JLabel("Bidirectional connections");
		biConn.setForeground(Color.green);
		colorsLegend.add(preConn);
		colorsLegend.add(postConn);
		colorsLegend.add(biConn);
		
		/* Weights panel */
		
		weightsPanel.setLayout(new BorderLayout());
		weightsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		weightsPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Synaptic weights"),
				BorderFactory.createEmptyBorder(5,5,5,5)));
		weightsPanel.add(new JLabel("Select a node"), BorderLayout.CENTER);
		
		
		/* Container panel */
		
		// This panel contains the commands, info and colors legend panels
		
		containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS));		
		containerPanel.add(commandsPanelTotal);
		containerPanel.add(Box.createRigidArea(new Dimension(0,5)));		
		containerPanel.add(infoPanel);
		containerPanel.add(Box.createRigidArea(new Dimension(0,5)));		
		containerPanel.add(colorsLegend);
		containerPanel.add(Box.createRigidArea(new Dimension(0,5)));		
		containerPanel.add(weightsPanel);
						
		// Constraints used for the total panel layout
		
		GridBagConstraints VLConstraint = new GridBagConstraints(), commandsConstraint = new GridBagConstraints(), 
				logConstraint = new GridBagConstraints();	
		
		VLConstraint.gridx = 0;
		VLConstraint.gridy = 0;
		VLConstraint.gridwidth = 2;	
		VLConstraint.fill = GridBagConstraints.BOTH;
		VLConstraint.weightx = 0.5;
		VLConstraint.weighty = 0.5;
		
		commandsConstraint.gridx = GridBagConstraints.RELATIVE;		
		commandsConstraint.gridy = 0;
		commandsConstraint.fill = GridBagConstraints.HORIZONTAL;
		
		logConstraint.gridx = 0;
		logConstraint.gridy = 1;
		
		/* Log panel */
		
		logPanel.add(new JLabel("Use the [scissor] & [pencil] to cut and create links"));
							
		/* Total panel */
		
		// Total panel is made of the container panel and the main view of the VLV, scroll pane
				
		totalPanel.setLayout(new GridBagLayout());
		totalPanel.add(scrollPane, VLConstraint);
		totalPanel.add(containerPanel, commandsConstraint);
		totalPanel.add(logPanel, logConstraint);
				
		VLVFrame.setTitle(frameTitle);
		VLVFrame.setResizable(false);
		VLVFrame.setContentPane(totalPanel);
		VLVFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);		
		VLVFrame.pack();
		VLVFrame.setVisible(true);	
				
	}
	
	/* Class for the panel which acts as a canvas on which we paint the synaptic connections */
	
	public class DrawablePanelVL extends JPanel {		

		public boolean paintAll = false;
		
		public DrawablePanelVL () {
			this.setPreferredSize(new Dimension(640, 480));
			this.setOpaque(true);
			this.setBackground(Color.white);
			this.setBorder(BorderFactory.createLineBorder(Color.black));
		}
		
		// Extends the paint method to draw the synaptic connections in addition to everything else		
		public void paintComponent(Graphics g) {
	        super.paintComponent(g); 

	        Point tmpPoint;
	        int radius;
	        
	        // Array used to store sequentially the values of the hasmap
	        JLabelVL[] nodeIconsArray = new JLabelVL[nodeIconsTable.size()];
	        
	        // Number of iterations of the following for loop
	        int forIterations = 1;
	        
	        Node node;
	        JLabel nodeLabel;
	        
	        // If paintAll then we need to iterate over all the values of the hasmap, thus they must be stored into
	        // an array
	        if (paintAll) {
	        	nodeIconsArray = nodeIconsTable.values().toArray(new JLabelVL[nodeIconsTable.size()]);	        	
	        	forIterations = nodeIconsArray.length;
	        } 
	        	        
	        // If paintAll iterate over all the nodes whose connections must be painted, otherwise paint those
	        // of the selectedNode only
	        for (int j = 0; j < forIterations; j++) {
	        	
	        	node = paintAll ? nodeIconsArray[j].node : selectedNode;
	        	nodeLabel = paintAll ? nodeIconsArray[j].nodeLabel : selectedNodeLabel;
	        		        	
				if (node != null) {

					g.setColor(Color.black);
					
					// Write node IP on top of the selected node icon
					g.drawString(node.terminal.ip, nodeLabel.getX(), nodeLabel.getY());

					// Use different colors for pre and postsynaptic connections only if only those of the selected 
					// node must be painted, otherwise use black for both																							 									
					
					ArrayList<Integer> servedPresynNodesIndexes = new ArrayList<>(node.presynapticNodes.size());
					ArrayList<Integer> servedPostsynNodesIndexes = new ArrayList<>(node.presynapticNodes.size());

					
					for (int i = 0; i < node.presynapticNodes.size(); i++) {
						
						if (!paintAll)
							g.setColor(Color.green); 
						
						if (node.postsynapticNodes.contains(node.presynapticNodes.get(i))) {
							
							tmpPoint = nodeIconsTable.get(node.presynapticNodes.get(i).virtualID).nodeLabel
									.getLocation();

							radius = nodeIconsTable.get(node.presynapticNodes.get(i).virtualID).nodeLabel
									.getWidth() / 2;

							g.drawLine(nodeLabel.getX() + nodeLabel.getWidth() / 2,
									nodeLabel.getY() + nodeLabel.getHeight() / 2, tmpPoint.x + radius,
									tmpPoint.y + radius);

							if (!paintAll) {
								g.setColor(Color.blue);							
								g.drawString(String.valueOf(node.terminal.numOfNeurons),
										(nodeLabel.getX() + tmpPoint.x) / 2 - 12,
										(nodeLabel.getY() + tmpPoint.y) / 2);
							}
							
							if (!paintAll) {
								g.setColor(Color.red);							
								g.drawString(String.valueOf(node.presynapticNodes.get(i).terminal.numOfNeurons),
										(nodeLabel.getX() + tmpPoint.x) / 2 + 12,
										(nodeLabel.getY() + tmpPoint.y) / 2);
							}
							
							servedPresynNodesIndexes.add(i);	
							servedPostsynNodesIndexes.add(node.postsynapticNodes.indexOf(node.presynapticNodes.get(i)));
							
						}
						
					}								
													
					
					// Presynaptic connections are drawn in red
					if (!paintAll)
						g.setColor(Color.red);

					// Iterate over the presynaptic connections of the node selected
					for (int i = 0; i < node.presynapticNodes.size(); i++) {
					
						// For the i-th connections, proceeds only if it doesn't belong to the server itself
						if (node.presynapticNodes.get(i).terminal.ip != VirtualLayerManager.serverIP && !servedPresynNodesIndexes.contains(i)) {

							// Store the location of the icon representing the i-th node
							tmpPoint = nodeIconsTable.get(node.presynapticNodes.get(i).virtualID).nodeLabel
									.getLocation();

							// Retrieve the radius of the icon of the i-th node
							radius = nodeIconsTable.get(node.presynapticNodes.get(i).virtualID).nodeLabel
									.getWidth() / 2;

							// Draw the line 
							g.drawLine(nodeLabel.getX() + nodeLabel.getWidth() / 2,
									nodeLabel.getY() + nodeLabel.getHeight() / 2, tmpPoint.x + radius,
									tmpPoint.y + radius);

							// Write the number of connections that the line represents just under it
							g.drawString(String.valueOf(node.terminal.numOfNeurons),
									(nodeLabel.getX() + tmpPoint.x) / 2,
									(nodeLabel.getY() + tmpPoint.y) / 2);

						}
					}

					// Postsynaptic connections
					if (!paintAll)
						g.setColor(Color.blue);

					// Just like the previous for, but this time with post instead of pre connections
					for (int i = 0; i < node.postsynapticNodes.size(); i++) {

						if (node.postsynapticNodes.get(i).terminal.ip != VirtualLayerManager.serverIP && !servedPostsynNodesIndexes.contains(i)) {

							tmpPoint = nodeIconsTable.get(node.postsynapticNodes.get(i).virtualID).nodeLabel
									.getLocation();

							radius = nodeIconsTable.get(node.postsynapticNodes.get(i).virtualID).nodeLabel
									.getWidth() / 2;

							g.drawLine(nodeLabel.getX() + nodeLabel.getWidth() / 2,
									nodeLabel.getY() + nodeLabel.getHeight() / 2, tmpPoint.x + radius,
									tmpPoint.y + radius);

							g.drawString(String.valueOf(node.postsynapticNodes.get(i).terminal.numOfNeurons),
									(nodeLabel.getX() + tmpPoint.x) / 2,
									(nodeLabel.getY() + tmpPoint.y) / 2);
						}
						/* [End of inner if] */

					}
					/* [End of inner for] */

				}
				/* [End of outer if] */
				
			}
	        /* [End of outer for] */
	        
		}
		/* [End of paint method] */
		
	}
	/* [End of DrawablePanelVL class] */	
	
	/* Class describing the panel on top of which the labels and the connections are drawn */
	
	public class LayeredPaneVL extends JPanel implements MouseMotionListener {
		
		// The panel containing the nodes labels
		public JLayeredPane VLPanel = new JLayeredPane();
		
		// The panel on which the connections are drawn
		public DrawablePanelVL connPanel = new DrawablePanelVL();
		
		public void setPreferredSize(Dimension d) {
			super.setPreferredSize(d);			
			this.connPanel.setPreferredSize(d);	
			this.VLPanel.setPreferredSize(d);	
		}
		
		public LayeredPaneVL () {
							
			connPanel.add(VLPanel);
			VLPanel.setPreferredSize(new Dimension(640, 480));
			VLPanel.setOpaque(false);
			VLPanel.addMouseMotionListener(this);
									
		}
		
		public void addNode (Node node) {
			
			// A new label for the node is created
			JLabelVL newNodeLabel = new JLabelVL(node);
			// The node is saved in the hash map
			nodeIconsTable.put(node.virtualID, newNodeLabel);
			// The label is added to the appropriate panel
			VLPanel.add(newNodeLabel.nodeLabel, new Integer(1), nodeIconsTable.size());			
			connPanel.revalidate();
			connPanel.repaint();
						
		}
		
		public void removeNode (Node node) {
			
			// If the selected node is the one to be removed further steps need to be taken
			if (selectedNode == node) {
				// Eliminate the reference to the selected node and its label
				selectedNode = null;
				selectedNodeLabel = null;
				// Restore the info panel to its default state
				infoPanel.removeAll();
				infoPanel.add(new JLabel("Select a node"));
				infoPanel.revalidate();
				infoPanel.repaint();
				// Restore the weights panel
				weightsPanel.removeAll();
				weightsPanel.add(new JLabel("Select a node"));
				weightsPanel.revalidate();
				weightsPanel.repaint();
				VLVFrame.pack();
				// No matter the status of the button we reset it to be on the safe side
				showTerminalFrame.setSelected(false);
				showTerminalFrame.setEnabled(false);
			}
			
			// Remove from the panel the label associated to the node, which must be retrieved
			// from the hash map		
			
			VLPanel.remove(nodeIconsTable.get(node.virtualID).nodeLabel);
			
			// Remove the reference to the label from the hash map and repaint the connection panel
			nodeIconsTable.remove(node.virtualID);
			connPanel.revalidate();
			connPanel.repaint();		
						
		}
		
		public void mouseMoved(MouseEvent e) {}
				
		public void mouseDragged(MouseEvent e) {	
			
			// Proceed only if a node has been selected before dragging the mouse
			if (selectedNode != null) {
				// Updated the location of the label of the selected node
				selectedNodeLabel.setLocation(e.getX(), e.getY());	
				// Proceed in the case the label is dragged close to the window border
				if (e.getX() > (VLPanel.getWidth() - 10) || e.getY() > (VLPanel.getHeight() - 10)) {
					// Increase the size of the panel underlying the scrolling panel
					this.setPreferredSize(new Dimension(VLPanel.getWidth() + 32, VLPanel.getHeight() + 32));
					connPanel.revalidate();
				}
				// Repaint the connections
				connPanel.paintImmediately(connPanel.getVisibleRect());
			}							
			
		}
			
	}
	
	/* The class which describes the label representing the nodes */
	
	public class JLabelVL extends JLabel implements MouseListener {
		
		private ImageIcon nodeIcon;
		public JLabel nodeLabel;
		private Node node;
		int dimension;
		
		public JLabelVL (Node n) {
			
			this.node = n;
			
			// Scale the dimension of the icon so that the node with more neurons appears bigger
			this.dimension = (int) (48.0f - (24.0f / 1023.0f) * (1024.0f - (float)n.terminal.numOfNeurons));
			
			// Get the image for the icon
			try {
				Image img = ImageIO.read(getClass().getResource("/icons/icons8-New Moon.png"));
				nodeIcon = (new ImageIcon(img.getScaledInstance(this.dimension, this.dimension, Image.SCALE_SMOOTH)));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			assert nodeIcon != null;
			
			// Associate the icon to the label and put it somewhere in the main window
			nodeLabel = new JLabel(nodeIcon);
			nodeLabel.setBounds(64, 64, this.dimension, this.dimension);
			
			nodeLabel.addMouseListener(this);
						
		}
		
		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		
		public void mouseClicked(MouseEvent e) {
						
			// If this node had already been selected before...
			if (selectedNode == this.node) {
												
				// ...then it must be deselected
				selectedNode = null;
				selectedNodeLabel = null;
				// If the terminal frame of this node was in focus, then the relative button must be deselected
				// now that the node itself has been deselected
				showTerminalFrame.setSelected(false);
				
				// When no node is selected the button to show the terminal frame must be grayed out
				showTerminalFrame.setEnabled(false);
				
				// Update the info panel appropriately
				infoPanel.removeAll();
				infoPanel.add(new JLabel("Select a node"));
				infoPanel.revalidate();
				infoPanel.repaint();
				
				// Update the weights panel
				
				weightsPanel.removeAll();
				weightsPanel.add(new JLabel("Select a node"));
				weightsPanel.revalidate();
				weightsPanel.repaint();
				
				// Get the icon representing a deselected node
				try {
					Image img = ImageIO.read(getClass().getResource("/icons/icons8-New Moon.png"));
					nodeIcon = (new ImageIcon(img.getScaledInstance(this.dimension, this.dimension, Image.SCALE_SMOOTH)));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				assert nodeIcon != null;
				
				// Set the icon and repaint the window
				nodeLabel.setIcon(nodeIcon);
				
				layeredPaneVL.connPanel.revalidate();
				layeredPaneVL.connPanel.repaint();
				
			} else {
				
				// If this node was not selected but some other was...
				if (selectedNode != null) {
					
					// ...then that other node must first be deselected
					try {
						Image img = ImageIO.read(getClass().getResource("/icons/icons8-New Moon.png"));
						nodeIcon = (new ImageIcon(
								img.getScaledInstance(this.dimension, this.dimension, Image.SCALE_SMOOTH)));
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
					assert nodeIcon != null;
					
					selectedNodeLabel.setIcon(nodeIcon);
					
				}
				
				// Now that a node has been selected it should be possible to toggle its terminal frame
				showTerminalFrame.setEnabled(true);
				
				// Reference to this node and its label is added to selecetedNode and selectedNodeLabel
				selectedNode = this.node;
				selectedNodeLabel = nodeIconsTable.get(selectedNode.virtualID).nodeLabel;
				
				// Change the status of the info button depending on visibility of the terminal frame of this node
				if (node.terminalFrame.frame.isVisible())
					showTerminalFrame.setSelected(true);
				else
					showTerminalFrame.setSelected(false);
				
				// Update the info panel
				infoPanel.removeAll();
				infoPanel.add(new JLabel("# neurons " + node.terminal.numOfNeurons));
				infoPanel.add(new JLabel("# synapses " + node.terminal.numOfSynapses));
				infoPanel.add(new JLabel("# dendrites " + node.terminal.numOfDendrites));
				infoPanel.revalidate();
				infoPanel.repaint();
				
				// Updated the weight panel
				
				WeightsTableModel weightsTableModel = new WeightsTableModel(VirtualLayerManager.weightsTable.get(node.virtualID));
				JTable weightsTable = new JTable(weightsTableModel);				
				weightsPanel.removeAll();
				weightsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
				weightsTable.getColumnModel().getColumn(0).setMaxWidth(60);
				weightsTable.getColumnModel().getColumn(1).setMaxWidth(60);
				weightsTable.getColumnModel().getColumn(2).setMaxWidth(60);
				weightsTable.setPreferredScrollableViewportSize(new Dimension(180, 200));
				JScrollPane weightsScrollPane = new JScrollPane(weightsTable);	
				weightsPanel.add(weightsScrollPane, BorderLayout.CENTER);
				weightsPanel.revalidate();
				weightsPanel.repaint();
				VLVFrame.pack();
				
				// Get the icon for the now selected node
				try {
					Image img = ImageIO.read(getClass().getResource("/icons/icons8-0 Percents.png"));
					nodeIcon = (new ImageIcon(img.getScaledInstance(this.dimension, this.dimension, Image.SCALE_SMOOTH)));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				assert nodeIcon != null;
						
				// set the icon and update the panel
				nodeLabel.setIcon(nodeIcon);				
				
				layeredPaneVL.connPanel.revalidate();
				layeredPaneVL.connPanel.repaint();
				
			}
			
			// If one of the two tools has been selected...
			if (cutLinkFlag || createLinkFlag) {						
					
				// If no editable node has been selected...
				if (editingNode == 1) {
					// ...then the first node to edit is this one, therefore add it to the array.
					editableNodeLabels[0] = this;
					// Change the flag describing the status of the editing operation
					editingNode = 2;
					// Update the log panel with the ip of the first editable node
					logPanel.removeAll();
					logPanel.add(new JLabel("1st node is: " + node.terminal.ip + " please select 2nd node"));
					logPanel.revalidate();
					logPanel.repaint();
				}
				else {
					
					// This node is the secon editable node, therefore add it to the array
					editableNodeLabels[1] = this;
					// Send the editable node to the appropriate function
					Node[] nodeToModify = {editableNodeLabels[0].node, editableNodeLabels[1].node};
					short successFlag = VirtualLayerManager.modifyNode(nodeToModify);	
					
					logPanel.removeAll();
					JLabel errorText = new JLabel();
					errorText.setForeground(Color.red);
					
					// Depending on whether the edit operation was succesfull or not display an error message in 
					// the log panel
					if (successFlag == 0) 
						logPanel.add(new JLabel("Operation succesfull!"));
					else if (successFlag == 1) {
						errorText.setText("Error: nodes are not connected in any way");
						logPanel.add(errorText);
					} else  if (successFlag == 2) {
						errorText.setText("Error: nodes are already connected");
						logPanel.add(errorText);
					} else if (successFlag == 3) {
						errorText.setText("Error: " + editableNodeLabels[0].node.terminal.ip + " has insufficient synapses or " + 
											editableNodeLabels[1].node.terminal.ip + " has insufficient dendrites");
						logPanel.add(errorText);
					}
					
					editingNode = 0;

					logPanel.revalidate();
					//logPanel.repaint();									
					
					if (cutLinkFlag) {
						cutLinkFlag = false;
						cutLink.doClick();
					}
					else {
						createLinkFlag = false;
						createLink.doClick();
					}
					
				}
								
			}
			
		}
				
	}

}
