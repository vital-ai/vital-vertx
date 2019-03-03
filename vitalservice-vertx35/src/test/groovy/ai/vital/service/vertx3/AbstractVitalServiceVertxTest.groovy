package ai.vital.service.vertx3

import groovy.lang.Closure
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import junit.framework.TestCase;

import ai.vital.service.vertx3.test.LocalTestPlatform
import ai.vital.service.vertx3.VitalServiceVertx3;;


/**
 * Base class for vitalservice vertx verticle tests, provides a local vertx platform
 * @author Derek
 *
 */
abstract class AbstractVitalServiceVertxTest extends TestCase {

	LocalTestPlatform ltp
	
//	public void testNop() {}
	
	@Override
	protected void setUp() throws Exception {

		ltp = LocalTestPlatform.newInstance { LocalTestPlatform ltp ->
			
			deployVerticle(ltp)
			
		}
		
	}
	
	/**
	 * Deploys all verticles, must call resume() when everything's complete, all exceptions should be caught saved into ex variable
	 *
	 */
	void deployVerticle(LocalTestPlatform ltp) {
		
		JsonObject cfg = new JsonObject();
		JsonObject serviceCfg = new JsonObject()
		serviceCfg.put('profile', 'default')
		serviceCfg.put("key", "aaaa-aaaa-aaaa")
		JsonArray ja = new JsonArray();
		ja.add(serviceCfg)
		cfg.put('services', ja)
		
		
		ltp.vertx.deployVerticle("groovy:" + VitalServiceVertx3.class.canonicalName, [worker: true, instances: 1, config: cfg]) { AsyncResult<String> asyncResult ->
			
			if (asyncResult.succeeded()) {
				println("Vital Service deployment ID is " + asyncResult.result());
			} else {
				ltp.ex = asyncResult.cause()
				ltp.ex.printStackTrace()
			}
				
			ltp.resume()
				
		}
	
	}
	
	
	@Override
	protected void tearDown() throws Exception {

		if(ltp != null) {
			ltp.destroyPlatform()
		}
		ltp = null
		
	}

}
