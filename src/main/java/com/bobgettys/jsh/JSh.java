package com.bobgettys.jsh;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.GroupLayout;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import org.apache.commons.lang3.SystemUtils;

public class JSh extends JFrame {

	private static final long serialVersionUID = 1L;

	private static final String prelude;

	static {
		String hostName;
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostName = "localhost";
		}
		//@formatter:off
		prelude = new StringBuilder(System.getProperty("user.name"))
		.append('@')
		.append(hostName)
		.toString();//@formatter:on
	}

	private final AttributeSet preludeAttributes;
	private final AttributeSet cwdAttributes;
	private final HashMap<String, ShellApp> appRegistry = new HashMap<>();
	final AttributeSet textAttributes;
	final StyledDocument shellDoc;
	final JTextPane shell;
	private final String cwd;
	int inputStart;

	public JSh() {
		super("Jsh");
		setFocusTraversalKeysEnabled(false);
		cwd = System.getProperty("user.dir");

		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		setSize(800, 600);
		shell = new JTextPane();
		shellDoc = shell.getStyledDocument();
		shell.setSize(getSize());
		shell.setBackground(Color.BLACK);
		shell.setMargin(new Insets(0, 1, 1, 0));
		shell.setFont(new Font("Monospaced", Font.PLAIN, 12));
		shell.setEditable(true);
		shell.getCaret().setVisible(true);

		StyleContext sc = StyleContext.getDefaultStyleContext();
		AttributeSet preludeAttributes = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground,
				new Color(0x0070FF70));
		this.preludeAttributes = sc.addAttribute(preludeAttributes, StyleConstants.Bold, true);
		sc = StyleContext.getDefaultStyleContext();
		AttributeSet cwdAttributes = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, new Color(
				0x000040FF));
		this.cwdAttributes = sc.addAttribute(cwdAttributes, StyleConstants.Bold, true);
		textAttributes = StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY,
				StyleConstants.Foreground, Color.WHITE);
		shell.setCharacterAttributes(textAttributes, true);
		addPrelude();

		InputMap inputMap = shell.getInputMap();
		ActionMap actionMap = shell.getActionMap();

		String interrupt = "interrupt";
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.VK_CONTROL), interrupt);
		actionMap.put(interrupt, new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
			}

		});
		String enter = "enter";
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), enter);
		actionMap.put(enter, new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (shell.getCaretPosition() < inputStart) {
					shell.setCaretPosition(shellDoc.getLength());
				}
				int pos = shell.getCaretPosition();
				String cmd;
				try {
					shell.setSelectionStart(inputStart);
					shell.setSelectionEnd(pos);
					// cmd = shellDoc.getText(inputStart, pos);
					cmd = shell.getSelectedText();
					shell.setSelectionStart(pos);
					shellDoc.insertString(pos, "\n", textAttributes);
					executeSystemCmd(cmd);
				} catch (BadLocationException e1) {
					e1.printStackTrace(System.err);
				}
				addPrelude();
			}

		});
		String backSpace = "back space";
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), backSpace);
		actionMap.put(backSpace, new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (shell.getCaretPosition() < inputStart) {
					shell.setCaretPosition(shellDoc.getLength());
				}
				int pos = shell.getCaretPosition();
				if (pos > inputStart) {
					try {
						shellDoc.remove(--pos, 1);
						shell.setCaretPosition(pos);
					} catch (BadLocationException e1) {
						e1.printStackTrace(System.err);
					}
				}
			}

		});
		final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		String copy = "copy";
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
				copy);
		actionMap.put(copy, new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				StringSelection sel = new StringSelection(shell.getSelectedText());
				clipboard.setContents(sel, sel);
			}

		});
		String paste = "paste";
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
				paste);
		actionMap.put(paste, new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
					if (shell.getCaretPosition() < inputStart) {
						shell.setCaretPosition(shellDoc.getLength());
					}
					int pos = shell.getCaretPosition();
					try {
						String pasteText = (String) clipboard.getData(DataFlavor.stringFlavor);
						shellDoc.insertString(pos, pasteText, textAttributes);
						shell.moveCaretPosition(pasteText.length());
					} catch (BadLocationException | UnsupportedFlavorException | IOException e1) {
					}
				}
			}

		});

		KeyListener listener = new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				if (shell.getCaretPosition() < inputStart) {
					shell.setCaretPosition(shellDoc.getLength());
				}
				int pos = shell.getCaretPosition();
				try {
					char c = e.getKeyChar();
					switch (c) {
					case '\n':
					case '\b':
						break;
					default:
						if (isPrintableChar(c)) {
							shellDoc.insertString(pos, String.valueOf(c), textAttributes);
							shell.setCaretPosition(pos + 1);
						}
					}

				} catch (BadLocationException ex) {
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

		};
		shell.addKeyListener(listener);
		addKeyListener(listener);

		JScrollPane scrollPane = new JScrollPane(shell);
		layout.setHorizontalGroup(layout.createSequentialGroup().addComponent(scrollPane));
		layout.setVerticalGroup(layout.createParallelGroup().addComponent(scrollPane));
	}

	void addPrelude() {
		try {
			shellDoc.insertString(shellDoc.getLength(), prelude, preludeAttributes);
			shellDoc.insertString(shellDoc.getLength(), ":", textAttributes);
			shellDoc.insertString(shellDoc.getLength(), cwd, cwdAttributes);
			shellDoc.insertString(shellDoc.getLength(), "$ ", textAttributes);
		} catch (BadLocationException e) {
		}
		shell.setCaretPosition(inputStart = shellDoc.getLength());
	}

	void executeSystemCmd(String cmd) {
		try {
			Process p = Runtime.getRuntime().exec(SystemUtils.IS_OS_WINDOWS ? "cmd /C " + cmd : cmd);
			// OutputStream out = p.getOutputStream();
			// out.write(cmd.getBytes());
			// out.flush();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				for (String line = reader.readLine(); line != null; line = reader.readLine()) {
					try {
						shellDoc.insertString(shellDoc.getLength(), line + "\n", textAttributes);
					} catch (BadLocationException e) {
						e.printStackTrace(System.err);
					}
				}
			}
			p.waitFor();
		} catch (IOException e) {
			try {
				shellDoc.insertString(shellDoc.getLength(), "I/O Error occured while executing command.\n",
						textAttributes);
				e.printStackTrace(System.err);
			} catch (BadLocationException e1) {
			}
		} catch (InterruptedException e) {
			try {
				shellDoc.insertString(shellDoc.getLength(), "\n", textAttributes);
			} catch (BadLocationException e1) {
			}
		}
	}

	public static void main(String[] args) throws InvocationTargetException, InterruptedException {
		SwingUtilities.invokeAndWait(new Runnable() {

			@Override
			public void run() {
				JSh jsh = new JSh();
				jsh.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				jsh.setVisible(true);
			}

		});

	}

	static boolean isPrintableChar(char c) {
		Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
		return !Character.isISOControl(c) && c != KeyEvent.CHAR_UNDEFINED && block != null
				&& block != Character.UnicodeBlock.SPECIALS;
	}
}
