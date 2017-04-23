package ocr.sales.channelrestocking;


import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
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

	
		QueryShippedHandler queryShippedHandler = new QueryShippedHandler(this);
		ret.add(queryShippedHandler);
		
	
		ChannelRestockingBatchCreateHandler channelRestockingBatchCreateHandler = new ChannelRestockingBatchCreateHandler(this);
		ret.add(channelRestockingBatchCreateHandler);
		
		ChannelRestockingCommitHandler channelRestockingCommitHandler = new ChannelRestockingCommitHandler(this);
		ret.add(channelRestockingCommitHandler);
		
	
		ChannelRestockingQuery4AcceptShipHandler channelRestockingQuery4AcceptShipHandler = new ChannelRestockingQuery4AcceptShipHandler(this);
		ret.add(channelRestockingQuery4AcceptShipHandler);
		
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
