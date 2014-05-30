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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.GroupLayout;
import javax.swing.InputMap;
import javax.swing.JPanel;
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

public class Shell extends JPanel {

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
	final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	private final HashMap<String, ShellApp> appRegistry = new HashMap<>();
	final AttributeSet textAttributes;
	final StyledDocument shellDoc;
	final JTextPane shell;
	private final String cwd;
	int inputStart;

	public Shell() {
		// Basic attributes of the editing pane
		setFocusTraversalKeysEnabled(false);
		cwd = System.getProperty("user.dir");
		GroupLayout layout = new GroupLayout(this);
		setLayout(layout);

		shell = new JTextPane();
		JScrollPane scrollPane = new JScrollPane(shell);
		layout.setHorizontalGroup(layout.createSequentialGroup().addComponent(scrollPane));
		layout.setVerticalGroup(layout.createParallelGroup().addComponent(scrollPane));
		shellDoc = shell.getStyledDocument();
		shell.setSize(getSize());
		shell.setBackground(Color.BLACK);
		shell.setMargin(new Insets(0, 1, 1, 0));
		shell.setFont(new Font("Monospaced", Font.PLAIN, 12));
		shell.setEditable(false);
		shell.getCaret().setVisible(true);

		// styles for the prelude (username@host), current working directory,
		// and normal text
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

		addPrompt();

		// hotkeys and hotkey overrides
		InputMap inputMap = shell.getInputMap();
		ActionMap actionMap = shell.getActionMap();

		String home = "home";
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), home);
		actionMap.put(home, new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				shell.setCaretPosition(inputStart);
			}

		});

		String interrupt = "interrupt";
		KeyStroke ctrlC = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK);
		inputMap.remove(ctrlC);
		inputMap.put(ctrlC, interrupt);
		actionMap.put(interrupt, new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				doInterrupt();
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
				addPrompt();
			}

		});

		String delete = "delete";
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), delete);
		actionMap.put(delete, new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				if (shell.getCaretPosition() < inputStart) {
					shell.setCaretPosition(shellDoc.getLength());
				}
				int pos = shell.getCaretPosition();
				if (pos >= inputStart && pos < shellDoc.getLength()) {
					try {
						shellDoc.remove(pos, 1);
					} catch (BadLocationException e1) {
					}
				}
			}

		});

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

		shell.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					doPaste();
				}
			}

		});
		String paste = "paste";
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
				paste);
		actionMap.put(paste, new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				doPaste();
			}

		});

		KeyAdapter listener = new KeyAdapter() {

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
							break;
						case '\b':
							if (pos > inputStart) {
								shellDoc.remove(--pos, 1);
								shell.setCaretPosition(pos);
							}
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

		};
		shell.addKeyListener(listener);
		addKeyListener(listener);

	}

	void addPrompt() {
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
		} catch (InterruptedException | InterruptedIOException e) {
			try {
				shellDoc.insertString(shellDoc.getLength(), "\n", textAttributes);
			} catch (BadLocationException e1) {
				e1.printStackTrace(System.err);
			}
		} catch (IOException e) {
			try {
				shellDoc.insertString(shellDoc.getLength(), e.getMessage() + '\n', textAttributes);
				e.printStackTrace(System.err);
			} catch (BadLocationException e1) {
				e1.printStackTrace(System.err);
			}
		}
	}

	void doPaste() {
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

	void doInterrupt() {
		System.out.println("interrupt");
		// TODO: implement later (Ctrl + C interrupt behavior)
	}

	static boolean isPrintableChar(char c) {
		return !Character.isISOControl(c) && c != KeyEvent.CHAR_UNDEFINED
				&& !Character.UnicodeBlock.SPECIALS.equals(Character.UnicodeBlock.of(c));
	}

}
