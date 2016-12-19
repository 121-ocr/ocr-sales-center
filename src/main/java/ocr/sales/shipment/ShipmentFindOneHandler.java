package ocr.sales.shipment;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.common.BizRoleDirection;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.CDOHandlerImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * TODO: 发货单
 * @date 2016年11月15日
 * @author lijing
 */
public class ShipmentFindOneHandler extends CDOHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "findone";

	public ShipmentFindOneHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ADDRESS;
	}

	//处理器
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		
		JsonObject queryParams = msg.body();
		
		String boId = queryParams.getString("bo_id");
	    
	    this.queryLatestFactData(appActivity.getBizObjectType(), boId, null, null, findRet->{
	        if (findRet.succeeded()) {
	            //msg.reply(findRet.result());	  
	        	JsonObject bo = findRet.result();
				String partner = bo.getString("partner");				
				
				this.queryLatestCDO(BizRoleDirection.FROM, partner, appActivity.getBizObjectType(), 
						boId, null, cdoRet->{
							if (findRet.succeeded()) {
								msg.reply(cdoRet.result());
							}else{
								Throwable errThrowable = cdoRet.cause();
								String errMsgString = errThrowable.getMessage();
								appActivity.getLogger().error(errMsgString, errThrowable);
								msg.fail(100, errMsgString);
							}
						});
	            
	            
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
