package ocr.sales.channelrestocking;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 渠道补货单发货： 1. 生成发货单 2. 保存补货单 3. 通知门店收货
 * 
 * @author wanghw
 *
 */
public class ChannelRestockingShipHandler extends ActionHandlerImpl<JsonObject> {

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
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		// 前处理
		beforeProess(msg, result -> {
			if (result.succeeded()) {
				// 处理
				proess(msg, result.result().body());
			} else {
				Throwable errThrowable = result.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.fail(100, errMsgString);
			}
		});
	}

	/**
	 * 保存补货单
	 * 
	 * @param msg
	 * @param result
	 */
	private void proess(OtoCloudBusMessage<JsonObject> msg, JsonObject bo) {
		// 根据状态不同调用不同的保存方法
		JsonObject replenishmentObj = msg.body();
		JsonObject body = replenishmentObj.getJsonObject("bo");
		
		Future<Void> afterFuture = Future.future();
		afterFuture.setHandler(afterHandle->{
			if (afterHandle.succeeded()) {
				// 后续处理
				afterProcess(body,bo,ret -> {
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
		
		String current_state = replenishmentObj.getString("current_state");
		// 发货记录上记录发货单id、判断整单是否完成，完成返回true
		if(ruleProcess(replenishmentObj,bo)){
			//设置整单完成状态
			String newStatus = ChannelRestockingConstant.SHIPPED_STATUS;
			this.recordFactData(this.appActivity.getBizObjectType(), body, 
					body.getString("bo_id"), current_state, newStatus, false, false, 
					replenishmentObj.getJsonObject("actor"), body.getJsonObject("channel").getString("link_account"),
					null, next->{
						if (next.succeeded()) {
							afterFuture.complete();
						}else{
							Throwable errThrowable = next.cause();
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
			handler.save(body, msg.headers(), result -> {
				if (result.succeeded()) {
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
	 * 2、回写发货单号到补货单的孙表-发货记录上
	 * 3、判断整单是否完成，完成返回true
	 * @param replenishment
	 * @param shipment
	 */
	private boolean ruleProcess(JsonObject replenishmentObj, JsonObject shipment) {
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
				detail_s.put("ship_code", shipment.getString("bo_id"));
				detail_s.put("is_shipped", true);
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
	private void afterProcess(JsonObject replenishment,JsonObject shipment, Handler<AsyncResult<Message<JsonObject>>> retHandler) {
		String from_account = this.appActivity.getAppInstContext().getAccount();
		String to_account = shipment.getJsonObject("channel").getString("link_account");
		// 调用门店的接口
		String invSrvName = this.appActivity.getDependencies().getJsonObject("pointofsale_service")
				.getString("service_name", "");
		String acceptAddress = to_account + "." + invSrvName + "." + "accept.create";
		// 创建收货通知的VO
		JsonObject accept = new JsonObject();
		accept.put("replenishments_id", replenishment.getString("bo_id"));
		JsonObject supplier = new JsonObject();
		supplier.put("link_org_acct_rel", "1");
		supplier.put("link_account", from_account);
		accept.put("supplier", supplier);
		accept.put("ship_date", shipment.getString("ship_date"));
		accept.put("ship_actor", shipment.getValue("ship_actor"));
		accept.put("shipment_id", shipment.getString("bo_id"));
		
		this.appActivity.getEventBus().send(acceptAddress, accept, retHandler);

	}

	/**
	 * 生成发货单
	 * 
	 * @param msg
	 */

	private void beforeProess(OtoCloudBusMessage<JsonObject> msg,
			Handler<AsyncResult<Message<JsonObject>>> retHandler) {
		// 创建发货单
		String shipmentAddress = appActivity.getAppInstContext().getAccount() + "."
				+ this.appActivity.getService().getRealServiceName() + ".shipment.create";
		// 构建发货单VO
		JsonObject replenishment = msg.body().getJsonObject("bo");
		JsonObject shipment = new JsonObject();
		// SimpleDateFormat dfDate=new SimpleDateFormat("yyyy-MM-dd");
		// String shipdate = dfDate.format(new Date());
		shipment.put("channel", replenishment.getJsonObject("channel"));
		shipment.put("target_warehose", replenishment.getJsonObject("target_warehose"));
		shipment.put("is_completed", "false");
		JsonArray replenishment_b = replenishment.getJsonArray("details");
		for (Object object : replenishment_b) {
			JsonObject detail = (JsonObject) object;
			JsonArray replenishment_s = detail.getJsonArray("shipments");
			if(replenishment_s == null || replenishment_s.isEmpty()){
				continue;
			}
			JsonArray shipment_b_list = new JsonArray();
			int row = 0;
			for (Object object2 : replenishment_s) {
				
				JsonObject detail_s = (JsonObject) object2;
				if(detail_s.getBoolean("is_shipped")){
					continue;
				}
				shipment.put("ship_date", detail_s.getString("ship_date"));
				shipment.put("ship_actor", detail_s.getValue("ship_actor"));
				JsonObject shipment_b = new JsonObject();
				shipment_b.put("detail_code", row++);
				shipment_b.put("restocking_warehose", detail.getJsonObject("restocking_warehose"));
				shipment_b.put("goods", detail.getJsonObject("goods"));
				shipment_b.put("invbatchcode", detail.getString("invbatchcode"));
				shipment_b.put("quantity", detail_s.getValue("ship_quantity"));

				shipment_b_list.add(shipment_b);
			}
			shipment.put("details", shipment_b_list);
		}
		this.appActivity.getEventBus().send(shipmentAddress, shipment, retHandler);
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
