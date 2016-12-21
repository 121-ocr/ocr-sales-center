package ocr.sales.shipment;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionContextTransfomer;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.app.function.CDOHandlerImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 发货单完成操作
 * 
 * @author wanghw
 *
 */
public class ShipmentCompleteHandler extends CDOHandlerImpl<JsonObject> {

	public ShipmentCompleteHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override
	public String getEventAddress() {
		return ShipmentConstant.COMPLETE_ADDRESS;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
				
		ActionURI uri = new ActionURI(getEventAddress(), HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		//状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, 
				ShipmentConstant.CREATE_STATUS, ShipmentConstant.COMPLETE_STATUS);
		bizStateSwitchDesc.setWebExpose(true); //是否向web端发布事件		
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);
		
		return actionDescriptor;
	}

	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		MultiMap headerMap = msg.headers();
		
		JsonObject body = msg.body();
		JsonObject shipmentBo = body.getJsonObject("bo");
		
    	String boId = body.getString("bo_id");
    	
    	String partnerAcct = body.getString("to_account");	
		
/*		JsonObject so = msg.body();
		
    	String boId = so.getString("bo_id");
    	
    	String partnerAcct = so.getJsonObject("channel").getString("link_account"); //交易单据一般要记录协作方
*/    	
    	//当前操作人信息
    	JsonObject actor = ActionContextTransfomer.fromMessageHeaderToActor(headerMap); 
    	
    	   	
/*    	//记录事实对象（业务数据），会根据ActionDescriptor定义的状态机自动进行状态变化，并发出状态变化业务事件
    	//自动查找数据源，自动进行分表处理
    	this.recordCDO(BizRoleDirection.FROM, partnerAcct, appActivity.getBizObjectType(), so, boId, actor, 
    			cdoResult->{
    		if (cdoResult.succeeded()) {	
    			String stubBoId = so.getString("bo_id"); */
    	
    			JsonObject stubBo = this.buildStubForCDO(shipmentBo, boId, partnerAcct);
    			
    	    	this.recordFactData(appActivity.getBizObjectType(), stubBo, boId, actor, null, result->{
    				if (result.succeeded()) {				
    					msg.reply(stubBo);
    				} else {
    					Throwable errThrowable = result.cause();
    					String errMsgString = errThrowable.getMessage();
    					appActivity.getLogger().error(errMsgString, errThrowable);
    					msg.fail(100, errMsgString);		
    				}

    	    	});

/*    		}else{
				Throwable errThrowable = cdoResult.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.fail(100, errMsgString);		

    		}
    	});    	*/

	}

}
