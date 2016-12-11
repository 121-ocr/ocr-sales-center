package ocr.sales.channelrestocking;

import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizStateChangedMessage;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.OtoCloudBusMessage;


/**
 * TODO: 拣货完成更新补货单已拣货量
 * @date 2016年11月15日
 * @author lijing
 */
public class ReplenishmentQuantityUpdateHandler extends ActionHandlerImpl<JsonObject> {

	/**
	 * Constructor.
	 *
	 * @param componentImpl
	 */
	public ReplenishmentQuantityUpdateHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}
	
    @Override
    public String getRealAddress(){
		String inventorycenterSrvName = this.appActivity.getDependencies().getJsonObject("inventorycenter_service").getString("service_name","");
		String address = this.appActivity.getAppInstContext().getAccount() + "." + inventorycenterSrvName + ".stockout-mgr." + BizStateSwitchDesc.buildStateSwitchEventAddress("bp_stockout", "onpicking", "pickouted");
		return address;
    }
	
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		
		MultiMap headerMap = msg.headers();		
		
		BizStateChangedMessage bizStateChangedMessage = new BizStateChangedMessage();
		bizStateChangedMessage.fromJsonObject(msg.body());
		
		JsonObject stockOutObj = bizStateChangedMessage.getFactData();
		String replenishmentId = stockOutObj.getString("replenishment_code");
		
		//获取补货单数据    	
		String srvName = this.appActivity.getService().getRealServiceName();
		String srvAddress = bizStateChangedMessage.getAccount() + "." + srvName + "." + "channel-restocking.findone";							
		DeliveryOptions options = new DeliveryOptions();
		options.setHeaders(headerMap);
		this.appActivity.getEventBus().<JsonObject>send(srvAddress,
				new JsonObject().put("bo_id", replenishmentId), ret->{
				if(ret.succeeded()){		
					
					JsonObject bo = ret.result().body();
					
					JsonObject replenishmentObj = bo.getJsonObject("bo");
					JsonArray repDetailsArray = replenishmentObj.getJsonArray("details");
					
					JsonArray stockOutDetailsArray = stockOutObj.getJsonArray("detail");
					
					//更新补货量
					if(stockOutDetailsArray != null && stockOutDetailsArray.size() > 0){
						stockOutDetailsArray.forEach(stockOutItem->{
							updateRepDetailQuantity(repDetailsArray, (JsonObject)stockOutItem);							
						});						
					}
					
					//保存补货单
					String boStatus = bo.getString("current_state");
					this.updateFactData(this.appActivity.getBizObjectType(), replenishmentObj, replenishmentId, boStatus, 
							bo.getJsonObject("actor"), null, next->{
						if(ret.succeeded()){		
							msg.reply("ok");									
						}else{
							Throwable errThrowable = next.cause();
							String errMsgString = errThrowable.getMessage();
							appActivity.getLogger().error(errMsgString, errThrowable);
							msg.fail(100, errMsgString);
						}								
					});
					
					
					
					
/*					String channelAccount = replenishmentObj.getJsonObject("channel").getString("link_account");
					
					JsonObject allotinvObj = convertToAllotinvObj(stockOutObj, replenishmentObj);
					
					//提交门店收货入库
					String pointofsaleSrvName = this.appActivity.getDependencies().getJsonObject("pointofsale_service").getString("service_name","");
					String pointofsaleSrvAddress = channelAccount + "." + pointofsaleSrvName + "." + "allotinv.create";							
					DeliveryOptions options2 = new DeliveryOptions();
					options2.setHeaders(headerMap);
					this.appActivity.getEventBus().send(pointofsaleSrvAddress, allotinvObj,
							options2, allotinvRet->{
							if(allotinvRet.succeeded()){							
								msg.reply(allotinvRet.result().body());
							}else{										
								Throwable errThrowable = allotinvRet.cause();
								String errMsgString = errThrowable.getMessage();
								appActivity.getLogger().error(errMsgString, errThrowable);
								msg.fail(100, errMsgString);
							}
							
						});				*/
					
					
			
					
				}else{										
					Throwable errThrowable = ret.cause();
					String errMsgString = errThrowable.getMessage();
					appActivity.getLogger().error(errMsgString, errThrowable);
					msg.fail(100, errMsgString);
				}
				
			});	
		
	

	}
	
	private void updateRepDetailQuantity(JsonArray repDetailsArray, JsonObject stockOutItem){
		for(Object item : repDetailsArray){
			JsonObject repDetail = (JsonObject)item;
			if(repDetail.getString("detail_code").equals(stockOutItem.getString("rep_detail_code"))){
				Double quantity = repDetail.getDouble("pick_quantity");
				quantity = quantity + stockOutItem.getDouble("quantity_should");
				//如果拣货量==补货量,则置拣货完成状态
				if(quantity.equals(repDetail.getDouble("quantity"))){
					repDetail.put("pick_completed", true);
				}else{
					repDetail.put("pick_completed", false);
				}
				repDetail.put("pick_quantity", quantity);
				break;
			}
		}
	}
	
/*	//转换为门店补货入库单
	private JsonObject convertToAllotinvObj(JsonObject stockOutObj, JsonObject replenishmentObj){
		JsonObject retObj = new JsonObject();
		retObj.put("supply_date", DateTimeUtil.now("yyyy-MM-dd"));
		
		JsonObject supplier = new JsonObject();
		supplier.put("link_org_acct_rel" , "1");
		supplier.put("link_account" , this.appActivity.getAppInstContext().getAccount());
		retObj.put("supplier", supplier);
		
		retObj.put("restocking_warehouse", stockOutObj.getJsonObject("warehouse"));
		retObj.put("warehouse", replenishmentObj.getJsonObject("target_warehose"));
		retObj.put("request_date", replenishmentObj.getString("req_date"));
		retObj.put("request_code", replenishmentObj.getString("req_code"));
		retObj.put("replenishment_code", replenishmentObj.getString("bo_id"));
		retObj.put("note", stockOutObj.getString("note"));
		
		JsonArray targetDetails = new JsonArray();
		retObj.put("detail", targetDetails);
		
		JsonArray details = stockOutObj.getJsonArray("detail");
		for(Object item : details){
			JsonObject detail = (JsonObject)item;	
			
			Integer detailNo = targetDetails.size() + 1;
			JsonObject newDetail = new JsonObject();
			newDetail.put("detail_code", detailNo.toString());
			newDetail.put("goods", detail.getJsonObject("goods"));
			newDetail.put("batch_code", detail.getString("batch_code"));
			newDetail.put("shelf_life", detail.getString("shelf_life"));			
			newDetail.put("quantity_should", detail.getDouble("quantity_should"));
			newDetail.put("quantity_fact", detail.getDouble("quantity_fact"));
			newDetail.put("supply_price", detail.getJsonObject("supply_price"));
			newDetail.put("supply_amount", detail.getJsonObject("supply_amount"));
			newDetail.put("retail_price", detail.getJsonObject("retail_price"));
			newDetail.put("retail_amount", detail.getJsonObject("retail_amount"));
			newDetail.put("commission", detail.getJsonObject("commission"));	
			
			targetDetails.add(newDetail);	

		}

		return retObj;
	}*/

	@Override
	public String getEventAddress() {
		return null;
	}

}
