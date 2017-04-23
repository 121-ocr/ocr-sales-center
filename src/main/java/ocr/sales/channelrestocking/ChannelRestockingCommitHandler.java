package ocr.sales.channelrestocking;

import java.util.HashMap;
import java.util.Map;

import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionContextTransfomer;
import otocloud.common.ActionURI;
import otocloud.common.util.DateTimeUtil;
import otocloud.framework.app.common.BizRoleDirection;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.app.function.CDOHandlerImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.CommandMessage;

/**
 * TODO: 渠道补货新增
 * @date 2016年11月15日
 * @author lijing
 */
public class ChannelRestockingCommitHandler extends CDOHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "commit";

	public ChannelRestockingCommitHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ADDRESS;
	}
	
/*	@Override
	public JsonObject buildStubForCDO(JsonObject factData, String boId, String partnerAcct){
		return null;
	}*/

	//处理器
	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		
		MultiMap headerMap = msg.headers();
		
		JsonObject so = msg.body().getJsonObject("content");
		
    	String boId = so.getString("bo_id");
    	
    	JsonObject channel = so.getJsonObject("channel");
    	
    	String partnerAcct = channel.getString("link_account"); 
    	String partnerBizUnit = channel.getString("link_biz_unit_id"); 
    	String bizUnit = channel.getString("biz_unit_id");
    	
    	//当前操作人信息
    	JsonObject actor = ActionContextTransfomer.fromMessageHeaderToActor(headerMap); 
    	
    	   	
    	//记录事实对象（业务数据），会根据ActionDescriptor定义的状态机自动进行状态变化，并发出状态变化业务事件
    	//自动查找数据源，自动进行分表处理
    	this.recordCDO(bizUnit, BizRoleDirection.FROM, partnerAcct, partnerBizUnit, appActivity.getBizObjectType(), so, boId, actor, 
    			cdoResult->{
    		if (cdoResult.succeeded()) {	
    			String stubBoId = so.getString("bo_id");
    			JsonObject stubBo = this.buildStubForCDO(so, stubBoId, partnerAcct);
    			
    	    	this.recordFactData(bizUnit, appActivity.getBizObjectType(), stubBo, stubBoId, actor, null, result->{
    				if (result.succeeded()) {				
    					//构建拣货单
    					JsonArray stockOuts = convertStockOut(so);
    					JsonObject stockOutsData = new JsonObject().put("content", stockOuts);

    					//提交拣货处理				
    					String invSrvName = this.appActivity.getDependencies().getJsonObject("inventorycenter_service").getString("service_name","");
    					String stockoutPickOutAddress = this.appActivity.getAppInstContext().getAccount() + "." + invSrvName + "." + "stockout-mgr.batch_create";							
    					DeliveryOptions options = new DeliveryOptions();
    					options.setHeaders(headerMap);
    					this.appActivity.getEventBus().send(stockoutPickOutAddress,
    							stockOutsData, invRet->{
    							if(invRet.succeeded()){							
    								msg.reply(invRet.result().body());
    							}else{										
    								Throwable errThrowable = invRet.cause();
    								String errMsgString = errThrowable.getMessage();
    								appActivity.getLogger().error(errMsgString, errThrowable);
    								msg.fail(100, errMsgString);
    							}
    							
    						});	

    				} else {
    					Throwable errThrowable = result.cause();
    					String errMsgString = errThrowable.getMessage();
    					appActivity.getLogger().error(errMsgString, errThrowable);
    					msg.fail(100, errMsgString);		
    				}

    	    	});

    		}else{
				Throwable errThrowable = cdoResult.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.fail(100, errMsgString);		

    		}
    	});
    	


	}
	
	//转换为拣货单
	private JsonArray convertStockOut(JsonObject replenishmentObj){
		Map<String, JsonObject> stockOuts = new HashMap<>();
		JsonArray retArray = new JsonArray();
		
		String boId = replenishmentObj.getString("bo_id");
		
		JsonArray details = replenishmentObj.getJsonArray("details");
		for(Object item : details){
			JsonObject detail = (JsonObject)item;
			JsonObject restockingWarehouse = detail.getJsonObject("restocking_warehouse");
			String warehouseCode = restockingWarehouse.getString("code");
			JsonObject stockOutObj = new JsonObject();
			JsonArray stockDetails = new JsonArray();
			if(stockOuts.containsKey(warehouseCode)){	
				stockOutObj = stockOuts.get(warehouseCode);
				stockDetails = stockOutObj.getJsonArray("detail");
			}else{				
				String dt = DateTimeUtil.now("yyyy-MM-dd");
				Integer idNum = retArray.size() + 1;
				stockOutObj.put("bo_id", boId + "_" + idNum.toString());
				stockOutObj.put("send_date", DateTimeUtil.now("yyyy-MM-dd"));
				stockOutObj.put("confirm_date", dt);
				JsonObject channel = replenishmentObj.getJsonObject("channel");
				if(channel.containsKey("_id")){
					channel.remove("_id");
				}
				stockOutObj.put("channel", channel);
				stockOutObj.put("note", "");
				stockOutObj.put("replenishment_code", replenishmentObj.getString("bo_id"));
				stockOutObj.put("warehouse", restockingWarehouse);
				stockOutObj.put("detail", stockDetails);
				stockOuts.put(warehouseCode, stockOutObj);
				
				retArray.add(stockOutObj);
			}
			
			Integer detailNo = stockDetails.size() + 1;
			JsonObject stockDetail = new JsonObject();
			stockDetail.put("detail_code", detailNo.toString());
			stockDetail.put("rep_detail_code", detail.getString("detail_code"));
			stockDetail.put("goods", detail.getJsonObject("goods"));
			
			if(detail.containsKey("invbatchcode")){
				stockDetail.put("batch_code", detail.getString("invbatchcode"));
			}
			if(detail.containsKey("shelf_life")){
				stockDetail.put("shelf_life", detail.getString("shelf_life"));	
			}
			
			stockDetail.put("quantity_should", detail.getDouble("quantity"));
			stockDetail.put("supply_price", detail.getJsonObject("supply_price"));
			stockDetail.put("supply_amount", detail.getJsonObject("supply_amount"));
			stockDetail.put("retail_price", detail.getJsonObject("retail_price"));
			stockDetail.put("retail_amount", detail.getJsonObject("retail_amount"));
			stockDetail.put("commission", detail.getJsonObject("commission"));	
			
			stockDetails.add(stockDetail);	

		}

		return retArray;
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
		
		//状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, 
				null, "commit");
		bizStateSwitchDesc.setWebExpose(true); //是否向web端发布事件		
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);
		
		return actionDescriptor;
	}
	
	
}
