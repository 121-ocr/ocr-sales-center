package ocr.sales.channelrestocking;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import otocloud.common.ActionURI;
import otocloud.framework.app.common.BizRoleDirection;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.CDOHandlerImpl;
import otocloud.framework.common.CallContextSchema;
import otocloud.framework.core.CommandMessage;
import otocloud.framework.core.HandlerDescriptor;

/**
 * 渠道补货单发货： 1. 生成发货单 2. 保存补货单 3. 通知门店收货
 * 
 * @author wanghw
 *
 */
public class ChannelRestockingShipHandler extends CDOHandlerImpl<JsonObject> {

	public ChannelRestockingShipHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	@Override
	public String getEventAddress() {
		return ChannelRestockingConstant.SHIP_ADDRESS;
	}

	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		// 前处理
		Future<Void> next = Future.future();
		next.setHandler(handler->{			
			proess(msg);			
		});
		
		beforeProess(msg, next);	

	}

	/**
	 * 保存补货单
	 * 
	 * @param msg
	 * @param result
	 */
	private void proess(CommandMessage<JsonObject> msg) {
		// 根据状态不同调用不同的保存方法
		JsonObject replenishmentObj = msg.body().getJsonObject("content");
		JsonObject body = replenishmentObj.getJsonObject("bo");
		
		String current_state = replenishmentObj.getString("current_state");
		
		Future<Void> afterFuture = Future.future();
		afterFuture.setHandler(afterHandle->{
			if (afterHandle.succeeded()) {
				// 后续处理
				afterProcess(replenishmentObj, current_state, ret -> {
					if (ret.succeeded()) {
						//返回完整补货单
						msg.reply(replenishmentObj); 
						//msg.reply(ret.result().body()); // 返回BO
					} else {
						Throwable errThrowable = ret.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						msg.fail(100, errMsgString);
					}
				});
			}else{
				Throwable errThrowable = afterHandle.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.fail(100, errMsgString);
			}
		});
		
		// 发货记录上记录发货单id、判断整单是否完成，完成返回true
		if(ruleProcess(replenishmentObj)){
			//设置整单完成状态
			String newStatus = ChannelRestockingConstant.SHIPPED_STATUS;
			
			String partner = replenishmentObj.getString("to_account");
			String to_biz_unit = replenishmentObj.getString("to_biz_unit");
			//JsonObject session = msg.getSession();
			//boolean is_global_bu =  session.getBoolean(SessionSchema.IS_GLOBAL_BU, true);
			String bizUnit = msg.getCallContext().getString(CallContextSchema.BIZ_UNIT_ID, null);

			//String partner = body.getJsonObject("channel").getString("link_account");
			String boIdString = body.getString("bo_id");
			
			JsonObject actor = replenishmentObj.getJsonObject("from_actor");
			
			JsonArray shipments = body.getJsonArray("shipments");
			body.remove("shipments");
			
			this.recordCDO(bizUnit, BizRoleDirection.FROM, partner, to_biz_unit, this.appActivity.getBizObjectType(), body, 
					boIdString, current_state, newStatus, false, false, 
					actor, cdoRet->{
						if (cdoRet.succeeded()) {							
							JsonObject stubBo = this.buildStubForCDO(body, boIdString, partner);
							this.recordFactData(bizUnit, this.appActivity.getBizObjectType(), stubBo, boIdString, current_state, newStatus, false, false, actor, null, stubRetp->{
								
							});		
							replenishmentObj.put("current_state", newStatus);
							body.put("shipments", shipments);
							afterFuture.complete();
						}else{
							Throwable errThrowable = cdoRet.cause();
							afterFuture.fail(errThrowable);
						}						
					});
			
		}else{

			ChannelRestocking2ShippingBaseHandler handler = null;
			
			if (ChannelRestockingConstant.COMMIT_STATUS.equals(current_state)) {
				handler = new ChannelRestockingCommit2ShippingHandler(appActivity);
	
			} else if (ChannelRestockingConstant.SHIPPING_STATUS.equals(current_state)) {
				handler = new ChannelRestockingShipping2ShippingHandler(appActivity);
			}
			if (handler == null) {				
				afterFuture.fail("无法创建对象");
				return;
			}
			
			String to_biz_unit = replenishmentObj.getString("to_biz_unit");
			JsonObject session = msg.getSession();
			//boolean is_global_bu =  session.getBoolean(SessionSchema.IS_GLOBAL_BU, true);
			String bizUnit = session.getString(CallContextSchema.BIZ_UNIT_ID, null);

			
			handler.save(bizUnit, to_biz_unit, body, msg.headers(), result -> {
				if (result.succeeded()) {
					if (ChannelRestockingConstant.COMMIT_STATUS.equals(current_state)) {
						replenishmentObj.put("current_state", ChannelRestockingConstant.SHIPPING_STATUS);
			
					} else if (ChannelRestockingConstant.SHIPPING_STATUS.equals(current_state)) {
						replenishmentObj.put("current_state", ChannelRestockingConstant.SHIPPED_STATUS);
					}
					
					afterFuture.complete();
				} else {
					Throwable errThrowable = result.cause();
					afterFuture.fail(errThrowable);
				}
			});
		}
	}

	/**
	 * 规则处理：
	 * 1、计算每行发货数量，如果完成，则置整行为完成状态
	 * 3、判断整单是否完成，完成返回true
	 * @param replenishment
	 * @param shipment
	 */
	private boolean ruleProcess(JsonObject replenishmentObj) {
		JsonObject replenishment = replenishmentObj.getJsonObject("bo");
		JsonArray replenishment_b = replenishment.getJsonArray("details");
		Boolean hasNoCompleted = false;
		for (Object object : replenishment_b) {
			JsonObject detail = (JsonObject) object;
			JsonArray replenishment_s = detail.getJsonArray("shipments");
			if(replenishment_s == null || replenishment_s.isEmpty()){
				hasNoCompleted = true;
				continue;
			}
			Double sumQuantity = 0.00;
			for (Object object2 : replenishment_s) {
				JsonObject detail_s = (JsonObject) object2;
				sumQuantity += detail_s.getDouble("ship_quantity");
				if(detail_s.getBoolean("is_shipped")){
					continue;
				}
			}
			//如果整行发货完成，置整行发货完成态
			if(sumQuantity.compareTo(detail.getDouble("quantity")) >= 0){
				detail.put("ship_completed", true);
			}else{
				hasNoCompleted = true;
			}
		}
		if(!hasNoCompleted){
			return true;		
		}
		return false;
	}

	/**
	 * 通知门店收货
	 * 
	 * @param bo
	 * @param retHandler
	 */
	private void afterProcess(JsonObject replenishmentObj, String current_state, Handler<AsyncResult<Void>> retHandler) {
		
		JsonObject replenishment = replenishmentObj.getJsonObject("bo");
		
		//String from_account = this.appActivity.getAppInstContext().getAccount();
		String to_account = replenishment.getJsonObject("channel").getString("link_account");
		// 调用门店的接口
		String invSrvName = this.appActivity.getDependencies().getJsonObject("pointofsale_service")
				.getString("service_name", "");
		
		Future<Void> retFuture = Future.future();
		retFuture.setHandler(retHandler);		
		
		if (ChannelRestockingConstant.COMMIT_STATUS.equals(current_state)) {
			String replenishmentAddress = to_account + "." + invSrvName + "." + "replenishment-mgr.create";
			//replenishmentObj.put("current_state", ChannelRestockingConstant.SHIPPING_STATUS);
			this.appActivity.getEventBus().send(replenishmentAddress, replenishmentObj, replenishmentHandler->{
				if (replenishmentHandler.succeeded()) {
					
					String shipmentAddress = to_account + "." + invSrvName + "." + "shipment-mgr.batch_create";
					this.appActivity.getEventBus().send(shipmentAddress, replenishment.getJsonArray("shipments"), shipmentHandler->{
						if (shipmentHandler.succeeded()) {
							retFuture.complete();						
						} else {
							Throwable errThrowable = shipmentHandler.cause();
							retFuture.fail(errThrowable);
						}
					});
					
				} else {
					Throwable errThrowable = replenishmentHandler.cause();
					retFuture.fail(errThrowable);
				}
			});
			
		} else if (ChannelRestockingConstant.SHIPPING_STATUS.equals(current_state)) {
			String shipmentAddress = to_account + "." + invSrvName + "." + "shipment-mgr.batch_create";
			//replenishmentObj.put("current_state", ChannelRestockingConstant.SHIPPED_STATUS);
			this.appActivity.getEventBus().send(shipmentAddress, replenishment.getJsonArray("shipments"), shipmentHandler->{
				if (shipmentHandler.succeeded()) {
					retFuture.complete();						
				} else {
					Throwable errThrowable = shipmentHandler.cause();
					retFuture.fail(errThrowable);
				}
			});
			
		}
		
/*		String acceptAddress = to_account + "." + invSrvName + "." + "accept.create";
		// 创建收货通知的VO
		JsonObject accept = new JsonObject();
		accept.put("replenishments_id", replenishment.getString("bo_id"));
		accept.put("shipments", replenishment.getJsonArray("shipments"));
		JsonObject supplier = new JsonObject();
		supplier.put("link_org_acct_rel", "1");
		supplier.put("link_account", from_account);
		accept.put("supplier", supplier);
		accept.put("ship_date", DateTimeUtil.now("yyyy-MM-dd"));
		accept.put("ship_actor", replenishmentObj.getJsonObject("actor"));
//		accept.put("shipment_id", shipment.getString("bo_id"));
		
		this.appActivity.getEventBus().send(acceptAddress, accept, retHandler);*/

	}

	/**
	 * 生成发货单
	 * 
	 * @param msg
	 */

/*	private void beforeProess(CommandMessage<JsonObject> msg,
			Handler<AsyncResult<Message<JsonObject>>> retHandler) {*/
		
	private void beforeProess(CommandMessage<JsonObject> msg,
			Future<Void> next) {
	
		// 创建发货单
		String shipmentAddress = appActivity.getAppInstContext().getAccount() + "."
				+ this.appActivity.getService().getRealServiceName() + ".shipment.create";
		// 构建发货单VO
		JsonObject replenishment = msg.body().getJsonObject("bo");
		
		//按照仓库进行分组
		Map<String, List<JsonObject>> shipmentRecordsMap = new HashMap<String, List<JsonObject>>();
		JsonArray replenishment_b = replenishment.getJsonArray("details");
		for (Object object : replenishment_b) {
			JsonObject detail = (JsonObject) object;
			String whCode = detail.getJsonObject("restocking_warehouse").getString("code");
			if(shipmentRecordsMap.containsKey(whCode)){
				shipmentRecordsMap.get(whCode).add(detail);
			}else{
				List<JsonObject> values = new ArrayList<JsonObject>();
				values.add(detail);
				shipmentRecordsMap.put(whCode, values);
			}
		}
		
		String boId = replenishment.getString("bo_id");
		JsonObject channel = replenishment.getJsonObject("channel");
		//JsonObject targetWarehouse = replenishment.getJsonObject("target_warehouse");
		
		List<Future> futures = new ArrayList<Future>();
		
		JsonArray shipmentIds = new JsonArray();
		
		//构建发货单
		shipmentRecordsMap.forEach((key,values)->{
			
			Future<JsonObject> returnFuture = Future.future();
			futures.add(returnFuture);
			
			JsonObject shipment = new JsonObject();
			
			shipment.put("replenishments_id", boId);
			shipment.put("channel", channel);
			shipment.put("target_warehouse", replenishment.getJsonObject("target_warehouse"));			
			shipment.put("restocking_warehouse", values.get(0).getJsonObject("restocking_warehouse"));
			shipment.put("is_completed", "false");
			
			for(Object item : values){
				
				JsonObject detail = (JsonObject) item;
				JsonArray replenishment_s = detail.getJsonArray("shipments");
				if(replenishment_s == null || replenishment_s.isEmpty()){
					continue;
				}
				JsonArray shipment_b_list = new JsonArray();
				int row = 1;
				for (Object object2 : replenishment_s) {
					
					JsonObject detail_s = (JsonObject) object2;
					if(detail_s.getBoolean("is_shipped")){
						continue;
					}
					shipment.put("ship_date", detail_s.getString("ship_date"));
					shipment.put("ship_actor", detail_s.getValue("ship_actor"));
					
					JsonObject shipment_b = new JsonObject();
					shipment_b.put("detail_code", row++);
					shipment_b.put("rep_detail_code", detail.getString("detail_code"));
					shipment_b.put("goods", detail.getJsonObject("goods"));
					shipment_b.put("invbatchcode", detail.getString("invbatchcode"));
					shipment_b.put("shelf_life", detail.getString("shelf_life"));
					shipment_b.put("quantity", detail_s.getValue("ship_quantity"));
					
					shipment_b.put("supply_price", detail.getValue("supply_price"));
					shipment_b.put("retail_price", detail.getValue("retail_price"));

					shipment_b_list.add(shipment_b);
				}
				if(shipment_b_list.size() > 0){		
					if(shipment.containsKey("details")){
						shipment.getJsonArray("details").addAll(shipment_b_list);
					}else{
						shipment.put("details", shipment_b_list);
					}
				}
				
			}
			
			JsonArray shipmentDetails = shipment.getJsonArray("details");
			if(shipmentDetails == null || shipmentDetails.size() <= 0){
				returnFuture.complete();
			}else{				
				//生成发货单
				this.appActivity.getEventBus().send(shipmentAddress, shipment, ret->{
					if (ret.succeeded()) {
						JsonObject shipmentBo = (JsonObject)ret.result().body();
						shipmentBo.put("current_state", "created");
						String shipmentBoId = shipmentBo.getString("bo_id");
						shipmentIds.add(shipmentBo);
						//回写补货单上的发货单号
						for(Object item : values){						
							JsonObject detail = (JsonObject) item;
							JsonArray replenishment_s = detail.getJsonArray("shipments");
							if(replenishment_s == null || replenishment_s.isEmpty()){
								continue;
							}
							for (Object object2 : replenishment_s) {
								JsonObject detail_s = (JsonObject) object2;
								if(detail_s.containsKey("is_shipped")){
									if(detail_s.getBoolean("is_shipped")){
										continue;
									}
								}
								detail_s.put("ship_code", shipmentBoId);
								detail_s.put("is_shipped", true);		
							}
						}					
						returnFuture.complete();
					}else{
						Throwable errThrowable = ret.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);	
						
						returnFuture.fail(errThrowable);
					}
				});
			}
			
		});
		
		CompositeFuture.join(futures).setHandler(ar -> {
			replenishment.put("shipments", shipmentIds);
			next.complete();
		});
		
	}
	
	/**
	 * 此action的自描述元数据
	 */
	@Override
	public ActionDescriptor getActionDesc() {		
		
		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
				
		ActionURI uri = new ActionURI(ChannelRestockingConstant.SHIP_ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		//状态变化定义
//		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, 
//				null, "created");
//		bizStateSwitchDesc.setWebExpose(true); //是否向web端发布事件		
//		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);
		
		return actionDescriptor;
	}
}
