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
import javax.swing.JToggleButton;

public class VirtualLayerVisualizer extends Thread{
	
	static JFrame VLVFrame = new JFrame();
	public LayeredPaneVL layeredPaneVL = new LayeredPaneVL();
	
	private JToggleButton showTerminalFrame = new JToggleButton();

	private boolean shutdown = false;
	private Node selectedNode;
	private JLabel selectedNodeLabel;
	private HashMap<Integer, JLabelVL> nodeIconsTable = new HashMap<>();
	private JPanel infoPanel = new JPanel();

	@Override
	public void run () {
		super.run();
		
		String frameTitle = new String("Virtual Layer Visualizer");
		
		JScrollPane scrollPane = new JScrollPane(layeredPaneVL.connPanel);
		
		JPanel totalPanel = new JPanel();
		JPanel containerPanel = new JPanel();
		JPanel commandsPanel = new JPanel();
		JPanel colorsLegend = new JPanel();
		
		JButton resizeX = new JButton();
		JButton resizeY = new JButton();
		JToggleButton cutLink = new JToggleButton();
		JToggleButton createLink = new JToggleButton();
		JToggleButton paintAll = new JToggleButton();

			
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
				if(ev.getStateChange()==ItemEvent.SELECTED)
					layeredPaneVL.connPanel.paintAll = true;
				else if(ev.getStateChange()==ItemEvent.DESELECTED)
					layeredPaneVL.connPanel.paintAll = false;
			}
		});
		
		/* Commands panel */
		
		commandsPanel.setLayout(new GridLayout(3,2));
		commandsPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Commands"),
					BorderFactory.createEmptyBorder(5,5,5,5)));
		commandsPanel.add(cutLink);
		commandsPanel.add(createLink);
		commandsPanel.add(resizeX);
		commandsPanel.add(resizeY);
		commandsPanel.add(showTerminalFrame);
		commandsPanel.add(paintAll);

		/* Info panel */
		
		infoPanel.setLayout(new GridLayout(3,0));
		infoPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Node info"),
					BorderFactory.createEmptyBorder(5,5,5,5)));
		infoPanel.add(new JLabel("Select a node"));
		
		/* Colors legend panel */
		
		colorsLegend.setLayout(new GridLayout(2,0));
		colorsLegend.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Colors legend"),
				BorderFactory.createEmptyBorder(5,5,5,5)));
		JLabel preConn = new JLabel("Presynaptic conn.");
		preConn.setForeground(Color.red);
		JLabel postConn = new JLabel("Postsynaptic conn.");
		postConn.setForeground(Color.blue);
		colorsLegend.add(preConn);
		colorsLegend.add(postConn);
		
		/* Container panel */
		
		// This panel contains the commands, info and colors legend panels
		
		containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS));		
		containerPanel.add(commandsPanel);
		containerPanel.add(Box.createRigidArea(new Dimension(0,5)));		
		containerPanel.add(infoPanel);
		containerPanel.add(Box.createRigidArea(new Dimension(0,5)));		
		containerPanel.add(colorsLegend);
				
		// Constraints used for the total panel layout
		
		GridBagConstraints VLConstraint = new GridBagConstraints(), commandsConstraint = new GridBagConstraints();	
		
		VLConstraint.gridx = 0;
		VLConstraint.gridy = 0;
		VLConstraint.gridwidth = 2;	
		VLConstraint.fill = GridBagConstraints.BOTH;
		VLConstraint.weightx = 0.5;
		VLConstraint.weighty = 0.5;
		
		commandsConstraint.gridx = GridBagConstraints.RELATIVE;		
		commandsConstraint.gridy = 0;
		commandsConstraint.fill = GridBagConstraints.HORIZONTAL;
					
		/* Total panel */
		
		// Total panel is made of the container panel and the main view of the VLV, scroll pane
				
		totalPanel.setLayout(new GridBagLayout());
		totalPanel.add(scrollPane, VLConstraint);
		totalPanel.add(containerPanel, commandsConstraint);
				
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
	        
	        JLabelVL[] nodeIconsArray = new JLabelVL[nodeIconsTable.size()];
	        int forIterations = 1;
	        
	        Node node;
	        JLabel nodeLabel;
	        
	        if (paintAll) {
	        	nodeIconsArray = nodeIconsTable.values().toArray(new JLabelVL[nodeIconsTable.size()]);	        	
	        	forIterations = nodeIconsArray.length;
	        } 
	        
	        for (int j = 0; j < forIterations; j++) {
	        	
	        	node = paintAll ? nodeIconsArray[j].node : selectedNode;
	        	nodeLabel = paintAll ? nodeIconsArray[j].nodeLabel : selectedNodeLabel;
	        		        	
				// Draw the connections only for the selected node
				if (node != null) {

					g.setColor(Color.black);
					
					// Write node IP on top of the selected node icon
					g.drawString(node.terminal.ip, nodeLabel.getX(), nodeLabel.getY());

					// Presynaptic connections are drawn in red
					g.setColor(Color.red);

					// Iterate over the presynaptic connections of the node selected
					for (int i = 0; i < node.presynapticNodes.size(); i++) {

						// For the i-th connections, proceeds only if it doesn't belong to the server itself
						if (node.presynapticNodes.get(i).terminal.ip != VirtualLayerManager.serverIP) {

							// Store the location of the icon representing the i-th node
							tmpPoint = nodeIconsTable.get(node.presynapticNodes.get(i).ipHashCode).nodeLabel
									.getLocation();

							// Retrieve the radius of the icon of the i-th node
							radius = nodeIconsTable.get(node.presynapticNodes.get(i).ipHashCode).nodeLabel
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
					g.setColor(Color.blue);

					// Just like the previous for, but this time with post instead of pre connections
					for (int i = 0; i < node.postsynapticNodes.size(); i++) {

						if (node.postsynapticNodes.get(i).terminal.ip != VirtualLayerManager.serverIP) {

							tmpPoint = nodeIconsTable.get(node.postsynapticNodes.get(i).ipHashCode).nodeLabel
									.getLocation();

							radius = nodeIconsTable.get(node.postsynapticNodes.get(i).ipHashCode).nodeLabel
									.getWidth() / 2;

							g.drawLine(nodeLabel.getX() + nodeLabel.getWidth() / 2,
									nodeLabel.getY() + nodeLabel.getHeight() / 2 + 4, tmpPoint.x + radius,
									tmpPoint.y + radius + 4);

							g.drawString(String.valueOf(node.postsynapticNodes.get(i).terminal.numOfNeurons),
									(nodeLabel.getX() + tmpPoint.x) / 2,
									(nodeLabel.getY() + tmpPoint.y) / 2 + 36);
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
		
	// TODO Resizing of the window should be achieved by implementing Scrollable
	
	
	
	public class LayeredPaneVL extends JPanel implements MouseMotionListener {
		
		public JLayeredPane VLPanel = new JLayeredPane();
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
					
			JLabelVL newNodeLabel = new JLabelVL(node);
			nodeIconsTable.put(node.ipHashCode, newNodeLabel);
			VLPanel.add(newNodeLabel.nodeLabel, new Integer(1), nodeIconsTable.size());
			connPanel.revalidate();
			connPanel.repaint();
						
		}
		
		public void removeNode (Node node) {
			
			if (selectedNode == node) {
				selectedNode = null;
				selectedNodeLabel = null;
				infoPanel.removeAll();
				infoPanel.add(new JLabel("Select a node"));
				infoPanel.revalidate();
				infoPanel.repaint();
			}
				
			VLPanel.remove(nodeIconsTable.get(node.ipHashCode).nodeLabel);
			connPanel.revalidate();
			connPanel.repaint();
			nodeIconsTable.remove(node.ipHashCode);
						
		}
		
		public void mouseMoved(MouseEvent e) {}
				
		public void mouseDragged(MouseEvent e) {	
			
			if (selectedNode != null) {
				selectedNodeLabel.setLocation(e.getX(), e.getY());	
				if (e.getX() > (VLPanel.getWidth() - 10) || e.getY() > (VLPanel.getHeight() - 10)) {
					this.setPreferredSize(new Dimension(VLPanel.getWidth() + 32, VLPanel.getHeight() + 32));
					connPanel.revalidate();
				}
				connPanel.paintImmediately(connPanel.getVisibleRect());
			}							
			
		}
			
	}
	
	public class JLabelVL extends JLabel implements MouseListener {
		
		private ImageIcon nodeIcon;
		private JLabel nodeLabel;
		private boolean mousePressed = false;
		private Node node = new Node(null, null);
		int dimension;
		
		public JLabelVL (Node n) {
			
			this.node = n;
			
			this.dimension = (int) (48.0f - (24.0f / 1023.0f) * (1024.0f - (float)n.terminal.numOfNeurons));
					
			try {
				Image img = ImageIO.read(getClass().getResource("/icons/icons8-New Moon.png"));
				nodeIcon = (new ImageIcon(img.getScaledInstance(this.dimension, this.dimension, Image.SCALE_SMOOTH)));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			assert nodeIcon != null;
			
			nodeLabel = new JLabel(nodeIcon);
			nodeLabel.setBounds(64, 64, this.dimension, this.dimension);
			
			nodeLabel.addMouseListener(this);
						
		}
		
		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		
		public void mouseClicked(MouseEvent e) {
						
			if (!mousePressed && selectedNode == null) {
				
				if (node.terminalFrame.frame.isVisible())
					showTerminalFrame.setSelected(true);
				else
					showTerminalFrame.setSelected(false);
				
				infoPanel.removeAll();
				infoPanel.add(new JLabel("# neurons " + node.terminal.numOfNeurons));
				infoPanel.add(new JLabel("# synapses " + node.terminal.numOfSynapses));
				infoPanel.add(new JLabel("# dendrites " + node.terminal.numOfDendrites));
				infoPanel.revalidate();
				infoPanel.repaint();
				
				try {
					Image img = ImageIO.read(getClass().getResource("/icons/icons8-0 Percents.png"));
					nodeIcon = (new ImageIcon(img.getScaledInstance(this.dimension, this.dimension, Image.SCALE_SMOOTH)));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				assert nodeIcon != null;
						
				nodeLabel.setIcon(nodeIcon);
				mousePressed = true;
				selectedNode = this.node;
				selectedNodeLabel = nodeIconsTable.get(selectedNode.ipHashCode).nodeLabel;
				
				layeredPaneVL.connPanel.revalidate();
				layeredPaneVL.connPanel.repaint();
				
			} else if (selectedNode == this.node) {
				
				selectedNode = null;
				selectedNodeLabel = null;
				showTerminalFrame.setSelected(false);
				
				infoPanel.removeAll();
				infoPanel.add(new JLabel("Select a node"));
				infoPanel.revalidate();
				infoPanel.repaint();
				
				try {
					Image img = ImageIO.read(getClass().getResource("/icons/icons8-New Moon.png"));
					nodeIcon = (new ImageIcon(img.getScaledInstance(this.dimension, this.dimension, Image.SCALE_SMOOTH)));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				assert nodeIcon != null;
				
				nodeLabel.setIcon(nodeIcon);
				mousePressed = false;	
				
				layeredPaneVL.connPanel.revalidate();
				layeredPaneVL.connPanel.repaint();
				
			}
			
		}
				
	}

}
