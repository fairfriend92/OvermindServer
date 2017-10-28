/**
 * Main class which displays the main panel and executes the main threads: 
 * 
 * 1) VirtualLayerManager: manages incoming connections and routes the peers.
 * 
 * 2) SpikesReceiver: listens for inbounds spikes that need to be displayed in the raster graph portion 
 * 		of the frame associated with the sending device.
 * 
 * 3) NodesShutdownPoller: polls the TerminalFrame's to know whether some have raised the
 * 		shutdown flags and eventually send to VirtualLayerManager.removeNodes the associated node
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class MainFrame {

	// TODO Manage application shutdown properly
	
	private static JLabel numOfUnsyncNodes = new JLabel("# of unsync nodes is 0");
	private static JLabel numOfSyncNodes = new JLabel("# of sync nodes is 0");
	private static JLabel numOfShadowNodes = new JLabel("# of shadow nodes is 0");
	private static JFrame frame = new JFrame();
	
	public static volatile boolean useShadowNodesFlag = true;
	
	static long rasterGraphRefresh = 0;

	public static void main(String[] args) {		
		
		displayMainFrame();
					
		// Class that connects, removes and updates the nodes that make up the network
		VirtualLayerManager VLManager = new VirtualLayerManager();		
		VLManager.start();				
		
		// Class that receives the spikes from the terminals and distribute them to the
		// raster graph threads
		SpikesReceiver spikesReceiver = new SpikesReceiver();
		spikesReceiver.start();
		
		// Class that polls shutdown signals of raster graph threads associated with an
		// inactive node
		NodesShutdownPoller nodesShutdownPoller = new NodesShutdownPoller();
		nodesShutdownPoller.start();
			
	}
	
	private static void displayMainFrame() {
		
		JPanel mainPanel = new JPanel();
		JPanel nodesInfoPanel = new JPanel();	
		JPanel commandsPanel = new JPanel();
		JPanel activeShadowNodesRatioPanel = new JPanel();
		JButton syncButton = new JButton();
		JButton increaseRatio = new JButton("+");
		JButton decreaseRatio = new JButton("-");
		JLabel ratio = new JLabel("A/S ratio is 2");
				
		/**
		 * Nodes info panel layout
		 */
		
		nodesInfoPanel.setLayout(new BoxLayout(nodesInfoPanel, BoxLayout.Y_AXIS));
		nodesInfoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Virtual Layer Info"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
		nodesInfoPanel.add(numOfUnsyncNodes);
		nodesInfoPanel.add(numOfSyncNodes);
		nodesInfoPanel.add(numOfShadowNodes);

		/*
		 * Active Shadow nodes ration panel
		 */
				
		activeShadowNodesRatioPanel.setLayout(new BoxLayout(activeShadowNodesRatioPanel, BoxLayout.X_AXIS));
		activeShadowNodesRatioPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		increaseRatio.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				VirtualLayerManager.activeShadowNodesRatio++;
				ratio.setText("A/S ratio is " + VirtualLayerManager.activeShadowNodesRatio);
				ratio.revalidate();
				ratio.repaint();
			}
		});		
		
		decreaseRatio.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (VirtualLayerManager.activeShadowNodesRatio > 0)
					VirtualLayerManager.activeShadowNodesRatio--;
				ratio.setText("A/S ratio is " + VirtualLayerManager.activeShadowNodesRatio);
				ratio.revalidate();
				ratio.repaint();
			}
		});	
		
		ratio.setBorder(BorderFactory.createLineBorder(Color.black));
		ratio.setOpaque(true);
		ratio.setBackground(Color.white);
		activeShadowNodesRatioPanel.add(ratio);
		activeShadowNodesRatioPanel.add(Box.createRigidArea(new Dimension(5,0)));
		activeShadowNodesRatioPanel.add(increaseRatio);
		activeShadowNodesRatioPanel.add(Box.createRigidArea(new Dimension(5,0)));
		activeShadowNodesRatioPanel.add(decreaseRatio);
		
		/**
		 * Commands panel layout		
		 */
		
		commandsPanel.setLayout(new BoxLayout(commandsPanel, BoxLayout.Y_AXIS));
		commandsPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Commands"),
					BorderFactory.createEmptyBorder(5,5,5,5)));
		syncButton.setText("Sync nodes");
		syncButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {							
				VirtualLayerManager.syncNodes();			
			}
		});				
		
		JCheckBox hideVLV = new JCheckBox("Hide Virtual Layer", false);
		hideVLV.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {							
				JCheckBox cb = (JCheckBox) e.getSource();
		        if (cb.isSelected()) {	
		        	VirtualLayerVisualizer.VLVFrame.setVisible(false);
		        } else {
		        	VirtualLayerVisualizer.VLVFrame.setVisible(true);
		        }
			}
		});	
		
		JCheckBox useShadowNodes = new JCheckBox("Shadow nodes", true);
		useShadowNodes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JCheckBox cb = (JCheckBox) e.getSource();
				if (cb.isSelected()) {
					useShadowNodesFlag = true;
					increaseRatio.setEnabled(true);
					decreaseRatio.setEnabled(true);
				} else {
					useShadowNodesFlag = false;
					increaseRatio.setEnabled(false);
					decreaseRatio.setEnabled(false);
				}
			}
			
		});
		
		useShadowNodes.setEnabled(true);		
		hideVLV.setEnabled(true);
						
		syncButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		commandsPanel.add(syncButton);
		commandsPanel.add(Box.createRigidArea(new Dimension(0,5)));
		
		hideVLV.setAlignmentX(Component.LEFT_ALIGNMENT);
		commandsPanel.add(hideVLV);
		commandsPanel.add(Box.createRigidArea(new Dimension(0,5)));			
		
		useShadowNodes.setAlignmentX(Component.LEFT_ALIGNMENT);
		commandsPanel.add(useShadowNodes);
		commandsPanel.add(Box.createRigidArea(new Dimension(0,5)));				
		
		activeShadowNodesRatioPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		commandsPanel.add(activeShadowNodesRatioPanel);		
							
		/**
		 * Main panel layout
		 */
		
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
		mainPanel.add(nodesInfoPanel);
		mainPanel.add(commandsPanel);
		
		/**
		 * Frame composition
		 */
		
		frame.setTitle("OvermindServer");
		frame.setContentPane(mainPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setAlwaysOnTop(true);
		frame.setVisible(true);
		
	}
	
	public static void updateMainFrame(MainFrameInfo updatedInfo) {
		
		numOfUnsyncNodes.setText("# of unsync nodes is " + updatedInfo.numOfUnsyncNodes);
		numOfSyncNodes.setText("# of sync nodes is " + updatedInfo.numOfSyncNodes);
		numOfShadowNodes.setText("# of shadow nodes is " + updatedInfo.numOfShadowNodes);
		
		/**
		 * Redraw the frame
		 */
		
		frame.revalidate();		
		frame.repaint();
		
	}

}
