package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created by luchukun on 10/29/16.
 */
public class SuspendVmOnHypervisorMsg extends NeedReplyMessage implements HostMessage{
    private VmInstanceInventory vmInventory;

    public VmInstanceInventory getVmInventory() {
        return vmInventory;
    }

    public void setVmInventory(VmInstanceInventory vmInventory){
        this.vmInventory = vmInventory;
    }
    @Override
    public String getHostUuid(){
        return vmInventory.getHostUuid();
    }
}
