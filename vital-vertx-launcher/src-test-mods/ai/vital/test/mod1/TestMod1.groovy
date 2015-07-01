package ai.vital.test.mod1

import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.Future;
import org.slf4j.Logger
import org.slf4j.LoggerFactory;

class TestMod1 extends Verticle {

	private final static Logger log = LoggerFactory.getLogger(TestMod1.class) 
	
	@Override
	public Object start(Future<Void> startedResult) {

		log.info("Starting module 1 ... worker ? " + vertx.worker)
		
		log.info("Module 1 started");
		
		startedResult.setResult(null)
	
		return new Object()	
	}

	
	
}
