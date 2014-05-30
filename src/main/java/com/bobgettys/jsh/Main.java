package com.bobgettys.jsh;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicButtonUI;

public class Main {

	static final MouseAdapter mouseListener = new MouseAdapter() {

		@Override
		public void mouseEntered(MouseEvent e) {
			Component component = e.getComponent();
			if (component instanceof AbstractButton) {
				AbstractButton button = (AbstractButton) component;
				button.setBorderPainted(true);
			}
		}

		@Override
		public void mouseExited(MouseEvent e) {
			Component component = e.getComponent();
			if (component instanceof AbstractButton) {
				AbstractButton button = (AbstractButton) component;
				button.setBorderPainted(false);
			}
		}

	};

	public static void main(String[] args) throws InvocationTargetException, InterruptedException {
		SwingUtilities.invokeAndWait(new Runnable() {

			@Override
			public void run() {
				JFrame window = new JFrame("Jsh");
				final JTabbedPane tabPane = new JTabbedPane();
				tabPane.setSize(window.getSize());
				addTab(tabPane);
				window.add(tabPane);
				window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				window.setSize(800, 600);
				window.setVisible(true);
				JRootPane rootPane = window.getRootPane();
				String newTab = "newTab";
				InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
				ActionMap actionMap = rootPane.getActionMap();
				inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK),
						newTab);
				actionMap.put(newTab, new AbstractAction() {

					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						addTab(tabPane);
					}

				});

			}

		});

	}

	static void addTab(final JTabbedPane tabPane) {
		if (!SwingUtilities.isEventDispatchThread()) {
			try {
				SwingUtilities.invokeAndWait(new Runnable() {

					@Override
					public void run() {
						addTab(tabPane);
					}
				});
			} catch (InvocationTargetException | InterruptedException e1) {
			}
			return;
		}
		final Shell jsh = new Shell();
		tabPane.addTab("Jsh", jsh);
		jsh.setSize(tabPane.getSize());
		final int index = tabPane.indexOfComponent(jsh);
		JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tabPanel.setOpaque(false);
		JLabel label = new JLabel() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getText() {
				int i = tabPane.indexOfComponent(jsh);
				if (i != -1) {
					return tabPane.getTitleAt(i);
				}
				return null;
			}

		};
		tabPanel.add(label);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));

		JButton button = new JButton() {

			private static final long serialVersionUID = 1L;

			{
				int size = 17;
				setPreferredSize(new Dimension(size, size));
				setToolTipText("Close this tab");
				// Make the button look the same for all Laf's
				setUI(new BasicButtonUI());
				setContentAreaFilled(false);
				setFocusable(false);
				setBorder(BorderFactory.createEtchedBorder());
				setBorderPainted(false);
				addMouseListener(mouseListener);
				setRolloverEnabled(true);
			}

			@Override
			public void updateUI() {
			}

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D) g.create();
				if (getModel().isPressed()) {
					g2.translate(1, 1);
				}
				g2.setStroke(new BasicStroke(2));
				g2.setColor(Color.BLACK);
				if (getModel().isRollover()) {
					g2.setColor(Color.MAGENTA);
				}
				int delta = 6;
				g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
				g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
				g2.dispose();
			}

		};
		tabPanel.add(button);
		tabPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		tabPane.setTabComponentAt(index, tabPanel);
		String closeTab = "closeTab";
		jsh.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK),
				closeTab);
		AbstractAction closeAction = new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				tabPane.remove(jsh);
				if (tabPane.getTabCount() == 0) {
					Container topLevel = tabPane.getTopLevelAncestor();
					topLevel.setVisible(false);
					if (topLevel instanceof Window) {
						((Window) topLevel).dispose();
					} else { // something strange happened, just exit
						System.exit(0);
					}
				}
			}

		};
		jsh.getActionMap().put(closeTab, closeAction);
		button.addActionListener(closeAction);
		tabPane.setSelectedIndex(index);
	}

}
