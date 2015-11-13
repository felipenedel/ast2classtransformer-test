package transformer;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AST2ClassInitializer {

	private static final Logger log = LoggerFactory.getLogger(AST2ClassInitializer.class);

	public ClassPool initializeClassPool(List<Class<?>> classes) {
		ClassPool classPool = ClassPool.getDefault();

		for (Class<?> clazz : classes) {
			try {
				classPool.get(clazz.getName());
			} catch (NotFoundException notFoundException) {
				CtClass ctAnnotation = classPool.makeAnnotation(clazz.getName());
				try {
					ctAnnotation.toClass();
				} catch (CannotCompileException e) {
					log.error("Cannot compile: " + e.getMessage());
				}
			}
		}

		return classPool;
	}

	public ClassLoader initializeClassLoader(List<Class<?>> classes) {
		List<URL> urls = new ArrayList<>();

		for (Class<?> clazz : classes) {
			try {
				URL classContainer = ClassURLGetter.getClassContainer(clazz);
				urls.add(classContainer);
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}

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
