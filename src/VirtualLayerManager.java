import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtualLayerManager extends Thread{
	
	public final static int SERVER_PORT = 4194;
	
	static boolean shutdown = false;
			
	@Override
	public void run() {
		super.run();
		
		BlockingQueue<Socket> clientSocketsQueue = new ArrayBlockingQueue<>(16);
		BlockingQueue<com.example.overmind.LocalNetwork> localNetworksQueue = new ArrayBlockingQueue<>(16);
		Population population = new Population(); 
		ExecutorService clientManagerExecutor = Executors.newCachedThreadPool();		
		
		// Create socket for this server
		ServerSocket serverSocket = null;		
		try {
			serverSocket = new ServerSocket(SERVER_PORT);
		} catch (IOException e) {
			System.out.println(e);
		}
		
		Socket clientSocket = null;
		clientManagerExecutor.execute(new clientManager(clientSocketsQueue, localNetworksQueue));
		// TODO verify that multiple threads are executed if concurrent requests from terminals are received
		while (!shutdown) {
			// Accept connections from the clients			
			try {
				clientSocket = serverSocket.accept();
				clientSocketsQueue.put(clientSocket);
			} catch (IOException|InterruptedException e) {
				System.out.println(e);			
			}
			
			// Retrieve the last local network from the queue
			com.example.overmind.LocalNetwork localNetwork = null; 
			try {				
				localNetwork = localNetworksQueue.take();				
			} catch (InterruptedException e) {
				System.out.println(e);
			}
			
			// Build the populations from the local networks
			assert localNetwork != null;
			if (population.numOfNeurons + localNetwork.numOfNeurons < 1024) {
				population.addrs.add(localNetwork.ip);
				population.numOfNeurons += localNetwork.numOfNeurons;
				System.out.println(population.numOfNeurons);
			}
		}			
					
	}
	
	public class clientManager implements Runnable {
		
		private BlockingQueue<Socket> clientSockets;
		private BlockingQueue<com.example.overmind.LocalNetwork> localNetworks;
		private com.example.overmind.LocalNetwork localNetwork = new com.example.overmind.LocalNetwork();
		
		public clientManager (BlockingQueue<Socket> b, BlockingQueue<com.example.overmind.LocalNetwork> b1) {
			this.clientSockets = b;
			this.localNetworks = b1;
		}
		
		@Override
		public void run () {
			
			while (!shutdown) {
				// Read from BlockingQueue next socket
				Socket s = null;
				try {
					s = clientSockets.take();
				} catch (InterruptedException e) {
					System.out.println(e);
				}
				// Receive data stream from the client
				ObjectInputStream input = null;
				try {
					input = new ObjectInputStream(s.getInputStream());
				} catch (IOException e) {
					System.out.println(e);
				}
				// Read the localNetwork class from the data stream
				try {
					localNetwork = (com.example.overmind.LocalNetwork) input.readObject();					
				} catch (IOException | ClassNotFoundException e) {
					System.out.println(e);
				}
				// Close the stream
				try {
					input.close();
				} catch (IOException | NullPointerException e) {
					System.out.println(e);
				} 
				
				// Put the new localNetwork in a queue
				try {
					localNetworks.put(localNetwork);
				} catch (InterruptedException e) {
					System.out.println(e);			
				}
			}
			/* [End of while loop] */
		    		    
		}
		/* [End of run () method] */
		
	}
	/* [End of runnable class] */
}
