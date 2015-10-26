package ai.vital.service.vertx

import groovy.transform.TypeChecked.TypeCheckingInfo;

import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle;

import ai.vital.service.vertx.handler.CallFunctionHandler;
import ai.vital.service.vertx.handler.VitalServiceHandler;
import ai.vital.vitalservice.VitalService;
import ai.vital.vitalservice.factory.VitalServiceFactory;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalServiceKey;

public class VitalServiceMod extends Verticle {
	
	public static String ADDRESS = "vitalservice"
	
	public static String SERVICE_NAME = 'vitalservicevertx'
	
	public Object start() {
		
		VitalSigns.get()
		
		VitalService vitalService = null
		
		synchronized (VitalServiceMod.class) {
			
			for(VitalService vs : VitalServiceFactory.listOpenServices()) {
				if(vs.getName().equals(SERVICE_NAME)) {
					vitalService = vs
					break
				}
			}
			
			
			if(vitalService == null) {
				
				container.logger.info "Starting Vital Service Verticle  ..."
				
				container.logger.info "Initializing vital service singleton..."
						
				Object profile = container.config.get("profile")
						
				if(profile == null) {
					container.logger.info "using default profile: ${VitalServiceFactory.DEFAULT_PROFILE}"
					profile = VitalServiceFactory.DEFAULT_PROFILE
				} else {
							
					if( ! ( profile instanceof String ) ) throw new RuntimeException("profile parameter must be a string")
							
					container.logger.info "vitalservice profile: ${profile}"
					
				}
				
				Object key = container.config.get("key")
				if(key == null) throw new RuntimeException("No vitalservice 'key' string parameter")
				if(!(key instanceof String)) throw new RuntimeException("vitalservice 'key' string parameter")
				
				
				container.logger.info ("Opening a vitalservice instance, name: ${SERVICE_NAME}")
				
				VitalServiceKey keyObj = new VitalServiceKey().generateURI((VitalApp) null)
				keyObj.key = key
				vitalService = VitalServiceFactory.openService(keyObj, profile, SERVICE_NAME)
		//						VitalServiceFactory.setServiceProfile(profile)
				
				container.logger.info ("vitalservice opened")
				
			} else {
			
				container.logger.info("Vitalservice instance already opened")
			
			}
			
		}
		
//		if(!initialized) {
//		
//			synchronized (VitalServiceMod.class) {
//		
//				if(!initialized) {
//					
//					container.logger.info "Starting Vital Service Verticle  ..."
//							
//					container.logger.info "Initializing vital service singleton..."
//							
//					Object profile = container.config.get("profile")
//							
//					if(profile == null) {
//						container.logger.info "using default profile: ${VitalServiceFactory.DEFAULT_PROFILE}"
//						profile = VitalServiceFactory.DEFAULT_PROFILE
//					} else {
//								
//						if( ! ( profile instanceof String ) ) throw new RuntimeException("profile parameter must be a string")
//								
//						container.logger.info "vitalservice profile: ${profile}"
//						
//					}
//					
//					Object key = container.config.get("key")
//					if(key == null) throw new RuntimeException("No vitalservice 'key' string parameter")
//					if(!(key instanceof String)) throw new RuntimeException("vitalservice 'key' string parameter")
//					
//					
//					container.logger.info ("Opening a vitalservice instance, name: ${SERVICE_NAME}")
//					
//					VitalServiceKey keyObj = new VitalServiceKey().generateURI((VitalApp) null)
//					keyObj.key = key
//					vitalService = VitalServiceFactory.openService(keyObj, profile, SERVICE_NAME)
////						VitalServiceFactory.setServiceProfile(profile)
//					
//					container.logger.info ("vitalservice opened")
//					
//					initialized = true
//					
//				}
//			}
//			
//		}
//		
//		if(vitalService == null) {
//			for(VitalService vs : VitalServiceFactory.listOpenServices()) {
//				if(vs.getName().equals(SERVICE_NAME)) {
//					vitalService = vs
//					break
//				}
//			}
//			if(vitalService == null) throw new RuntimeException("Service instance '${SERVICE_NAME}' not found")
//		}
				

		VitalServiceHandler handler = new VitalServiceHandler(vertx, vitalService)		
		
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
			synchronized (VitalServiceMod.class) {
				for(VitalService vs : VitalServiceFactory.listOpenServices()) {
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

