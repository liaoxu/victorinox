package victorinox.cache;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;

import victorinox.blocks.NewArrayList;
import victorinox.blocks.NewIntArrayList;
import victorinox.util.CalcSizeOf;

public class NewCacheProxy implements Cache
{

	@SuppressWarnings("rawtypes")
	private static final Class[] validClasses = new Class[]
	{ List.class, String.class, Map.class, Set.class, BitSet.class, AtomicInteger.class, NewIntArrayList.class,
			NewArrayList.class };

	@Autowired
	private NewCache cache;

	public void setCache(NewCache cache)
	{
		this.cache = cache;
	}

	@Override
	public Set<String> keys()
	{
		return cache.keys();
	}

	@Override
	public Object get(String key)
	{
		return cache.get(key);
	}

	@Override
	public void set(String key, Object value)
	{
		typeCheck(value);
		cache.set(key, value);
	}

	@Override
	public void set(String key, Object value, long expireAt)
	{
		typeCheck(value);
		cache.set(key, value, expireAt);
	}

	@Override
	public void flushall()
	{
		cache.flushall();
	}

	@Override
	public void del(String key)
	{
		cache.del(key);
	}

	@Override
	public int incr(String key)
	{
		if (!cache.containsKey(key))
		{
			synchronized (this)
			{
				if (!cache.containsKey(key))
				{
					set(key, new AtomicInteger(0));
					return 0;
				}
			}
		}
		Object v = cache.get(key);
		if (!(v instanceof AtomicInteger))
		{
			throw new RuntimeException("not a AtomicInteger");
		}
		return ((AtomicInteger) v).incrementAndGet();
	}

	@Override
	public int incrBy(String key, int integer)
	{
		if (integer < 0)
		{
			return integer;
		}
		if (!cache.containsKey(key))
		{
			synchronized (this)
			{
				if (!cache.containsKey(key))
				{
					set(key, new AtomicInteger(integer));
					return integer;
				}
			}
		}
		Object v = cache.get(key);
		if (!(v instanceof AtomicInteger))
		{
			throw new RuntimeException("not a AtomicInteger");
		}
		return ((AtomicInteger) v).addAndGet(integer);
	}

	@Override
	public int hset(String key, String subkey, Object value)
	{
		if (!cache.containsKey(key))
		{
			synchronized (this)
			{
				if (!cache.containsKey(key))
				{
					Map m = new HashMap();
					m.put(subkey, value);
					set(key, m);
					return 1;
				}
			}
		}
		Object v = cache.get(key);
		if (!(v instanceof Map))
		{
			throw new RuntimeException("not a map");
		}
		Map map = (Map) v;
		int result = map.containsKey(subkey) ? 0 : 1;
		map.put(subkey, value);
		return result;
	}

	@Override
	public int hset(String key, int subkey, Object value)
	{
		if (!cache.containsKey(key))
		{
			synchronized (this)
			{
				if (!cache.containsKey(key))
				{
					NewArrayList list = new NewArrayList();
					list.set(subkey, value);
					set(key, list);
					return 1;
				}
			}
		}
		Object v = cache.get(key);
		if (!(v instanceof NewArrayList))
		{
			throw new RuntimeException("not a NewArrayList");
		}
		NewArrayList list = (NewArrayList) v;
		int result = list.get(subkey) == null ? 0 : 1;
		list.set(subkey, value);
		return result;
	}

	@Override
	public int hset(String key, int subkey, int value)
	{
		if (!cache.containsKey(key))
		{
			synchronized (this)
			{
				if (!cache.containsKey(key))
				{
					NewIntArrayList list = new NewIntArrayList();
					list.set(subkey, value);
					set(key, list);
					return 1;
				}
			}
		}
		Object v = cache.get(key);
		if (!(v instanceof NewIntArrayList))
		{
			throw new RuntimeException("not a NewIntArrayList");
		}
		NewIntArrayList list = (NewIntArrayList) v;
		int result = list.get(subkey) == 0 ? 0 : 1;
		list.set(subkey, value);
		return result;
	}

	@Override
	public Object hget(String key, String subkey)
	{
		if (!cache.containsKey(key))
		{
			return null;
		}
		Object o = cache.get(key);
		if (!(o instanceof Map))
		{
			throw new RuntimeException("not a map");
		}
		Map m = (Map) o;
		return m.get(subkey);
	}

	@Override
	public Object hget(String key, int subkey)
	{
		if (!cache.containsKey(key))
		{
			return null;
		}
		Object v = cache.get(key);
		if (!(v instanceof NewArrayList))
		{
			throw new RuntimeException("not a NewArrayList");
		}
		NewArrayList nal = (NewArrayList) v;
		return nal.get(subkey);
	}

	@Override
	public int hgetitoi(String key, int subkey)
	{
		if (!cache.containsKey(key))
		{
			return Integer.MIN_VALUE;
		}
		Object v = cache.get(key);
		if (!(v instanceof NewIntArrayList))
		{
			throw new RuntimeException("not a NewIntArrayList");
		}
		NewIntArrayList list = (NewIntArrayList) v;
		return list.get(subkey);
	}

	@Override
	public int setbit(String key, int offset, boolean value)
	{
		BitSet bs = null;
		if (!cache.containsKey(key))
		{
			bs = new BitSet();
		}
		else
		{
			Object o = cache.get(key);
			if (!(o instanceof BitSet))
			{
				throw new RuntimeException("not a BitSet");
			}
			bs = (BitSet) o;
		}
		bs.set(offset, value);
		set(key, bs);
		return 0;
	}

	@Override
	public String dump()
	{
		StringBuilder sb = new StringBuilder("****begin dumping****\n<br />");
		Set<String> keys = keys();
		long size = 0;
		for (String key : keys)
		{
			//			System.out.println("*********************" + key);
			Object v = cache.get(key);
			sb.append("\n<br />key = ").append(key).append(", type = ").append(v.getClass()).append(", value is ");
			if (v instanceof String)
			{
				sb.append(v);
			}
			else if (v instanceof Set)
			{
				Set s = (Set) v;
				for (Object o : s)
				{
					sb.append(o).append(" ");
				}
				//				continue;
			}
			else if (v instanceof List)
			{
				List s = (List) v;
				for (Object o : s)
				{
					sb.append(o).append(" ");
				}
			}
			else if (v instanceof Map)
			{
				Map m = (Map) v;
				Set mkeys = m.keySet();
				for (Object mkey : mkeys)
				{
					sb.append(mkey).append(":").append(m.get(mkey)).append("; ");
				}
			}
			else
			{
				sb.append(v);
			}
			long kvSize = CalcSizeOf.calcSize(v);
			size += kvSize;
			sb.append("\n<br />size :" + kvSize + " bytes");
		}
		sb.append("\n<br /><br />****end dumping****<br />total keys: ").append(keys.size());
		sb.append("\n<br /><br />").append(size / 1024 + " kbytes");
		//		cache.dumpSize();
		return sb.toString();
	}

	@Override
	public Map<String, Long> dumpTTL()
	{
		return cache.dumpTTL();
	}

	@Override
	public int sadd(String key, String... values)
	{
		if (!cache.containsKey(key))
		{
			synchronized (this)
			{
				if (!cache.containsKey(key))
				{
					Set<String> s = new HashSet<String>();
					s.addAll(Arrays.asList(values));
					set(key, s);
					return values.length;
				}
			}
		}
		Object v = cache.get(key);
		if (!(v instanceof Set))
		{
			throw new RuntimeException("not a set");
		}
		Set<String> set = (Set) v;
		int i = 0;
		for (String val : values)
		{
			if (!set.contains(val))
			{
				set.add(val);
				i++;
			}
		}
		return i;
	}

	@Override
	public int srem(String key, String... members)
	{
		if (!cache.containsKey(key))
		{
			return 0;
		}
		Object v = cache.get(key);
		if (!(v instanceof Set))
		{
			throw new RuntimeException("not a set");
		}
		Set set = (Set) v;
		int i = 0;
		for (String member : members)
		{
			if (set.contains(member))
			{
				set.remove(member);
				i++;
			}
		}
		return i;
	}

	@Override
	public long scard(String key)
	{
		if (!cache.containsKey(key))
		{
			return 0;
		}
		Object v = cache.get(key);
		if (!(v instanceof Set))
		{
			throw new RuntimeException("not a set");
		}
		Set set = (Set) v;
		return set.size();
	}

	@Override
	public Set smembers(String key)
	{
		if (!cache.containsKey(key))
		{
			return Collections.EMPTY_SET;
		}
		Object v = cache.get(key);
		if (!(v instanceof Set))
		{
			throw new RuntimeException("not a set");
		}
		return (Set) v;
	}

	@SuppressWarnings(
	{ "rawtypes", "unchecked" })
	private void typeCheck(Object val)
	{
		for (Class c : validClasses)
		{
			if (c.isAssignableFrom(val.getClass()))
				return;
		}
		//		throw new Exception("invalid type " + val.getClass());
	}

	@Override
	public boolean containsKey(String key)
	{
		return cache.containsKey(key);
	}
}
