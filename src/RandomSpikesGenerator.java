import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

public class RandomSpikesGenerator implements Runnable {
	
	// TODO Create a method to stop the generation of random spikes
	
	public final static int UDP_CLIENT_PORT = 4194;	
	
	public com.example.overmind.LocalNetwork targetDevice;
	
	RandomSpikesGenerator(com.example.overmind.LocalNetwork l) {
		this.targetDevice = l;
	}
	
	public boolean shutdown = false;	
	
	@Override 
	public void run() {	
		
		Random rand = new Random();
        long lastTime = 0, newTime = 0, sendTime = 0;   
        int[] waitARP = new int[targetDevice.numOfDendrites];
        short dataBytes = targetDevice.numOfDendrites % 8 == 0 ? 
        		(short) (targetDevice.numOfDendrites / 8) : (short) (targetDevice.numOfDendrites / 8 + 1);
        
        com.example.overmind.LocalNetwork targetDeviceOld = new com.example.overmind.LocalNetwork();
        
        /**
         * Procedure to set external stimulus and update local network info
         */
        		
        // Store locally the info of the target device before sending the stimulus
        targetDeviceOld = targetDevice;   
        
        // Update the info of the targetDevice according to the chosen stimulus
        targetDevice.numOfDendrites = 0;
        
        // Create a local network representing this server
        com.example.overmind.LocalNetwork server = new com.example.overmind.LocalNetwork();
        server.postsynapticNodes = new ArrayList<>();
        server.presynapticNodes = new ArrayList<>();
        
        try {
			server.ip = InetAddress.getLocalHost().toString().substring(1);
		} catch (UnknownHostException e1) {
			System.out.println(e1);
		}
        
        server.postsynapticNodes.add(targetDevice);
        server.numOfNeurons = 1024;
        server.numOfSynapses = (short)(1024 - targetDevice.numOfNeurons);
        server.numOfDendrites = 1024;
        server.natPort = VirtualLayerManager.SERVER_PORT;
        
        // Add the server to the list of presynaptic devices connected to the target device
        targetDevice.presynapticNodes.add(server);
        
        VirtualLayerManager.connectDevices(targetDevice);    
        VirtualLayerManager.syncNodes();

        /**
         * Open the socket for sending the spikes and build the InetAddress of the target device
         */
        		
        DatagramSocket outputSocket = null;

        try {
    	    outputSocket = new DatagramSocket();
        } catch (SocketException e) {
		 	System.out.println(e);
        }
        
        assert outputSocket != null;
        
        InetAddress targetDeviceAddr = null;
        		
        try {
			targetDeviceAddr = InetAddress.getByName(targetDeviceOld.ip);
		} catch (UnknownHostException e) {
			System.out.println(e);
		}
        
        assert targetDeviceAddr != null;
		
        while (!shutdown) {       	
        	
        	/**
        	 * Generate spikes randomly, with the only condition that a period of time at least equal to the ARP 
        	 * should pass between two subsequent spikes
        	 */
        	
        	byte[] outputSpikes = new byte[dataBytes];       	
      	
        	for (int index = 0; index < targetDeviceOld.numOfDendrites; index++) {  
        		
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
        	
        	/**
        	 * Send the generated spikes every 2 milliseconds
        	 */
        	
        	lastTime = newTime;  
        	newTime = System.nanoTime();               
        	
        	// New spikes are sent to the clients every millisecond
        	while (newTime - lastTime < Constants.SAMPLING_RATE * 1000000 - sendTime) {
        		newTime = System.nanoTime();            	   
        	}                    	   
        	
            try {
                DatagramPacket outputSpikesPacket = new DatagramPacket(outputSpikes, dataBytes, targetDeviceAddr, targetDeviceOld.natPort);	
				outputSocket.send(outputSpikesPacket);
			} catch (IOException e) {
				System.out.println(e);
			}
            
            //System.out.println("Spikes sent to device with IP " + targetDeviceAddr.toString().substring(1) + " and nat port " + targetDevice.natPort);
   	                             
        	sendTime = System.nanoTime() - newTime;
        }
        /* [End of while for loop] */       
        
        VirtualLayerManager.connectDevices(targetDeviceOld);
        VirtualLayerManager.syncNodes();
        
	}
	/* [End of run method] */ 
	
}



	
	
