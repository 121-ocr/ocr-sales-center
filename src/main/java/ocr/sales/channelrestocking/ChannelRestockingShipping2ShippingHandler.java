package ocr.sales.channelrestocking;

import otocloud.framework.app.function.AppActivityImpl;

/**
 * 渠道补货单由提交变为发货中
 * 
 * @author wanghw
 *
 */
public class ChannelRestockingShipping2ShippingHandler extends ChannelRestocking2ShippingBaseHandler{

	public ChannelRestockingShipping2ShippingHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	private String getNewState() {
		return ChannelRestockingConstant.SHIPPING_STATUS;
	}

	private String getPreStatus() {
		return ChannelRestockingConstant.SHIPPING_STATUS;
	}
}
