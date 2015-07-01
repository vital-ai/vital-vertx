package ai.vital.vertx.launcher

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValue;

import groovy.util.CliBuilder
import java.util.Map.Entry

import org.vertx.java.core.AsyncResult
import org.vertx.java.core.Handler
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformManager
import org.vertx.java.platform.PlatformLocator
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.Level;
import org.slf4j.Logger
import org.slf4j.LoggerFactory;

/**
 * Simple main block to start standalone vertx instance that will deploy all modules using launcher
 * @author Derek
 *
 */
class VitalVertxLauncherMain {

	private final static Logger log = LoggerFactory.getLogger(VitalVertxLauncherMain.class)
	
	static main(args) {
	
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO)
		org.apache.log4j.Logger.getRootLogger().addAppender(new ConsoleAppender())
		
		CliBuilder cli = new CliBuilder(usage: 'vitalcustomerapp [options]')
		cli.with {
			c longOpt: '--config', "config file", args:1, required: true
		}
		
		def options = cli.parse(args)
		if(!options) return
		
		boolean deployAdmin = options.a
		
		File configFile = new File(options.c)
		
		if(!configFile.isFile()) {
			System.err.println("Config file not found or not a file: " +configFile.absolutePath)
			System.exit(1)
			return
		}
		
		final PlatformManager pm = PlatformLocator.factory.createPlatformManager();
		
		println "Config file: ${configFile.absolutePath}"
		
		
		JsonObject jc = new JsonObject();
		jc.putString("configPath", configFile.absolutePath); 
		pm.deployModule("vital-ai~vital-vertx-launcher~1.0.0", jc, 1, false, new Handler<AsyncResult<String>>(){
			
			public void handle(AsyncResult<String> result){
				
				if( result.succeeded() ) {
					log.info("Launcher succeeded, deploymentID: ${result.result()}")
				} else {
					log.error("Launcher failed", result.cause())
					System.exit(1)
				}
				
			}
			
		});
				
	
		while(true) {
			
			java.lang.Thread.sleep(10000)
			
		}
	
	}

}
