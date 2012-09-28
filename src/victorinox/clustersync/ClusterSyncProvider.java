package victorinox.clustersync;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.context.ApplicationContext;

import victorinox.annotation.ClusterSync;

public interface ClusterSyncProvider
{
	public Object doBefore(ApplicationContext context, ProceedingJoinPoint pjp, ClusterSync clusterSync)
			throws Throwable;

	public void process(ApplicationContext context, ClusterSync clusterSync);
}
