import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class MainFrame {

	public static void main(String[] args) {		
		JFrame frame = new JFrame("Main frame");
		JPanel panel = new JPanel();
		JButton pauseResumeButton = new JButton();
		OvermindServer server = new OvermindServer();
		
		server.start();
		
		pauseResumeButton.setText("Stop");
		pauseResumeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {							
				server.shutdown = true;
			}
		});	
		
		panel.setLayout(new FlowLayout());
		panel.add(pauseResumeButton);	
		
		frame.getContentPane().add(panel, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

}
