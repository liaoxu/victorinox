package victorinox.blocks;

import java.io.Serializable;
import java.util.BitSet;
import java.util.RandomAccess;

public class OffSetBit implements RandomAccess, Cloneable, Serializable, Marshalable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -4539170807958045283L;

	private String id;
	private int offset;
	private BitSet bits = new BitSet();
	private boolean ifAwarded;

	public OffSetBit(String id, int offset)
	{
		this.id = id;
		this.offset = offset;
		setOffset(0);
	}

	public void set(int i)
	{
		bits.set(i - offset);
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public int getOffset()
	{
		return offset;
	}

	public void setOffset(int offset)
	{
		this.offset = offset;
	}

	public BitSet getBits()
	{
		return bits;
	}

	public void setBits(BitSet bits)
	{
		this.bits = bits;
	}

	public boolean isIfAwarded()
	{
		return ifAwarded;
	}

	public void setIfAwarded(boolean ifAwarded)
	{
		this.ifAwarded = ifAwarded;
	}

	private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException
	{
		s.defaultWriteObject();
		s.writeInt(offset);
		s.writeBoolean(ifAwarded);
		s.writeUTF(id);
		s.writeObject(bits);
	}

	private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException
	{
		s.defaultReadObject();
		offset = s.readInt();
		ifAwarded = s.readBoolean();
		id = s.readUTF();
		bits = (BitSet) s.readObject();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OffSetBit other = (OffSetBit) obj;
		if (id == null)
		{
			if (other.id != null)
				return false;
		}
		else if (!id.equals(other.id))
			return false;
		return true;
	}

}