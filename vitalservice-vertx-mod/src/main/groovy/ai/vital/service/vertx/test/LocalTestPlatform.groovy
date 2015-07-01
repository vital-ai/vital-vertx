package ai.vital.service.vertx.test

import groovy.lang.Closure;

import org.vertx.groovy.core.Vertx;
import org.vertx.java.core.AsyncResult
import org.vertx.java.core.AsyncResultHandler
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;

/**
 * Local vertx platform for tests
 * @author Derek
 *
 */
class LocalTestPlatform {

	PlatformManager pm
	
	Vertx vertx
	
	Throwable ex
	
//	public void testNop() {}
	
	private LocalTestPlatform() {}
	
	static LocalTestPlatform newInstance(Closure deployModulesClosure) {
		
		LocalTestPlatform ltp = new LocalTestPlatform()
		
		ltp.pm = PlatformLocator.factory.createPlatformManager();
			
		ltp.vertx = new Vertx(ltp.pm.vertx)
			
		deployModulesClosure.call(ltp)
		
		ltp.waitNow()
			
		if(ltp.ex != null) throw ltp.ex
		
		return ltp
		
	}
	
	/**
	 * Deploys all modules, must call resume() when everything's complete, all exceptions should be caught saved into ex variable
	 *
	 */
	/*void deployModules() {
		
		pm.deployModule("ai.vital~service~0.2.250", new JsonObject(), 1, new AsyncResultHandler<String>() {
			public void handle(AsyncResult<String> asyncResult) {
				if (asyncResult.succeeded()) {
					println("Vital Service deployment ID is " + asyncResult.result());
				} else {
					ex = asyncResult.cause()
					ex.printStackTrace()
				}
				
				resume()
				
			}
		});
		
	}
	*/
	
	void destroyPlatform() {
		
		if(pm != null) {
			
			pm.undeployAll(new Handler<AsyncResult<Void>>(){
				
				void handle(AsyncResult<Void> event) {
					
					pm.vertx().stop()
					pm.stop()
					
					resume()
				}
				
			});
			
			waitNow()
		}
		pm = null
		
	}
	
	void waitNow() {
		synchronized (pm) {
			pm.wait()
		}
	}
	
	void resume() {
		synchronized (pm) {
			pm.notify()
		}
	}
	
	public void delayed(Closure cl ) {
		
		Thread t = new Thread(){
			
			void run() {
				
				Thread.sleep(10)
				cl.call()
				
			};
		}
		
		t.setDaemon(true)
		t.start()
		
	}
	
}
