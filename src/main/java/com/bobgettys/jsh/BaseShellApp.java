package com.bobgettys.jsh;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public abstract class BaseShellApp implements ShellApp {

	private final Options opts;
	private final String cmdLineSyntax;

	protected final InputStream in;
	protected final PrintStream out;
	protected final PrintStream err;

	public BaseShellApp(Options opts, InputStream in, PrintStream out, PrintStream err, String cmdLineSyntax) {
		this.opts = opts;
		this.in = in;
		this.out = out;
		this.err = err;
		this.cmdLineSyntax = cmdLineSyntax;
	}

	protected CommandLine getCommandLine(String[] args) throws ShellException {
		try {
			return createParser().parse(opts, args);
		} catch (ParseException e) {
			throw new ShellException(e);
		}
	}

	protected void printHelp() {
		HelpFormatter hf = new HelpFormatter();
		hf.printHelp(new PrintWriter(out), hf.getWidth(), cmdLineSyntax, null, opts, hf.getLeftPadding(),
				hf.getDescPadding(), null, false);
	}

	protected abstract CommandLineParser createParser();

}
