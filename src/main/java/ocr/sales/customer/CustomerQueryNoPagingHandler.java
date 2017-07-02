package ocr.sales.customer;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.CommandMessage;
import otocloud.framework.core.HandlerDescriptor;

/**
 * TODO: 渠道列表查询
 * @date 2016年11月15日
 * @author lijing
 */
public class CustomerQueryNoPagingHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "findall-nopaging";

	public CustomerQueryNoPagingHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ADDRESS;
	}

    /**
     * 查询所有客户
     */
	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		
	    this.queryBizDataList(null, appActivity.getBizObjectType(),  null, findRet -> {
	        if (findRet.succeeded()) {
	            msg.reply(findRet.result());
	        } else {
				Throwable errThrowable = findRet.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.fail(100, errMsgString);		
	        }

	    });

	}
	

	/**
	 * 此action的自描述元数据
	 */
	@Override
	public ActionDescriptor getActionDesc() {		
		
		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
		//handlerDescriptor.setMessageFormat("command");
		
		//参数
/*		List<ApiParameterDescriptor> paramsDesc = new ArrayList<ApiParameterDescriptor>();
		paramsDesc.add(new ApiParameterDescriptor("targetacc",""));		
		paramsDesc.add(new ApiParameterDescriptor("soid",""));		
		
		actionDescriptor.getHandlerDescriptor().setParamsDesc(paramsDesc);	*/
				
		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		return actionDescriptor;
	}
	
	
}
