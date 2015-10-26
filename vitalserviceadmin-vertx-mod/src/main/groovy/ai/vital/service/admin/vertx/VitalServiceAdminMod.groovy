package ai.vital.service.admin.vertx

import groovy.transform.TypeChecked.TypeCheckingInfo;

import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle;

import ai.vital.service.admin.vertx.handler.VitalServiceAdminHandler;
import ai.vital.vitalservice.admin.VitalServiceAdmin;
import ai.vital.vitalservice.factory.VitalServiceFactory;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalServiceAdminKey;

public class VitalServiceAdminMod extends Verticle {
	
	public static String ADDRESS = "vitalserviceadmin"
	
	public static String SERVICE_NAME = 'vitalserviceadminvertx'
	
	public Object start() {
		
		VitalSigns.get()
		
		
		VitalServiceAdmin vitalServiceAdmin = null
		
		synchronized (VitalServiceAdminMod.class) {
			
			for(VitalServiceAdmin vs : VitalServiceFactory.listOpenAdminServices()) {
				if(vs.getName().equals(SERVICE_NAME)) {
					vitalServiceAdmin = vs
					break
				}
			}
			
			
			if(vitalServiceAdmin == null) {
				
				container.logger.info "Starting Vital Service Admin Verticle  ..."
				
				container.logger.info "Initializing vital service admin singleton..."
						
				Object profile = container.config.get("profile")
						
				if(profile == null) {
					container.logger.info "using default profile: ${VitalServiceFactory.DEFAULT_PROFILE}"
					profile = VitalServiceFactory.DEFAULT_PROFILE
				} else {
							
					if( ! ( profile instanceof String ) ) throw new RuntimeException("profile parameter must be a string")
							
					container.logger.info "vitalservice admin profile: ${profile}"
					
				}
				
				Object key = container.config.get("key")
				if(key == null) throw new RuntimeException("No vitalservice admin 'key' string parameter")
				if(!(key instanceof String)) throw new RuntimeException("vitalservice admin 'key' string parameter")
				
				
				container.logger.info ("Opening a vitalservice instance, name: ${SERVICE_NAME}")
				
				VitalServiceAdminKey keyObj = new VitalServiceAdminKey().generateURI((VitalApp) null)
				keyObj.key = key
				vitalServiceAdmin = VitalServiceFactory.openAdminService(keyObj, profile, SERVICE_NAME)
		//						VitalServiceFactory.setServiceProfile(profile)
				
				container.logger.info ("vitalservice admin opened")
				
			} else {
			
				container.logger.info("Vitalservice admin instance already opened")
			
			}
			
		}
		
//		if(!initialized) {
//		
//			synchronized (VitalServiceAdminMod.class) {
//		
//				if(!initialized) {
//					
//					container.logger.info "Starting Vital Service Admin Verticle  ..."
//							
//					container.logger.info "Initializing vital service admin singleton..."
//							
//					Object profile = container.config.get("profile")
//							
//					if(profile == null) {
//						container.logger.info "using default profile"
//					} else {
//								
//						if( ! ( profile instanceof String ) ) throw new RuntimeException("profile parameter must be a string")
//								
//						container.logger.info "vitalservice profile: ${profile}"
//								
//						VitalServiceFactory.setServiceProfile(profile)
//					}
//					
//					container.logger.info ("Obtaining vitalservice admin instance")
//					VitalServiceAdmin adminService = VitalServiceFactory.getVitalServiceAdmin()
//					
//					container.logger.info ("vitalservice admin obtained")
//					
//					initialized = true
//					
//				}
//			}
//			
//		}
				
			
		
		
		VitalServiceAdminHandler handler = new VitalServiceAdminHandler(vertx, vitalServiceAdmin)		
		
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
		
//		try {
//			container.logger.info "Stopping Vital Service module, closing active service instance..."
//			VitalServiceFactory.getVitalServiceAdmin().close()
//		} catch(Exception e) {
//			container.logger.error( e )
//		}
//		
//		container.logger.info "Stopped"
//		return new Object()

		try {
			container.logger.info "Stopping Vital Service module, closing active service instance..."
			synchronized (VitalServiceAdminMod.class) {
				for(VitalServiceAdmin vs : VitalServiceFactory.listOpenAdminServices()) {
					if(vs.getName().equals(SERVICE_NAME)) {
						vs.close()
					}
				}
			}
		} catch(Exception e) {
			container.logger.error( e )
		}
		
		container.logger.info "Stopped"
		return new Object()
						
	}
	
	
	
	
	
}

