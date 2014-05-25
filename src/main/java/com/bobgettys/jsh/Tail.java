package com.bobgettys.jsh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

public class Tail extends BaseShellApp {

	@SuppressWarnings("static-access")
	public Tail(InputStream in, PrintStream out, PrintStream err) {
		//@formatter:off
		super(new Options()
		.addOption("f", "follow", false, "Whether to follow the file")
		.addOption(OptionBuilder
				.withArgName("k")
				.hasArg()
				.withDescription("Number of lines to output")
				.create('n'))
				.addOption("h", "help", false, "Help"), in, out, err, "tail [options] <file>");//@formatter:on
	}

	@Override
	protected CommandLineParser createParser() {
		return new GnuParser();
	}

	public static void main(String[] args) throws ShellException {
		new Tail(System.in, System.out, System.err).run(args);
	}

	// public static void main(String[] args) throws ParseException {
	// @SuppressWarnings("static-access")
	//		//@formatter:off
	//		Options opts = new Options()
	//		.addOption("f", "follow", false, "Whether to follow the file")
	//		.addOption(OptionBuilder
	//				.withArgName("k")
	//				.hasArg()
	//				.withDescription("Number of lines to output")
	//				.create('n'))
	//				.addOption("h", "help", false, "Help");//@formatter:on
	// CommandLine cl = new GnuParser().parse(opts, args);
	// if (cl.hasOption('h')) {
	// printHelp(opts);
	// return;
	// }
	// final String[] remainingArgs = cl.getArgs();
	// if (remainingArgs.length != 1) {
	// System.err.println("Invalid arguments.");
	// printHelp(opts);
	// return;
	// }
	// File file = new File(remainingArgs[0]);
	// int lines;
	// try {
	// lines = cl.hasOption('n') ? Integer.parseInt(cl.getOptionValue('n')) :
	// 10;
	// } catch (NumberFormatException e) {
	// System.err.println("Invalid number of lines.");
	// printHelp(opts);
	// return;
	// }
	// byte[] buf = new byte[8192];
	// int found = 0;
	// if (lines > 0) {
	// try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
	// long fileSize = raf.length();
	// /*
	// * align to buffer size, which should (hopefully) be a multiple
	// * of block size, though it may not even matter since it may
	// * over the network
	// */
	// int lastRead = (int) (fileSize % buf.length);
	// if (lastRead == 0) {
	// lastRead = buf.length;
	// }
	// int i = 0;
	// long pos = Math.max(fileSize - lastRead, 0L);
	// outer: while (found < lines && pos >= 0) {
	// raf.seek(pos);
	// lastRead = raf.read(buf, 0, lastRead);
	// for (i = lastRead - 1; i >= 0; --i) {
	// // 0x0A == \n. it is not appropriate to compare to a
	// // char
	// // further, only count the line break if it's not
	// // bordering the end of the file
	// if (buf[i] == 0x0A && i != fileSize - pos && ++found >= lines) {
	// break outer;
	// }
	// }
	// pos -= lastRead;
	// }
	// ++i;
	// lastRead -= i;
	// do {
	// System.out.write(buf, i, lastRead);
	// raf.seek(pos += buf.length);
	// i = 0;
	// long remaining;
	// if ((remaining = fileSize - pos) >= buf.length) {
	// lastRead = raf.read(buf);
	// } else if (remaining > 0) {
	// lastRead = raf.read(buf, 0, (int) remaining);
	// }
	// } while (pos < fileSize && lastRead > 0);
	// } catch (FileNotFoundException e) {
	// System.err.println("File not found: " + file);
	// return;
	// } catch (IOException e) {
	// System.err.println("Error while reading file: " + e.getMessage());
	// e.printStackTrace(System.err);
	// return;
	// }
	// }
	// if (cl.hasOption('f')) {
	// class TailerStop {
	//
	// private volatile Tailer tailer;
	//
	// public void setTailer(Tailer tailer) {
	// this.tailer = tailer;
	// }
	//
	// public void stop() {
	// Tailer tailer = this.tailer;
	// if (tailer != null) {
	// tailer.stop();
	// }
	// }
	//
	// }
	// final TailerStop stopper = new TailerStop();
	// Tailer tailer = Tailer.create(file, new TailerListener() {
	//
	// @Override
	// public void init(Tailer tailer) {
	// }
	//
	// @Override
	// public void fileNotFound() {
	// System.err.println("File not found: " + remainingArgs[0]);
	// stopper.stop();
	// }
	//
	// @Override
	// public void fileRotated() {
	// System.err.println("File " + remainingArgs[0] + " has been rotated.");
	// stopper.stop();
	// }
	//
	// @Override
	// public void handle(String line) {
	// System.out.println(line);
	// }
	//
	// @Override
	// public void handle(Exception ex) {
	// System.err.println("Error while following file: " + ex.getMessage());
	// ex.printStackTrace(System.err);
	// stopper.stop();
	// }
	//
	// }, 1000L, true);
	// stopper.setTailer(tailer);
	// tailer.run();
	// }
	// }
	//
	// private static void printHelp(Options opts) {
	// new HelpFormatter().printHelp("tail [options] <file>", opts, false);
	// }

	@Override
	public void run(String[] args) throws ShellException {
		CommandLine cl = getCommandLine(args);
		if (cl.hasOption('h')) {
			printHelp();
			return;
		}
		final String[] remainingArgs = cl.getArgs();
		if (remainingArgs.length != 1) {
			err.println("Invalid arguments.");
			printHelp();
			return;
		}
		File file = new File(remainingArgs[0]);
		int lines;
		try {
			lines = cl.hasOption('n') ? Integer.parseInt(cl.getOptionValue('n')) : 10;
		} catch (NumberFormatException e) {
			err.println("Invalid number of lines.");
			printHelp();
			return;
		}
		byte[] buf = new byte[8192];
		int found = 0;
		if (lines > 0) {
			try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
				long fileSize = raf.length();
				/*
				 * align to buffer size, which should (hopefully) be a multiple
				 * of block size, though it may not even matter since it may
				 * over the network
				 */
				int lastRead = (int) (fileSize % buf.length);
				if (lastRead == 0) {
					lastRead = buf.length;
				}
				int i = 0;
				long pos = Math.max(fileSize - lastRead, 0L);
				outer: while (found < lines && pos >= 0) {
					raf.seek(pos);
					lastRead = raf.read(buf, 0, lastRead);
					for (i = lastRead - 1; i >= 0; --i) {
						// 0x0A == \n. it is not appropriate to compare to a
						// char
						// further, only count the line break if it's not
						// bordering the end of the file
						if (buf[i] == 0x0A && i != fileSize - pos && ++found >= lines) {
							break outer;
						}
					}
					pos -= lastRead;
				}
				++i;
				lastRead -= i;
				do {
					out.write(buf, i, lastRead);
					raf.seek(pos += buf.length);
					i = 0;
					long remaining;
					if ((remaining = fileSize - pos) >= buf.length) {
						lastRead = raf.read(buf);
					} else if (remaining > 0) {
						lastRead = raf.read(buf, 0, (int) remaining);
					}
				} while (pos < fileSize && lastRead > 0);
			} catch (FileNotFoundException e) {
				err.println("File not found: " + file);
				return;
			} catch (IOException e) {
				err.println("Error while reading file: " + e.getMessage());
				e.printStackTrace(err);
				return;
			}
		}
		if (cl.hasOption('f')) {
			class TailerStop {

				private volatile Tailer tailer;

				public void setTailer(Tailer tailer) {
					this.tailer = tailer;
				}

				public void stop() {
					Tailer tailer = this.tailer;
					if (tailer != null) {
						tailer.stop();
					}
				}

			}
			final TailerStop stopper = new TailerStop();
			Tailer tailer = Tailer.create(file, new TailerListener() {

				@Override
				public void init(Tailer tailer) {
				}

				@Override
				public void fileNotFound() {
					err.println("File not found: " + remainingArgs[0]);
					stopper.stop();
				}

				@Override
				public void fileRotated() {
					err.println("File " + remainingArgs[0] + " has been rotated.");
					stopper.stop();
				}

				@Override
				public void handle(String line) {
					out.println(line);
				}

				@Override
				public void handle(Exception ex) {
					err.println("Error while following file: " + ex.getMessage());
					ex.printStackTrace(err);
					stopper.stop();
				}

			}, 1000L, true);
			stopper.setTailer(tailer);
			tailer.run();
		}
	}

}
