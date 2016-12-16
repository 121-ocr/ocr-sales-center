package ocr.sales.channelrestocking;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * TODO: 待收货货补货单查询
 * @date 2016年12月10日
 * @author wanghw
 */
public class ChannelRestockingQuery4AcceptShipHandler extends ActionHandlerImpl<JsonObject> {
	
	public ChannelRestockingQuery4AcceptShipHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ChannelRestockingConstant.QUERY4ACCEPT_ADDRESS;
	}

	/**
	 * 要查询的单据状态
	 * 
	 * @return
	 */
	public String getStatus() {
		// TODO Auto-generated method stub
		return ChannelRestockingConstant.SHIPPING_STATUS;
	}	
	
	/**
	 * 根据bo_id查询单据
	 */
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {

		JsonObject queryParams = msg.body();
		this.queryLatestFactDataList(appActivity.getBizObjectType(), getStatus(), 
				null, queryParams, null, findRet -> {
			if (findRet.succeeded()) {
				msg.reply(findRet.result());
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

		ActionURI uri = new ActionURI(getEventAddress(), HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		return actionDescriptor;
	}

}
