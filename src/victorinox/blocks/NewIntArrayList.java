package victorinox.blocks;

import java.util.Arrays;
import java.util.RandomAccess;

/**
 * 
 * @author KevinLiao
 *
 */
public class NewIntArrayList implements RandomAccess, Cloneable, java.io.Serializable, Marshalable
{
	private static final long serialVersionUID = -7535391314476843758L;
	private transient int[] elementData;
	private int size;

	public NewIntArrayList(int initialCapacity)
	{
		super();
		if (initialCapacity < 0)
			throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
		this.elementData = new int[initialCapacity];
		this.size = initialCapacity;
	}

	public NewIntArrayList()
	{
		this(10);
	}

	public boolean add(int e)
	{
		ensureCapacity(size + 1); // Increments modCount!!
		elementData[size++] = e;
		return true;
	}

	public int get(int index)
	{
		RangeCheck(index);
		return elementData[index];
	}

	public int set(int index, int element)
	{
		RangeCheck(index);
		int oldValue = elementData[index];
		elementData[index] = element;
		return oldValue;
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
			int newCapacity = (oldCapacity * 3) / 2 + 1;
			if (newCapacity < minCapacity)
				newCapacity = minCapacity;
			// minCapacity is usually close to size, so this is a win:
			elementData = Arrays.copyOf(elementData, newCapacity);
			size = newCapacity;
		}
	}

	private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException
	{
		s.defaultWriteObject();

		// Write out array length
		s.writeInt(elementData.length);

		// Write out all elements in the proper order.
		for (int i = 0; i < size; i++)
			s.writeInt(elementData[i]);

	}

	private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException
	{
		// Read in size, and any hidden stuff
		s.defaultReadObject();

		// Read in array length and allocate array
		int arrayLength = s.readInt();
		elementData = new int[arrayLength];
		// Read in all elements in the proper order.
		for (int i = 0; i < arrayLength; i++)
			elementData[i] = s.readInt();
	}

	@Override
	public String toString()
	{
		return "NewIntArrayList [elementData=" + Arrays.toString(elementData) + ", size=" + size + "]";
	}
}
