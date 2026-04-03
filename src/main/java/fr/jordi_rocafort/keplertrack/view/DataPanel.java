package fr.jordi_rocafort.keplertrack.view;

import javax.swing.*;

import fr.jordi_rocafort.keplertrack.controller.*;

import java.awt.*;

public class DataPanel extends JPanel {
	private InputPanel inputPanel;
	private OutputPanel outputPanel;

	private static DataPanel instance = null;

	public static DataPanel getInstance() {
		if (instance == null) {
			instance = new DataPanel();
		}

		return instance;
	}

	public DataPanel() {
		this.setLayout(new BorderLayout());

		inputPanel = InputPanel.getInstance();
		outputPanel = OutputPanel.getInstance();
		FileSelectionController.getInstance();

		// Le séparateur vertical entre Input et Output reste, pour que tu puisses
		// ajuster la hauteur de la zone de saisie si besoin.
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputPanel, outputPanel);
		splitPane.setContinuousLayout(true);
		splitPane.setDividerSize(6);

		splitPane.setResizeWeight(0.0);
		splitPane.setDividerLocation(180);

		this.add(splitPane, BorderLayout.CENTER);

		DecodeController.getInstance();
	}
}