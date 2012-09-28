package victorinox.clustersync;

import org.apache.zookeeper.ZooKeeper;

public class ZKCliFactory
{
	private static ZKCli zkCli;

	public void setZkCli(ZKCli zk)
	{
		zkCli = zk;
	}

	public static ZooKeeper getZK()
	{
		return zkCli.getZk();
	}

}
