package ocr.sales.customer;

import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * 客户档案
 * @author lijing
 *
 */
public class CustomerComponent extends AppActivityImpl {

	//业务活动组件名
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "customer-mgr";
	}
	
	//业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		// TODO Auto-generated method stub
		return "ba_customer";
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

		CustomerCreateHandler customerCreateHandler = new CustomerCreateHandler(this);
		ret.add(customerCreateHandler);
		
		CustomerQueryHandler customerQueryHandler = new CustomerQueryHandler(this);
		ret.add(customerQueryHandler);
		
		CustomerUpdateHandler customerUpdateHandler = new CustomerUpdateHandler(this);
		ret.add(customerUpdateHandler);
		
		CustomerQueryNoPagingHandler customerQueryNoPagingHandler = new CustomerQueryNoPagingHandler(this);
		ret.add(customerQueryNoPagingHandler);
		
		return ret;
	}

}
