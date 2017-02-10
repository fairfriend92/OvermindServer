import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class SpikesSorter extends Thread{
	
	private static ArrayList<LocalNetworkFrame> localSyncFrames = new ArrayList<>();
	private final static int IPTOS_THROUGHPUT = 0x08;
	
	@Override
	public void run() {
		super.run();
		
		DatagramSocket spikesReceiver = null;

        try {
            spikesReceiver = new DatagramSocket(4194);
    	    spikesReceiver.setTrafficClass(0x10);  

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
        
		//spikesReceiver.close();

	}
	
	public static synchronized void updateNodeFrames (ArrayList<LocalNetworkFrame> syncFrames) {
		
		localSyncFrames = new ArrayList<>(syncFrames);
		
	}
	
	private static synchronized void sendSpikesToFrame (ArrayList<LocalNetworkFrame> localSyncFrames, byte[] spikesBuffer, InetAddress senderAddr) {
		
		boolean frameFound = false;
		LocalNetworkFrame tmpFrame = new LocalNetworkFrame();
		tmpFrame.ip = senderAddr.toString().substring(1);
		
		for (int index = 0; (index < localSyncFrames.size()) && !frameFound; index++) {
			
			if (localSyncFrames.get(index).equals(tmpFrame)) {
				
				frameFound = true;				
								
				try {
					localSyncFrames.get(index).receivedSpikesQueue.offer(spikesBuffer, 100, TimeUnit.MILLISECONDS);
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
