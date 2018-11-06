package overmind_server;
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

import com.example.overmind.Terminal;

public class RandomSpikesGenerator implements Runnable {	
	
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
                                                
        /*
         * Procedure to set external stimulus and update Terminal info
         */
        
        // Create a Terminal representing this server
        com.example.overmind.Terminal server = null;
        
        // We can't use the reference contained in VirtualLayerManager since its information are general
        // and not specific to the target terminal
        for (Terminal postsynTerminal : targetTerminal.postsynapticTerminals)        	
        	if (postsynTerminal.id == VirtualLayerManager.thisServer.id)
        		server = postsynTerminal;        
        
        assert server != null;
        
        server.ip = Constants.USE_LOCAL_CONNECTION ? VirtualLayerManager.localIP : VirtualLayerManager.serverIP;    
        server.natPort = Constants.UDP_PORT;
        server.id = server.customHashCode();        server.postsynapticTerminals.add(targetTerminal);
        server.numOfNeurons = targetTerminal.numOfDendrites;
        server.numOfSynapses = (short)(32767 - targetTerminal.numOfNeurons); 
        
        // Update the info of the targetTerminal according to the chosen stimulus
        targetTerminal.numOfDendrites = 0;
        
        // Add the server to the list of presynaptic devices connected to the target device
        targetTerminal.presynapticTerminals.add(server);
        
        VirtualLayerManager.connectNodes(new Node[]{parentFrame.localUpdatedNode});    
               
        InetAddress targetDeviceAddr = null;
        		
        try {
			targetDeviceAddr = InetAddress.getByName(targetTerminal.ip);
		} catch (UnknownHostException e) {
        	e.printStackTrace();
		}
        
        assert targetDeviceAddr != null; 
        
        short rateMultiplier = parentFrame.rateMultiplier;
		
        while (!shutdown) {    
            long startTime = System.nanoTime();            
        	
        	/*
        	 * Generate spikes randomly, with the only condition that a period of time at least equal to the ARP 
        	 * should pass between two subsequent spikes
        	 */        	
        	
        	byte[] outputSpikes = new byte[dataBytes];       	      	
        	
        	for (int index = 0; index < server.numOfNeurons; index++) {  
        		
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
                DatagramPacket outputSpikesPacket = new DatagramPacket(outputSpikes, dataBytes, targetDeviceAddr, targetTerminal.natPort);	
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
        
        targetTerminal.removeTerminal(server);
    	targetTerminal.numOfDendrites +=  server.numOfNeurons;
    	VirtualLayerVisualizer.partTool.buildPopulationsMatrix(targetTerminal.populations, targetTerminal);
       
        if (VirtualLayerManager.nodesTable.containsKey(parentFrame.localUpdatedNode.id)) {
			VirtualLayerManager.connectNodes(new Node[]{parentFrame.localUpdatedNode});
			//VirtualLayerManager.syncNodes();
		}
                     
	}
	/* [End of run method] */ 
	
}



	
	