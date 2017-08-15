/**
 * Thread executed by MainFrame to listen for spikes sent by the clients. The spikes,
 * together with an hashcode identifying the sending terminal, are passed to a separate thread.
 * This thread uses the hashcode to retrieve from a concurrent hashmap the node associated 
 * with the terminal that has sent the spikes, so that its raster graph can be updated with
 * the new spikes. 
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SpikesReceiver extends Thread{
	
	private final static int IPTOS_THROUGHPUT = 0x08;
	
	private ExecutorService spikesSorterExecutor = Executors.newCachedThreadPool();	
	
	@Override
	public void run() {
		super.run();
		
		DatagramSocket spikesReceiver = null;

        try {
            spikesReceiver = new DatagramSocket(Constants.UDP_PORT);
    	    spikesReceiver.setTrafficClass(IPTOS_THROUGHPUT);  

        } catch (SocketException e) {
        	e.printStackTrace();
        }
        
        assert spikesReceiver != null;
       
		int ipHashCode = 0;     
		
        while (true) {
        	
			try {				
				
				byte[] spikesBuffer = new byte[128];
				
				DatagramPacket spikesPacket = new DatagramPacket(spikesBuffer, 128);				
			
				spikesReceiver.receive(spikesPacket);			
								
				spikesBuffer = spikesPacket.getData();			
			
				ipHashCode = spikesPacket.getAddress().hashCode();			
												
				spikesSorterExecutor.execute(new SpikesSorter(spikesBuffer, ipHashCode));
				
			} catch (IOException e) {
	        	e.printStackTrace();
			}	       
			
			
			
        }
        
        // TODO Close the socket when the server is shutdown
		//spikesReceiver.close();

	}
	
	private class SpikesSorter implements Runnable {
		
		private byte[] spikesBuffer = new byte[128];
		private int ipHashCode = 0;
		
		SpikesSorter(byte[] b, int i) {
			
			this.spikesBuffer = b;
			this.ipHashCode = i;
			
		}
		
		@Override
		public void run() {		
			
			Node tmpNode = VirtualLayerManager.nodesTable.get(ipHashCode);		
		
			if (tmpNode != null) {
				try {
					tmpNode.terminalFrame.receivedSpikesQueue.offer(spikesBuffer, 1, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
			} 
			
			//System.out.println(Thread.activeCount());
			
		}

	}

}
