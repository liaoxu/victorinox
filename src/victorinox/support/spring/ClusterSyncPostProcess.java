package victorinox.support.spring;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import victorinox.annotation.ClusterSync;
import victorinox.clustersync.ZKCandidate;
import victorinox.clustersync.ZKConf;
import victorinox.clustersync.ZkPool;

/**
 * 
 * @author liaoxu
 *
 */
public class ClusterSyncPostProcess implements BeanPostProcessor,
		ApplicationContextAware {

	
	private final Log logger = LogFactory.getLog(getClass());
	private ApplicationContext context;
	String znodePrefix = "cluster_lock";
	ZKConf zkConf = null;
	
	@Override
	public void setApplicationContext(ApplicationContext context)
			throws BeansException {
		this.context = context;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		Method[] methods = bean.getClass().getMethods();
		for (Method method : methods)
		{
			Annotation annotation = method.getAnnotation(ClusterSync.class);
			if (null == annotation)
			{
				continue;
			}
			ClusterSync clusterSync = (ClusterSync) annotation;
			try
			{
				makeCandidateNode(clusterSync);
			}
			catch (Exception e)
			{
				logger.fatal(e.getMessage(), e);
			}
		}
		return bean;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	private ZKCandidate makeCandidateNode(ClusterSync clusterSync) throws Exception
	{
		// 取得生成zkClient的三要素
		String rootDirectory = clusterSync.path();
		if (zkConf == null)
		{
			//从服务器获取
			zkConf = (ZKConf) context.getBean("zkConf");
		}

		ZKCandidate zkCandidate = new ZKCandidate();
		zkCandidate.setConf(zkConf);
		zkCandidate.setRootDirectory(rootDirectory);
		zkCandidate.setZnodePrefix(znodePrefix);
		zkCandidate.init();
		ZkPool.add(rootDirectory, znodePrefix, zkCandidate);
		return zkCandidate;
	}
	
	@Around("@annotation(victorinox.annotation.ClusterSync) && @annotation(clusterSync)")
	public Object doBeforeZkMethod(ProceedingJoinPoint pjp, ClusterSync clusterSync) throws Throwable{
		String rootDirectory = clusterSync.path();
//		System.out.println("doBeforeZkMethod, " + rootDirectory + ", " + znodePrefix);
		ZKCandidate zkClient = (ZKCandidate) ZkPool.get(rootDirectory, znodePrefix);
		if (null == zkClient || !zkClient.isAlive())
		{
			try
			{
				zkClient = makeCandidateNode(clusterSync);
			}
			catch (Exception e)
			{
				logger.fatal(e.getMessage(), e);
				throw e;
			}
		}
		if (null != zkClient && zkClient.isAlive() && zkClient.isMaster())
		{
			int count = zkClient.getAcessCount();
			zkClient.setAcessCount(count + 1);
			return pjp.proceed();
		}
		return null;
	}
	
}
