package ocr.sales;

import java.util.List;

import otocloud.framework.app.engine.AppService;
import otocloud.framework.app.engine.AppServiceEngineImpl;
import otocloud.framework.core.OtoCloudComponent;


/**
 * TODO: DOCUMENT ME!
 * @date 2016年11月26日
 * @author lijing@yonyou.com
 */
public class App extends AppServiceEngineImpl
{

	//创建此APP中租户的应用服务实例时调用
	@Override
	public AppService newAppInstance() {
		return new SalesCenterService();
	}


/*    public static void main( String[] args )
    {
    	App app = new App();

    	AppServiceEngineImpl.internalMain("log4j2.xml",
    										"ocr-sales.center.json", 
    										app);
    	
    }*/

	@Override
	public List<OtoCloudComponent> createServiceComponents() {
		// TODO Auto-generated method stub
		return null;
	}   

}
