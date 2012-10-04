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

	@Around(ANNOTATION)
	public Object cacheMethodWrapper(ProceedingJoinPoint pjp, Cached cached) {
		pjp.getSignature();
		return pjp.proceed();
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		// TODO Auto-generated method stub
		return null;
	}

}
