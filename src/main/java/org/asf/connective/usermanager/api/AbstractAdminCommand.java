package org.asf.connective.usermanager.api;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.http.ProviderContext;

/**
 * 
 * Abstract wrapper around the complicated IAdminCommand interface.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class AbstractAdminCommand implements IAdminCommand {

	private ArrayList<CommandArgument<?>> arguments;
	private HttpRequest request;
	private HttpResponse response;
	private ConnectiveHTTPServer server;
	private ProviderContext context;
	private String contextRoot;

	private boolean result = true;
	private HashMap<String, String> output = new HashMap<String, String>();

	@Override
	public boolean result() {
		return result;
	}

	@Override
	public Map<String, String> output() {
		return output;
	}

	/**
	 * Sets the result value
	 * 
	 * @param success True if command successfuly executed, false otherwise
	 */
	protected void setResult(boolean success) {
		this.result = success;
	}

	/**
	 * Sets output values
	 * 
	 * @param category Key name
	 * @param value    Value
	 */
	protected void setOutput(String category, String value) {
		this.output.put(category, value);
	}

	/**
	 * Sets the output header
	 */
	protected void setOutput(String value) {
		this.output.put("", value);
	}

	protected class CommandArgument<T> {

		private CommandArgument() {
		}

		private String name = null;
		private Class<T> type = null;

		private ArgumentSpecification specification = null;
		private T value = null;

		@SuppressWarnings("unchecked")
		private void set(Object value) {
			this.value = (T) value;
		}

		private void setup(String name, Class<T> type, boolean required) {
			this.name = name;
			this.type = type;
			specification = ArgumentSpecification.create(name, type, required);
		}

		public ArgumentSpecification toSpecification() {
			return specification;
		}

		public T getValue() {
			return value;
		}

	}

	/**
	 * Prepares the command, assign arguments here
	 */
	public abstract void prepare();

	@Override
	public ArgumentSpecification[] specification() {
		if (this.arguments == null) {
			this.arguments = new ArrayList<CommandArgument<?>>();
			prepare();
		}

		return arguments.stream().map(t -> t.toSpecification()).toArray(t -> new ArgumentSpecification[t]);
	}

	@Override
	public IAdminCommand setup(Map<String, Object> arguments, HttpRequest request, HttpResponse response,
			ConnectiveHTTPServer server, ProviderContext context, String contextRoot) {
		this.request = request;
		this.response = response;
		this.server = server;
		this.context = context;
		this.contextRoot = contextRoot;

		if (this.arguments == null) {
			this.arguments = new ArrayList<CommandArgument<?>>();
			prepare();
		}

		arguments.forEach((k, v) -> {
			setInternal(k, v);
		});

		return this;
	}

	private void setInternal(String key, Object value) {
		for (CommandArgument<?> arg : arguments) {
			if (arg.name.equalsIgnoreCase(key) && arg.type.isAssignableFrom(value.getClass())) {
				arg.set(value);
			}
		}
	}

	/**
	 * Registers a serializing argument
	 * 
	 * @param <T>  Argument type
	 * @param name Argument name
	 * @param type Argument class
	 */
	protected <T> CommandArgument<T> registerArgument(String name, Class<T> type) {
		CommandArgument<T> arg = new CommandArgument<T>();
		arg.setup(name, type, true);
		arguments.add(arg);
		return arg;
	}

	/**
	 * Registers a optional serializing argument
	 * 
	 * @param <T>  Argument type
	 * @param name Argument name
	 * @param type Argument class
	 */
	protected <T> CommandArgument<T> registerOptionalArgument(String name, Class<T> type) {
		CommandArgument<T> arg = new CommandArgument<T>();
		arg.setup(name, type, false);
		arguments.add(arg);
		return arg;
	}

	/**
	 * Registers an argument
	 * 
	 * @param name Argument name
	 */
	protected CommandArgument<String> registerArgument(String name) {
		return registerArgument(name, String.class);
	}

	/**
	 * Registers an optional argument
	 * 
	 * @param name Argument name
	 */
	protected CommandArgument<String> registerOptionalArgument(String name) {
		return registerOptionalArgument(name, String.class);
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
	 * Retrieves the value of an argument (unchecked)
	 * 
	 * @param <T>      Value type
	 * @param argument Argument name
	 * @return Value or null
	 */
	@SuppressWarnings("unchecked")
	protected <T> T getValue(String argument) {
		return (T) getValue(argument, Object.class);
	}

	/**
	 * Retrieves all values for a given argument name
	 * 
	 * @param <T>      Value type
	 * @param argument Argument name
	 * @return Array of values
	 */
	protected Object[] getAll(String argument) {
		return getAll(argument, Object.class);
	}

	/**
	 * Retrieves the value of an argument
	 * 
	 * @param <T>      Value type
	 * @param argument Argument name
	 * @param type     Argument type
	 * @return Value or null
	 */
	@SuppressWarnings("unchecked")
	protected <T> T getValue(String argument, Class<T> type) {
		for (CommandArgument<?> arg : arguments) {
			if (arg.name.equalsIgnoreCase(argument) && type.isAssignableFrom(arg.type)) {
				return (T) arg.getValue();
			}
		}

		return null;
	}

	/**
	 * Retrieves all values for a given argument name and type
	 * 
	 * @param <T>      Value type
	 * @param argument Argument name
	 * @param type     Argument type
	 * @return Array of values
	 */
	@SuppressWarnings("unchecked")
	protected <T> T[] getAll(String argument, Class<T> type) {
		ArrayList<T> values = new ArrayList<T>();
		for (CommandArgument<?> arg : arguments) {
			if (arg.name.equalsIgnoreCase(argument) && type.isAssignableFrom(arg.type)) {
				values.add((T) arg.getValue());
			}
		}
		return values.toArray(t -> (T[]) Array.newInstance(type, t));
	}

	/**
	 * Checks if a given argument is present
	 * 
	 * @param argument Argument name
	 * @return True if present, false otherwise.
	 */
	protected boolean isPresent(String argument) {
		return isPresent(argument, Object.class);
	}

	/**
	 * Checks if a given argument is present
	 * 
	 * @param <T>      Argument type
	 * @param argument Argument name
	 * @param type     Argument class
	 * @return True if present, false otherwise.
	 */
	protected boolean isPresent(String argument, Class<?> type) {
		for (CommandArgument<?> arg : arguments) {
			if (arg.name.equalsIgnoreCase(argument) && type.isAssignableFrom(arg.type)) {
				return true;
			}
		}
		return false;
	}

}
