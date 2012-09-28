package victorinox.util;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;

public class CalcSizeOf
{
	static Instrumentation inst;
	static int REFERENCE_SIZE = 4; // for hotspot
	static int OBJECT_HEAD_SIZE = 8;
	static int EMPTY_ARR_VAR_SIZE = 12; //for hotspot

	public static void premain(String agentArgs, Instrumentation instP)
	{
		inst = instP;
	}

	/**
	 * 调用java.lang.instrument.Instrumentation.getObjectSize(Object)方法测量
	 * @param o
	 * @return
	 */
	public static long nativeSizeOf(Object o)
	{
		if (null == inst)
		{
			throw new IllegalStateException("Can not access instrumentation environment.\n"
					+ "Please check if jar file containing SizeOfAgent class is \n"
					+ "specified in the java's \"-javaagent\" command line argument.");
		}
		return inst.getObjectSize(o);
	}

	/**
	 * 使用递归方式确定一个实例的占用空间
	 * @param obj
	 * @return
	 */
	public static long fullSizeOf(Object obj)
	{//深入检索对象，并计算大小
		Map<Object, Object> visited = new IdentityHashMap<Object, Object>();
		Stack<Object> stack = new Stack<Object>();
		long result = internalSizeOf(obj, stack, visited);
		while (!stack.isEmpty())
		{//通过栈进行遍历
			result += internalSizeOf(stack.pop(), stack, visited);
		}
		visited.clear();
		return result;
	}

	//判定哪些是需要跳过的
	private static boolean skipObject(Object obj, Map<Object, Object> visited)
	{
		//		if (obj instanceof String)
		//		{
		//			if (obj == ((String) obj).intern())
		//			{
		//				return true;
		//			}
		//		}
		return (obj == null) || visited.containsKey(obj);
	}

	private static long internalSizeOf(Object obj, Stack<Object> stack, Map<Object, Object> visited)
	{
		if (skipObject(obj, visited))
		{//跳过常量池对象、跳过已经访问过的对象
			return 0;
		}
		visited.put(obj, null);//将当前对象放入栈中
		long result = 0;
		result += nativeSizeOf(obj);
		Class<?> clazz = obj.getClass();
		if (clazz.isArray())
		{//如果数组
			if (clazz.getName().length() != 2)
			{// skip primitive type array
				int length = Array.getLength(obj);
				for (int i = 0; i < length; i++)
				{
					stack.add(Array.get(obj, i));
				}
			}
			return result;
		}
		return getNodeSize(clazz, result, obj, stack);
	}

	//这个方法获取非数组对象自身的大小，并且可以向父类进行向上搜索
	private static long getNodeSize(Class<?> clazz, long result, Object obj, Stack<Object> stack)
	{
		while (clazz != null)
		{
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields)
			{
				if (!Modifier.isStatic(field.getModifiers()))
				{//这里抛开静态属性
					if (field.getType().isPrimitive())
					{//这里抛开基本关键字（因为基本关键字在调用java默认提供的方法就已经计算过了）
						continue;
					}
					else
					{
						field.setAccessible(true);
						try
						{
							Object objectToAdd = field.get(obj);
							if (objectToAdd != null)
							{
								stack.add(objectToAdd);//将对象放入栈中，一遍弹出后继续检索
							}
						}
						catch (IllegalAccessException ex)
						{
							assert false;
						}
					}
				}
			}
			clazz = clazz.getSuperclass();//找父类class，直到没有父类
		}
		return result;
	}

	public static long calcSize(Object obj)
	{
		AtomicLong size = new AtomicLong();
		HashMap visited = new HashMap();
		long result = calcSize(obj, size, visited);
		visited.clear();
		return result;
	}

	public static long calcSize(Object obj, AtomicLong size, HashMap visited)
	{
		if (null == obj)
		{
			return 0;
		}
		if (visited.containsKey(obj))
		{
			return 0;
		}
		visited.put(obj, null);
		Class<?> clazz = obj.getClass();
		//		System.out.println("1clazz" + clazz + "," + clazz.getName() + ", size: " + size.get());
		boolean isSupperClass = false;
		for (; clazz != Object.class; clazz = clazz.getSuperclass())
		{
			recursiveGetSize(clazz, obj, isSupperClass, size, visited);
			isSupperClass = true;
			//			System.out.println(clazz);
		}
		//		System.out.println("2clazz" + clazz + "," + clazz.getName() + ", size: " + size.get());
		return size.get();
	}

	private static void recursiveGetSize(Class<?> clazz, Object obj, boolean isSuperClass, AtomicLong currentSize,
			HashMap visited)
	{
		if (null == clazz || null == obj)
		{
			return;
		}
		if (clazz.isArray())
		{
			long size = EMPTY_ARR_VAR_SIZE;
			Class<?> componentType = clazz.getComponentType();
			if (componentType.isPrimitive())
			{
				size += lengthOfPrimitiveArray(obj) * sizeofPrimitiveClass(componentType);
				currentSize.addAndGet(padding(size));
			}
			else
			{
				size += REFERENCE_SIZE * ((Object[]) obj).length;
				currentSize.addAndGet(padding(size));
				for (Object o : (Object[]) obj)
				{
					if (null == o)
						continue;
					calcSize(o, currentSize, visited);
					//					recursiveGetSize(componentType, o, false, currentSize);
					//					System.out.println("clazz in array" + componentType + "," + componentType.getName() + ", size: "
					//							+ currentSize);
				}
			}
		}
		else
		{
			long size = isSuperClass ? 0 : OBJECT_HEAD_SIZE;
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields)
			{
				if (Modifier.isStatic(field.getModifiers()))
				{
					continue;
				}
				field.setAccessible(true);
				Class<?> type = field.getType();
				if (type.isPrimitive())
				{
					size += sizeofPrimitiveClass(type);
					//					System.out.println("clazz in primitive fields " + type + ", size: " + size);
				}
				else
				{
					size += REFERENCE_SIZE;
					try
					{
						calcSize(field.get(obj), currentSize, visited);
						//						recursiveGetSize(type, field.get(obj), false, currentSize);
						//						System.out.println("clazz in other fields " + type + "," + type.getName() + ", size: "
						//								+ currentSize);
					}
					catch (IllegalAccessException e)
					{
						assert false;
					}
				}
			}
			currentSize.addAndGet(padding(size));
		}
	}

	private static int sizeofPrimitiveClass(Class<?> clazz)
	{
		return clazz == boolean.class || clazz == byte.class ? 1 : clazz == char.class || clazz == short.class ? 2
				: clazz == int.class || clazz == float.class ? 4 : 8;
	}

	private static int lengthOfPrimitiveArray(Object object)
	{
		Class<?> clazz = object.getClass();
		return clazz == boolean[].class ? ((boolean[]) object).length
				: clazz == byte[].class ? ((byte[]) object).length : clazz == char[].class ? ((char[]) object).length
						: clazz == short[].class ? ((short[]) object).length
								: clazz == int[].class ? ((int[]) object).length
										: clazz == float[].class ? ((float[]) object).length
												: clazz == long[].class ? ((long[]) object).length
														: ((double[]) object).length;
	}

	public static int occupyof(boolean variable)
	{
		return 1;
	}

	public static int occupyof(byte variable)
	{
		return 1;
	}

	public static int occupyof(short variable)
	{
		return 2;
	}

	public static int occupyof(char variable)
	{
		return 2;
	}

	public static int occupyof(int variable)
	{
		return 4;
	}

	public static int occupyof(float variable)
	{
		return 4;
	}

	public static int occupyof(long variable)
	{
		return 8;
	}

	public static int occupyof(double variable)
	{
		return 8;
	}

	private static long padding(long size)
	{
		return (size + 7) / 8 * 8;
	}
}
