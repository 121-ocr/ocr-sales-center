package ocr.sales.channelrestocking;

import io.vertx.core.AsyncResult;
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
		JsonObject body = msg.body().getJsonObject("bo");
		// 发货记录上记录发货单id
		setShipmentCode(body,bo);
		String current_state = msg.body().getString("current_state");
		ChannelRestocking2ShippingBaseHandler handler = null;
		if (ChannelRestockingConstant.COMMIT_STATUS.equals(current_state)) {
			handler = new ChannelRestockingCommit2ShippingHandler(appActivity);

		} else if (ChannelRestockingConstant.SHIPPING_STATUS.equals(current_state)) {
			handler = new ChannelRestockingShipping2ShippingHandler(appActivity);
		}
		if (handler == null) {
			return;
		}
		handler.save(body, msg.headers(), result -> {
			if (result.succeeded()) {
				// 后续处理
				afterProcess(bo, ret -> {
					if (ret.succeeded()) {
						msg.reply(ret.result()); // 返回BO
					} else {
						Throwable errThrowable = ret.cause();
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
	}

	/**
	 * 回写发货单号到补货单的孙表-发货记录上
	 * @param replenishment
	 * @param shipment
	 */
	private void setShipmentCode(JsonObject replenishment, JsonObject shipment) {
		JsonArray replenishment_b = replenishment.getJsonArray("details");
		for (Object object : replenishment_b) {
			JsonObject detail = (JsonObject) object;
			JsonArray replenishment_s = detail.getJsonArray("shipments");
			if(replenishment_s == null || replenishment_s.isEmpty()){
				continue;
			}
			for (Object object2 : replenishment_s) {
				JsonObject detail_s = (JsonObject) object2;
				if(detail_s.getBoolean("is_shipped")){
					continue;
				}
				detail_s.put("ship_code", shipment.getString("bo_id"));
				detail_s.put("is_shipped", true);
			}
		}
	}

	/**
	 * 通知门店收货
	 * 
	 * @param bo
	 * @param retHandler
	 */
	private void afterProcess(JsonObject bo, Handler<AsyncResult<Message<JsonObject>>> retHandler) {
		String from_account = this.appActivity.getAppInstContext().getAccount();
		// 调用门店的接口
		String invSrvName = this.appActivity.getDependencies().getJsonObject("pointofsale_service")
				.getString("service_name", "");
		String acceptAddress = from_account + "." + invSrvName + "." + "accept.create";
		// 创建收货通知的VO
		JsonObject accept = new JsonObject();
		accept.put("replenishments_id", bo.getString("bo_id"));
		accept.put("supplier", bo.getString("actor"));
		JsonArray details = bo.getJsonArray("details");
		JsonArray shipments = ((JsonObject)details.getValue(0)).getJsonArray("shipments");
		accept.put("ship_date", ((JsonObject)shipments.getValue(0)).getString("ship_date"));
		accept.put("ship_actor", ((JsonObject)shipments.getValue(0)).getJsonObject("ship_actor"));
		accept.put("shipment_id", ((JsonObject)shipments.getValue(0)).getString("shipment_id"));
		
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
