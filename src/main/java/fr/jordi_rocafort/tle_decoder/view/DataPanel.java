package fr.jordi_rocafort.tle_decoder.view;

import javax.swing.*;
import java.awt.*;

public class DataPanel extends JPanel {
	private InputPanel inputPanel;
	private OutputPanel outputPanel;

	public DataPanel() {
		this.setLayout(new BorderLayout());

		inputPanel = new InputPanel();
		outputPanel = new OutputPanel();

		// Le séparateur vertical entre Input et Output reste, pour que tu puisses
		// ajuster la hauteur de la zone de saisie si besoin.
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputPanel, outputPanel);
		splitPane.setContinuousLayout(true);
		splitPane.setDividerSize(6);

		splitPane.setResizeWeight(0.0);
		splitPane.setDividerLocation(180);

		this.add(splitPane, BorderLayout.CENTER);
	}
}