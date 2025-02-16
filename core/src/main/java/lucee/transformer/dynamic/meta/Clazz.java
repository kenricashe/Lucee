package lucee.transformer.dynamic.meta;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.Type;

import lucee.commons.io.SystemUtil;
import lucee.commons.io.log.Log;
import lucee.commons.io.res.Resource;
import lucee.commons.lang.ClassException;
import lucee.commons.lang.ClassUtil;
import lucee.commons.lang.Pair;
import lucee.commons.lang.types.RefInteger;
import lucee.commons.lang.types.RefIntegerImpl;
import lucee.runtime.config.Constants;
import lucee.runtime.exp.PageException;
import lucee.runtime.java.JavaObject;
import lucee.runtime.op.Caster;
import lucee.runtime.reflection.Reflector;
import lucee.runtime.type.Collection;
import lucee.transformer.dynamic.meta.dynamic.ClazzDynamic;
import lucee.transformer.dynamic.meta.reflection.ClazzReflection;

public abstract class Clazz implements Serializable {

	public static final int VERSION = 1;

	private static final long serialVersionUID = 4236939474343760825L;
	private static Boolean allowReflection = null;

	public abstract List<Method> getMethods(String methodName, boolean nameCaseSensitive, int argumentLength) throws IOException;

	public abstract List<Method> getDeclaredMethods(String methodName, boolean nameCaseSensitive, int argumentLength) throws IOException;

	public abstract List<Constructor> getConstructors(int argumentLength) throws IOException;

	public abstract List<Constructor> getDeclaredConstructors(int argumentLength) throws IOException;

	public abstract Method getDeclaredMethod(String methodName, Class[] arguments, boolean nameCaseSensitive) throws IOException, NoSuchMethodException;

	public abstract Method getMethod(String methodName, Class[] arguments, boolean nameCaseSensitive) throws IOException, NoSuchMethodException;

	public abstract Constructor getConstructor(Class[] arguments) throws IOException, NoSuchMethodException;

	public abstract Constructor getDeclaredConstructor(Class[] arguments) throws IOException, NoSuchMethodException;

	public abstract Class getDeclaringClass();

	public abstract Type getDeclaringType();

	protected abstract String id();

	public static boolean allowReflection() {
		if (allowReflection == null) {
			allowReflection = Caster.toBooleanValue(SystemUtil.getSystemPropOrEnvVar("lucee.allow.reflection", null), false);
		}
		return allowReflection;
	}

	public static Clazz getClazzReflection(Class clazz) {
		return new ClazzReflection(clazz);
	}

	public static Clazz getClazz(Class clazz, Resource root, Log log, boolean useReflection) {
		if (useReflection) return new ClazzReflection(clazz);
		return getClazz(clazz, root, log);
	}

	public static Clazz getClazz(Class clazz, Resource root, Log log) {
		try {
			return ClazzDynamic.getInstance(clazz, root, log);
		}
		catch (Exception e) {
			if (log != null) log.error("dynamic", e);
			if (allowReflection()) return new ClazzReflection(clazz);
			else throw new RuntimeException(e);
		}
	}

	private static Map<String, SoftReference<Pair<Method, Boolean>>> cachedMethods = new ConcurrentHashMap<>();

	public static Method getMethodMatch(Clazz clazz, final Collection.Key methodName, Object[] args, boolean convertArgument)
			throws NoSuchMethodException, IOException, PageException {
		// print.e("------ " + methodName + " -------");
		// print.e(clazz.getDeclaringClass());
		// print.e(args.length);
		args = Reflector.cleanArgs(args);
		Reflector.checkAccessibility(clazz.getDeclaringClass(), methodName);
		List<Method> methods = clazz.getMethods(methodName.getString(), false, args.length);

		if (methods != null && methods.size() > 0) {
			Class[] clazzArgs = Reflector.getClasses(args);

			// cache
			StringBuilder sb = new StringBuilder(clazz.id()).append(methodName).append(';');
			for (Class cls: clazzArgs) {
				sb.append(cls.getName()).append(';');
			}

			String key = sb.toString();

			SoftReference<Pair<Method, Boolean>> sr = cachedMethods.get(key);
			if (sr != null) {
				Pair<Method, Boolean> p = sr.get();
				if (p != null) {
					// print.e("used cached match(" + p.getValue() + "):" + key + ":" + cachedMethods.size());
					// convert arguments
					if (p.getValue()) {
						// print.e("------- " + clazz.getDeclaringClass().getName() + ":" + methodName + " -----");
						Class[] trgArgs = p.getName().getArgumentClasses();
						for (int x = 0; x < trgArgs.length; x++) {
							if (args[x] != null) args[x] = Caster.castTo(null, trgArgs[x], args[x]);
							// print.e("- " + clazzArgs[x].getName() + "->" + trgArgs[x].getName());
						}

					}
					return p.getName();
				}
			}

			// exact comparsion
			outer: for (Method m: methods) {
				if (m != null) {
					Class[] parameterTypes = m.getArgumentClasses();
					for (int y = 0; y < parameterTypes.length; y++) {
						if (Reflector.toReferenceClass(parameterTypes[y]) != clazzArgs[y]) continue outer;
					}
					// print.e("exact match:" + key + ":");
					cachedMethods.put(key, new SoftReference<Pair<Method, Boolean>>(new Pair<Method, Boolean>(m, Boolean.FALSE)));
					return m;
				}
			}

			// like comparsion
			outer: for (Method m: methods) {
				if (m != null) {
					Class[] parameterTypes = m.getArgumentClasses();
					for (int y = 0; y < parameterTypes.length; y++) {
						if (!Reflector.like(clazzArgs[y], Reflector.toReferenceClass(parameterTypes[y]))) continue outer;
					}
					// print.e("like match:" + key + ":");
					cachedMethods.put(key, new SoftReference<Pair<Method, Boolean>>(new Pair<Method, Boolean>(m, Boolean.FALSE)));
					return m;
				}
			}

			// convert comparsion
			Pair<Method, Object[]> result = null;
			int _rating = 0;
			outer: for (Method m: methods) {
				if (m != null) {
					RefInteger rating = (methods.size() > 1) ? new RefIntegerImpl(0) : null;
					Class[] parameterTypes = m.getArgumentClasses();
					Object[] newArgs = new Object[args.length];

					for (int y = 0; y < parameterTypes.length; y++) {
						try {
							newArgs[y] = Reflector.convert(args[y], Reflector.toReferenceClass(parameterTypes[y]), rating);
						}
						catch (PageException e) {
							continue outer;
						}
					}
					if (result == null || rating.toInt() > _rating) {
						if (rating != null) _rating = rating.toInt();
						result = new Pair<Method, Object[]>(m, newArgs);
					}
				}
			}

			if (result != null) {
				if (convertArgument) {
					Object[] newArgs = result.getValue();
					for (int x = 0; x < args.length; x++) {
						args[x] = newArgs[x];
					}
				}
				// print.e("conv match:" + key + ":");
				cachedMethods.put(key, new SoftReference<Pair<Method, Boolean>>(new Pair<Method, Boolean>(result.getName(), Boolean.TRUE)));
				return result.getName();
			}
		}
		Class[] classes = Reflector.getClasses(args);
		// StringBuilder sb=null;
		JavaObject jo;
		Class c;
		Constructor cc;
		for (int i = 0; i < classes.length; i++) {
			if (args[i] instanceof JavaObject) {
				jo = (JavaObject) args[i];
				c = jo.getClazz();
				cc = Reflector.getConstructorInstance(c, new Object[0]).getConstructor(null);
				if (cc == null) {

					throw new NoSuchMethodException(
							"The " + Reflector.pos(i + 1) + " parameter of " + methodName + "(" + Reflector.getDspMethods(classes) + ") ia an object created "
									+ "by the createObject function (JavaObject/JavaProxy). This object has not been instantiated because it does not have a constructor "
									+ "that takes zero arguments. " + Constants.NAME
									+ " cannot instantiate it for you, please use the .init(...) method to instantiate it with the correct parameters first");

				}
			}
		}
		/*
		 * the argument list contains objects created by createObject, that are no instantiated
		 * (first,third,10th) and because this object have no constructor taking no arguments, Lucee cannot
		 * instantiate them. you need first to instantiate this objects.
		 */
		StringBuilder msg = new StringBuilder();
		msg.append("No matching method for ").append(lucee.runtime.type.util.Type.getName(clazz.getDeclaringClass())).append(".").append(methodName).append("(")
				.append(Reflector.getDspMethods(Reflector.getClasses(args))).append(") found. ");
		if (methods.size() > 0) {
			msg.append("there are similar methods with the same name, but diferent arguments:\n ");
			for (Method m: methods) {
				msg.append(methodName).append('(').append(Reflector.getDspMethods(m.getArgumentClasses())).append(");\n");
			}

		}
		else {
			msg.append("there are no methods with this name.");
		}

		throw new NoSuchMethodException(msg.toString());
	}

	public static Constructor getConstructorMatch(Clazz clazz, Object[] args, boolean convertArgument) throws NoSuchMethodException, IOException {
		args = Reflector.cleanArgs(args);
		List<Constructor> constructors = clazz.getConstructors(args.length);
		if (constructors != null && constructors.size() > 0) {
			Class[] clazzArgs = Reflector.getClasses(args);
			// exact comparsion
			outer: for (Constructor c: constructors) {
				if (c != null) {

					Class[] parameterTypes = c.getArgumentClasses();
					for (int y = 0; y < parameterTypes.length; y++) {
						if (Reflector.toReferenceClass(parameterTypes[y]) != clazzArgs[y]) continue outer;
					}
					return c;
				}
			}
			// like comparsion
			outer: for (Constructor c: constructors) {
				if (c != null) {
					Class[] parameterTypes = c.getArgumentClasses();
					for (int y = 0; y < parameterTypes.length; y++) {
						if (!Reflector.like(clazzArgs[y], Reflector.toReferenceClass(parameterTypes[y]))) continue outer;
					}
					return c;
				}
			}
			// convert comparsion
			Pair<Constructor, Object[]> result = null;
			int _rating = 0;
			outer: for (Constructor c: constructors) {
				if (c != null) {
					RefInteger rating = (constructors.size() > 1) ? new RefIntegerImpl(0) : null;
					Class[] parameterTypes = c.getArgumentClasses();
					Object[] newArgs = new Object[args.length];
					for (int y = 0; y < parameterTypes.length; y++) {
						try {
							newArgs[y] = Reflector.convert(args[y], Reflector.toReferenceClass(parameterTypes[y]), rating);
						}
						catch (PageException e) {
							continue outer;
						}
					}
					if (result == null || rating.toInt() > _rating) {
						if (rating != null) _rating = rating.toInt();
						result = new Pair<Constructor, Object[]>(c, newArgs);
					}
					// return new ConstructorInstance(constructors[i],newArgs);
				}
			}
			if (result != null) {
				if (convertArgument) {
					Object[] newArgs = result.getValue();
					for (int x = 0; x < args.length; x++) {
						args[x] = newArgs[x];
					}
				}
				return result.getName();
			}
		}
		throw new NoSuchMethodException(
				"No matching Constructor for " + clazz.getDeclaringClass().getName() + "(" + Reflector.getDspMethods(Reflector.getClasses(args)) + ") found");
	}

	public static String id(FunctionMember fm) {
		// public java.lang.String java.lang.String.toString()
		StringBuilder sb = new StringBuilder();

		// name
		sb.append(fm.getName())

				.append('(');

		// arguments;
		String[] args = fm.getArguments();
		if (args != null && args.length > 0) {
			for (String cn: args) {
				sb.append(cn).append(';');
			}
		}

		sb.append(")");

		return sb.toString();
	}

	public static String getAccessModifierAsString(FunctionMember fm) {
		if (fm.isPublic()) return "public";
		if (fm.isProtected()) return "protected";
		if (fm.isPrivate()) return "private";

		return "";
	}

	public static ClassLoader getClassLoader(Class clazz) {
		ClassLoader tmp = clazz.getClassLoader();
		if (tmp == null) tmp = ClassLoader.getSystemClassLoader();
		return tmp;
	}

	public static Type[] toTypes(Class[] classes) {
		Type[] types = new Type[classes.length];
		for (int i = 0; i < classes.length; i++) {
			types[i] = Type.getType(classes[i]);
		}
		return types;
	}

	public static String toTypeNames(Class[] classes) {
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (int i = 0; i < classes.length; i++) {
			sb.append(del).append(Caster.toTypeName(classes[i]));
			del = ",";
		}
		return sb.toString();
	}

	public static boolean isPrimitive(Type type) {
		int sort = type.getSort();
		return sort >= Type.BOOLEAN && sort <= Type.DOUBLE;
	}

	public static Class toClass(ClassLoader cl, Type type) throws ClassException {
		return ClassUtil.loadClass(cl, type.getClassName());
	}

	public static String getPackagePrefix() {
		return "lucee/invoc/wrap/v" + VERSION + "/";
	}
}
