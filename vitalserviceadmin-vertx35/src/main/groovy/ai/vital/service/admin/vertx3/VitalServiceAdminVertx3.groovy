package ai.vital.service.admin.vertx3

import groovy.transform.TypeChecked.TypeCheckingInfo

import org.slf4j.Logger;
import org.slf4j.LoggerFactory

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message

import ai.vital.service.admin.vertx3.handler.VitalServiceAdminHandler
import ai.vital.service.admin.vertx3.VitalServiceAdminVertx3;
import ai.vital.vitalservice.admin.VitalServiceAdmin;
import ai.vital.vitalservice.factory.VitalServiceFactory;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalServiceAdminKey;

public class VitalServiceAdminVertx3 extends AbstractVerticle {
	
	public static String ADDRESS = "vitalserviceadmin"
	
	public static String SERVICE_NAME = 'vitalserviceadminvertx'
	
	private final static Logger log = LoggerFactory.getLogger(VitalServiceAdminVertx3.class)
	
	public void start() {
		
		VitalSigns.get()
		
		context = vertx.getOrCreateContext()
		
		VitalServiceAdmin vitalServiceAdmin = null
		
		synchronized (VitalServiceAdminVertx3.class) {
			
			for(VitalServiceAdmin vs : VitalServiceFactory.listOpenAdminServices()) {
				if(vs.getName().equals(SERVICE_NAME)) {
					vitalServiceAdmin = vs
					break
				}
			}
			
			
			if(vitalServiceAdmin == null) {
				
				log.info "Starting Vital Service Admin Verticle  ..."
				
				log.info "Initializing vital service admin singleton..."
						
				Object profile = context.config().get("profile")
						
				if(profile == null) {
					log.info "using default profile: ${VitalServiceFactory.DEFAULT_PROFILE}"
					profile = VitalServiceFactory.DEFAULT_PROFILE
				} else {
							
					if( ! ( profile instanceof String ) ) throw new RuntimeException("profile parameter must be a string")
							
					log.info "vitalservice admin profile: ${profile}"
					
				}
				
				Object key = context.config().get("key")
				if(key == null) throw new RuntimeException("No vitalservice admin 'key' string parameter")
				if(!(key instanceof String)) throw new RuntimeException("vitalservice admin 'key' string parameter")
				
				
				log.info ("Opening a vitalservice instance, name: ${SERVICE_NAME}")
				
				VitalServiceAdminKey keyObj = new VitalServiceAdminKey().generateURI((VitalApp) null)
				keyObj.key = key
				vitalServiceAdmin = VitalServiceFactory.openAdminService(keyObj, profile, SERVICE_NAME)
		//						VitalServiceFactory.setServiceProfile(profile)
				
				log.info ("vitalservice admin opened")
				
			} else {
			
				log.info("Vitalservice admin instance already opened")
			
			}
			
		}
		
		
		VitalServiceAdminHandler handler = new VitalServiceAdminHandler(vertx, vitalServiceAdmin)		
		
//vertx.eventBus.registerHandler("vitalservice") { message ->
//	message.reply("pong!")
//	container.logger.info("Sent back pong groovy!")
//}
		
		log.info "Registering vital service admin handler..."
		
		vertx.eventBus().consumer(ADDRESS) { Message message ->
			handler.handle(message)
		}
		
		log.info "Handler registered."
		
	}

	@Override
	public void stop() {
		
		try {
			log.info "Stopping Vital Service Vertx3, closing active service instance..."
			synchronized (VitalServiceAdminVertx3.class) {
				for(VitalServiceAdmin vs : VitalServiceFactory.listOpenAdminServices()) {
					if(vs.getName().equals(SERVICE_NAME)) {
						vs.close()
					}
				}
			}
		} catch(Exception e) {
			log.error( e )
		}
		
		log.info "Stopped"
						
	}
	
	
	
	
	
}

