package org.zstack.network.service.portforwarding;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by frank on 6/15/2015.
 */
@Action(category = PortForwardingConstant.ACTION_CATEGORY)
public class APIUpdatePortForwardingRuleMsg extends APIMessage {
    @APIParam(resourceType = PortForwardingRuleVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
