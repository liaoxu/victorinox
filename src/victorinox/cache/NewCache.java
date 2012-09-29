package victorinox.cache;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import victorinox.util.CalcSizeOf;

/**
 * 
 * @author KevinLiao
 *
 */
public class NewCache
{
	private static final long THRESHOLD = 60 * 1000; // 每次trigger查询的时间范围，单位s
	private final Map<String, Object> cacheMap = new ConcurrentHashMap<String, Object>();
	private final DelayQueue<DelayKey> delayQueue = new DelayQueue<DelayKey>();

	private long currentNextFireTime = Long.MAX_VALUE;

	@SuppressWarnings("rawtypes")
	private Future future = null;
	private ExecutorService exec = null;

	/**
	 * 初始化方法，加载trigger
	 */
	@PostConstruct
	public void init()
	{
		exec = Executors.newCachedThreadPool();
		future = exec.submit(new Monitor());
	}

	@PreDestroy
	public void destroy()
	{
		future.cancel(true);
		exec.shutdown();
	}

	/**
	 * 
	 * @author KevinLiao
	 *
	 */
	class DelayKey implements Delayed
	{
		String key;
		long expireAt;

		private DelayKey(String key)
		{
			this.key = key;
		}

		private DelayKey(String key, long expireAt)
		{
			this.key = key;
			this.expireAt = expireAt;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((key == null) ? 0 : key.hashCode());
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
			DelayKey other = (DelayKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (key == null)
			{
				if (other.key != null)
					return false;
			}
			else if (!key.equals(other.key))
				return false;
			return true;
		}

		@Override
		public int compareTo(Delayed o)
		{
			if (o == this)
			{
				return 0;
			}
			long diff = this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS);
			return ((diff == 0) ? 0 : ((diff < 0) ? -1 : 1));
		}

		@Override
		public long getDelay(TimeUnit unit)
		{
			return unit.convert(expireAt - System.currentTimeMillis() - 2, unit);
		}

		@Override
		public String toString()
		{
			return "ExpireKey [key=" + key + ", expireAt=" + expireAt + "]";
		}

		private NewCache getOuterType()
		{
			return NewCache.this;
		}

	}

	/**
	 * 
	 * @author KevinLiao
	 *
	 */
	class Monitor implements Runnable
	{
		@Override
		public void run()
		{
			boolean ifDone = false;
			// 当两者其一条件满足时，终止线程
			while (!Thread.interrupted() && !ifDone)
			{
				//				System.out.println("run");
				try
				{
					long nextFireTime = getNextFireTime();
					//					System.out.println("next fire at " + nextFireTime + " ms later.");
					TimeUnit.MILLISECONDS.sleep(nextFireTime);
					DelayKey dk = null;
					do
					{
						dk = delayQueue.poll(500, TimeUnit.MILLISECONDS);
						if (dk != null)
						{
							//							System.out.println("expire cache " + dk.key);
							del(dk.key);
						}
					}
					while (dk != null && delayQueue.size() > 0 && getNextFireTime() > 500);
				}
				catch (InterruptedException e)
				{
					ifDone = true;
					System.out.println("interrupted");
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	private long getNextFireTime()
	{
		DelayKey dk = delayQueue.peek();
		//		System.out.println("next fire time for key " + (dk == null ? "none" : dk.key));
		if (dk == null)
		{
			currentNextFireTime = System.currentTimeMillis() + THRESHOLD;
			return THRESHOLD;
		}
		currentNextFireTime = dk.expireAt;
		return dk.getDelay(TimeUnit.MILLISECONDS);
	}

	private void putNewDelay(String key, long expireAt)
	{
		DelayKey dk = new DelayKey(key, expireAt);
		delayQueue.put(dk);
		// 如果比最近一个remove时间要近，则打断monitor，重新开始指定时间表
		if (expireAt < currentNextFireTime)
		{
			//			System.out.println("interrupt with key " + key);
			future.cancel(true);
			future = exec.submit(new Monitor());
		}
	}

	/**
	 * 查看key是否存在
	 * @param key
	 * @return
	 */
	public boolean containsKey(String key)
	{
		return cacheMap.containsKey(key);
	}

	/**
	 * 获取key值
	 * @param key
	 * @return
	 */
	public Object get(String key)
	{
		return cacheMap.get(key);
	}

	/**
	 * 
	 * @param key
	 * @param value
	 */
	public void set(String key, Object value)
	{
		cacheMap.put(key, value);
	}

	/**
	 * 设置key值
	 * @param key
	 * @param value
	 * @param expireAt 过期时间的long值，如果没有则使用0
	 */
	public void set(String key, Object value, long expireAt)
	{
		//		System.out.println("set key " + key + ", expireAt"
		//				+ (expireAt == 0 ? 0 : (expireAt - System.currentTimeMillis())) + " ms later");
		if (null == value || expireAt < 0 || expireAt < System.currentTimeMillis())
		{
			return;
		}
		set(key, value);
		// 加入过期队列
		putNewDelay(key, expireAt);
	}

	/**
	 * 设置key值
	 * @param key
	 * @param value
	 * @param expireAt
	 */
	public void set(String key, Object value, Timestamp expireAt)
	{
		set(key, value, expireAt.getTime());
	}

	/**
	 * 
	 * @param key
	 */
	public void del(String key)
	{
		cacheMap.remove(key);
	}

	public void flushall()
	{
		cacheMap.clear();
		delayQueue.clear();
	}

	public void dumpDelayQueue()
	{
		System.out.println("-----dump start-----");
		Iterator<DelayKey> iter = delayQueue.iterator();
		while (iter.hasNext())
		{
			DelayKey dk = iter.next();
			System.out.println("key " + dk.key + " expireAt " + dk.getDelay(TimeUnit.MILLISECONDS) + " ms later, "
					+ dk.expireAt);
		}
		System.out.println("-----dump end-----");
	}

	public boolean contains(String key)
	{
		return cacheMap.containsKey(key);
	}

	public Set<String> keys()
	{
		return cacheMap.keySet();
	}

	public Map<String, Long> dumpTTL()
	{
		Map<String, Long> result = new HashMap<String, Long>();
		Iterator<DelayKey> iter = delayQueue.iterator();
		while (iter.hasNext())
		{
			DelayKey dk = iter.next();
			result.put(dk.key, dk.getDelay(TimeUnit.MILLISECONDS) + System.currentTimeMillis());
		}
		return result;
	}

	public String dumpSize()
	{
		return new StringBuilder("total size " + (CalcSizeOf.calcSize(cacheMap) / 1024) + " kbytes").toString();
	}
}
