/**
 * Called by a particular instance of TerminalFrame to generate random spikes to be sent to the 
 * terminal whose frame is managed by that instance
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

public class RandomSpikesGenerator implements Runnable {	

	public final static int UDP_CLIENT_PORT = 4194;	
	private final static int IPTOS_THROUGHPUT = 0x08;
	
	private com.example.overmind.Terminal targetTerminal;
	private TerminalFrame parentFrame;
	
	RandomSpikesGenerator(TerminalFrame l) {
		this.parentFrame = l;
	}
	
	public boolean shutdown = true;	
	
	@Override 
	public void run() {			
		
		targetTerminal = parentFrame.localUpdatedNode.terminal;
		
		Random rand = new Random();
        long staticRefresh = parentFrame.rateMultiplier * 1000000, 
        		dynamicRefresh = 0, rasterGraphRefresh;   
        int[] waitARP = new int[targetTerminal.numOfDendrites];
        short dataBytes = targetTerminal.numOfDendrites % 8 == 0 ? 
        		(short) (targetTerminal.numOfDendrites / 8) : (short) (targetTerminal.numOfDendrites / 8 + 1);
        
        com.example.overmind.Terminal targetTerminalOld = new com.example.overmind.Terminal();      
                                        
        /**
         * Procedure to set external stimulus and update Terminal info
         */
        		
        // Store locally the info of the target terminal before sending the stimulus       
        targetTerminalOld.update(targetTerminal);   
        
        // Update the info of the targetTerminal according to the chosen stimulus
        targetTerminal.numOfDendrites = 0;
        
        // Create a Terminal representing this server
        com.example.overmind.Terminal server = new com.example.overmind.Terminal();
        server.postsynapticTerminals = new ArrayList<>();
        server.presynapticTerminals = new ArrayList<>();
        server.ip = VirtualLayerManager.serverIP;
        //server.ip = "192.168.1.213";
        
        server.postsynapticTerminals.add(targetTerminal);
        server.numOfNeurons = targetTerminalOld.numOfDendrites;
        server.numOfSynapses = (short)(1024 - targetTerminalOld.numOfNeurons); // TODO: change into 0
        server.numOfDendrites = 1024; // TODO: change into targetTerminalOld.numOfNeurons
        server.natPort = Constants.OUT_UDP_PORT;
        
        // Add the server to the list of presynaptic devices connected to the target device
        targetTerminal.presynapticTerminals.add(server);
        
        VirtualLayerManager.connectNodes(new Node[]{parentFrame.localUpdatedNode});    
        //VirtualLayerManager.syncNodes();                         
               
        InetAddress targetDeviceAddr = null;
        		
        try {
			targetDeviceAddr = InetAddress.getByName(targetTerminalOld.ip);
		} catch (UnknownHostException e) {
        	e.printStackTrace();
		}
        
        assert targetDeviceAddr != null; 
        
        short rateMultiplier = parentFrame.rateMultiplier;
		
        while (!shutdown) {    
            long startTime = System.nanoTime();            
        	
        	/**
        	 * Generate spikes randomly, with the only condition that a period of time at least equal to the ARP 
        	 * should pass between two subsequent spikes
        	 */        	
        	
        	byte[] outputSpikes = new byte[dataBytes];       	      	
        	
        	for (int index = 0; index < targetTerminalOld.numOfDendrites; index++) {  
        		
        		int byteIndex = (int) index / 8;
        		
        		// Generate randomly either 1 or 0
        		int randomNum = rand.nextInt(2);
        		
        		// If the ARP has passed and the synapse carries a spike...
        		if (randomNum == 1 && waitARP[index] == 0) {
        			
        			// Set the bit corresponding to the synapse in the byte given by byteIndex
        			outputSpikes[byteIndex] |= (1 << index - byteIndex * 8);
        			
        			// Start the ARP counter again
        			waitARP[index] = (int) (Constants.ABSOLUTE_REFRACTORY_PERIOD / Constants.SAMPLING_RATE);  
        			
        		} else if (waitARP[index] > 0) { // Else if the ARP has not elapsed yet...
        			
        			waitARP[index]--;
        			outputSpikes[byteIndex] &= ~(1 << index - byteIndex * 8);
        			
        		} else { 
        			outputSpikes[byteIndex] &= ~(1 << index - byteIndex * 8);
        		}
        		
        	}       	
        	
        	// Retrieve the average refresh rate of the raster graph
        	rasterGraphRefresh = (parentFrame.rastergraphPanel.time) * 1000000;  
        	
        	// Use the raster graph refresh rate or the one chosen by the user depending on which is slower
    		dynamicRefresh = (staticRefresh < rasterGraphRefresh) && parentFrame.waitForLatestPacket ? rasterGraphRefresh : staticRefresh;     	        	        	    
    		    	
            try {
                DatagramPacket outputSpikesPacket = new DatagramPacket(outputSpikes, dataBytes, targetDeviceAddr, targetTerminalOld.natPort);	
				SpikesReceiver.datagramSocket.send(outputSpikesPacket);			
			} catch (IOException e) {
				System.out.println(e);
			}	                                  
            
            // Send a signal to the terge terminal every 1 / dynamicRefresh
        	while ((System.nanoTime() - startTime) < dynamicRefresh) {
            	//rasterGraphRefresh = parentFrame.rastergraphPanel.time * 1000000;   	
        		dynamicRefresh = (staticRefresh < rasterGraphRefresh) && parentFrame.waitForLatestPacket ? rasterGraphRefresh : staticRefresh;
        	}         	                             
        }
        /* [End of while for loop] */         
        
        //datagramSocket.close();
        
        // In the meantime the stimulated device may have formed new postsynaptic connections which need to be carried on to the old Terminal
        targetTerminalOld.numOfSynapses = targetTerminal.numOfSynapses;
        targetTerminalOld.postsynapticTerminals = new ArrayList<>(targetTerminal.postsynapticTerminals);
        
        // The update method must be used because we can't reference targetTerminalOld since it is
        // a local variable that gets destroyed the moment this runnable ends
        parentFrame.localUpdatedNode.terminal.update(targetTerminalOld);
       
        // The old node is substituted to the one connected with the server
        if (VirtualLayerManager.nodesTable.containsKey(parentFrame.localUpdatedNode.physicalID)) {
			VirtualLayerManager.connectNodes(new Node[]{parentFrame.localUpdatedNode});
			//VirtualLayerManager.syncNodes();
		}
                     
	}
	/* [End of run method] */ 
	
}



	
	