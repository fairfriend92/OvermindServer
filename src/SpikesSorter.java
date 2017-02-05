import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;


public class SpikesSorter extends Thread{
	
	private static ArrayList<LocalNetworkFrame> localSyncFrames = new ArrayList<>();
	
	@Override
	public void run() {
		super.run();
		
		DatagramSocket spikesReceiver = null;

        try {
            spikesReceiver = new DatagramSocket(4194);
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
				
				System.out.println("Received packet from ip " + senderAddr.toString().substring(1));				
				
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
	
	public synchronized void sendSpikesToFrame (ArrayList<LocalNetworkFrame> localSyncFrames, byte[] spikesBuffer, InetAddress senderAddr) {
		
		boolean frameFound = false;
		LocalNetworkFrame tmpFrame = new LocalNetworkFrame();
		tmpFrame.ip = senderAddr.toString().substring(1);
		
		for (int index = 0; index < localSyncFrames.size() || !frameFound; index++) {
			
			if (localSyncFrames.get(index).equals(tmpFrame)) {
				
				frameFound = true;
				localSyncFrames.get(index).displaysSpikes(spikesBuffer);
				
			}
			
		}
		
		frameFound = false;
		
	}

}
