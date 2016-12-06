package ocr.sales.shippingadvise;


import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRoleDescriptor;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * TODO: 发货通知
 * @date 2016年11月15日
 * @author lijing
 */
public class ShippingAdviseComponent extends AppActivityImpl {

	
/*	@Override 
    public void start(Future<Void> startFuture) throws Exception {
		Future<Void> innerFuture = Future.future();
		super.start(innerFuture);
		innerFuture.setHandler(handler->{			
		
			ShippingAdviseNoticeHandler shippingAdviseNoticeHandler = new ShippingAdviseNoticeHandler(this);
			String inventorycenterSrvName = this.getDependencies().getJsonObject("inventorycenter_service").getString("service_name","");
			String address = inventorycenterSrvName + ".stockout-mgr." + BizStateSwitchDesc.buildStateSwitchEventAddress("bp_stockout", "pickouted", "shippingouted");
			this.getEventBus().consumer(address, shippingAdviseNoticeHandler::handle);	
			
			startFuture.complete();	
			
		});		
		
	}
*/
	@Override
	public String getName() {		
		return "shipping-advise";
	}

	@Override
	public String getBizObjectType() {
		// TODO Auto-generated method stub
		return "";
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

		List<OtoCloudEventHandlerRegistry> ret = new ArrayList<OtoCloudEventHandlerRegistry>();

		ShippingAdviseNoticeHandler shippingAdviseNoticeHandler = new ShippingAdviseNoticeHandler(this);
		ret.add(shippingAdviseNoticeHandler);
		
		return ret;
	}

}
