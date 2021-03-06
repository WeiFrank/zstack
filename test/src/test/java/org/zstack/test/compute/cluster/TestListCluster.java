package org.zstack.test.compute.cluster;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;

import java.util.ArrayList;
import java.util.List;
public class TestListCluster {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ClusterManager.xml").addXml("ZoneManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

	@Test
	public void test() throws ApiSenderException {
        try {
            ZoneInventory zone = api.createZones(1).get(0);
            api.createClusters(10, zone.getUuid());
            List<ClusterInventory> invs = api.listClusters(null);
            Assert.assertEquals(10, invs.size());
            List<String> uuids = new ArrayList<String>(5);
            for (int i=0; i<uuids.size(); i++) {
                uuids.add(invs.get(i).getUuid());
            }
            invs = api.listClusters(uuids);
            for (int i=0; i<uuids.size(); i++) {
                Assert.assertEquals(uuids.get(i), invs.get(i).getUuid());
            }
        } finally {
            api.stopServer();
        }
	}

}
