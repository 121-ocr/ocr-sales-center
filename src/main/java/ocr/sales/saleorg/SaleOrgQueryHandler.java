package ocr.sales.saleorg;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import otocloud.common.ActionURI;
import otocloud.framework.app.common.PagingOptions;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.CommandMessage;
import otocloud.framework.core.HandlerDescriptor;

/**
 * 营销中心：销售组织查询
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 销售组织查询
public class SaleOrgQueryHandler extends ActionHandlerImpl<JsonObject> {

	public static final String ADDRESS = "query";

	public SaleOrgQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);

	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	// 处理器
	@Override
	public void handle(CommandMessage<JsonObject> msg) {
	
		JsonObject queryParams = msg.getContent();
	    PagingOptions pagingObj = PagingOptions.buildPagingOptions(queryParams);        
	    this.queryBizDataList(null, appActivity.getBizObjectType(), pagingObj, null, findRet -> {
	        if (findRet.succeeded()) {
	        	JsonObject result = findRet.result();
	        	JsonArray datas = result.getJsonArray("datas");
				if(datas != null && datas.size() > 0){
					
					List<Future> futures = new ArrayList<Future>();
					
					String acctOrgSrvName = this.appActivity.getDependencies().getJsonObject("otocloud-acct-org_service")
							.getString("service_name", "");
					String bizUnitFindone = acctOrgSrvName + "." + "my-bizunit.findone";
					
					Long acctId = Long.parseLong(this.appActivity.getAppInstContext().getAccount());
					
					datas.forEach(item->{
						JsonObject invOrg = (JsonObject)item;
						String biz_unit_code = invOrg.getString("biz_unit_code");
						
						Future<Void> retFuture = Future.future();
						futures.add(retFuture);

						msg.<JsonArray>send(bizUnitFindone, 
								new JsonObject().put("acct_id", acctId).put("unit_code", biz_unit_code), bizUnitRet -> {
							if (bizUnitRet.succeeded()) {
								JsonArray bizUnits = bizUnitRet.result().body();
								if(bizUnits != null && bizUnits.size() > 0){
									invOrg.put("bizunit", bizUnits.getJsonObject(0));
								}
								retFuture.complete();
								
							} else {
								Throwable ex = bizUnitRet.cause();
								String err = ex.getMessage();
								this.appActivity.getLogger().error(err, ex);
								msg.fail(400, err);
								retFuture.fail(ex);
							}
						});
						
						
					});							
					
					CompositeFuture.join(futures).setHandler(ar -> { // 合并所有for循环结果，返回外面
						msg.reply(result);
					});
					
				}else{
					msg.reply(result);
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

		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		return actionDescriptor;
	}

}
