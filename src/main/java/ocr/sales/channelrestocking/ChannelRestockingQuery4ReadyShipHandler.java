package ocr.sales.channelrestocking;

import io.vertx.core.json.JsonObject;
import ocr.common.handler.SampleBillBaseQueryHandler;
import otocloud.framework.app.function.AppActivityImpl;

/**
 * TODO: 待发货补货单查询
 * @date 2016年12月10日
 * @author wanghw
 */
public class ChannelRestockingQuery4ReadyShipHandler extends SampleBillBaseQueryHandler{
	
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
	@Override
	public String getStatus(JsonObject msgBody) {
		// TODO Auto-generated method stub
		return ChannelRestockingConstant.COMMIT_STATUS;
	}	
}
