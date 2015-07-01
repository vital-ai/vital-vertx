package ai.vital.service.vertx

import groovy.transform.TypeChecked.TypeCheckingInfo;

import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle;

import ai.vital.service.vertx.handler.CallFunctionHandler;
import ai.vital.service.vertx.handler.VitalServiceHandler;
import ai.vital.vitalservice.VitalService;
import ai.vital.vitalservice.factory.VitalServiceFactory;
import ai.vital.vitalsigns.VitalSigns;

public class VitalServiceMod extends Verticle {
	
	public static String ADDRESS = "vitalservice"
	
	public static boolean initialized = false
	
	public Object start() {
		
		VitalSigns.get()
		
		if(!initialized) {
		
			synchronized (VitalServiceMod.class) {
		
				if(!initialized) {
					
					container.logger.info "Starting Vital Service Verticle  ..."
							
					container.logger.info "Initializing vital service singleton..."
							
					Object profile = container.config.get("profile")
							
					if(profile == null) {
						container.logger.info "using default profile"
					} else {
								
						if( ! ( profile instanceof String ) ) throw new RuntimeException("profile parameter must be a string")
								
						container.logger.info "vitalservice profile: ${profile}"
								
						VitalServiceFactory.setServiceProfile(profile)
					}
					
					container.logger.info ("Obtaining vitalservice instance")
					VitalService service = VitalServiceFactory.getVitalService()
					
					container.logger.info ("vitalservice obtained")
					
					initialized = true
					
				}
			}
			
		}
				

		VitalServiceHandler handler = new VitalServiceHandler(vertx)		
		
//vertx.eventBus.registerHandler("vitalservice") { message ->
//	message.reply("pong!")
//	container.logger.info("Sent back pong groovy!")
//}
		
		container.logger.info "Registering vital service handler..."
		
		vertx.eventBus.registerHandler(ADDRESS) { Message message ->
//			message.
			handler.handle(message)
		}
		
		container.logger.info "Handler registered."
		
		return new Object()
		
	}

	@Override
	public Object stop() {
		
		try {
			container.logger.info "Stopping Vital Service module, closing active service instance..."
			VitalServiceFactory.getVitalService().close()
		} catch(Exception e) {
			container.logger.error( e )
		}
		
		container.logger.info "Stopped"
		return new Object()
				
	}
	
	
	
	
	
}

