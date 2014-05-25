package com.bobgettys.jsh;

import java.io.InputStream;
import java.io.PrintStream;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

public class CD extends BaseShellApp {

	public CD(Options opts, InputStream in, PrintStream out, PrintStream err, String cmdLineSyntax) {
		super(opts, in, out, err, cmdLineSyntax);
	}

	@Override
	public void run(String[] args) throws ShellException {
		// TODO Auto-generated method stub

	}

	@Override
	protected CommandLineParser createParser() {
		// TODO Auto-generated method stub
		return null;
	}

}
