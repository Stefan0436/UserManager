package org.asf.connective.usermanager.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.asf.connective.usermanager.implementation.commands.admin.CreateProductKeys;
import org.asf.connective.usermanager.implementation.commands.admin.ListActivationKeys;
import org.asf.connective.usermanager.implementation.commands.admin.ListAllowedGroups;
import org.asf.connective.usermanager.implementation.commands.admin.ListProductGroups;
import org.asf.connective.usermanager.implementation.commands.admin.ListProductKeys;
import org.asf.connective.usermanager.implementation.commands.admin.ListUsers;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.packet.PacketEntry;
import org.asf.cyan.api.packet.PacketParser;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.http.ProviderContext;

/**
 * 
 * Admin frontend panel - admin commands run from here.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class AdminPanelFrontend extends CyanComponent {

	protected static class EntryTypeManager extends PacketParser {
		private static EntryTypeManager inst = new EntryTypeManager();

		public static void registerEntryType(PacketEntry<?> entry) {
			inst.registerType(entry);
		}

		public static PacketEntry<?> getEntryForType(long type) {
			try {
				@SuppressWarnings("rawtypes")
				Constructor<? extends PacketEntry> ctor = inst.entryTypes.get(type).getDeclaredConstructor();
				ctor.setAccessible(true);
				return ctor.newInstance();
			} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				return null;
			}
		}
	}

	/**
	 * Main implementation, use assignMainImplementation to set it
	 */
	protected static AdminPanelFrontend implementation;

	private CommandEntry firstEntry = null;

	private ConnectiveHTTPServer server;
	private HttpRequest request;
	private HttpResponse response;
	private ProviderContext context;
	private String contextRoot;

	/**
	 * Runs the given command
	 * 
	 * @param command Admin command to run
	 * @return Executed instance or null if a syntax error occured.
	 */
	protected IAdminCommand runCommand(IAdminCommand command, Supplier<String[]> argumentKeyProvider,
			Function<String, String> argumentValueProvider) {
		ArrayList<String> keys = new ArrayList<String>(Arrays.asList(argumentKeyProvider.get()));
		for (ArgumentSpecification spec : command.specification()) {
			if (spec.required && !keys.contains(spec.name)) {
				return null;
			}
		}

		HashMap<String, Object> arguments = new HashMap<String, Object>();
		for (String key : keys) {
			String val = argumentValueProvider.apply(key);
			Optional<ArgumentSpecification> optSpec = Stream.of(command.specification()).filter(t -> t.name.equals(key))
					.findFirst();
			if (optSpec.isEmpty())
				return null;

			ArgumentSpecification spec = optSpec.get();
			if (spec.type.getTypeName().equals(String.class.getTypeName())) {
				arguments.put(key, val);
			} else {
				try {
					byte[] data = Base64.getDecoder().decode(val);
					PacketEntry<?> ent = EntryTypeManager
							.getEntryForType(ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 8)).getLong());
					ByteArrayInputStream strm = new ByteArrayInputStream(Arrays.copyOfRange(data, 8, data.length));
					try {
						arguments.put(key, ent.importStream(strm, data.length - 8).get());
					} catch (IOException e) {
					}
				} catch (Exception e) {
				}
			}
		}

		IAdminCommand inst = command.newInstance().setup(arguments, request, response, server, context, contextRoot);
		inst.run();
		return inst;
	}

	private class CommandEntry implements Iterable<IAdminCommand> {
		public IAdminCommand command;
		public CommandEntry next;

		@Override
		public Iterator<IAdminCommand> iterator() {
			return new CommandIterator(this);
		}

		private class CommandIterator implements Iterator<IAdminCommand> {

			public CommandEntry current;

			public CommandIterator(CommandEntry mainEntry) {
				current = mainEntry;
			}

			@Override
			public boolean hasNext() {
				return current != null;
			}

			@Override
			public IAdminCommand next() {
				CommandEntry c = current;
				current = c.next;
				return c.command;
			}

		}
	}

	/**
	 * Assigns the main frontend implementation, also scans the commands and adds
	 * them.
	 * 
	 * @param frontend Admin panel implementation
	 */
	protected static void assignMainImplementation(AdminPanelFrontend frontend) {
		implementation = frontend;
		frontend.addDefaultCommands();
		frontend.scanCommands();
	}

	/**
	 * Retrieves the HTTP request
	 */
	protected HttpRequest getRequest() {
		return request;
	}

	/**
	 * Retrieves the HTTP response
	 */
	protected HttpResponse getResponse() {
		return response;
	}

	/**
	 * Retrieves the File Provider Context
	 */
	protected ProviderContext getContext() {
		return context;
	}

	/**
	 * Retrieves the context root
	 */
	protected String getContextRoot() {
		return contextRoot;
	}

	/**
	 * Retrieves the server running the frontend
	 */
	protected ConnectiveHTTPServer getServer() {
		return server;
	}

	/**
	 * Retrieves the commands as an iterable instance
	 */
	protected Iterable<IAdminCommand> getCommands() {
		return firstEntry;
	}

	/**
	 * Adds all default commands (automatically called on assign)
	 */
	protected void addDefaultCommands() {
		this.addCommand(new CreateProductKeys());
		this.addCommand(new ListProductKeys());
		this.addCommand(new ListProductGroups());
		this.addCommand(new ListAllowedGroups());
		this.addCommand(new ListUsers());
		this.addCommand(new ListActivationKeys());
	}

	/**
	 * Adds admin commands
	 * 
	 * @param command Admin command to add
	 */
	protected void addCommand(IAdminCommand command) {
		if (firstEntry == null) {
			firstEntry = new CommandEntry();
			firstEntry.command = command;
			return;
		}

		CommandEntry owner = firstEntry;
		if (owner.command.id().equalsIgnoreCase(command.id())) {
			return;
		}
		while (owner.next != null) {
			if (owner.command.id().equalsIgnoreCase(command.id())) {
				return;
			}
			owner = owner.next;
		}

		owner.next = new CommandEntry();
		owner.next.command = command;
	}

	/**
	 * Scans all classes for commands (automatically called on assign)
	 */
	protected void scanCommands() {
		for (Class<IAdminCommand> cls : findClasses(getMainImplementation(), IAdminCommand.class)) {
			try {
				if (!Modifier.isAbstract(cls.getModifiers()))
					addCommand(cls.getConstructor().newInstance());
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				error("Failed to instanciate admin command: " + cls.getTypeName(), e);
			}
		}
	}

	/**
	 * Creates a new instance (override for main implementation)
	 */
	protected abstract AdminPanelFrontend newInstance();

	/**
	 * Runs the frontend processor
	 */
	protected abstract void runFrontend();

	/**
	 * Runs the admin panel frontend
	 * 
	 * @param request     HTTP request
	 * @param response    HTTP response
	 * @param server      Server processing the request
	 * @param context     File provider context
	 * @param contextRoot Context root
	 */
	public static void runFrontend(HttpRequest request, HttpResponse response, ConnectiveHTTPServer server,
			ProviderContext context, String contextRoot) {
		AdminPanelFrontend inst = implementation.newInstance();
		inst.context = context;
		inst.contextRoot = contextRoot;
		inst.firstEntry = implementation.firstEntry;
		inst.request = request;
		inst.response = response;
		inst.server = server;
		inst.runFrontend();
	}
}
