package overmind_server;
import com.example.overmind.*;

import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PartitionTool extends JFrame implements WindowListener {	
	 boolean isOpen = false;
	 private Node selectedNode = null;	
	 
	 private JPanel populationsPanel = new JPanel();
	 
	 int open() {
		 isOpen = true;
		 this.setVisible(true);
		 this.selectedNode = VirtualLayerVisualizer.selectedNode;
		 
		 if (selectedNode == null) {
			 System.out.println("Partition Tool: selectedNode is null when it shouldn't");
			 return Constants.ERROR;
		 }
		 
		 int numOfPopulations = selectedNode.terminal.populations.size();
		 int numOfLayers = 1; // TODO: When we'll have multiple layers on the same terminal, the population array will be a matrix 
		 					  // and numOfLayers will be equal to the y order
		 
		 populationsPanel.setLayout(new GridLayout(numOfPopulations, numOfLayers));
		 
		 // TODO: This becomes 2 nested for when layers are introduced
		 for (int i = 0; i < numOfPopulations; i++) {
			 populationsPanel.add(new PopulationLabel(selectedNode.terminal.populations.get(i)));
		 }
		 
		 
		 this.setTitle("Partition Tool");
		 this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		 
		 return Constants.SUCCESS;
	 }

	@Override
	public void windowOpened(WindowEvent e) {		
	}

	@Override
	public void windowClosing(WindowEvent e) {		
		isOpen = false;
		this.setVisible(false);
	}

	@Override
	public void windowClosed(WindowEvent e) {	
	}

	@Override
	public void windowIconified(WindowEvent e) {		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {		
	}

	@Override
	public void windowActivated(WindowEvent e) {		
	}

	@Override
	public void windowDeactivated(WindowEvent e) {		
	}
	
	private class PopulationLabel extends JLabel implements MouseListener {
		private ImageIcon labelIcon = new ImageIcon();		
		
		private PopulationLabel (Population population) {
			try {
				Image img = ImageIO.read(getClass().getResource("/icons/neurons.png"));
				labelIcon = (new ImageIcon(img.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
