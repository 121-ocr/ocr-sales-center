package ocr.sales.shipment;

import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * 发货单组件
 * @author wanghw
 *
 */
public class ShipmentComponent extends AppActivityImpl {

	//业务活动组件名
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "shipment";
	}
	
	//业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		// TODO Auto-generated method stub
		return "bp_shipment";
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

		ShipmentCreateHandler shipMentCreateHandler = new ShipmentCreateHandler(this);
		ret.add(shipMentCreateHandler);
		
		ShipmentCreatedQueryHandler shipmentQueryHandler = new ShipmentCreatedQueryHandler(this);
		ret.add(shipmentQueryHandler);
		
		ShipmentCompletedQueryHandler shipmentCompletedQueryHandler = new ShipmentCompletedQueryHandler(this);
		ret.add(shipmentCompletedQueryHandler);
		
		ShipmentFindOneHandler shipmentFindOneHandler = new ShipmentFindOneHandler(this);
		ret.add(shipmentFindOneHandler);
		
		ShipmentCompleteHandler shipmentCompleteHandler = new ShipmentCompleteHandler(this);
		ret.add(shipmentCompleteHandler);
		
		return ret;
	}

}
