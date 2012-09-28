package victorinox.clustersync;

public interface ZKClient
{

	void doSth();

	void doFollowerThings();

	boolean isMaster();

	boolean isAlive();

	void init() throws Exception;

	void destroy();
}
