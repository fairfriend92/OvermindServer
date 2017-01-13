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
		ExecutorService clientManagerExecutor = Executors.newCachedThreadPool();		
		
		// Create socket for this server
		ServerSocket serverSocket = null;		
		try {
			serverSocket = new ServerSocket(SERVER_PORT);
		} catch (IOException e) {
			System.out.println(e);
		}
		
		Socket clientSocket = null;
		clientManagerExecutor.execute(new clientManager(clientSocketsQueue));
		while (!shutdown) {
			// Accept connections from the clients			
			try {
				clientSocket = serverSocket.accept();
				clientSocketsQueue.put(clientSocket);
			} catch (IOException|InterruptedException e) {
				System.out.println(e);			
			}
		}			
					
	}
	
	public class clientManager implements Runnable {
		
		private BlockingQueue<Socket> clientSockets;
		private com.example.overmind.localNetwork localNetwork = new com.example.overmind.localNetwork();
		
		public clientManager (BlockingQueue<Socket> b) {
			this.clientSockets = b;
		}
		
		@Override
		public void run () {
		
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
		    }
		    catch (IOException e) {
		       System.out.println(e);
		    }
		    
		    try {
		    	localNetwork = (com.example.overmind.localNetwork) input.readObject();
		    	System.out.println("localNetwork ip is " + localNetwork.ip);
            } catch (IOException | ClassNotFoundException e) {
            	System.out.println(e);
            }
		    
		}
		
	}
}
