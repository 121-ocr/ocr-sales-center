package ocr.sales.channelrestocking;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.CompositeFutureImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.common.BizRoleDirection;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.CDOHandlerImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;

/**
 * TODO: 待发货补货单查询
 * @date 2016年12月10日
 * @author wanghw
 */
public class ChannelRestockingQuery4ReadyShipHandler extends CDOHandlerImpl<JsonObject> {
	
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
		ret.add("commit");
		ret.add("shipping");
		return ret;
	}	
	
	/**
	 * 处理器
	 */
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {

		JsonObject queryParams = msg.body().getJsonObject("content");
		//PagingOptions pagingObj = PagingOptions.buildPagingOptions(queryParams);
		JsonObject fields = queryParams.getJsonObject("fields");		
		JsonObject queryCond = queryParams.getJsonObject("query");
		JsonObject pagingInfo = queryParams.getJsonObject("paging");
		this.queryLatestFactDataList(null, appActivity.getBizObjectType(), getStatus(queryParams), fields, pagingInfo, queryCond, null, findRet -> {
			if (findRet.succeeded()) {
				//msg.reply(findRet.result());
				JsonObject retObj = findRet.result();
				JsonArray stubBoList = retObj.getJsonArray("datas");
				if(stubBoList != null && stubBoList.size() > 0){
					List<Future> futures = new ArrayList<Future>();
					//List<JsonObject> retList = new ArrayList<>();
					for(Object item : stubBoList){
						JsonObject stubBo = (JsonObject)item;
						Future<JsonObject> cdoFuture = Future.future();
						futures.add(cdoFuture);						

						JsonObject bo = stubBo.getJsonObject("bo");
						String partner = bo.getString("partner");
						String boId = bo.getString("bo_id");
						
						this.queryLatestCDO(BizRoleDirection.FROM, partner, appActivity.getBizObjectType(), 
								boId, null, cdoRet->{
									if (findRet.succeeded()) {
										cdoFuture.complete(cdoRet.result());
									}else{
										cdoFuture.fail(cdoRet.cause());
									}									
									
								});
						
					}
					
					CompositeFuture.join(futures).setHandler(ar -> {
						JsonArray retList = new JsonArray();
						CompositeFutureImpl comFutures = (CompositeFutureImpl)ar;
						if(comFutures.size() > 0){										
							for(int i=0;i<comFutures.size();i++){
								if(comFutures.succeeded(i)){
									JsonObject cdo = comFutures.result(i);
									retList.add(cdo);
								}
							}
						}
						
						retObj.put("datas", retList);
						
						msg.reply(retObj);
					});
					
				}else{
					msg.reply(retObj);
				}
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
