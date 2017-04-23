package ocr.sales.channelrestocking;


import ocr.common.Counter;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionContextTransfomer;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.CommandMessage;

/**
 * TODO: 渠道补货批量新增
 * @date 2016年11月15日
 * @author lijing
 */
public class ChannelRestockingBatchCreateHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "batch_create";

	public ChannelRestockingBatchCreateHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ADDRESS;
	}

	//处理器
	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		
		MultiMap headerMap = msg.headers();
		
    	//当前操作人信息
    	JsonObject actor = ActionContextTransfomer.fromMessageHeaderToActor(headerMap);    	

		
		JsonArray datas = msg.body().getJsonArray("content");
		int size = datas.size();
		if(size == 0){
			msg.reply("ok");
			return;
		}		
		
		Future<JsonObject> repFuture = Future.future();
		
		Counter<Integer> successed_count = new Counter<Integer>();
		successed_count.total = 0;
		Counter<Integer> failed_count = new Counter<Integer>();
		failed_count.total = 0;
		recordReplicationData(actor, 0, size, datas, successed_count, failed_count, repFuture);
		
		repFuture.setHandler(handler->{
			if(handler.succeeded()){
				msg.reply(handler.result());
			}else{
				Throwable errThrowable = handler.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.fail(250, errMsgString);
			}
		});

	}
	
	
	private void recordReplicationData(JsonObject actor, int i, int size, JsonArray datas, 
			Counter<Integer> successed_count, Counter<Integer> failed_count, Future<JsonObject> repFuture){
		
		JsonObject replenishment = datas.getJsonObject(i);
		
    	String boId = replenishment.getString("bo_id");
    	String partnerAcct = replenishment.getJsonObject("channel").getString("link_account"); //交易单据一般要记录协作方
    	
    	//记录事实对象（业务数据），会根据ActionDescriptor定义的状态机自动进行状态变化，并发出状态变化业务事件
    	//自动查找数据源，自动进行分表处理
    	this.recordFactData(null, appActivity.getBizObjectType(), replenishment, boId, actor, null, result->{
			if (result.succeeded()) {
				successed_count.total++;
			} else {
				Throwable errThrowable = result.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				failed_count.total++;
			}
			final int idx = i + 1;
			if(idx < size){			
				recordReplicationData(actor, idx, size, datas, successed_count, failed_count, repFuture);
			} else{
				repFuture.complete(new JsonObject().put("successed_count", successed_count.total)
									  .put("failed_count", failed_count.total));
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
		//handlerDescriptor.setMessageFormat("command");
		
		//参数
/*		List<ApiParameterDescriptor> paramsDesc = new ArrayList<ApiParameterDescriptor>();
		paramsDesc.add(new ApiParameterDescriptor("targetacc",""));		
		paramsDesc.add(new ApiParameterDescriptor("soid",""));		
		
		actionDescriptor.getHandlerDescriptor().setParamsDesc(paramsDesc);	*/
				
		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		//状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, 
				null, "created");
		bizStateSwitchDesc.setWebExpose(true); //是否向web端发布事件		
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);
		
		return actionDescriptor;
	}
	
	
}
