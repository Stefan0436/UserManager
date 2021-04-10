package org.asf.connective.usermanager.api;

/**
 * 
 * Command argument specification system.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ArgumentSpecification {
	protected String name;
	protected boolean required = true;
	protected Class<?> type;

	public static ArgumentSpecification create(String name, Class<?> type) {
		ArgumentSpecification spec = new ArgumentSpecification();
		spec.name = name;
		spec.type = type;
		return spec;
	}

	public static ArgumentSpecification create(String name, Class<?> type, boolean required) {
		ArgumentSpecification spec = new ArgumentSpecification();
		spec.name = name;
		spec.type = type;
		spec.required = required;
		return spec;
	}
	
	public Class<?> getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isRequired() {
		return required;
	}
}
