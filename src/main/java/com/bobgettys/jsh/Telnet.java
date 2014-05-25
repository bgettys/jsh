package com.bobgettys.jsh;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Telnet {

	public static void main(String[] args) {
		Options opts = new Options();
		CommandLine cl;
		try {
			cl = new GnuParser().parse(opts, args);
		} catch (ParseException e) {
			printHelp(opts);
			return;
		}
		String remainingArgs[] = cl.getArgs();
		if (remainingArgs.length < 1 || remainingArgs.length > 2) {
			System.err.println("Invalid arguments.");
			printHelp(opts);
			return;
		}
		String host = remainingArgs[0];
		int port;
		try {
			port = remainingArgs.length > 1 ? Integer.parseInt(remainingArgs[1]) : 23;
		} catch (NumberFormatException e) {
			System.err.println("Invalid port number.");
			printHelp(opts);
			return;
		}
		try (final Socket socket = new Socket(host, port)) {
			class Stopper {

				volatile boolean stop;

			}
			final Stopper stopper = new Stopper();
			new Thread() {

				@Override
				public void run() {
					try {
						InputStream is = socket.getInputStream();
						byte[] buf = new byte[8192];
						while (!stopper.stop) {
							int bytesRead = is.read(buf);
							synchronized (System.out) {
								System.out.write(buf, 0, bytesRead);
							}
						}
					} catch (IOException e) {
						synchronized (System.err) {
							System.err.println("Error reading from socket: " + e.getMessage());
						}
						stopper.stop = true;
						return;
					}
				}

			}.start();
			while (!stopper.stop) {
				// TODO: how to intercept ctrl+c to prevent exiting?
				// Signal sig = sun.misc.Signal.getSignal(); not available in
				// this jdk :(
			}
		} catch (UnknownHostException e) {
			System.err.println("Unknown Host: " + host);
			e.printStackTrace(System.err);
		} catch (IOException e) {
			System.err.println("Error while connecting to remote host: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}

	private static void printHelp(Options opts) {
		new HelpFormatter().printHelp("telnet <host> [port number]", opts);
	}

}
