import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class MainFrame {
	
	// TODO Make the access of the static variables of the VirtualLayerManager class thread safe (allow to press 
	// the sync button only when the variables are not being accessed)

	public static void main(String[] args) {		
		JFrame frame = new JFrame("OvermindServer");
		JPanel panel = new JPanel();
		JButton syncButton = new JButton();
		VirtualLayerManager VLManager = new VirtualLayerManager();
		
		VLManager.start();
	
		syncButton.setText("Sync nodes");
		syncButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {							
				VirtualLayerManager.syncNodes();			
			}
		});	
		
		panel.setLayout(new FlowLayout());
		panel.add(syncButton);	
		
		frame.getContentPane().add(panel, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

}
