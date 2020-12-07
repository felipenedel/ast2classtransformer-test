package transformer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesReader {

	private final Properties properties;

	public PropertiesReader(String propertyFileName) {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(propertyFileName);
		this.properties = new Properties();

		try {
			this.properties.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getProperty(String propertyName) {
		return this.properties.getProperty(propertyName);
	}

}
