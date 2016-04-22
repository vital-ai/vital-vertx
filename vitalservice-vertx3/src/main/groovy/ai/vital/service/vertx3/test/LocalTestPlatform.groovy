package ai.vital.service.vertx3.test

import groovy.lang.Closure;
import io.vertx.groovy.core.Vertx

/**
 * Local vertx platform for tests
 * @author Derek
 *
 */
class LocalTestPlatform {

	Vertx vertx
	
	Throwable ex
	
//	public void testNop() {}
	
	private LocalTestPlatform() {}
	
	static LocalTestPlatform newInstance(Closure deployClosure) {
		
		LocalTestPlatform ltp = new LocalTestPlatform()
		
		ltp.vertx = Vertx.vertx()
			
		deployClosure.call(ltp)
		
		ltp.waitNow()
			
		if(ltp.ex != null) throw ltp.ex
		
		return ltp
		
	}
	
	void destroyPlatform() {

		vertx.close()		
		
	}
	
	void waitNow() {
		synchronized (this) {
			this.wait()
		}
	}
	
	void resume() {
		synchronized (this) {
			this.notify()
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
