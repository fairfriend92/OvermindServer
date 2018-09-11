package overmind_server;
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
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SpikesReceiver extends Thread{
	
	private final static int IPTOS_THROUGHPUT = 0x08;
	private ExecutorService spikesSorterExecutor = Executors.newCachedThreadPool();	
	boolean shutdown = false;
	static DatagramSocket datagramSocket = null;
	
	@Override
	public void run() {
		super.run();		

        try {
            datagramSocket = new DatagramSocket(Constants.UDP_PORT);
    	    datagramSocket.setTrafficClass(IPTOS_THROUGHPUT);      	    
        } catch (SocketException e) {
			e.printStackTrace();
        }
        
        assert datagramSocket != null;       
		
        while (!shutdown) {
        	
			try {				
				
				byte[] spikesBuffer = new byte[Constants.MAX_DATA_BYTES];
				
				DatagramPacket spikesPacket = new DatagramPacket(spikesBuffer, Constants.MAX_DATA_BYTES);				
			
				datagramSocket.receive(spikesPacket);					
								
				spikesBuffer = spikesPacket.getData();			
			
				String ip = spikesPacket.getAddress().toString().substring(1);
				int natPort = spikesPacket.getPort();
				
				spikesSorterExecutor.execute(new SpikesSorter(spikesBuffer, ip, natPort));
				
			} catch(SocketException e) {
				System.out.println("datagramSocket is closed");
				break;
			} catch (IOException e) {
	        	e.printStackTrace();
			}	    
						
        }
        
        /*
         * Shutdown worker threads.
         */
        
		spikesSorterExecutor.shutdown();
		try {
			boolean spikesSorterIsShutdown = spikesSorterExecutor.awaitTermination(1, TimeUnit.SECONDS);
			if (!spikesSorterIsShutdown) {
				System.out.println("ERROR: spikesSorter did not shutdown in time.");
			} 
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		

	}
	
	private class SpikesSorter implements Runnable {
		
		private byte[] spikesBuffer;
		private int natPort;
		String ip;
		
		SpikesSorter(byte[] b, String ip, int natPort) {
			
			this.spikesBuffer = b;
			this.ip = ip;
			this.natPort = natPort;
			
		}
		
		@Override
		public void run() {				
			byte[] firstHalf = ip.getBytes();
	    	byte secondHalf = new Integer(natPort).byteValue();
	    	byte[] data = new byte[firstHalf.length + 1];
	    	System.arraycopy(firstHalf, 0, data, 0, firstHalf.length);
	    	data[firstHalf.length] = secondHalf;
	    	
	    	// Implementation of the FNV-1 algorithm
	    	int hash = 0x811c9dc5;    	
	    	for (int i = 0; i < data.length; i++) {
	    		hash ^= (int)data[i];
	    		hash *= 16777619;
	    	}		  
	    	
	    	Integer virtualId = VirtualLayerManager.physical2VirtualID.get(hash);	    	
			Node tmpNode = virtualId == null ? null : VirtualLayerManager.nodesTable.get(virtualId);				
		
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
