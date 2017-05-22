/**
 * Thread executed by MainFram to listen for spikes sent by the clients. The spikes are then passed to the
 * appropriate frame to be displayed in the raster graph
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class SpikesSorter extends Thread{
	
	private final static int IPTOS_THROUGHPUT = 0x08;
	
	@Override
	public void run() {
		super.run();
		
		DatagramSocket spikesReceiver = null;

        try {
            spikesReceiver = new DatagramSocket(4194);
    	    spikesReceiver.setTrafficClass(IPTOS_THROUGHPUT);  

        } catch (SocketException e) {
        	e.printStackTrace();
        }
        
        assert spikesReceiver != null;
       
		int ipHashCode = 0;
		Node tmpNode = new Node();
        
        while (true) {
        	
			try {				
				
				byte[] spikesBuffer = new byte[128];
				
				DatagramPacket spikesPacket = new DatagramPacket(spikesBuffer, 128);				
			
				spikesReceiver.receive(spikesPacket);			
								
				spikesBuffer = spikesPacket.getData();			
			
				ipHashCode = spikesPacket.getAddress().hashCode();			
				
				tmpNode = VirtualLayerManager.nodesTable.get(ipHashCode);					
	
				try {
					if (spikesBuffer != null) {
						tmpNode.terminalFrame.receivedSpikesQueue.offer(spikesBuffer, 100, TimeUnit.MILLISECONDS);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
																
				
			} catch (IOException e) {
	        	e.printStackTrace();
			}	       
			
			
			
        }
        
        // TODO Close the socket when the server is shutdown
		//spikesReceiver.close();

	}
	


}
