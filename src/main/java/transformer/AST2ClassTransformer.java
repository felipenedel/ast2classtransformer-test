package transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.ByteMemberValue;
import javassist.bytecode.annotation.CharMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.DoubleMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.FloatMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.LongMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Primitives;

public class AST2ClassTransformer {

	// TODO Logger not working;
	// TODO Remove print fields;
	// TODO Add suport for interfaces, methods and superclasses;
	// TODO Add suport for lists inside annotations;

	private static final Logger log = LoggerFactory.getLogger(AST2ClassTransformer.class);

	ConstPool constantPool;

	Elements elementsUtil;
	ClassPool classPool;
	ClassLoader customClassLoader;

	public AST2ClassTransformer(Elements elementsUtil, ClassLoader classLoader, ClassPool classPool) {
		this.elementsUtil = elementsUtil;
		this.customClassLoader = classLoader;
		this.classPool = classPool;
	}

	/**
	 * Process this list of {@link TypeElement} and returns a list of converted classes;
	 *
	 * @param elements
	 * @return
	 * @throws CannotCompileException
	 * @throws RuntimeException
	 * @throws NotFoundException
	 */
	public List<Class<?>> processTypeElements(Set<? extends Element> elements) {
		List<Class<?>> createdClasses = new ArrayList<>();

		for (Element element : elements) {
			TypeElement typeElement = (TypeElement) element;

			try {
				this.classPool.get(element.toString());
				createdClasses.add(this.customClassLoader.loadClass(element.toString()));
			} catch (NotFoundException nfe) {
				try {
					// Just

					String packageName = this.elementsUtil.getPackageOf(typeElement).toString();
					this.classPool.makePackage(this.customClassLoader, packageName);
					String elementClassName = typeElement.getSimpleName().toString();

					CtClass ctClass;
					try {
						ctClass = this.classPool.get(packageName + "." + elementClassName);
						ctClass.defrost();
					} catch (Exception e) {
						ctClass = this.classPool.makeClass(packageName + "." + elementClassName);
					}

					ClassFile classfile = ctClass.getClassFile();
					this.constantPool = classfile.getConstPool();

					for (VariableElement elementField : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
						CtField ctField = this.resolveCtField(ctClass, elementField.asType().toString(), elementField.getSimpleName().toString());

						if (ctField == null) {
							continue;
						}

						ctField.setModifiers(Modifier.PUBLIC);

						List<? extends AnnotationMirror> annotationMirrors = this.elementsUtil.getAllAnnotationMirrors(elementField);
						for (AnnotationMirror annotationMirror : annotationMirrors) {
							AttributeInfo annotationsAttribute = this.extractAnnotationFromMirror(annotationMirror);

							if (annotationsAttribute != null) {
								ctField.getFieldInfo().addAttribute(annotationsAttribute);
							}
						}

						ctClass.addField(ctField);
					}

					Class<?> createdClass = ctClass.toClass(this.customClassLoader, null);
					createdClasses.add(createdClass);

					// this.printClassFields(createdClass);
				} catch (CannotCompileException e) {
					log.warn("Cannot compile type element: " + typeElement.toString() + "\n" + e.getMessage());
				} catch (NotFoundException e) {
					log.warn("Not found: " + typeElement.toString() + "\n" + e.getMessage());
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		return createdClasses;
	}

	/**
	 * Resolves the ctField associated with the given ctClass.<br>
	 * If the classPool doesn't contains the fieldType, it'll create one;
	 *
	 * @param ctClass - the ctClass that will contain the new ctField
	 * @param fieldType - the type of the field;
	 * @param fieldName - the name of the field;
	 * @return a new ctField with the given name and type, associated with the given ctClass
	 * @throws CannotCompileException
	 * @throws RuntimeException
	 */
	private CtField resolveCtField(CtClass ctClass, String fieldType, String fieldName) throws CannotCompileException, RuntimeException {
		CtClass newFieldCtType = null;

		try {
			// TODO Accept other parameterized types;
			if (fieldType.contains("java.util.List")) {
				newFieldCtType = this.classPool.get("java.util.List");
			} else {
				newFieldCtType = this.classPool.get(fieldType);
			}
		} catch (NotFoundException e) {
			log.warn("Field type should exist on ClassPool. Add it to the ClassLoader: " + fieldName + ": " + fieldType);
			log.warn("Will try to create a fake Class;");
			newFieldCtType = this.classPool.makeClass(fieldType);
			newFieldCtType.toClass();
		}

		CtField ctField = new CtField(newFieldCtType, fieldName, ctClass);

		return ctField;
	}

	/**
	 * Extract an {@link AttributeInfo} from the given annotation mirror;
	 *
	 * @param annotationMirror
	 * @return
	 * @throws NotFoundException
	 * @throws CannotCompileException
	 */
	private AttributeInfo extractAnnotationFromMirror(AnnotationMirror annotationMirror) throws CannotCompileException, NotFoundException {
		String annotationType = annotationMirror.getAnnotationType().toString();
		Annotation annotation = this.resolveCtAnnotation(annotationType);

		if (annotation == null) {
			return null;
		}

		Map<? extends ExecutableElement, ? extends AnnotationValue> annotationValues = annotationMirror.getElementValues();
		for (ExecutableElement annotationMemberName : annotationValues.keySet()) {
			this.addMemberToAnnotation(annotation, annotationMemberName.getSimpleName().toString(), annotationValues.get(annotationMemberName));
		}

		AnnotationsAttribute annotationsAttribute = new AnnotationsAttribute(this.constantPool, AnnotationsAttribute.visibleTag);
		annotationsAttribute.addAnnotation(annotation);

		return annotationsAttribute;
	}

	/**
	 * Resolves the Annotation associated with the given annotation type.<br>
	 * If the classPool doesn't contains the type, it'll create one;
	 *
	 * @param annotationType
	 * @return
	 * @throws CannotCompileException
	 * @throws NotFoundException
	 */
	private Annotation resolveCtAnnotation(String annotationType) throws CannotCompileException, NotFoundException {
		CtClass ctAnnotation = null;

		try {
			ctAnnotation = this.classPool.get(annotationType);
		} catch (Exception e) {
			log.warn("Annotation type should exist on ClassPool. Add it to the ClassLoader: " + annotationType);
			return null;
		}

		return new Annotation(this.constantPool, ctAnnotation);
	}

	/**
	 * Adds a new member to the annotation;
	 *
	 * @param annotation - the annotation that will receive the new member;
	 * @param memberName - the name of the new member;
	 * @param annotationMemberValue - the value of the original annotation;
	 * @throws NotFoundException
	 * @throws CannotCompileException
	 */
	private void addMemberToAnnotation(Annotation annotation, String memberName, AnnotationValue annotationMemberValue) throws CannotCompileException, NotFoundException {
		Object annotationValue = annotationMemberValue.getValue();
		MemberValue memberValue = null;

		if (annotationValue instanceof String) {
			memberValue = new StringMemberValue((String) annotationValue, this.constantPool);
		} else if (annotationValue instanceof TypeMirror) {
			String className = annotationValue.toString();
			this.classPool.makeClass(className); // FIXME Remove this zueira;

			memberValue = new ClassMemberValue(className, this.constantPool);
		} else if (Primitives.isWrapperType(annotationValue.getClass())) {
			memberValue = this.createPrimitiveMemberValue(annotationValue);
		} else if (annotationValue instanceof VariableElement) {
			EnumMemberValue enumMemberValue = new EnumMemberValue(this.constantPool);
			enumMemberValue.setType(((VariableElement) annotationValue).asType().toString());
			enumMemberValue.setValue(((VariableElement) annotationValue).getSimpleName().toString());
			memberValue = enumMemberValue;
		} else if (annotationValue instanceof List) {
			ArrayMemberValue arrayMemberValue = new ArrayMemberValue(this.constantPool);
			List<?> annotationValueList = (List<?>) annotationValue;
			List<MemberValue> membersSemNome = new ArrayList<>();

			for (Object object : annotationValueList) {
				AnnotationMirror annotationMirror = (AnnotationMirror) object;

				AnnotationsAttribute attributeInfo = (AnnotationsAttribute) this.extractAnnotationFromMirror(annotationMirror);
				if (attributeInfo == null) {
					continue;
				}

				Annotation[] annotations = attributeInfo.getAnnotations();
				for (int index = 0; index < annotations.length; index++) {
					membersSemNome.add(new AnnotationMemberValue(annotations[index], this.constantPool));
				}
			}

			arrayMemberValue.setValue(membersSemNome.toArray(new MemberValue[0]));
			memberValue = arrayMemberValue;
		} else {
			log.warn("Can't handle type: " + annotationMemberValue.getValue());
		}

		if (memberValue == null) {
			log.warn("Couldn't resolve type: " + annotationMemberValue.getValue());
			return;
		}

		annotation.addMemberValue(memberName, memberValue);
	}

	/**
	 * Creates a new member value based on the primitive type;
	 *
	 * @param object
	 * @return a new {@link MemberValue}
	 */
	private MemberValue createPrimitiveMemberValue(Object object) {
		MemberValue memberValue = null;

		if (object instanceof Double) {
			memberValue = new DoubleMemberValue((double) object, this.constantPool);
		} else if (object instanceof Boolean) {
			memberValue = new BooleanMemberValue((boolean) object, this.constantPool);
		} else if (object instanceof Long) {
			memberValue = new LongMemberValue((long) object, this.constantPool);
		} else if (object instanceof Integer) {
			memberValue = new IntegerMemberValue(this.constantPool, (int) object);
		} else if (object instanceof Float) {
			memberValue = new FloatMemberValue((float) object, this.constantPool);
		} else if (object instanceof Character) {
			memberValue = new CharMemberValue((char) object, this.constantPool);
		} else if (object instanceof Short) {
			memberValue = new LongMemberValue((short) object, this.constantPool);
		} else if (object instanceof Byte) {
			memberValue = new ByteMemberValue((byte) object, this.constantPool);
		}

		return memberValue;
	}

	// private void printClassFields(Class<?> translatedClass) {
	// System.out.println();
	// java.lang.annotation.Annotation[] annotations = translatedClass.getAnnotations();
	// for (int i = 0; i < annotations.length; i++) {
	// System.out.println(annotations[i]);
	// }
	// Field[] fields = translatedClass.getFields();
	// for (int i = 0; i < fields.length; i++) {
	// System.out.print(fields[i] + ": ");
	// java.lang.annotation.Annotation[] annotations2 = fields[i].getAnnotations();
	// for (int j = 0; j < annotations2.length; j++) {
	// System.out.print(annotations2[j].toString() + "; ");
	// }
	// System.out.println();
	// }
	// }
}
