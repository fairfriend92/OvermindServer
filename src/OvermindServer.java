import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket; 
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;

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
		byte[] oldPresynapticSpikes;
		int byteIndex; 
		Random random = new Random();
        int[] waitARP = new int[Constants.NUMBER_OF_EXC_SYNAPSES + Constants.NUMBER_OF_INH_SYNAPSES];   
        long lastTime = 0, newTime = 0, sendTime = 0;
        
        int indexK = 0;
        
        while (!shutdown) {        	
        	oldPresynapticSpikes = presynapticSpikes.clone();
        	/**
    		 * For testing purposes we randomly generate the spikes we send to the client. 		
    		 */
        	
        	   for (int index = 0; index < Constants.NUMBER_OF_EXC_SYNAPSES + Constants.NUMBER_OF_INH_SYNAPSES; index++) {  
        		   byteIndex = (int) index / 8;
        		   if (index == indexK) {
        			   presynapticSpikes[byteIndex] |= (1 << index - byteIndex * 8);
        		   } else {
        			   presynapticSpikes[byteIndex] &= ~(1 << index - byteIndex * 8);
        		   }
        	   }
        		   /*
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
        	   */
        	   /* [End of the for loop] */               	   

        	   lastTime = newTime;  
        	   newTime = System.nanoTime();               
        	   
               while (newTime - lastTime < Constants.SAMPLING_RATE * 100000 - sendTime) {
            	   newTime = System.nanoTime();            	   
               }                      	   
                     
        	   // TODO mask output stream and send only if different from previous output data 
               if (!Arrays.equals(oldPresynapticSpikes, presynapticSpikes)) {
            	   try {        		   
            		   output.write(presynapticSpikes, 0, (Constants.NUMBER_OF_EXC_SYNAPSES + Constants.NUMBER_OF_INH_SYNAPSES) / 8);
               	   } catch (IOException e) {
            		   System.out.println(e);
            	   }             	   
               } 
               
               sendTime = System.nanoTime() - newTime;
               indexK = indexK < 8 ? indexK + 1 : 0;
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


	
	
