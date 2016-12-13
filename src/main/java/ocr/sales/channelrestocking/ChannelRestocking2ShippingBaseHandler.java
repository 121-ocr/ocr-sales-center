package ocr.sales.channelrestocking;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionContextTransfomer;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * 渠道补货单由提交变为发货中
 * 
 * @author wanghw
 *
 */
public class ChannelRestocking2ShippingBaseHandler extends ActionHandlerImpl<JsonObject> {

	public ChannelRestocking2ShippingBaseHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	public void save(JsonObject bo, MultiMap headerMap, Handler<AsyncResult<JsonObject>> retHandler) {

		String boId = bo.getString("bo_id");
		// 当前操作人信息
		JsonObject actor = ActionContextTransfomer.fromMessageHeaderToActor(headerMap);
		String partnerAcct = bo.getJsonObject("channel").getString("link_account"); 
		// 记录事实对象（业务数据），会根据ActionDescriptor定义的状态机自动进行状态变化，并发出状态变化业务事件
		// 自动查找数据源，自动进行分表处理
		Future<JsonObject> future = Future.future();
		future.setHandler(retHandler);
		this.recordFactData(appActivity.getBizObjectType(), bo, boId, getPreStatus(), getNewState(), 
				needPublishEvent(),isContainsFactData(),actor, partnerAcct, null, result -> {
			if (result.succeeded()) {
				future.complete(bo);
			} else {
				Throwable errThrowable = result.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				future.fail(errMsgString);
			}
		});
	}

	public boolean isContainsFactData() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean needPublishEvent() {
		// TODO Auto-generated method stub
		return false;
	}

	public String getNewState() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getPreStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void handle(OtoCloudBusMessage<JsonObject> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return null;
	}

}
