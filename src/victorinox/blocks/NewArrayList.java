package victorinox.blocks;

import java.util.Arrays;
import java.util.RandomAccess;

public class NewArrayList<E> implements RandomAccess, Cloneable, java.io.Serializable, Marshalable
{
	private static final long serialVersionUID = -5837588297756939027L;
	private transient Object[] elementData;
	private int size;

	public NewArrayList(int initialCapacity)
	{
		super();
		if (initialCapacity < 0)
			throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
		this.elementData = new Object[initialCapacity];
		this.size = initialCapacity;
	}

	public NewArrayList()
	{
		this(10);
	}

	public boolean add(E e)
	{
		ensureCapacity(size + 1); // Increments modCount!!
		elementData[size++] = e;
		return true;
	}

	@SuppressWarnings("unchecked")
	public E set(int index, E element)
	{
		RangeCheck(index);

		E oldValue = (E) elementData[index];
		elementData[index] = element;
		return oldValue;
	}

	@SuppressWarnings("unchecked")
	public E get(int index)
	{
		RangeCheck(index);
		return (E) elementData[index];
	}

	public int indexOf(Object o)
	{
		if (o == null)
		{
			for (int i = 0; i < size; i++)
				if (elementData[i] == null)
					return i;
		}
		else
		{
			for (int i = 0; i < size; i++)
				if (o.equals(elementData[i]))
					return i;
		}
		return -1;
	}

	public boolean contains(Object o)
	{
		return indexOf(o) >= 0;
	}

	public void remove(int index)
	{
		elementData[index] = null;
	}

	public int size()
	{
		return size;
	}

	private void RangeCheck(int index)
	{
		if (index >= size)
			ensureCapacity(index + 1);
	}

	public synchronized void ensureCapacity(int minCapacity)
	{
		int oldCapacity = elementData.length;
		if (minCapacity > oldCapacity)
		{
			int newCapacity = (oldCapacity * 4) / 3 + 1;
			if (newCapacity < minCapacity)
				newCapacity = minCapacity;
			// minCapacity is usually close to size, so this is a win:
			elementData = Arrays.copyOf(elementData, newCapacity);
			size = newCapacity;
		}
	}

	public Object getMax()
	{
		if (size < 100)
		{
			return getMax(0, size);
		}
		else
		{
			return getMax(size / 2, size);
		}
	}

	@SuppressWarnings(
	{ "unchecked", "rawtypes" })
	public Object getMax(int from, int to)
	{
		if (from < 0)
			from = 0;
		if (to > size)
			to = size - 1;
		if (from > to)
			return null;
		if (from == to)
			return elementData[from];
		Object max = null;
		int i = from;
		do
		{

			if (null == elementData[i])
				continue;
			if (null == max)
			{
				max = elementData[i];
				if (!(max instanceof Comparable))
					return null;
			}
			Object next = elementData[i + 1];
			if (null == next)
				continue;
			if (((Comparable) max).compareTo(next) < 0)
			{
				max = next;
			}
		}
		while (++i < to);
		return max;
	}

	private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException
	{
		// Write out element count, and any hidden stuff
		s.defaultWriteObject();

		// Write out array length
		s.writeInt(elementData.length);

		// Write out all elements in the proper order.
		for (int i = 0; i < size; i++)
			s.writeObject(elementData[i]);

	}

	private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException
	{
		// Read in size, and any hidden stuff
		s.defaultReadObject();

		// Read in array length and allocate array
		int arrayLength = s.readInt();
		elementData = new Object[arrayLength];

		// Read in all elements in the proper order.
		for (int i = 0; i < arrayLength; i++)
			elementData[i] = s.readObject();
	}

	@Override
	public String toString()
	{
		return "NewArrayList [elementData=" + Arrays.toString(elementData) + ", size=" + size + "]";
	}
}
