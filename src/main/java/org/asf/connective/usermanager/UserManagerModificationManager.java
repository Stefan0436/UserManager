package org.asf.connective.usermanager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.Memory;
import org.asf.rats.http.ProviderContextFactory;
import org.asf.rats.http.providers.FileUploadHandler;
import org.asf.rats.http.providers.IFileAlias;
import org.asf.rats.http.providers.IFileExtensionProvider;
import org.asf.rats.http.providers.IFileRestrictionProvider;
import org.asf.rats.http.providers.IVirtualFileProvider;

import org.asf.rats.processors.HttpGetProcessor;
import org.asf.rats.processors.HttpUploadProcessor;

// Our superclass for managing modifications made to the server
public abstract class UserManagerModificationManager extends CyanComponent {

	private static UserManagerModificationManager instance;
	private static boolean prepared = false;

	/**
	 * Assigns the instance.
	 */
	protected static void assign(UserManagerModificationManager instance) {
		UserManagerModificationManager.instance = instance;
	}

	/**
	 * Checks if the module has been prepared
	 */
	public static boolean hasBeenPrepared() {
		return prepared;
	}

	/**
	 * Registers all modifications of the ExampleModificationManager instance.
	 * 
	 * @throws IOException If preparing fails
	 */
	public static void prepareModifications() throws IOException {
		if (prepared)
			return;

		prepared = true;

		InputStream smdf = instance.getClass().getResourceAsStream("/modules." + instance.moduleId() + ".ctxf");
		String data = new String(smdf.readAllBytes());
		smdf.close();

		for (String line : data.replaceAll("\r", "").split("\n")) {
			if (line.isEmpty() || line.startsWith("#"))
				continue;

			ArrayList<String> arguments = parseCommand(line);
			if (arguments.size() != 0) {
				String cmd = arguments.get(0);
				arguments.remove(0);

				try {
					if (cmd.equals("processor")) {

						instance.registerProcessor(
								(HttpGetProcessor) Class.forName(arguments.get(0).substring("class:".length()), false,
										instance.getClass().getClassLoader()).getConstructor().newInstance());

					} else if (cmd.equals("extension")) {

						instance.registerExtension(
								(IFileExtensionProvider) Class.forName(arguments.get(0).substring("class:".length()),
										false, instance.getClass().getClassLoader()).getConstructor().newInstance());

					} else if (cmd.equals("restriction")) {

						instance.registerRestriction(
								(IFileRestrictionProvider) Class.forName(arguments.get(0).substring("class:".length()),
										false, instance.getClass().getClassLoader()).getConstructor().newInstance());

					} else if (cmd.equals("alias")) {

						instance.registerAlias((IFileAlias) Class.forName(arguments.get(0).substring("class:".length()),
								false, instance.getClass().getClassLoader()).getConstructor().newInstance());

					} else if (cmd.equals("uploadhandler")) {

						instance.registerUploadHandler(
								(FileUploadHandler) Class.forName(arguments.get(0).substring("class:".length()), false,
										instance.getClass().getClassLoader()).getConstructor().newInstance());

					} else if (cmd.equals("virtualfile")) {

						instance.registerVirtualFile(
								(IVirtualFileProvider) Class.forName(arguments.get(0).substring("class:".length()), false,
										instance.getClass().getClassLoader()).getConstructor().newInstance());

					}
				} catch (Exception e) {
					error("Module error, could not run command: " + line, e);
				}
			}
		}
	}

	// Modification storage
	private ArrayList<HttpGetProcessor> processors = new ArrayList<HttpGetProcessor>();
	private ArrayList<IFileAlias> aliases = new ArrayList<IFileAlias>();
	private ArrayList<IFileExtensionProvider> extensions = new ArrayList<IFileExtensionProvider>();
	private ArrayList<IFileRestrictionProvider> restrictions = new ArrayList<IFileRestrictionProvider>();
	private ArrayList<FileUploadHandler> handlers = new ArrayList<FileUploadHandler>();
	private ArrayList<IVirtualFileProvider> virtualFiles = new ArrayList<IVirtualFileProvider>();

	/**
	 * Register GET and/or POST processors
	 */
	protected void registerProcessor(HttpGetProcessor processor) {
		processors.add(processor);
	}

	/**
	 * Register extensions
	 */
	protected void registerExtension(IFileExtensionProvider extension) {
		extensions.add(extension);
	}

	/**
	 * Register restrictions
	 */
	protected void registerRestriction(IFileRestrictionProvider restriction) {
		restrictions.add(restriction);
	}

	/**
	 * Register virtual files
	 */
	protected void registerVirtualFile(IVirtualFileProvider restriction) {
		virtualFiles.add(restriction);
	}

	/**
	 * Register aliases
	 */
	protected void registerAlias(IFileAlias alias) {
		aliases.add(alias);
	}

	/**
	 * Register file upload handlers
	 */
	protected void registerUploadHandler(FileUploadHandler handler) {
		handlers.add(handler);
	}

	protected static ArrayList<String> parseCommand(String args) {
		ArrayList<String> args3 = new ArrayList<String>();
		char[] argarray = args.toCharArray();
		boolean ignorespaces = false;
		String last = "";
		int i = 0;
		for (char c : args.toCharArray()) {
			if (c == '"' && (i == 0 || argarray[i - 1] != '\\')) {
				if (ignorespaces)
					ignorespaces = false;
				else
					ignorespaces = true;
			} else if (c == ' ' && !ignorespaces && (i == 0 || argarray[i - 1] != '\\')) {
				args3.add(last);
				last = "";
			} else if (c != '\\' || (i + 1 < argarray.length && argarray[i + 1] != '"'
					&& (argarray[i + 1] != ' ' || ignorespaces))) {
				last += c;
			}

			i++;
		}

		if (last == "" == false)
			args3.add(last);

		return args3;
	}

	/**
	 * Starts the module
	 */
	public static void start() {
		if (instance == null)
			throw new IllegalStateException("Module not assigned!");

		if (!hasBeenPrepared()) {
			try {
				prepareModifications();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		instance.startModule();
		Memory.getInstance().getOrCreate("bootstrap.call").<Runnable>append(() -> {
			for (HttpGetProcessor proc : instance.processors) {
				if (proc instanceof HttpUploadProcessor)
					ConnectiveHTTPServer.getMainServer().registerProcessor((HttpUploadProcessor) proc);
				else
					ConnectiveHTTPServer.getMainServer().registerProcessor(proc);
			}
		});
	}

	/**
	 * Applies the modifications registered
	 */
	public static void appy(ProviderContextFactory arg0) {
		arg0.addAliases(instance.aliases);
		arg0.addExtensions(instance.extensions);
		arg0.addProcessors(instance.processors);
		arg0.addRestrictions(instance.restrictions);
		arg0.addUploadHandlers(instance.handlers);
		arg0.addVirtualFiles(instance.virtualFiles);
	}

	/**
	 * Starts the module
	 */
	protected abstract void startModule();

	/**
	 * Retrieves the module id
	 */
	protected abstract String moduleId();
}
