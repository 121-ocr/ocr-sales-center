package ocr.sales.customer;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.CDOHandlerImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 发货单创建操作
 * 
 * @author lj
 *
 */
public class CustomerUpdateHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String CREATE_ADDRESS = "update";

	public CustomerUpdateHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}


	@Override
	public String getEventAddress() {
		return CREATE_ADDRESS;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
				
		ActionURI uri = new ActionURI(CREATE_ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
			
		return actionDescriptor;
	}

	/***
	 * {
	 * 	  code:
	 *    name:
	 *    level：等级
	 *    trade: 所属行业
	 *    customer_acct: 客户租户 
	 * }
	 */
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
	
		JsonObject customer = msg.body().getJsonObject("content");    	
		Long customer_acct = customer.getLong("customer_acct", -1L);
		
		Long acctId = Long.parseLong(this.appActivity.getAppInstContext().getAccount());

		this.appActivity.getAppDatasource().getMongoClient_oto().save(
				appActivity.getDBTableName(appActivity.getBizObjectType()), customer, result -> {
			if (result.succeeded()) {
				
				if(customer_acct > 0L){				
					String authSrvName = componentImpl.getDependencies().getJsonObject("otocloud-acct").getString("service_name","");
					String address = authSrvName + ".acct-relation.create";
	
					JsonObject acctRelJsonObject = new JsonObject()
							.put("from_acct_id", acctId)
							.put("to_acct_id", customer_acct)
							.put("desc", "供货关系");
					JsonObject sendMsg = new JsonObject().put("content", acctRelJsonObject);
					
					
					componentImpl.getEventBus().send(address,
							sendMsg, acctRelationRet->{
								if(acctRelationRet.succeeded()){
									msg.reply(result.result());
								}else{		
									Throwable err = acctRelationRet.cause();						
									err.printStackTrace();		
									msg.fail(100, err.getMessage());
								}	
								
					});	
				}else{
					msg.reply(result.result());
				}
				
			} else {
				Throwable errThrowable = result.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.fail(100, errMsgString);
			}
		});	

	}

}
