package ai.vital.service.vertx.main

import org.vertx.java.core.AsyncResult
import org.vertx.java.core.AsyncResultHandler
import org.vertx.java.core.json.JsonObject
import org.vertx.java.platform.PlatformManager
import org.vertx.java.platform.PlatformLocator

/**
 * A sample application with embedded vertx to test the module as a standalone component
 * @author Derek
 *
 */
class VertxMain {

	public static void main(String[] args) {
		
		PlatformManager pm = PlatformLocator.factory.createPlatformManager();
		
		println "Deploying vital service module..."
		
		pm.deployModule("ai.vital~service~1.0", new JsonObject(), Runtime.getRuntime().availableProcessors(), new AsyncResultHandler<String>() {
			public void handle(AsyncResult<String> asyncResult) {
				if (asyncResult.succeeded()) {
					println("Vital Service deployment ID is " + asyncResult.result());
				} else {
					asyncResult.cause().printStackTrace();
				}
			}
		});
	
		println "CWD: " + new File(".").absolutePath
		
		// Configuration for the web server
		def webServerConf = [
		
		  // Normal web server stuff
		  web_root: new File(".").absolutePath + '/web',
		  port: 8080,
		  host: 'localhost',
		  ssl: false,
		
		  // Configuration for the event bus client side bridge
		  // This bridges messages from the client side to the server side event bus
		  bridge: true,
		
		  // This defines which messages from the client we will let through
		  // to the server side
		  inbound_permitted: [
			// Allow calls to login
			[
	//		  address: 'vertx.basicauthmanager.login'
			  address: 'vitalservice'
			]
		  ],
		
		  // This defines which messages from the server we will let through to the client
		  outbound_permitted: [
			[:]
		  ]
		]
		
		pm.deployModule("io.vertx~mod-web-server~2.0.0-final", new JsonObject(webServerConf), 1, new AsyncResultHandler<String>() {
			public void handle(AsyncResult<String> asyncResult) {
				if (asyncResult.succeeded()) {
					println("Mod-web-server deployment ID is " + asyncResult.result());
				} else {
					asyncResult.cause().printStackTrace();
				}
			}
		});

		Thread.sleep(3000)
		
		pm.deployVerticle("groovy:ai.vital.service.vertx.test.TestVerticle", null, new URL[0], 1, null, null)
			
		// Prevent the JVM from exiting
		System.in.read();
	}
	
}
