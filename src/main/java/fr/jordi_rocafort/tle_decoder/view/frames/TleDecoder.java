package fr.jordi_rocafort.tle_decoder.view.frames;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

public class TleDecoder extends JFrame {
	private static TleDecoder instance;

	public TleDecoder() {
		super("TLE-Decoder");

		instance = this;

		this.setSize(900, 600);
		this.setLocationRelativeTo(null);

		this.setLayout(new BorderLayout());

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}
		});
	}

	public static TleDecoder getInstance() {
		return instance;
	}

	public void close() {
		System.exit(0);
	}
}
