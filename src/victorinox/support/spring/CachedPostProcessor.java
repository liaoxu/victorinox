package victorinox.support.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import victorinox.annotation.Cached;
import victorinox.blocks.CacheResult;
import victorinox.cache.Cache;

public class CachedPostProcessor implements BeanPostProcessor,
		ApplicationContextAware {

	private static final String ANNOTATION = "@annotation(victorinox.annotation.Cached) && @annotation(cached)";

	private final Log logger = LogFactory.getLog(getClass());
	private ApplicationContext context;
	private Cache cache;

	@Override
	public void setApplicationContext(ApplicationContext context)
			throws BeansException {
		this.context = context;
	}

	public void setCache(Cache cache) {
		this.cache = cache;
	}

	@SuppressWarnings("rawtypes")
	@Around(ANNOTATION)
	public CacheResult cacheMethodWrapper(ProceedingJoinPoint pjp, Cached cached) {
		// get the key of cache
		String key = pjp.getSignature().toLongString();
		if (cache.containsKey(key)) {
			return (CacheResult) cache.get(key);
		}
		try {
			CacheResult result = (CacheResult) pjp.proceed();
			cache.set(key, result);
			return result;
		} catch (Throwable e) {
			logger.error("error occurs while wrap cacheMethod annotation", e);
			return null;
		}
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

}
