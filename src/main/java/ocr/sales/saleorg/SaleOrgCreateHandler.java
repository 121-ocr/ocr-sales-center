package ocr.sales.saleorg;


import io.vertx.core.http.HttpMethod;
import ocr.common.handler.SampleSingleDocBaseHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;
/**
 * 营销中心：销售组织创建
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//销售组织创建
public class SaleOrgCreateHandler  extends SampleSingleDocBaseHandler {
	
	public static final String ADDRESS = "create";
	
	public SaleOrgCreateHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override 
	public String getEventAddress() {
		return ADDRESS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		// 外部访问url定义
		ActionURI uri = new ActionURI(getEventAddress(), HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		// 状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, null, ADDRESS);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}
	
	
}
