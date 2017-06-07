import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class VirtualLayerVisualizer extends Thread{
	
	static JFrame VLVFrame = new JFrame();
		
	@Override
	public void run () {
		super.run();
		
		String frameTitle = new String("Virtual Layer Visualizer");
		JPanel totalPanel = new JPanel();
		JPanel VLPanel = new JPanel();
		JPanel commandsPanel = new JPanel();
		JButton cutLink = new JButton();
		JButton createLink = new JButton();
		GridBagConstraints VLConstraint = new GridBagConstraints(), commandsConstraint = new GridBagConstraints();	
		
		try {
			Image img = ImageIO.read(new FileInputStream("resources/icons/icons8-Cut.png"));
			cutLink.setIcon(new ImageIcon(img));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			Image img = ImageIO.read(new FileInputStream("resources/icons/icons8-Pencil.png"));
			createLink.setIcon(new ImageIcon(img));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		VLConstraint.gridx = 0;
		VLConstraint.gridy = 0;
		VLConstraint.gridwidth = 2;	
		VLConstraint.fill = GridBagConstraints.BOTH;
		VLConstraint.weightx = 0.5;
		VLConstraint.weighty = 0.5;
		
		commandsConstraint.gridx = GridBagConstraints.RELATIVE;		
		commandsConstraint.gridy = 0;
		commandsConstraint.fill = GridBagConstraints.HORIZONTAL;
		
		VLPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		VLPanel.setBackground(Color.white);
					
		totalPanel.setLayout(new GridBagLayout());
		totalPanel.add(VLPanel, VLConstraint);
		totalPanel.add(commandsPanel, commandsConstraint);
		
		commandsPanel.setLayout(new GridLayout(0,2));
		commandsPanel.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Commands"),
					BorderFactory.createEmptyBorder(5,5,5,5)));
		commandsPanel.add(cutLink);
		commandsPanel.add(createLink);
		
		VLVFrame.setTitle(frameTitle);
		VLVFrame.setContentPane(totalPanel);
		VLVFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);		
		VLVFrame.pack();
		VLVFrame.setVisible(true);	
		
	}


}
