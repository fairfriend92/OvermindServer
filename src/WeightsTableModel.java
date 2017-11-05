import javax.swing.table.AbstractTableModel;

public class WeightsTableModel extends AbstractTableModel {	
	
	float[] data;
	
	public WeightsTableModel (float[] f) {
		data = f;
	}

	@Override
	public Class<?> getColumnClass(int arg0) {
		return arg0 == 2 ? Float.class : String.class;
	}

	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public String getColumnName(int arg0) {

		String name = "Default";
		
		switch (arg0) {
		case 0: name = "Neuron";
				break;		
		case 1: name = "Synapse";
				break;		
		case 2: name = "Value";
				break;		
		}
		
		return name;
	}

	@Override
	public int getRowCount() {
		return data.length;
	}

	@Override
	public Object getValueAt(int arg0, int arg1) {

		Object result = null;
		
		switch (arg1) {
		case 0: result = Integer.toString(((int) (arg0 / VirtualLayerVisualizer.selectedNode.originalNumOfSynapses)));
				break;
		case 1: result = Integer.toString(arg0 - (int) (arg0 / VirtualLayerVisualizer.selectedNode.originalNumOfSynapses) * 
				VirtualLayerVisualizer.selectedNode.originalNumOfSynapses);
				break;
		case 2: result = data[arg0];
				break;
		}
		
		return result;
	}

	@Override
	public boolean isCellEditable(int arg0, int arg1) {		
		return arg1 == 2;		
	}

	@Override
	public void setValueAt(Object arg0, int arg1, int arg2) {	
		
		data[arg1] = (float) arg0;
		fireTableCellUpdated(arg1, arg2);
		
		// Save in the Node object the new weight so that it can be used by VirtualLayerManager to update the terminal.		
		VirtualLayerVisualizer.selectedNode.terminal.newWeights = new float[] {(float)arg0};
		VirtualLayerVisualizer.selectedNode.terminal.newWeightsIndexes = new int[] {arg1};
		
		// Update the collection of weights for the selected node with the new weight. 
		VirtualLayerManager.weightsTable.get(VirtualLayerVisualizer.selectedNode.virtualID)[arg1] = (float)arg0; 
		
		VirtualLayerManager.connectNodes(new Node[] {VirtualLayerVisualizer.selectedNode});
		
	}

}
