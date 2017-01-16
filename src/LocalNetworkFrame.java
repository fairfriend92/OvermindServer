import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class LocalNetworkFrame {
	
	private static com.example.overmind.LocalNetwork localNetwork;	

	public LocalNetworkFrame (com.example.overmind.LocalNetwork l) {
		LocalNetworkFrame.localNetwork = l;
	}
	
	public void display() {
		JFrame frame = new JFrame(localNetwork.ip);
		JPanel panel = new JPanel();		
	
		panel.setLayout(new FlowLayout());
		frame.getContentPane().add(panel, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);		
		frame.pack();
		frame.setVisible(true);		
	}
	
}
