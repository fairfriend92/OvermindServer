import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

public class LocalNetworkFrame {
	
	private JFrame frame = new JFrame();
	
	private JPanel panel = new JPanel(new BorderLayout());
	private JPanel preConnPanel = new JPanel(new BorderLayout());
	private JPanel postConnPanel = new JPanel(new BorderLayout());
	
	private String ip = new String();
	private JLabel numOfNeurons = new JLabel();
	private JLabel numOfDendrites = new JLabel();
	private JLabel numOfSynapses = new JLabel();
	
	private JList<String> presynapticConnections = new JList<>();
	private JList<String> postsynapticConnections = new JList<>();
	private DefaultListModel<String> preConnListModel = new DefaultListModel<>();
	private DefaultListModel<String> postConnListModel = new DefaultListModel<>();
		
	public void display() {		
		
		frame.setTitle(ip);
				
		presynapticConnections.setModel(preConnListModel);
		postsynapticConnections.setModel(postConnListModel);
		
		panel.add(numOfNeurons, BorderLayout.CENTER);
		panel.add(numOfDendrites, BorderLayout.NORTH);
		panel.add(numOfSynapses, BorderLayout.SOUTH);
		
		preConnPanel.add(presynapticConnections, BorderLayout.SOUTH);
		preConnPanel.add(new JLabel("Presynaptic connections"), BorderLayout.NORTH);
		postConnPanel.add(postsynapticConnections, BorderLayout.SOUTH);		
		postConnPanel.add(new JLabel("Postsynaptic connections"), BorderLayout.NORTH);
		
		frame.setLayout(new FlowLayout());
		frame.add(panel);		
		frame.add(preConnPanel);		
		frame.add(postConnPanel);	
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);		
		frame.pack();
		frame.setVisible(true);		
	}
	
	public void update(com.example.overmind.LocalNetwork updatedNode) {
		
		ip = updatedNode.ip;
		numOfNeurons.setText("# neurons: " + updatedNode.numOfNeurons);
		numOfDendrites.setText("# available dendrites: " + updatedNode.numOfDendrites);
		numOfSynapses.setText("# available synapses: " + updatedNode.numOfSynapses);		
		
		preConnListModel.clear();
		postConnListModel.clear();		
		
		for (int i = 0; i < updatedNode.presynapticNodes.size(); i++) {
			com.example.overmind.LocalNetwork presynapticNode = updatedNode.presynapticNodes.get(i);
			preConnListModel.addElement(presynapticNode.ip);
		}

		for (int i = 0; i < updatedNode.postsynapticNodes.size(); i++) {
			com.example.overmind.LocalNetwork postsynapticNode = updatedNode.postsynapticNodes.get(i);
			postConnListModel.addElement(postsynapticNode.ip);
		}
		
		panel.revalidate();
		preConnPanel.revalidate();
		postConnPanel.revalidate();
		frame.revalidate();
		
		panel.repaint();
		preConnPanel.repaint();
		postConnPanel.repaint();
		frame.repaint();
	}
	
}
