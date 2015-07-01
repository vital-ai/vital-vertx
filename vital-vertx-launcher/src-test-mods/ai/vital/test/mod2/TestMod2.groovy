package ai.vital.test.mod2

import org.vertx.groovy.platform.Verticle;
import org.vertx.java.core.Future;
import org.slf4j.Logger
import org.slf4j.LoggerFactory;

class TestMod2 extends Verticle {

	private final static Logger log = LoggerFactory.getLogger(TestMod2.class)
	
	@Override
	public Object start(Future<Void> startedResult) {

		log.info("Starting module 2 ... worker ? " + vertx.worker)
		
		Boolean mod2Flag = container.config.get("mod2flag")
		
		
		def timerID = vertx.setTimer(1000) {
			
//			timerID -> println "And one second later this is displayed"
			 
			if(mod2Flag == null) {
				startedResult.setFailure(new RuntimeException("No mod2flag boolean param"))
				return
			}
			
			log.info("Module 2 started");
			
			startedResult.setResult(null)
			
		}
		
		return startedResult
		
	}
	
}
