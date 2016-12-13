package ocr.sales.channelrestocking;


import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRoleDescriptor;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * TODO: 渠道补货
 * @date 2016年11月15日
 * @author lijing
 */
public class ChannelRestockingComponent extends AppActivityImpl {

	//业务活动组件名
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "channel-restocking";
	}
	
	//业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		// TODO Auto-generated method stub
		return "bp_replenishments";
	}

	//发布此业务活动关联的业务角色
	@Override
	public List<BizRoleDescriptor> exposeBizRolesDesc() {
		// TODO Auto-generated method stub
		BizRoleDescriptor bizRole = new BizRoleDescriptor("2", "核心企业");
		
		List<BizRoleDescriptor> ret = new ArrayList<BizRoleDescriptor>();
		ret.add(bizRole);
		return ret;
	}

	//发布此业务活动对外暴露的业务事件
	@Override
	public List<OtoCloudEventDescriptor> exposeOutboundBizEventsDesc() {
		// TODO Auto-generated method stub
		return null;
	}


	//业务活动组件中的业务功能
	@Override
	public List<OtoCloudEventHandlerRegistry> registerEventHandlers() {
		// TODO Auto-generated method stub
		List<OtoCloudEventHandlerRegistry> ret = new ArrayList<OtoCloudEventHandlerRegistry>();
		
		ReplenishmentQuantityUpdateHandler replenishmentQuantityUpdateHandler = new ReplenishmentQuantityUpdateHandler(this);
		ret.add(replenishmentQuantityUpdateHandler);

		ChannelRestockingHandler channelRestockingHandler = new ChannelRestockingHandler(this);
		ret.add(channelRestockingHandler);
		
		ChannelRestockingQueryHandler channelRestockingQueryHandler = new ChannelRestockingQueryHandler(this);
		ret.add(channelRestockingQueryHandler);
		
		ChannelRestockingRemoveHandler channelRestockingRemoveHandler = new ChannelRestockingRemoveHandler(this);
		ret.add(channelRestockingRemoveHandler);
		
		ChannelRestockingBatchCreateHandler channelRestockingBatchCreateHandler = new ChannelRestockingBatchCreateHandler(this);
		ret.add(channelRestockingBatchCreateHandler);
		
		ChannelRestockingCommitHandler channelRestockingCommitHandler = new ChannelRestockingCommitHandler(this);
		ret.add(channelRestockingCommitHandler);
		
		ChannelRestockingFindOneHandler channelRestockingFindOneHandler = new ChannelRestockingFindOneHandler(this);
		ret.add(channelRestockingFindOneHandler);
		
		ChannelRestockingQuery4ReadyShipHandler channelRestockingQuery4ReadyShipHandler = new ChannelRestockingQuery4ReadyShipHandler(this);
		ret.add(channelRestockingQuery4ReadyShipHandler);
		
		ChannelRestockingShipHandler channelRestockingShipHandler = new ChannelRestockingShipHandler(this);
		ret.add(channelRestockingShipHandler);
		
		ChannelRestockingCommit2ShippingHandler channelRestockingCommit2ShippingHandler = new ChannelRestockingCommit2ShippingHandler(this);
		ret.add(channelRestockingCommit2ShippingHandler);
		
		ChannelRestockingShipping2ShippingHandler channelRestockingShipping2ShippingHandler = new ChannelRestockingShipping2ShippingHandler(this);
		ret.add(channelRestockingShipping2ShippingHandler);
		
		return ret;
	}

}
