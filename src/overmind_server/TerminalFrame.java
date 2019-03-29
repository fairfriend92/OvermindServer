package overmind_server;
/**
 * Class which describes the frame used to monitor the activity of a terminal
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;

public class TerminalFrame {
	
	public boolean shutdown = false;	
		
	public JFrame frame = new JFrame();	
	
	public String ip = new String();
	
	private JLabel numOfNeurons = new JLabel();
	private JLabel numOfDendrites = new JLabel();
	private JLabel numOfSynapses = new JLabel();
	private JLabel refreshRate = new JLabel("Clock is: 3 ms");
	private JLabel averageSpikesInterval = new JLabel(" 0 ms");
	
	private JButton activateNodeButton = new JButton();

	private DefaultListModel<String> preConnListModel = new DefaultListModel<>();
	private DefaultListModel<String> postConnListModel = new DefaultListModel<>();
	
	public ExecutorService stimulusExecutor = Executors.newSingleThreadExecutor();	
	
	public volatile Node localUpdatedNode;
	
	public RandomSpikesGenerator thisTerminalRSG = new RandomSpikesGenerator(this);	
	public RefreshSignalSender thisTerminalRSS = new RefreshSignalSender(this);

	public BlockingQueue<byte[]> receivedSpikesQueue = new ArrayBlockingQueue<>(16);
	public ExecutorService spikesMonitorExecutor = Executors.newSingleThreadExecutor();
	public ExecutorService tcpKeepAliveExecutor = Executors.newSingleThreadExecutor();
	public boolean spikesMonitorIsActive = false;
	
	public MyPanel rastergraphPanel;

	public JPanel mainPanel = new JPanel(); 
	
	public short rateMultiplier = 3;
	
	public volatile boolean waitForLatestPacket = false;
	
	public final Object tcpKeepAliveLock = new Object ();
	
	private Image redLed, greenLed;	
	private JLabel ledStatus = new JLabel();
	private Timer ledStatusTimer;
	private volatile boolean redIsOn = true; 
	private volatile boolean ledStatusChanged = false;
	
	TerminalFrame () {
		tcpKeepAliveExecutor.execute(new tcpKeepAlivePackageSender());		
	}
			
	public JRadioButton randomSpikesRadioButton = new JRadioButton("Random spikes");
	public JRadioButton refreshSignalRadioButton = new JRadioButton("Refresh signal");
	public JRadioButton noneRadioButton = new JRadioButton("None");
	
	/**
	 * Custom panel to display the raster graph
	 */
	
	class MyPanel extends JPanel {		
		
		public short xCoordinate = 0;
		public volatile long time = 0;
		public byte[][] latestSpikes = new byte[40][];				
		
	    public MyPanel(short numOfNeurons) {
	        setBorder(BorderFactory.createLineBorder(Color.black));
			setBackground(Color.white);
			short dataBytes = (numOfNeurons % 8) == 0 ?
	                (short) (numOfNeurons / 8) : (short)(numOfNeurons / 8 + 1);
			for (char i = 0; i < 40; i++)
				latestSpikes[i] = new byte[dataBytes]; 
	    }

	    @Override
	    public Dimension getPreferredSize() {
	        return new Dimension(this.getWidth(), localUpdatedNode.terminal.numOfNeurons + 30);
	    }	  	    

	    @Override
	    public void paintComponent(Graphics g) {
	        super.paintComponent(g); 
	        
	        // Draw string in the left corner of the raster graph
	        g.drawString("Average spikes interval: " + time + " ms", 10, localUpdatedNode.terminal.numOfNeurons + 20);
	    		    	
	        // Compute the number of bytes of the vector holding the spikes
	    	short dataBytes = (localUpdatedNode.terminal.numOfNeurons % 8) == 0 ? 
	    			(short) (localUpdatedNode.terminal.numOfNeurons / 8) : (short)(localUpdatedNode.terminal.numOfNeurons / 8 + 1);	    		    
	    	
	    	// Iterate over the latest 40 spikes vectors
			for (int k = 0; k < 40; k++) {
				
				// Get the current spikes vector
				byte[] spikesData = latestSpikes[k];			

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
		
		/*
		 * Various elements of the frame: panels, buttons, checkboxes...
		 */
		
		JPanel totalPanel = new JPanel();
		JPanel infoPanel = new JPanel();
		JPanel stimulusPanel = new JPanel();
		JPanel infoAndStimulusPanel = new JPanel();
		JPanel preConnPanel = new JPanel();
		JPanel postConnPanel = new JPanel();	
		JPanel commandsPanel = new JPanel();
		JPanel refreshRatePanel = new JPanel();
		JPanel ledPanel = new JPanel();
		
		JButton increaseRate = new JButton("+");
		JButton decreaseRate = new JButton("-");
		JButton removeTerminalButton = new JButton();
		
		JScrollPane preConnScrollPanel = new JScrollPane();
		JScrollPane postConnScrollPanel = new JScrollPane();
		
		JList<String> presynapticConnections = new JList<>();
		JList<String> postsynapticConnections = new JList<>();		
						
		JCheckBox refreshAfterSpike = new JCheckBox("Dynamic refresh rate", false);
		refreshAfterSpike.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {							
				JCheckBox cb = (JCheckBox) e.getSource();
		        if (cb.isSelected()) {
		        	increaseRate.setEnabled(false);
		        	decreaseRate.setEnabled(false);
		        	waitForLatestPacket = true;
		        } else {
		        	increaseRate.setEnabled(true);
		        	decreaseRate.setEnabled(true);
		        	waitForLatestPacket = false;
		        }
			}
		});	
		
		refreshAfterSpike.setEnabled(true);
		
		JCheckBox showRasterGraph = new JCheckBox("Show the raster graph", true);
		showRasterGraph.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {							
				JCheckBox cb = (JCheckBox) e.getSource();
		        if (cb.isSelected()) {		        	
		        	rastergraphPanel.setVisible(true);
		        	frame.pack();
		        } else {
		        	rastergraphPanel.setVisible(false);		    
		        	frame.pack();
		        }
			}
		});	
		
		showRasterGraph.setEnabled(true);		
		
		JCheckBox enableLateralConnections = new JCheckBox("Enable lateral conn.", localUpdatedNode.hasLateralConnections());
		enableLateralConnections.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean operationSuccesful = localUpdatedNode.changeLateralConnectionsOption();
				if (!operationSuccesful)
					enableLateralConnections.setSelected(!enableLateralConnections.isSelected());
			}
		});
		
		enableLateralConnections.setEnabled(true);
		
		/*
		 * Timer for the led status
		 */
		
		ActionListener ledTimerActionListener = new ActionListener () {
			
			@Override
			public void actionPerformed(ActionEvent e) {				
				
				if (redIsOn && ledStatusChanged) 
					ledStatus.setIcon(new ImageIcon(greenLed.getScaledInstance(12, 12, Image.SCALE_SMOOTH)));				
				else if (!redIsOn && ledStatusChanged) 
					ledStatus.setIcon(new ImageIcon(redLed.getScaledInstance(12, 12, Image.SCALE_SMOOTH)));
								
				ledStatusChanged = false;
				ledPanel.repaint();
				ledPanel.revalidate();								
			}
			
		};
		
		ledStatusTimer = new Timer(100, ledTimerActionListener);
		ledStatusTimer.setInitialDelay(0);
		ledStatusTimer.start();
		
			
		/*
		 * Radio buttons used to select the external stimulus
		 */
		
		randomSpikesRadioButton.setEnabled(!localUpdatedNode.isExternallyStimulated);
		randomSpikesRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (thisTerminalRSG.shutdown) {
					thisTerminalRSG.shutdown = false;	
					thisTerminalRSS.shutdown = true;
					stimulusExecutor.execute(thisTerminalRSG);	
				}
			}
		});	
		
		refreshSignalRadioButton.setEnabled(!localUpdatedNode.isExternallyStimulated);
		refreshSignalRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (thisTerminalRSS.shutdown) {
					thisTerminalRSG.shutdown = true;	
					thisTerminalRSS.shutdown = false;
					stimulusExecutor.execute(thisTerminalRSS);	
				}
			}
		});
		
		noneRadioButton.setSelected(true);
		noneRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				thisTerminalRSG.shutdown = true;
				thisTerminalRSS.shutdown = true;
			}
		});	
		
		ButtonGroup stimulusButtonsGroup = new ButtonGroup();
	    stimulusButtonsGroup.add(randomSpikesRadioButton);
	    stimulusButtonsGroup.add(refreshSignalRadioButton);
	    stimulusButtonsGroup.add(noneRadioButton);			    
			
		/*
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
		
		/*
		 * Total panel layout
		 */
		
		totalPanel.setLayout(new BoxLayout(totalPanel, BoxLayout.Y_AXIS));
		totalPanel.add(mainPanel);
		if (!localUpdatedNode.isShadowNode) 
			totalPanel.add(rastergraphPanel);
			
		/*
		 * Main panel layout
		 */
		
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
		infoAndStimulusPanel.setLayout(new BoxLayout(infoAndStimulusPanel, BoxLayout.Y_AXIS));
		infoAndStimulusPanel.add(infoPanel);
		if (!localUpdatedNode.isShadowNode) 
			infoAndStimulusPanel.add(stimulusPanel);
		mainPanel.add(infoAndStimulusPanel);
		if (!localUpdatedNode.isShadowNode) {
			mainPanel.add(preConnPanel);
			mainPanel.add(postConnPanel);	
		}
		mainPanel.add(commandsPanel);
					
		/*
		 * Info panel layout
		 */

		infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
		infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Local network info"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
		
		// Create the led panel containing the led icon and the average spikes interval label
		ledPanel.setLayout(new BoxLayout(ledPanel, BoxLayout.X_AXIS));
		ledPanel.add(ledStatus);
		ledPanel.add(averageSpikesInterval);
		ledPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
				
		infoPanel.add(ledPanel);
		infoPanel.add(numOfDendrites);
		infoPanel.add(numOfNeurons);		
		infoPanel.add(numOfSynapses);		

		if (!localUpdatedNode.isShadowNode) {
		
			/*
			 * Stimulus panel layout
			 */
			
			stimulusPanel.setLayout(new BoxLayout(stimulusPanel, BoxLayout.Y_AXIS));
			stimulusPanel.setBorder(BorderFactory.createCompoundBorder(
	                BorderFactory.createTitledBorder("Stimulus selection"),
	                BorderFactory.createEmptyBorder(5,5,5,5)));
			stimulusPanel.add(noneRadioButton);
			stimulusPanel.add(randomSpikesRadioButton);
			stimulusPanel.add(refreshSignalRadioButton);
			
			/*
			 * Presynaptic connections panel layout
			 */
			
			preConnPanel.setLayout(new BoxLayout(preConnPanel, BoxLayout.Y_AXIS));
			preConnPanel.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder("Presynaptic connections"),
						BorderFactory.createEmptyBorder(5,5,5,5)));
			preConnPanel.add(preConnScrollPanel);
			
			/*
			 * Postsynaptic connections panel layout
			 */
			
			postConnPanel.setLayout(new BoxLayout(postConnPanel, BoxLayout.Y_AXIS));
			postConnPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Postsynaptic connections"),
					BorderFactory.createEmptyBorder(5,5,5,5)));
			postConnPanel.add(postConnScrollPanel);	
			
			/*
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
					if (rateMultiplier > 1) {
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
		
		}
		
		/*
		 * Command panel layout
		 */
		
		commandsPanel.setLayout(new BoxLayout(commandsPanel, BoxLayout.Y_AXIS));
		commandsPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Commands"),
					BorderFactory.createEmptyBorder(5,5,5,5)));
		removeTerminalButton.setText("Remove this node");
		removeTerminalButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {				
				if (!localUpdatedNode.isShadowNode) 
					VirtualLayerManager.removeNode(localUpdatedNode, false);
				else
					VirtualLayerManager.removeShadowNode(localUpdatedNode);
			}
		});	
				
		removeTerminalButton.setAlignmentX(Component.LEFT_ALIGNMENT);		
		commandsPanel.add(removeTerminalButton);
		commandsPanel.add(Box.createRigidArea(new Dimension(0,5)));
		if (localUpdatedNode.isShadowNode)
			activateNodeButton.setEnabled(true);
		else
			activateNodeButton.setEnabled(false);
		activateNodeButton.setText("Activate this node");
		activateNodeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				VirtualLayerManager.activateNode(localUpdatedNode);
			}
		});					
		activateNodeButton.setAlignmentX(Component.LEFT_ALIGNMENT);		
		commandsPanel.add(activateNodeButton);
		commandsPanel.add(Box.createRigidArea(new Dimension(0,5)));
		commandsPanel.add(enableLateralConnections);
		
		if (!localUpdatedNode.isShadowNode) {

			commandsPanel.add(refreshRatePanel);
			commandsPanel.add(Box.createRigidArea(new Dimension(0,5)));
			
			refreshAfterSpike.setAlignmentX(Component.LEFT_ALIGNMENT);
			commandsPanel.add(refreshAfterSpike);		
			commandsPanel.add(Box.createRigidArea(new Dimension(0,5)));
		
			showRasterGraph.setAlignmentX(Component.LEFT_ALIGNMENT);
			commandsPanel.add(showRasterGraph);
		}
			
		/*
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
	
	public synchronized void update(Node updatedNode) {		
		localUpdatedNode = updatedNode;
		
		// If the rastergraph has not been created yet, do so passing the number of neurons
		if (rastergraphPanel == null)
			rastergraphPanel = new MyPanel(updatedNode.terminal.numOfNeurons);
			
		// If the dendrites are saturated and the random spikes input is currently not selected
		// disable the input options. 
		if (updatedNode.terminal.numOfDendrites == 0 && !randomSpikesRadioButton.isSelected()) {
			refreshSignalRadioButton.setEnabled(false);
			randomSpikesRadioButton.setEnabled(false);
		} else {
			refreshSignalRadioButton.setEnabled(true);
			randomSpikesRadioButton.setEnabled(true);
		}
		
		// Launches the thread that updates the raster graph using the spikes gathered by
		// SpikesReceiver
		if (!spikesMonitorIsActive) {
			try {
				redLed = ImageIO.read(getClass().getResource("/icons/red_led.png"));
				greenLed = ImageIO.read(getClass().getResource("/icons/green_led.png"));
				ledStatus.setIcon(new ImageIcon(redLed.getScaledInstance(12, 12, Image.SCALE_SMOOTH)));
			} catch (IOException e) {
				e.printStackTrace();
			}
			spikesMonitorExecutor.execute(new SpikesMonitor(receivedSpikesQueue));
			spikesMonitorIsActive = true;
		}						
						
		/*
		 * Update info about local network
		 */
		
		ip = localUpdatedNode.terminal.ip;
		numOfNeurons.setText("# neurons: " + localUpdatedNode.terminal.numOfNeurons);
		numOfDendrites.setText("# available dendrites: " + localUpdatedNode.terminal.numOfDendrites);
		numOfSynapses.setText("# available synapses: " + localUpdatedNode.terminal.numOfSynapses);		
	
		/*
		 * Update info about connected devices
		 */
		
		preConnListModel.clear();
		postConnListModel.clear();		
		
		String serverIP; 
		
		serverIP = Constants.USE_LOCAL_CONNECTION ? VirtualLayerManager.localIP : VirtualLayerManager.serverIP;
	
		preConnListModel.clear();
		postConnListModel.clear();
						
		// Go over the presynaptic connections
		for (int i = 0; i < localUpdatedNode.terminal.presynapticTerminals.size(); i++) {
		
			// Get the i-th connection
			com.example.overmind.Terminal presynapticTerminal = localUpdatedNode.terminal.presynapticTerminals.get(i);
			
			// Established if said connection is a lateral one
			boolean isThisTerminal = presynapticTerminal.ip.equals(localUpdatedNode.terminal.ip) &
					presynapticTerminal.natPort == localUpdatedNode.terminal.natPort;
						
			// Depending on the connection kind put a different string in the list model
			if (presynapticTerminal.ip.equals(serverIP) & !isThisTerminal) {
				
				Integer key = VirtualLayerManager.physical2VirtualID.get(presynapticTerminal.id);
				
				if (presynapticTerminal.natPort == Constants.UDP_PORT)
					preConnListModel.addElement("Presynaptic device # " + i + " is this server");
				else if (key != null && VirtualLayerManager.nodesTable.containsKey(key)) 
					preConnListModel.addElement("Presynaptic device # " + i + " is under local connection");
				else 						
					preConnListModel.addElement("Presynaptic device # " + i + " is an app");
			} else if (isThisTerminal) {
				preConnListModel.addElement("Lateral connections");
			} else {
				preConnListModel.addElement("Presynaptic device # " + i + " has ip: " + presynapticTerminal.ip);
			}			

		}
		
		// If no connections are present put a special string
		if (localUpdatedNode.terminal.presynapticTerminals.size() == 0) {
			preConnListModel.addElement("No presynaptic connection has been established yet");
		} else if (preConnListModel.contains("No presynaptic connection has been established yet")) {
			preConnListModel.remove(0);
		}

		// Do as before for the postsynaptic connections now
		for (int i = 0; i < localUpdatedNode.terminal.postsynapticTerminals.size(); i++) {
			
			com.example.overmind.Terminal postsynapticTerminal = localUpdatedNode.terminal.postsynapticTerminals.get(i);
			Integer key = VirtualLayerManager.physical2VirtualID.get(postsynapticTerminal.id);
			
			boolean isThisTerminal = postsynapticTerminal.ip.equals(localUpdatedNode.terminal.ip) &
					postsynapticTerminal.natPort == localUpdatedNode.terminal.natPort;
			
			if (postsynapticTerminal.ip.equals(serverIP) & !isThisTerminal) {
				if (postsynapticTerminal.natPort == Constants.UDP_PORT)
					postConnListModel.addElement("Postsynaptic device # " + i + " is this server");
				else if (key != null && VirtualLayerManager.nodesTable.containsKey(key))
					postConnListModel.addElement("Postsynaptic device # " + i + " is under local connection");
				else 
					postConnListModel.addElement("Postsynaptic device # " + i + " is an app");
			} else if (isThisTerminal) {
				postConnListModel.addElement("Lateral connections");
			} else {
				postConnListModel.addElement("Postsynaptic device # " + i + " has ip: " + postsynapticTerminal.ip);

			}
		}
		
		if (localUpdatedNode.terminal.postsynapticTerminals.size() == 0) {
			postConnListModel.addElement("No postsynaptic connection has been established yet");
		} else if (postConnListModel.contains("No postynaptic connection has been established yet")) {
			preConnListModel.remove(0);
		}
		
		/*
		 * Redraw the frame
		 */
		
		mainPanel.revalidate();		
		mainPanel.repaint();
	}
	
	@Override
    public synchronized boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) { return false; }
        TerminalFrame compare = (TerminalFrame) obj;
        return compare.ip.equals(this.ip);
    }		
			
	/**
	 * Method which reads the incoming spikes and pass them to the raster graph panel
	 */
	
	private class SpikesMonitor implements Runnable {
		
		private BlockingQueue<byte[]> spikesReceivedQueue = new ArrayBlockingQueue<>(4);
		private byte[][] localLatestSpikes = new byte[40][]; 
		private short arrayLength = 0, dataBytes = 0;
		
		SpikesMonitor(BlockingQueue<byte[]> b) {			
			this.spikesReceivedQueue = b;
			
			// dataBytes is the number of bytes which make up the vector containing the spikes
			dataBytes = (localUpdatedNode.terminal.numOfNeurons % 8) == 0 ? 
		    		(short) (localUpdatedNode.terminal.numOfNeurons / 8) : (short)(localUpdatedNode.terminal.numOfNeurons / 8 + 1);				
			for (char i = 0; i < 40; i++)
				localLatestSpikes[i] = new byte[dataBytes];			
		}
		
		@Override
		public void run() {	    				
		    // Used to store temporarily the x coordinate of the upper right vertex of the rectangle 
		    // defining the region of the raster graph that needs to be redrawn
		    short tmpXCoordinate = 0;
		    
		    // Used to compute the time interval between spikes
		    long lastTime = 0;
			
			while (!shutdown) {		
				
				byte[] spikesReceived = null;
				
				// The vector containing the spikes is put in this queue by the SpikesSorter class
				try {
					if (!redIsOn && !ledStatusChanged) {
						redIsOn = true;
						ledStatusChanged = true;
					}
					spikesReceived = spikesReceivedQueue.poll(10, TimeUnit.SECONDS);
				} catch (InterruptedException e ) {
					e.printStackTrace();
				} 
				
				// Proceed only if the vector contains meaningful info
				if (spikesReceived != null) {				
					
					if (redIsOn && !ledStatusChanged) {
						redIsOn = false;
						ledStatusChanged = true;
					}
																							
					// The raster graph is updated every 40 iterations of the simulation to prevent screen flickering 
					if (arrayLength < 40 && !localUpdatedNode.isShadowNode) {
						
						System.arraycopy(spikesReceived, 0, localLatestSpikes[arrayLength], 0, dataBytes);
						arrayLength++;
						
					} else if (!localUpdatedNode.isShadowNode) { 
						
						arrayLength = 0;
						
						// Update the info regarding the time interval between spikes
						rastergraphPanel.time = (long)(System.nanoTime() - lastTime) / (40 * 1000000);
						
						// Display the latest average spikes interval in the ledPanel portion of the interface
						averageSpikesInterval.setText(" " + rastergraphPanel.time + " ms");
						averageSpikesInterval.repaint();
						
						// Update the list holding the last 40 spikes vectors
						for (char i = 0; i < 40; i++)
							System.arraycopy(localLatestSpikes[i], 0, rastergraphPanel.latestSpikes[i], 0, localLatestSpikes[i].length);
						
						// If there is still room in the raster graph write in the same buffer
						if (rastergraphPanel.xCoordinate < rastergraphPanel.getWidth()) {
							
							// Update the x coordinate of the portion of the raster graph that needs to be redrawn							
							rastergraphPanel.xCoordinate  = tmpXCoordinate;
							
							// Update the height 
							int stringHeight = rastergraphPanel.getHeight() - localUpdatedNode.terminal.numOfNeurons;
							
							// Redraw the region of the raster graph where the new 40 spikes vectors should appear
							rastergraphPanel.paintImmediately(0, localUpdatedNode.terminal.numOfNeurons, rastergraphPanel.getWidth(), stringHeight);
							
							// Redraw the region of the raster graph where the spikes time interval is displayed
							rastergraphPanel.paintImmediately(rastergraphPanel.xCoordinate, 0, 40, localUpdatedNode.terminal.numOfNeurons);
							
							// Increase the x coordinate
							tmpXCoordinate += 40;
							
					    } else {
					    	
					    	// If there isn't room in the raster graph the hole graph needs to be refreshed
					    	rastergraphPanel.xCoordinate = 0;
					    	tmpXCoordinate = 40;
					    	rastergraphPanel.repaint();					    	
					    }					
												
						lastTime = System.nanoTime();
					}					
				
				} else if (!shutdown) { // Check if some other process is not removing the node already

					NodesShutdownPoller.nodesToBeRemoved.add(localUpdatedNode);
					shutdown = true; 	
										
				} 
				
			}	
											
		}
		/* [End of run() ] */
		
	}
	/* [End of SpikesMonitor] */
	
	/**
	 * Runnable which periodically sends a TCP keep alive packet to the terminal
	 * associated with this frame
	 */
	
	private class tcpKeepAlivePackageSender implements Runnable {		
	
		@Override
		public void run() {
			
			while (!shutdown) {				
				if (localUpdatedNode != null) {
					try {
						localUpdatedNode.writeObjectIntoStream(new Boolean(true));
					} catch (IOException e) {
						System.out.println("Error with tcp keep alive stream"); // TODO: Handle this error
					}
				}
				synchronized (tcpKeepAliveLock) {
					
					try {
						tcpKeepAliveLock.wait(30000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
				}
				
				
			}
			
		}
		
	}

	
}