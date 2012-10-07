package victorinox.blocks;

public class CacheResult<T> {
	T result;
	long expireAt;

	public CacheResult(T result, long expireAt) {
		this.result = result;
		this.expireAt = expireAt;
	}

}
