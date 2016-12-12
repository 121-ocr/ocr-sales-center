package ocr.sales.channelrestocking;

import otocloud.framework.app.function.AppActivityImpl;

/**
 * 渠道补货单由提交变为发货中
 * 
 * @author wanghw
 *
 */
public class ChannelRestockingCommit2ShippingHandler extends ChannelRestocking2ShippingBaseHandler{

	public ChannelRestockingCommit2ShippingHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	private String getNewState() {
		return ChannelRestockingConstant.SHIPPING_STATUS;
	}

	private String getPreStatus() {
		return ChannelRestockingConstant.COMMIT_STATUS;
	}
}
