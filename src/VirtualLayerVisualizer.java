import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class VirtualLayerVisualizer extends Thread{
	
	static JFrame VLVFrame = new JFrame();
	LayeredPaneVL layeredPaneVL = new LayeredPaneVL();
	boolean shutdown = false;
	Node selectedNode;
	private HashMap<Integer, JLabelVL> nodeIconsTable = new HashMap<>();
	private JPanel infoPanel = new JPanel();

	@Override
	public void run () {
		super.run();
		
		String frameTitle = new String("Virtual Layer Visualizer");
		
		JPanel totalPanel = new JPanel();
		JPanel containerPanel = new JPanel();
		JPanel commandsPanel = new JPanel();
	
		JToggleButton cutLink = new JToggleButton();
		JToggleButton createLink = new JToggleButton();
		JToggleButton getInfo = new JToggleButton();
		
		GridBagConstraints VLConstraint = new GridBagConstraints(), commandsConstraint = new GridBagConstraints();	
		
		try {
			Image img = ImageIO.read(new FileInputStream("resources/icons/icons8-Cut.png"));
			cutLink.setIcon(new ImageIcon(img.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			Image img = ImageIO.read(new FileInputStream("resources/icons/icons8-Pencil.png"));
			createLink.setIcon(new ImageIcon(img.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			Image img = ImageIO.read(new FileInputStream("resources/icons/icons8-Info.png"));
			getInfo.setIcon(new ImageIcon(img.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
		commandsPanel.setLayout(new GridLayout(2,2));
		commandsPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Commands"),
					BorderFactory.createEmptyBorder(5,5,5,5)));
		commandsPanel.add(cutLink);
		commandsPanel.add(createLink);
		commandsPanel.add(getInfo);
		
		infoPanel.setLayout(new GridLayout(3,0));
		infoPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Node info"),
					BorderFactory.createEmptyBorder(5,5,5,5)));
		infoPanel.add(new JLabel("Select a node"));
		
		containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS));		
		containerPanel.add(commandsPanel);
		containerPanel.add(Box.createRigidArea(new Dimension(0,5)));		
		containerPanel.add(infoPanel);
				
		VLConstraint.gridx = 0;
		VLConstraint.gridy = 0;
		VLConstraint.gridwidth = 2;	
		VLConstraint.fill = GridBagConstraints.BOTH;
		VLConstraint.weightx = 0.5;
		VLConstraint.weighty = 0.5;
		
		commandsConstraint.gridx = GridBagConstraints.RELATIVE;		
		commandsConstraint.gridy = 0;
		commandsConstraint.fill = GridBagConstraints.HORIZONTAL;
					
		totalPanel.setLayout(new GridBagLayout());
		totalPanel.add(layeredPaneVL.VLPanel, VLConstraint);
		totalPanel.add(containerPanel, commandsConstraint);
				
		VLVFrame.setTitle(frameTitle);
		VLVFrame.setContentPane(totalPanel);
		VLVFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);		
		VLVFrame.pack();
		VLVFrame.setVisible(true);	
				
	}
	
	public class LayeredPaneVL extends JPanel implements MouseMotionListener {
		
		public JLayeredPane VLPanel = new JLayeredPane();
		
		public LayeredPaneVL () {
							
			VLPanel.setBorder(BorderFactory.createLineBorder(Color.black));
			VLPanel.setPreferredSize(new Dimension(640, 480));
			VLPanel.setOpaque(true);
			VLPanel.setBackground(Color.white);
			VLPanel.addMouseMotionListener(this);
									
		}
		
		public void addNode (Node node) {
					
			JLabelVL newNodeLabel = new JLabelVL(node);
			nodeIconsTable.put(node.ipHashCode, newNodeLabel);
			VLPanel.add(newNodeLabel.nodeLabel, new Integer(1), nodeIconsTable.size());
						
		}
		
		public void removeNode (Node node) {
			
			if (selectedNode == node) {
				selectedNode = null;
				infoPanel.removeAll();
				infoPanel.add(new JLabel("Select a node"));
				infoPanel.revalidate();
				infoPanel.repaint();
			}
				
			VLPanel.remove(nodeIconsTable.get(node.ipHashCode).nodeLabel);
			VLPanel.revalidate();
			VLPanel.repaint();
			nodeIconsTable.remove(node.ipHashCode);
						
		}
		
		public void mouseMoved(MouseEvent e) {}
				
		public void mouseDragged(MouseEvent e) {	
			
			if (selectedNode != null)
			nodeIconsTable.get(selectedNode.ipHashCode).nodeLabel.setLocation(e.getX(), e.getY());	
			
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
			
			this.dimension = (int) ( 10 * n.terminal.numOfNeurons / 1024) * 24;
			
			try {
				Image img = ImageIO.read(new FileInputStream("resources/icons/icons8-New Moon.png"));
				nodeIcon = (new ImageIcon(img.getScaledInstance(this.dimension, this.dimension, Image.SCALE_SMOOTH)));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			assert nodeIcon != null;
			
			nodeLabel = new JLabel(nodeIcon);
			nodeLabel.setBounds(0, 0, this.dimension, this.dimension);
			
			nodeLabel.addMouseListener(this);
						
		}
		
		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		
		public void mouseClicked(MouseEvent e) {
						
			if (!mousePressed && selectedNode == null) {
				
				infoPanel.removeAll();
				infoPanel.add(new JLabel("# neurons " + node.terminal.numOfNeurons));
				infoPanel.add(new JLabel("# synapses " + node.terminal.numOfSynapses));
				infoPanel.add(new JLabel("# dendrites " + node.terminal.numOfDendrites));
				infoPanel.revalidate();
				infoPanel.repaint();
				
				try {
					Image img = ImageIO.read(new FileInputStream("resources/icons/icons8-0 Percents.png"));
					nodeIcon = (new ImageIcon(img.getScaledInstance(this.dimension, this.dimension, Image.SCALE_SMOOTH)));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				assert nodeIcon != null;
				
				nodeLabel.setIcon(nodeIcon);
				mousePressed = true;
				selectedNode = this.node;
				
			} else if (selectedNode == this.node) {
				
				infoPanel.removeAll();
				infoPanel.add(new JLabel("Select a node"));
				infoPanel.revalidate();
				infoPanel.repaint();
				
				try {
					Image img = ImageIO.read(new FileInputStream("resources/icons/icons8-New Moon.png"));
					nodeIcon = (new ImageIcon(img.getScaledInstance(this.dimension, this.dimension, Image.SCALE_SMOOTH)));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				assert nodeIcon != null;
				
				nodeLabel.setIcon(nodeIcon);
				mousePressed = false;	
				selectedNode = null;
			}
			
		}
				
	}

}
