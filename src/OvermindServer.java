import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket; 
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class OvermindServer extends Thread {
	
	public final static int SERVER_PORT = 4194;

	
	static boolean shutdown = false;
	
	@Override 
	public void run() {
		super.run();		
			
		ServerSocket serverSocket = null;		
		try {
			serverSocket = new ServerSocket(SERVER_PORT);
		} catch (IOException e) {
			System.out.println(e);
		}
		
		// TODO timeout if connection can't be established
		Socket clientSocket = null;
		try {
			clientSocket = serverSocket.accept();
		} catch (IOException e) {
			System.out.println(e);			
		}
		
		DataOutputStream output = null; 
		try {
			output = new DataOutputStream(clientSocket.getOutputStream());
		} catch (IOException e) {
			System.out.println(e);
		}
						
		byte[] presynapticSpikes = new byte[(Constants.NUMBER_OF_EXC_SYNAPSES + Constants.NUMBER_OF_INH_SYNAPSES) / 8];
		int byteIndex; 
		Random random = new Random();
        int[] waitARP = new int[Constants.NUMBER_OF_EXC_SYNAPSES + Constants.NUMBER_OF_INH_SYNAPSES];   
        long lastTime = 0, newTime = 0;
        
        while (!shutdown) {
        	/**
    		 * For testing purposes we randomly generate the spikes we send to the client. 		
    		 */
        	   for (int index = 0; index < Constants.NUMBER_OF_EXC_SYNAPSES + Constants.NUMBER_OF_INH_SYNAPSES; index++) {  
        		   byteIndex = (int) index / 8;
                   // A new spike is randomly generated only if the absolute refractory period has elapsed
                   if (waitARP[index] == 0) {
                	   if (random.nextBoolean()) {
                		   // Set the bit corresponding to the index-th synapse 
                		   presynapticSpikes[byteIndex] |= (1 << index - byteIndex * 8);
                		   // 
                		   waitARP[index] = (int) (Constants.ABSOLUTE_REFRACTORY_PERIOD / Constants.SAMPLING_RATE);
                	   } else {
                		   // Clear the bit 
                		   presynapticSpikes[byteIndex] &= ~(1 << index - byteIndex * 8);                		   
                	   }
                   } else {
                	   presynapticSpikes[byteIndex] &= ~(1 << index - byteIndex * 8);
                	   waitARP[index]--;
                   }
        	   }
        	   /* [End of the for loop] */       
        	   
        	   try {        		   
        		   output.write(presynapticSpikes);
        	   } catch (IOException e) {
        		   System.out.println(e);
        	   } 
        	   
        	   lastTime = newTime;
               newTime = System.nanoTime();
               
               try {
   				TimeUnit.NANOSECONDS.sleep(600000 - (newTime - lastTime));
               	} catch (InterruptedException e) {
               		System.out.println(e);
               	}               	
        }
        /* [End of while for loop] */
        
        try {
        	output.close();
        	clientSocket.close();
        	serverSocket.close();
        } catch (IOException e) {
        	System.out.println(e);
        } 
	}
	/* [End of run method] */ 
}


	
	
