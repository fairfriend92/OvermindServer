import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
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

import com.example.overmind.LocalNetwork;

public class LocalNetworkFrame {
	
	public boolean shutdown = false;	
	
	public JFrame frame = new JFrame();
	
	public String ip = new String();
	private JLabel numOfNeurons = new JLabel();
	private JLabel numOfDendrites = new JLabel();
	private JLabel numOfSynapses = new JLabel();	

	private DefaultListModel<String> preConnListModel = new DefaultListModel<>();
	private DefaultListModel<String> postConnListModel = new DefaultListModel<>();
	
	public ExecutorService randomSpikesGeneratorExecutor = Executors.newSingleThreadExecutor();	
	
	public com.example.overmind.LocalNetwork localUpdatedNode = new com.example.overmind.LocalNetwork();
	private com.example.overmind.LocalNetwork oldNode = new com.example.overmind.LocalNetwork();
	
	// TODO This being initialized only once causes problems...
	public RandomSpikesGenerator thisNodeRSG = new RandomSpikesGenerator(this);	
	
	private boolean randomSpikeRadioButton;
	
	public BlockingQueue<byte[]> receivedSpikesQueue = new ArrayBlockingQueue<>(4);
	public ExecutorService spikesMonitorExecutor = Executors.newSingleThreadExecutor();
	public boolean spikesMonitorIsActive = false;

	public void display() {				
		
		JPanel mainPanel = new JPanel(); 
		JPanel infoPanel = new JPanel();
		JPanel stimulusPanel = new JPanel();
		JPanel infoAndStimulusPanel = new JPanel();
		JPanel preConnPanel = new JPanel();
		JPanel postConnPanel = new JPanel();		
		
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
				if (!randomSpikeRadioButton) {
					thisNodeRSG.shutdown = false;	
					randomSpikesGeneratorExecutor.execute(thisNodeRSG);	
					randomSpikeRadioButton = true;
				}
			}
		});	
		
		JRadioButton noneRadioButton = new JRadioButton("None");
		noneRadioButton.setSelected(true);
		noneRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {							
				thisNodeRSG.shutdown = true;
				randomSpikeRadioButton = false;
			}
		});	
		
		ButtonGroup stimulusButtonsGroup = new ButtonGroup();
	    stimulusButtonsGroup.add(randomSpikesRadioButton);
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
		 * Main panel layout
		 */
		
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
		infoAndStimulusPanel.setLayout(new BoxLayout(infoAndStimulusPanel, BoxLayout.Y_AXIS));
		infoAndStimulusPanel.add(infoPanel);
		infoAndStimulusPanel.add(stimulusPanel);
		mainPanel.add(infoAndStimulusPanel);
		mainPanel.add(preConnPanel);
		mainPanel.add(postConnPanel);
				
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
		 * Frame composition
		 */
		
		frame.setTitle(ip);
		frame.setContentPane(mainPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);		
		frame.pack();
		frame.setVisible(true);		
	}
	
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
		
		frame.revalidate();		
		frame.repaint();
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
	
	private class SpikesMonitor implements Runnable {
		
		private BlockingQueue<byte[]> spikesReceivedQueue = new ArrayBlockingQueue<>(4);
		private byte[] spikesReceived = new byte[128];
		
		SpikesMonitor(BlockingQueue<byte[]> b) {
			
			this.spikesReceivedQueue = b;
			
		}
		
		@Override
		public void run() {
			
			long lastTime = 0;
			
			while (!shutdown) {
				
				try {
					spikesReceived = spikesReceivedQueue.poll(1000, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e ) {
					e.printStackTrace();
				} 
				
				// TODO restrict bytes only to meaningful information 				
				if (spikesReceived != null) {
					/*
					char[] hexArray = "0123456789ABCDEF".toCharArray();
					char[] hexChars = new char[spikesReceived.length * 2];
					for ( int j = 0; j < spikesReceived.length; j++ ) {
						int v = spikesReceived[j] & 0xFF;
						hexChars[j * 2] = hexArray[v >>> 4];
						hexChars[j * 2 + 1] = hexArray[v & 0x0F];
					}					
					*/
					
					System.out.println("Device with ip " + ip + " has sent spikes with rate " + (System.nanoTime() - lastTime));
					lastTime = System.nanoTime();
				
				} else {
					shutdown = true;
				}
				
			}
			
			VirtualLayerManager.removeNode(localUpdatedNode);	
			
		}
		
	}
	
}
