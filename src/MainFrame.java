/**
 * Main class which displays the main panel and executes the two main threads: 1) the VirtualLayerManager and the 
 * 2) SpikesSorter. The first manages incoming connections and routes the peers, the second constantly listen for inbounds spikes
 * that need to be displayed in the raster graph portion of the frame associated with the sending device
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class MainFrame {
	
	// TODO Make the access of the static variables of the VirtualLayerManager class thread safe (allow to press 
	// the sync button only when the variables are not being accessed)
	
	// TODO Manager application shutdown properly
	
	private static JLabel numOfUnsyncNodes = new JLabel("# of unsync nodes is 0");
	private static JLabel numOfSyncNodes = new JLabel("# of sync nodes is 0");
	private static JFrame frame = new JFrame();
	
	static long rasterGraphRefresh = 0;

	public static void main(String[] args) {		
		
		displayMainFrame();
					
		VirtualLayerManager VLManager = new VirtualLayerManager();		
		VLManager.start();				
		
		SpikesReceiver spikesSorter = new SpikesReceiver();
		spikesSorter.start();
		
		NodesShutdownPoller nodesShutdownPoller = new NodesShutdownPoller();
		nodesShutdownPoller.start();
		
		//VirtualLayerVisualizer VLVisualizer = new VirtualLayerVisualizer();
		//VLVisualizer.start();
		
	}
	
	private static void displayMainFrame() {
		
		JPanel mainPanel = new JPanel();
		JPanel nodesInfoPanel = new JPanel();	
		JPanel commandsPanel = new JPanel();
		JPanel rasterGraphPanel = new JPanel();
		
		JLabel refreshRate = new JLabel("Raster graph fhz undefined");
		
		JButton increaseRate = new JButton("+");
		JButton decreaseRate = new JButton("-");		
		JButton syncButton = new JButton();
		
			
		
		/**
		 * Nodes info panel layout
		 */
		
		nodesInfoPanel.setLayout(new BoxLayout(nodesInfoPanel, BoxLayout.Y_AXIS));
		nodesInfoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Virtual Layer Info"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
		nodesInfoPanel.add(numOfUnsyncNodes);
		nodesInfoPanel.add(numOfSyncNodes);
		
		
		rasterGraphPanel.setLayout(new BoxLayout(rasterGraphPanel, BoxLayout.X_AXIS));
		rasterGraphPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		increaseRate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				rasterGraphRefresh++;
				refreshRate.setText("Raster graph fhz is: " + rasterGraphRefresh + " ms");
				refreshRate.revalidate();
				refreshRate.repaint();
			}
		});		
		decreaseRate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (rasterGraphRefresh > 1) {
					rasterGraphRefresh--;
					refreshRate.setText("Raster graph fhz is " + rasterGraphRefresh + " ms");
					refreshRate.revalidate();
					refreshRate.repaint();
				}
			}
		});		
		
		refreshRate.setBorder(BorderFactory.createLineBorder(Color.black));
		refreshRate.setOpaque(true);
		refreshRate.setBackground(Color.white);
		
		rasterGraphPanel.add(refreshRate);
		rasterGraphPanel.add(Box.createRigidArea(new Dimension(5,0)));
		rasterGraphPanel.add(increaseRate);
		rasterGraphPanel.add(Box.createRigidArea(new Dimension(5,0)));
		rasterGraphPanel.add(decreaseRate);
		
		
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
		syncButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		commandsPanel.add(syncButton);
		commandsPanel.add(Box.createRigidArea(new Dimension(0,5)));
		commandsPanel.add(rasterGraphPanel);
					
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
		frame.setAlwaysOnTop(true);
		frame.pack();
		frame.setVisible(true);
		
	}
	
	public static void updateMainFrame(MainFrameInfo updatedInfo) {
		
		numOfUnsyncNodes.setText("# of unsync nodes is " + updatedInfo.numOfUnsyncNodes);
		numOfSyncNodes.setText("# of sync nodes is " + updatedInfo.numOfSyncNodes);
		
		/**
		 * Redraw the frame
		 */
		
		frame.revalidate();		
		frame.repaint();
		
	}

}
