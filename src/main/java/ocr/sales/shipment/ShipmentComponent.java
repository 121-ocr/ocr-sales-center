package ocr.sales.shipment;

import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRoleDescriptor;
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

		ShipmentCreateHandler shipMentCreateHandler = new ShipmentCreateHandler(this);
		ret.add(shipMentCreateHandler);
		
		ShipmentQueryHandler shipmentQueryHandler = new ShipmentQueryHandler(this);
		ret.add(shipmentQueryHandler);
		
		ShipmentFindOneHandler shipmentFindOneHandler = new ShipmentFindOneHandler(this);
		ret.add(shipmentFindOneHandler);
		
		ShipmentCompleteHandler shipmentCompleteHandler = new ShipmentCompleteHandler(this);
		ret.add(shipmentCompleteHandler);
		
		return ret;
	}

}
