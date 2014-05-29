package com.bobgettys.jsh;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class Main {

	public static void main(String[] args) throws InvocationTargetException, InterruptedException {
		SwingUtilities.invokeAndWait(new Runnable() {

			@Override
			public void run() {
				Shell jsh = new Shell();
				JTabbedPane tabPane = new JTabbedPane();
				tabPane.addTab("Jsh", jsh);
				JFrame window = new JFrame("Jsh");
				tabPane.setSize(window.getSize());
				jsh.setSize(tabPane.getSize());
				window.add(tabPane);
				window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				window.setSize(800, 600);
				window.setVisible(true);
				JRootPane rootPane = window.getRootPane();
				String newTab = "newTab";
				rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
						KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK), newTab);
				rootPane.getActionMap().put(newTab, new AbstractAction() {

					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						Shell jsh = new Shell();
						tabPane.addTab("Jsh", jsh);
						jsh.setSize(tabPane.getSize());
					}

				});
			}

		});

	}

}
