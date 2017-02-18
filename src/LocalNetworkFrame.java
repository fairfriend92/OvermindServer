/**
 * Class which describes the frame used to monitor the activity of a node
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

public class LocalNetworkFrame {
	
	public boolean shutdown = false;	
		
	public JFrame frame = new JFrame();
	
	public String ip = new String();
	
	private JLabel numOfNeurons = new JLabel();
	private JLabel numOfDendrites = new JLabel();
	private JLabel numOfSynapses = new JLabel();
	private JLabel refreshRate = new JLabel("Clock is: 3 ms");

	private DefaultListModel<String> preConnListModel = new DefaultListModel<>();
	private DefaultListModel<String> postConnListModel = new DefaultListModel<>();
	
	public ExecutorService stimulusExecutor = Executors.newSingleThreadExecutor();	
	
	public com.example.overmind.LocalNetwork localUpdatedNode = new com.example.overmind.LocalNetwork();
	
	public RandomSpikesGenerator thisNodeRSG = new RandomSpikesGenerator(this);	
	public RefreshSignalSender thisNodeRSS = new RefreshSignalSender(this);

	public BlockingQueue<byte[]> receivedSpikesQueue = new ArrayBlockingQueue<>(4);
	public ExecutorService spikesMonitorExecutor = Executors.newSingleThreadExecutor();
	public boolean spikesMonitorIsActive = false;
	
	public MyPanel rastergraphPanel = new MyPanel();	

	public JPanel mainPanel = new JPanel(); 
	
	public short rateMultiplier = 3;

	
	/**
	 * Custom panel to display the raster graph
	 */
	
	class MyPanel extends JPanel {		
		
		public short xCoordinate = 0;
		public long time = 0;
		public ArrayList<byte[]> latestSpikes = new ArrayList<>();
		
	    public MyPanel() {
	        setBorder(BorderFactory.createLineBorder(Color.black));
			setBackground(Color.white);
	    }

	    public Dimension getPreferredSize() {
	        return new Dimension(this.getWidth(), localUpdatedNode.numOfNeurons + 30);
	    }	  	    

	    public void paintComponent(Graphics g) {
	        super.paintComponent(g); 
	        
	        // Draw string in the left corner of the raster graph
	        g.drawString("Average spikes interval: " + time + " ms", 10, localUpdatedNode.numOfNeurons + 20);
	    		    	
	        // Compute the number of bytes of the vector holding the spikes
	    	short dataBytes = (localUpdatedNode.numOfNeurons % 8) == 0 ? 
	    			(short) (localUpdatedNode.numOfNeurons / 8) : (short)(localUpdatedNode.numOfNeurons / 8 + 1);	    		    
	    	
	    	// Iterate over the latest # latestSpikes.size() spikes vectors
			for (int k = 0; k < latestSpikes.size(); k++) {
				
				// Get the current spikes vector
				byte[] spikesData = latestSpikes.get(k);

				// Iterate over the bytes of the vector
				for (int i = 0; i < dataBytes; i++) {
					
					// Iterate over the single bit, each one representing a spike
					for (int j = 0; j < 8; j++) {
												
						if ((spikesData[i] & (1 << j)) != 0) {
							
							// If the bit is set a spike has been emitted and therefore a pixel needs to be turned on
							g.drawLine(xCoordinate + k, i * 8 + j, xCoordinate + k, i * 8 + j);

						}
						/* [End of inner if] */
						
					}
					/* [End of for over bit] */
					
				}
				/* [End of for over byte] */

			} 
			/* [End of outer for] */			
				    
	    }  
	    
	}
	
	/**
	 * Main method used to display the frame for the first time
	 */

	public void display() {				
		
		JPanel totalPanel = new JPanel();
		JPanel infoPanel = new JPanel();
		JPanel stimulusPanel = new JPanel();
		JPanel infoAndStimulusPanel = new JPanel();
		JPanel preConnPanel = new JPanel();
		JPanel postConnPanel = new JPanel();	
		JPanel commandsPanel = new JPanel();
		JPanel refreshRatePanel = new JPanel();
		
		JButton removeNodeButton = new JButton();
		JButton increaseRate = new JButton("+");
		JButton decreaseRate = new JButton("-");
		
		JScrollPane preConnScrollPanel = new JScrollPane();
		JScrollPane postConnScrollPanel = new JScrollPane();
		
		JList<String> presynapticConnections = new JList<>();
		JList<String> postsynapticConnections = new JList<>();		
	
		/**
		 * Radio buttons used to select the external stimulus
		 */
		
		JRadioButton randomSpikesRadioButton = new JRadioButton("Random spikes");
		randomSpikesRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (thisNodeRSG.shutdown) {
					thisNodeRSG.shutdown = false;	
					thisNodeRSS.shutdown = true;
					stimulusExecutor.execute(thisNodeRSG);	
				}
			}
		});	
		
		JRadioButton refreshSignalRadioButton = new JRadioButton("Refresh signal");
		refreshSignalRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (thisNodeRSS.shutdown) {
					thisNodeRSG.shutdown = true;	
					thisNodeRSS.shutdown = false;
					stimulusExecutor.execute(thisNodeRSS);	
				}
			}
		});
		
		JRadioButton noneRadioButton = new JRadioButton("None");
		noneRadioButton.setSelected(true);
		noneRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {							
				thisNodeRSG.shutdown = true;
				thisNodeRSS.shutdown = true;
			}
		});	
		
		ButtonGroup stimulusButtonsGroup = new ButtonGroup();
	    stimulusButtonsGroup.add(randomSpikesRadioButton);
	    stimulusButtonsGroup.add(refreshSignalRadioButton);
	    stimulusButtonsGroup.add(noneRadioButton);			    
			
		/**
		 * Lists options		
		 */
		
		preConnScrollPanel.setViewportView(presynapticConnections);
				
		presynapticConnections.setModel(preConnListModel);
		presynapticConnections.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		presynapticConnections.setLayoutOrientation(JList.VERTICAL);
		
		postConnScrollPanel.setViewportView(postsynapticConnections);
		
		postsynapticConnections.setModel(postConnListModel);	
		postsynapticConnections.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		postsynapticConnections.setLayoutOrientation(JList.VERTICAL);
		
		/**
		 * Total panel layout
		 */
		
		totalPanel.setLayout(new BoxLayout(totalPanel, BoxLayout.Y_AXIS));
		totalPanel.add(mainPanel);
		totalPanel.add(rastergraphPanel);
			
		/**
		 * Main panel layout
		 */
		
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
		infoAndStimulusPanel.setLayout(new BoxLayout(infoAndStimulusPanel, BoxLayout.Y_AXIS));
		infoAndStimulusPanel.add(infoPanel);
		infoAndStimulusPanel.add(stimulusPanel);
		mainPanel.add(infoAndStimulusPanel);
		mainPanel.add(preConnPanel);
		mainPanel.add(postConnPanel);	
		mainPanel.add(commandsPanel);
					
		/**
		 * Info panel layout
		 */

		infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
		infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Local network info"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
		infoPanel.add(numOfDendrites);
		infoPanel.add(numOfNeurons);		
		infoPanel.add(numOfSynapses);				

		/**
		 * Stimulus panel layout
		 */
		
		stimulusPanel.setLayout(new BoxLayout(stimulusPanel, BoxLayout.Y_AXIS));
		stimulusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Stimulus selection"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
		stimulusPanel.add(noneRadioButton);
		stimulusPanel.add(randomSpikesRadioButton);
		stimulusPanel.add(refreshSignalRadioButton);
		
		/**
		 * Presynaptic connections panel layout
		 */
		
		preConnPanel.setLayout(new BoxLayout(preConnPanel, BoxLayout.Y_AXIS));
		preConnPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Presynaptic connections"),
					BorderFactory.createEmptyBorder(5,5,5,5)));
		preConnPanel.add(preConnScrollPanel);
		
		/**
		 * Postsynaptic connections panel layout
		 */
		
		postConnPanel.setLayout(new BoxLayout(postConnPanel, BoxLayout.Y_AXIS));
		postConnPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Postsynaptic connections"),
				BorderFactory.createEmptyBorder(5,5,5,5)));
		postConnPanel.add(postConnScrollPanel);	
		
		/**
		 * Refresh rate panel layout
		 */
		
		refreshRatePanel.setLayout(new BoxLayout(refreshRatePanel, BoxLayout.X_AXIS));
		refreshRatePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		increaseRate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				rateMultiplier++;
				refreshRate.setText("Clock is: " + rateMultiplier + " ms");
				refreshRate.revalidate();
				refreshRate.repaint();
			}
		});		
		decreaseRate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (rateMultiplier > 3) {
					rateMultiplier--;
					refreshRate.setText("Clock is: " + rateMultiplier + " ms");
					refreshRate.revalidate();
					refreshRate.repaint();
				}
			}
		});		
		
		refreshRate.setBorder(BorderFactory.createLineBorder(Color.black));
		refreshRate.setOpaque(true);
		refreshRate.setBackground(Color.white);
		refreshRatePanel.add(refreshRate);
		refreshRatePanel.add(Box.createRigidArea(new Dimension(5,0)));
		refreshRatePanel.add(increaseRate);
		refreshRatePanel.add(Box.createRigidArea(new Dimension(5,0)));
		refreshRatePanel.add(decreaseRate);
		
		/**
		 * Command panel layout
		 */
		
		commandsPanel.setLayout(new BoxLayout(commandsPanel, BoxLayout.Y_AXIS));
		commandsPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Commands"),
					BorderFactory.createEmptyBorder(5,5,5,5)));
		removeNodeButton.setText("Remove this node");
		removeNodeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//shutdown = true;		
				VirtualLayerManager.removeNode(localUpdatedNode);
			}
		});		
		removeNodeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		commandsPanel.add(removeNodeButton);
		commandsPanel.add(Box.createRigidArea(new Dimension(0,5)));
		commandsPanel.add(refreshRatePanel);
			
		/**
		 * Frame composition
		 */
		
		frame.setTitle(ip);
		frame.setContentPane(totalPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);		
		frame.pack();
		frame.setVisible(true);		
	}
	
	/**
	 * Method used to update the frame
	 */
	
	public synchronized void update(com.example.overmind.LocalNetwork updatedNode) {
		
		localUpdatedNode = updatedNode;			
						
		/**
		 * Update info about local network
		 */
		
		ip = updatedNode.ip;
		numOfNeurons.setText("# neurons: " + updatedNode.numOfNeurons);
		numOfDendrites.setText("# available dendrites: " + updatedNode.numOfDendrites);
		numOfSynapses.setText("# available synapses: " + updatedNode.numOfSynapses);		
		
		/**
		 * Update info about connected devices
		 */
		
		preConnListModel.clear();
		postConnListModel.clear();		
		
		String serverIP; 
		
		serverIP = VirtualLayerManager.serverIP;
	
		preConnListModel.clear();
		postConnListModel.clear();
				
		for (int i = 0; i < updatedNode.presynapticNodes.size(); i++) {
			com.example.overmind.LocalNetwork presynapticNode = updatedNode.presynapticNodes.get(i);
			if (presynapticNode.ip == serverIP) {
				preConnListModel.addElement("Presynaptic device # " + i + " is this server");
			} else {
				preConnListModel.addElement("Presynaptic device # " + i + " has ip: " + presynapticNode.ip);
			}
		}
		
		if (updatedNode.presynapticNodes.size() == 0) {
			preConnListModel.addElement("No presynaptic connection has been established yet");
		} else if (preConnListModel.contains("No presynaptic connection has been established yet")) {
			preConnListModel.remove(0);
		}

		for (int i = 0; i < updatedNode.postsynapticNodes.size(); i++) {
			com.example.overmind.LocalNetwork postsynapticNode = updatedNode.postsynapticNodes.get(i);
			if (postsynapticNode.ip == serverIP) {
				postConnListModel.addElement("Postsynaptic device # " + i + " is this server");
			} else {
				postConnListModel.addElement("Postsynaptic device # " + i + " has ip: " + postsynapticNode.ip);

			}
		}
		
		if (updatedNode.postsynapticNodes.size() == 0) {
			postConnListModel.addElement("No postsynaptic connection has been established yet");
		} else if (postConnListModel.contains("No postynaptic connection has been established yet")) {
			preConnListModel.remove(0);
		}
		
		/**
		 * Redraw the frame
		 */
		
		mainPanel.revalidate();		
		mainPanel.repaint();
	}
	
	@Override
    public synchronized boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) { return false; }
        LocalNetworkFrame compare = (LocalNetworkFrame) obj;
        return compare.ip.equals(ip);
    }		
	
	public void startSpikesMonitor () {
		
		spikesMonitorExecutor.execute(new SpikesMonitor(receivedSpikesQueue));
		spikesMonitorIsActive = true;
		
	}
	
	/**
	 * Method which reads the incoming spikes and pass them to the raster graph panel
	 */
	
	private class SpikesMonitor implements Runnable {
		
		private BlockingQueue<byte[]> spikesReceivedQueue = new ArrayBlockingQueue<>(4);
		private ArrayList<byte[]> localLatestSpikes = new ArrayList<>(40);
		
		SpikesMonitor(BlockingQueue<byte[]> b) {
			
			this.spikesReceivedQueue = b;
			
		}
		
		@Override
		public void run() {
			
		    /**
		     * dataBytes is the number of bytes which make up the vector containing the spikes
		     */
			
		    short dataBytes = (localUpdatedNode.numOfNeurons % 8) == 0 ? 
		    		(short) (localUpdatedNode.numOfNeurons / 8) : (short)(localUpdatedNode.numOfNeurons / 8 + 1);			
			
		    // Used to store temporarily the x coordinate of the upper right vertex of the rectangle 
		    // defining the region of the raster graph that needs to be redrawn
		    short tmpXCoordinate = 0;
		    
		    // Used to compute the time interval between spikes
		    long lastTime = 0;
			
			while (!shutdown) {
				
				byte[] spikesReceived = new byte[dataBytes];

				// The vector containing the spikes is put in this queue by the SpikesSorter class
				try {
					spikesReceived = spikesReceivedQueue.poll(100, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e ) {
					e.printStackTrace();
				} 
				
				// Proceed only if the vector contains meaningful info
				if (spikesReceived != null) {	
															
					// The raster graph is updated every 40 iterations of the simulation to prevent screen flickering 
					if (localLatestSpikes.size() < 40) {
						
						localLatestSpikes.add(spikesReceived);
						
					} else {
						
						// Update the info regarding the time interval between spikes
						rastergraphPanel.time = (long)(System.nanoTime() - lastTime) / (40 * 1000000);
						
						// Update the list holding the last 40 spikes vectors
						rastergraphPanel.latestSpikes = new ArrayList<>(localLatestSpikes);													
						
						// If there is still room in the raster graph write in the same buffer
						if (rastergraphPanel.xCoordinate < rastergraphPanel.getWidth()) {
							
							// Update the x coordinate of the portion of the raster graph that needs to be redrawn							
							rastergraphPanel.xCoordinate  = tmpXCoordinate;
							
							// Update the height 
							int stringHeight = rastergraphPanel.getHeight() - localUpdatedNode.numOfNeurons;
							
							// Redraw the region of the raster graph where the new 40 spikes vectors should appear
							rastergraphPanel.paintImmediately(0, localUpdatedNode.numOfNeurons, rastergraphPanel.getWidth(), stringHeight);
							
							// Redraw the region of the raster graph where the spikes time interval is displayed
							rastergraphPanel.paintImmediately(rastergraphPanel.xCoordinate, 0, rastergraphPanel.latestSpikes.size(), localUpdatedNode.numOfNeurons);
							
							// Increase the x coordinate
							tmpXCoordinate += rastergraphPanel.latestSpikes.size();
							
					    } else {
					    	
					    	// If there isn't room in the raster graph the hole graph needs to be refreshed
					    	rastergraphPanel.xCoordinate = 0;
					    	tmpXCoordinate = (short)rastergraphPanel.latestSpikes.size();
					    	rastergraphPanel.repaint();
					    	
					    }					
												
						lastTime = System.nanoTime();
						localLatestSpikes.clear();
					}					
				
				} else if ((System.nanoTime() - lastTime) / (40 * 1000000000) > 5) { 
					
					// TODO Manage disconnections automatically in a better way
					
					//shutdown = true;
					
				} 
				
			}
			
			VirtualLayerManager.removeNode(localUpdatedNode);	
			
		}
		
	}
	
}
