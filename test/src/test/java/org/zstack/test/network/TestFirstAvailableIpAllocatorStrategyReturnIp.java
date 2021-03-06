package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.*;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;

import java.util.concurrent.TimeUnit;

public class TestFirstAvailableIpAllocatorStrategyReturnIp {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    CloudBus bus;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ZoneManager.xml").addXml("NetworkManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        bus = loader.getComponent(CloudBus.class);
        api = new Api();
        api.startServer();
    }

    @After
    public void tearDown() throws Exception {
        api.stopServer();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        ZoneInventory zone = api.createZones(1).get(0);
        L2NetworkInventory linv = api.createNoVlanL2Network(zone.getUuid(), "eth0");
        L3NetworkInventory l3inv = api.createL3BasicNetwork(linv.getUuid());
        L3NetworkVO vo = dbf.findByUuid(l3inv.getUuid(), L3NetworkVO.class);
        Assert.assertNotNull(vo);
        IpRangeInventory ipInv = api.addIpRange(l3inv.getUuid(), "10.223.110.10", "10.223.110.20", "10.223.110.1", "255.255.255.0");
        IpRangeVO ipvo = dbf.findByUuid(ipInv.getUuid(), IpRangeVO.class);
        Assert.assertNotNull(ipvo);
        
        AllocateIpMsg msg = new AllocateIpMsg();
        msg.setL3NetworkUuid(l3inv.getUuid());
        msg.setServiceId(bus.makeLocalServiceId(L3NetworkConstant.SERVICE_ID));
        msg.setAllocateStrategy(L3NetworkConstant.FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY);
        AllocateIpReply reply = (AllocateIpReply) bus.call(msg);
        UsedIpInventory uinv = reply.getIpInventory();
        Assert.assertEquals("10.223.110.10", uinv.getIp());
        ReturnIpMsg rmsg = new ReturnIpMsg();
        rmsg.setL3NetworkUuid(l3inv.getUuid());
        rmsg.setUsedIpUuid(uinv.getUuid());
        rmsg.setServiceId(bus.makeLocalServiceId(L3NetworkConstant.SERVICE_ID));
        bus.send(rmsg);
        TimeUnit.SECONDS.sleep(1);
        UsedIpVO uvo = dbf.findByUuid(uinv.getUuid(), UsedIpVO.class);
        Assert.assertEquals(null, uvo);
    }
}
