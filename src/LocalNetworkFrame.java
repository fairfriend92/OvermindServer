import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

public class LocalNetworkFrame {
	
	private JFrame frame = new JFrame();
	
	private JPanel panel = new JPanel();
	private JPanel preConnPanel = new JPanel();
	private JPanel postConnPanel = new JPanel();
	
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
		
		panel.add(numOfNeurons);
		panel.add(numOfDendrites);
		panel.add(numOfSynapses);
		
		preConnPanel.add(presynapticConnections);
		preConnPanel.add(new JLabel("Presynaptic connections"));
		postConnPanel.add(postsynapticConnections);		
		postConnPanel.add(new JLabel("Postsynaptic connections"));
		
		frame.setLayout(new GridLayout());
		frame.add(panel, BorderLayout.CENTER);		
		frame.add(preConnPanel, BorderLayout.CENTER);		
		frame.add(postConnPanel, BorderLayout.CENTER);	
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
		
		System.out.println(updatedNode.postsynapticNodes.size());
		
		for (int i = 0; i < updatedNode.postsynapticNodes.size(); i++) {
			com.example.overmind.LocalNetwork postsynapticNode = updatedNode.postsynapticNodes.get(i);
			postConnListModel.addElement(postsynapticNode.ip);
		}
				
	}
	
}
