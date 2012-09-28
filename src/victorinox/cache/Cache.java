package victorinox.cache;

import java.util.Map;
import java.util.Set;

public interface Cache
{

	boolean containsKey(String key);

	/**
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	int sadd(String key, String... value);

	/**
	 * 
	 * @param keys
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	Set smembers(String key);

	/**
	 * 
	 * @return
	 */
	Set<String> keys();

	/**
	 * 
	 * @return
	 */
	int incr(String key);

	/**
	 * 
	 * @param key
	 * @param integer
	 * @return
	 */
	int incrBy(String key, int integer);

	/**
	 * 
	 * @param key
	 * @param value
	 */
	void set(String key, Object value);

	/**
	 * 
	 * @param key
	 * @param value
	 * @param expireAt
	 */
	void set(String key, Object value, long expireAt);

	/**
	 * 
	 * @param key
	 * @param subkey
	 * @param value
	 */
	int hset(String key, String subkey, Object value);

	/**
	 * 
	 * @param key
	 * @param subkey
	 * @param value
	 * @return
	 */
	int hset(String key, int subkey, Object value);

	/**
	 * 
	 * @param key
	 * @param subkey
	 * @param value
	 * @return
	 */
	int hset(String key, int subkey, int value);

	/**
	 * 
	 * @param key
	 * @param subkey
	 * @return
	 */
	Object hget(String key, String subkey);

	/**
	 * 
	 * @param key
	 * @param subkey
	 * @return
	 */
	Object hget(String key, int subkey);

	/**
	 * 
	 * @param key
	 * @param subkey
	 * @return
	 */
	int hgetitoi(String key, int subkey);

	/**
	 * 
	 * @param key
	 * @return
	 */
	long scard(String key);

	/**
	 * 
	 * @param key
	 * @param offset
	 * @param value
	 * @return
	 */
	int setbit(String key, int offset, boolean value);

	/**
	 * 
	 * @param key
	 * @return
	 */
	Object get(String key);

	/**
	 * 
	 */
	String dump();

	/**
	 * 
	 * @return
	 */
	Map<String, Long> dumpTTL();

	/**
	 * 
	 */
	void flushall();

	/**
	 * 
	 * @param key
	 */
	void del(String key);

	/**
	 * 
	 * @param key
	 * @param members
	 * @return
	 */
	int srem(String key, String... members);
}
