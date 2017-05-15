/**
 * Thread executed by MainFram to listen for spikes sent by the clients. The spikes are then passed to the
 * appropriate frame to be displayed in the raster graph
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class SpikesSorter extends Thread{
	
	private static ArrayList<TerminalFrame> localSyncFrames = new ArrayList<>();
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
        
        while (true) {
        	
			try {				
				
				byte[] spikesBuffer = new byte[128];
				
				DatagramPacket spikesPacket = new DatagramPacket(spikesBuffer, 128);				
			
				spikesReceiver.receive(spikesPacket);			
								
				spikesBuffer = spikesPacket.getData();
				
				InetAddress senderAddr = spikesPacket.getAddress();	
								
				sendSpikesToFrame(localSyncFrames, spikesBuffer, senderAddr);
				
			} catch (IOException e) {
	        	e.printStackTrace();
			}	        	
			
        }
        
        // TODO Close the socket when the server is shutdown
		//spikesReceiver.close();

	}
	
	/**
	 * Method called externally by VirtualLayerManager to update the list of sync frames
	 */
	
	public static synchronized void updateNodeFrames (ArrayList<TerminalFrame> syncFrames) {
		
		localSyncFrames = new ArrayList<>(syncFrames);
		
	}
	
	/**
	 * Method to identify the appropriate frame for the last spikes vector received. The vector is then sent
	 * to the frame so that the spikes can be displayed in the raster graph
	 */
	
	private static synchronized void sendSpikesToFrame (ArrayList<TerminalFrame> localSyncFrames, byte[] spikesBuffer, InetAddress senderAddr) {
		
		boolean frameFound = false;
		TerminalFrame tmpFrame = new TerminalFrame();
		tmpFrame.ip = senderAddr.toString().substring(1);
		
		for (int index = 0; (index < localSyncFrames.size()) && !frameFound; index++) {
			
			if (localSyncFrames.get(index).equals(tmpFrame)) {
				
				frameFound = true;				
								
				try {
					if (spikesBuffer != null) {
						localSyncFrames.get(index).receivedSpikesQueue.offer(spikesBuffer, 100, TimeUnit.MILLISECONDS);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (!localSyncFrames.get(index).spikesMonitorIsActive) {
					localSyncFrames.get(index).startSpikesMonitor();
				}
				
			}
			
		}
		
		frameFound = false;
		
	}

}
