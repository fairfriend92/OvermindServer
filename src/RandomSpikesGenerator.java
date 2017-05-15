/**
 * Called by a particular instance of LocalNetworkFrame to generate random spikes to be sent to the 
 * device whose frame is managed by that instance
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
		
		targetTerminal = parentFrame.localUpdatedTerminal;
		
		Random rand = new Random();
        long lastTime = 0, newTime = 0, sendTime = 0;   
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
        
        // TODO Some of these fields are unnecessary
        server.postsynapticTerminals.add(targetTerminal);
        server.numOfNeurons = 1024;
        server.numOfSynapses = (short)(1024 - targetTerminal.numOfNeurons);
        server.numOfDendrites = 1024;
        server.natPort = VirtualLayerManager.SERVER_PORT_UDP;
        
        // Add the server to the list of presynaptic devices connected to the target device
        targetTerminal.presynapticTerminals.add(server);
        
        VirtualLayerManager.connectTerminals(new Node(null, targetTerminal));    
        VirtualLayerManager.syncTerminals();          
                
        /**
         * Open the socket for sending the spikes and build the InetAddress of the target device
         */
        		
        DatagramSocket outputSocket = null;

        try {
    	    outputSocket = new DatagramSocket();
    	    outputSocket.setTrafficClass(IPTOS_THROUGHPUT);   
        } catch (SocketException e) {
        	e.printStackTrace();
        }
        
        assert outputSocket != null;
        
        InetAddress targetDeviceAddr = null;
        		
        try {
			targetDeviceAddr = InetAddress.getByName(targetTerminalOld.ip);
		} catch (UnknownHostException e) {
        	e.printStackTrace();
		}
        
        assert targetDeviceAddr != null; 
        
        short rateMultiplier = parentFrame.rateMultiplier;
		
        while (!shutdown) {       	
        	
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
        	
        	newTime = System.nanoTime();              
       	        	
        	while (newTime - lastTime < rateMultiplier * 1000000 - sendTime) {
        		newTime = System.nanoTime();         
        	}          	                 	   
        	        	
            try {
                DatagramPacket outputSpikesPacket = new DatagramPacket(outputSpikes, dataBytes, targetDeviceAddr, targetTerminalOld.natPort);	
				outputSocket.send(outputSpikesPacket);			
			} catch (IOException e) {
				System.out.println(e);
			}		
                                   
            lastTime = System.nanoTime();            
   	                             
        }
        /* [End of while for loop] */         
        
        outputSocket.close();
        
        // In the meantime the stimulated device may have formed new postsynaptic connections which need to be carried on to the old Terminal
        targetTerminalOld.numOfSynapses = targetTerminal.numOfSynapses;
        targetTerminalOld.postsynapticTerminals = new ArrayList<>(targetTerminal.postsynapticTerminals);
       
        if (VirtualLayerManager.availableTerminals.contains(targetTerminalOld)) {
        	VirtualLayerManager.connectTerminals(new Node(null, targetTerminalOld));
        	VirtualLayerManager.syncTerminals();
        }
        
        
	}
	/* [End of run method] */ 
	
}



	
	