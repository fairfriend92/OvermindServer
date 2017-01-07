import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket; 
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OvermindServer extends Thread {
	
	public final static int SERVER_PORT = 4194;

	
	static boolean shutdown = false;
	
	@Override 
	public void run() {
		super.run();		
		
		ExecutorService dataReceiverExecutor = Executors.newSingleThreadExecutor();
		
		// Create socket for this server
		ServerSocket serverSocket = null;		
		try {
			serverSocket = new ServerSocket(SERVER_PORT);
		} catch (IOException e) {
			System.out.println(e);
		}
		
		// Accept connections from the clients
		Socket clientSocket = null;
		try {
			clientSocket = serverSocket.accept();
			//clientSocket.setTrafficClass(0x08);
			//clientSocket.setTcpNoDelay(true);
		} catch (IOException e) {
			System.out.println(e);			
		}
		
		// Send data to the clients
		DataOutputStream output = null; 
		try {
			output = new DataOutputStream(clientSocket.getOutputStream());
		} catch (IOException e) {
			System.out.println(e);
		}
		
		// Receive data from the clients
		DataInputStream input = null;
	    try {
	       input = new DataInputStream(clientSocket.getInputStream());
	    }
	    catch (IOException e) {
	       System.out.println(e);
	    }
	    // Execute the thread that reads the incoming spikes from the client
	    dataReceiverExecutor.execute(new DataReceiver(input));
			    
		byte[] presynapticSpikes = new byte[(Constants.NUMBER_OF_EXC_SYNAPSES + Constants.NUMBER_OF_INH_SYNAPSES) / 8];
		int byteIndex, randomNum;
		// Every synapse has an array to count down to the expiration of the Absolute Refractory Period
		int[] waitARP = new int[Constants.NUMBER_OF_EXC_SYNAPSES + Constants.NUMBER_OF_INH_SYNAPSES];
        long lastTime = 0, newTime = 0, sendTime = 0;
        Random rand = new Random();

        /**
         * For testing purposes we randomly generate the spikes to send to the clients every Absolute Refractory Period
         */
        while (!shutdown) {       	
       	
        	for (int index = 0; index < Constants.NUMBER_OF_EXC_SYNAPSES + Constants.NUMBER_OF_INH_SYNAPSES; index++) {  
        		byteIndex = (int) index / 8;
        		// Generate randomly either 1 or 0
        		randomNum = rand.nextInt(2);
        		// If the ARP has passed and the synapse carries a spike...
        		if (randomNum == 1 && waitARP[index] == 0) {
        			// Set the bit corresponding to the synapse in the byte given by byteIndex
        			presynapticSpikes[byteIndex] |= (1 << index - byteIndex * 8);
        			// Start the ARP counter again
        			waitARP[index] = (int) (Constants.ABSOLUTE_REFRACTORY_PERIOD / Constants.SAMPLING_RATE);        			
        		} else if (waitARP[index] > 0) { // Else if the ARP has not elapsed yet...
        			waitARP[index]--;
        			presynapticSpikes[byteIndex] &= ~(1 << index - byteIndex * 8);
        		} else { // If the ARP has elapsed no need to decrement the counter...
        			presynapticSpikes[byteIndex] &= ~(1 << index - byteIndex * 8);
        		}
        	}
        	/* [End of the for loop] */               	   
        	
        	lastTime = newTime;  
        	newTime = System.nanoTime();               
        	
        	// New spikes are sent to the clients every ms
        	while (newTime - lastTime < Constants.SAMPLING_RATE * 1000000 - sendTime) {
        		newTime = System.nanoTime();            	   
        	}                      	   

        	try {        		   
        		output.write(presynapticSpikes, 0, (Constants.NUMBER_OF_EXC_SYNAPSES + Constants.NUMBER_OF_INH_SYNAPSES) / 8);
        	} catch (IOException e) {
        		System.out.println(e);
        	}         	   
                          
        	sendTime = System.nanoTime() - newTime;
        }
        /* [End of while for loop] */
        
        dataReceiverExecutor.shutdown();
        try {
        	output.close();
        	input.close();
        	clientSocket.close();
        	serverSocket.close();
        } catch (IOException e) {
        	System.out.println(e);
        } 
	}
	/* [End of run method] */ 
}


	
	
