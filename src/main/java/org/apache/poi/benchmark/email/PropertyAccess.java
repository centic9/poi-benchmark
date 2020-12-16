package org.apache.poi.benchmark.email;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class PropertyAccess {
	private static final String PROPERTIES = "benchmark.properties";

	private static PropertyAccess instance = null;

	private final Properties properties;

	// private constructor to prevent access from outside
	private PropertyAccess() {
		properties = new Properties();
		try (final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPERTIES)) {
    		if(stream == null) {
    			throw new IllegalStateException("Could not read properties file '" + PROPERTIES + "'.");
    		}
			properties.load(stream);
		} catch (IOException e) {
			throw new IllegalStateException("IOException while loading properties", e);
		}

	}

	public static void init() {
		if(instance == null) {
			instance = new PropertyAccess();
		}
	}

	public static String getProperty(final String name) {
		init();

		return instance.properties.getProperty(name);
	}
}
