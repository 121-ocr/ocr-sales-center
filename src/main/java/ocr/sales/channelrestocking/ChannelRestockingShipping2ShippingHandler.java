package ocr.sales.channelrestocking;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.common.SessionSchema;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.CommandMessage;

/**
 * 渠道补货单由发货中变为发货中
 * 
 * @author wanghw
 *
 */
public class ChannelRestockingShipping2ShippingHandler extends ChannelRestocking2ShippingBaseHandler {

	public ChannelRestockingShipping2ShippingHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	@Override
	public String getNewState() {
		return ChannelRestockingConstant.SHIPPING_STATUS;
	}

	@Override
	public String getPreStatus() {
		return ChannelRestockingConstant.SHIPPING_STATUS;
	}

	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		JsonObject body = msg.body().getJsonObject("content");
		
		String to_biz_unit = body.getString("to_biz_unit");
		JsonObject session = msg.getSession();
		//boolean is_global_bu =  session.getBoolean(SessionSchema.IS_GLOBAL_BU, true);
		String bizUnit = session.getString(SessionSchema.BIZ_UNIT_ID, null);

		
		super.save(bizUnit, to_biz_unit, body, msg.headers(), result -> {
			if (result.succeeded()) {
				msg.reply(result.result()); // 返回BO
			} else {
				Throwable errThrowable = result.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.fail(100, errMsgString);
			}
		});

	}

	@Override
	public String getEventAddress() {
		return ChannelRestockingConstant.ACCEPT_ADDRESS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		ActionURI uri = new ActionURI(ChannelRestockingConstant.ACCEPT_ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		// 状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT,
				ChannelRestockingConstant.SHIPPING_STATUS, ChannelRestockingConstant.SHIPPING_STATUS);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}
}
