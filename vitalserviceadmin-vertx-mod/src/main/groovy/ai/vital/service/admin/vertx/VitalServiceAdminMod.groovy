package ai.vital.service.admin.vertx

import groovy.transform.TypeChecked.TypeCheckingInfo;

import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle;

import ai.vital.service.admin.vertx.handler.VitalServiceAdminHandler;
import ai.vital.vitalservice.admin.VitalServiceAdmin;
import ai.vital.vitalservice.factory.VitalServiceFactory;
import ai.vital.vitalsigns.VitalSigns;

public class VitalServiceAdminMod extends Verticle {
	
	public static String ADDRESS = "vitalserviceadmin"
	
	public static boolean initialized = false
	
	public Object start() {
		
		VitalSigns.get()
		
		if(!initialized) {
		
			synchronized (VitalServiceAdminMod.class) {
		
				if(!initialized) {
					
					container.logger.info "Starting Vital Service Admin Verticle  ..."
							
					container.logger.info "Initializing vital service admin singleton..."
							
					Object profile = container.config.get("profile")
							
					if(profile == null) {
						container.logger.info "using default profile"
					} else {
								
						if( ! ( profile instanceof String ) ) throw new RuntimeException("profile parameter must be a string")
								
						container.logger.info "vitalservice profile: ${profile}"
								
						VitalServiceFactory.setServiceProfile(profile)
					}
					
					container.logger.info ("Obtaining vitalservice admin instance")
					VitalServiceAdmin adminService = VitalServiceFactory.getVitalServiceAdmin()
					
					container.logger.info ("vitalservice admin obtained")
					
					initialized = true
					
				}
			}
			
		}
				
			
		
		
		VitalServiceAdminHandler handler = new VitalServiceAdminHandler()		
		
//vertx.eventBus.registerHandler("vitalservice") { message ->
//	message.reply("pong!")
//	container.logger.info("Sent back pong groovy!")
//}
		
		container.logger.info "Registering vital service admin handler..."
		
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
			VitalServiceFactory.getVitalServiceAdmin().close()
		} catch(Exception e) {
			container.logger.error( e )
		}
		
		container.logger.info "Stopped"
		return new Object()
				
	}
	
	
	
	
	
}

