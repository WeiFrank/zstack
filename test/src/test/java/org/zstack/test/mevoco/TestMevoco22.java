package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.rest.BeforeAsyncJsonPostInterceptor;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.KVMAgentCommands.AttachNicCommand;
import org.zstack.mevoco.KVMAddOns.NicQos;
import org.zstack.mevoco.MevocoConstants;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.network.service.flat.FlatUserdataBackend.ApplyUserdataCmd;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * 1. create a vm with user data
 * 2. delete the l3 network
 * 3. attach a new l3 network
 *
 * confirm the user data applied on the new l3 network
 *
 */
public class TestMevoco22 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    RESTFacade restf;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);
    String userdata = "hello, world";

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/mevoco/TestMevoco21.xml", con);
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        fconfig = loader.getComponent(FlatNetworkServiceSimulatorConfig.class);
        restf = loader.getComponent(RESTFacade.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);
        config.capacityMap.put("host2", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

	@Test
	public void test() throws ApiSenderException, InterruptedException {
        ImageInventory img = deployer.images.get("TestImage");

        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        final L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        VmCreator creator = new VmCreator(api);
        creator.imageUuid = img.getUuid();
        creator.session = api.getAdminSession();
        creator.instanceOfferingUuid = ioinv.getUuid();
        creator.name = "vm";
        creator.systemTags.add(VmSystemTags.USERDATA.instantiateTag(map(e(VmSystemTags.USERDATA_TOKEN, userdata))));
        creator.addL3Network(l3.getUuid());
        VmInstanceInventory vm = creator.create();

        fconfig.applyUserdataCmds.clear();
        final L3NetworkInventory l32 = deployer.l3Networks.get("TestL3Network2");
        api.deleteL3Network(l3.getUuid());

        final Map<String, NicQos> qos = new HashMap<String, NicQos>();
        restf.installBeforeAsyncJsonPostInterceptor(new BeforeAsyncJsonPostInterceptor() {
            @Override
            public void beforeAsyncJsonPost(String url, Object body, TimeUnit unit, long timeout) {
                if (body instanceof AttachNicCommand) {
                    AttachNicCommand cmd = (AttachNicCommand) body;
                    qos.putAll((Map<? extends String, ? extends NicQos>) cmd.getAddons().get(MevocoConstants.KVM_NIC_QOS));
                }
            }

            @Override
            public void beforeAsyncJsonPost(String url, String body, TimeUnit unit, long timeout) {
            }
        });


        vm = api.attachNic(vm.getUuid(), l32.getUuid());
        VmNicInventory nic = CollectionUtils.find(vm.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory nic) {
                return nic.getL3NetworkUuid().equals(l32.getUuid()) ? nic : null;
            }
        });

        NicQos in = qos.get(nic.getUuid());
        Assert.assertNotNull(in);
        Assert.assertEquals(1000, in.outboundBandwidth.intValue());
        Assert.assertEquals(100, in.inboundBandwidth.intValue());

        VmInstanceVO vo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vo.getUuid());
        Assert.assertEquals(1, fconfig.applyUserdataCmds.size());
        ApplyUserdataCmd cmd = fconfig.applyUserdataCmds.get(0);
        Assert.assertEquals(userdata, cmd.userdata.userdata);
    }
}
