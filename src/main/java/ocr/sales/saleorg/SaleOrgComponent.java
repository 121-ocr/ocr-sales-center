package ocr.sales.saleorg;

import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * 销售组织
 * 
 * @date 2016年11月20日
 * @author LCL
 */
public class SaleOrgComponent extends AppActivityImpl {

	// 业务活动组件名
	@Override
	public String getName() {
		return "saleorg-mgr";
	}

	// 业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return "ba_saleorg";
	}


	// 发布此业务活动对外暴露的业务事件
	@Override
	public List<OtoCloudEventDescriptor> exposeOutboundBizEventsDesc() {
		return null;
	}

	// 业务活动组件中的业务功能
	@Override
	public List<OtoCloudEventHandlerRegistry> registerEventHandlers() {

		List<OtoCloudEventHandlerRegistry> ret = new ArrayList<OtoCloudEventHandlerRegistry>();

		SaleOrgCreateHandler createHandler = new SaleOrgCreateHandler(this);
		ret.add(createHandler);
		
		SaleOrgQueryHandler saleOrgQueryHandler = new SaleOrgQueryHandler(this);
		ret.add(saleOrgQueryHandler);


		return ret;
	}

}
