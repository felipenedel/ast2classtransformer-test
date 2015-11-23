package transformer;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import javassist.ClassPool;
import javassist.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AST2ClassInitializer {

	// TODO Analisar consequências;
	// TODO Javadoc;

	private static final Logger log = LoggerFactory.getLogger(AST2ClassInitializer.class);

	public ClassPool initializeClassPool(List<URL> urls) {
		ClassPool classPool = ClassPool.getDefault();

		for (URL url : urls) {
			try {
				classPool.insertClassPath(url.getPath());
			} catch (NotFoundException e) {
				e.printStackTrace();
			}
		}

		return classPool;
	}

	public ClassLoader initializeClassLoader(List<URL> urls) {
		return this.addURLsToClassLoader(urls);
	}

	private ClassLoader addURLsToClassLoader(List<URL> urls) {
		URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Class<?> clazz = URLClassLoader.class;

		try {
			Method method = clazz.getDeclaredMethod("addURL", new Class[] { URL.class });
			method.setAccessible(true);

			for (URL url : urls) {
				method.invoke(classLoader, new Object[] { url });
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		return this.getClass().getClassLoader();
	}

}
