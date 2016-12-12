package ocr.sales.channelrestocking;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * TODO: 待发货补货单查询
 * @date 2016年12月10日
 * @author wanghw
 */
public class ChannelRestockingQuery4ReadyShipHandler extends ActionHandlerImpl<JsonObject> {
	
	public ChannelRestockingQuery4ReadyShipHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ChannelRestockingConstant.QUERY4READYSHIP_ADDRESS;
	}

	/**
	 * 要查询的单据状态
	 * 
	 * @return
	 */
	public List<String> getStatus(JsonObject msgBody) {
		// TODO Auto-generated method stub
//		return ChannelRestockingConstant.COMMIT_STATUS;
		List<String> ret = new ArrayList<>();
		ret.add("created");
		ret.add("commit");
		ret.add("shipping");
		return ret;
	}	
	
	/**
	 * 处理器
	 */
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {

		JsonObject queryParams = msg.body();
		//PagingOptions pagingObj = PagingOptions.buildPagingOptions(queryParams);
		JsonObject fields = queryParams.getJsonObject("fields");		
		JsonObject queryCond = queryParams.getJsonObject("query");
		JsonObject pagingInfo = queryParams.getJsonObject("paging");
		this.queryLatestFactDataList(appActivity.getBizObjectType(), getStatus(queryParams), fields, pagingInfo, queryCond, null, findRet -> {
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
