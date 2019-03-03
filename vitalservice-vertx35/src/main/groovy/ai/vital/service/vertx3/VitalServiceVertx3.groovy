package ai.vital.service.vertx3

import io.vertx.core.eventbus.Message
import io.vertx.core.AbstractVerticle

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import ai.vital.service.vertx3.handler.VitalServiceHandler
import ai.vital.service.vertx3.VitalServiceVertx3;
import ai.vital.vitalservice.VitalService
import ai.vital.vitalservice.config.VitalServiceConfig
import ai.vital.vitalservice.factory.VitalServiceFactory
import ai.vital.vitalservice.factory.VitalServiceFactory.ServiceConfigWrapper
import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalServiceKey

/**
 * Registers a set of vitalservice instances
 * @author Derek
 */
public class VitalServiceVertx3 extends AbstractVerticle {
	
	private final static Logger log = LoggerFactory.getLogger(VitalServiceVertx3.class)
	
	public static String ADDRESS_PREFIX = "vitalservice."
	
	public static String SERVICE_NAME_PREFIX = 'vitalservicevertx.'
	
	public static Map<String, VitalService> registeredServices = [:]
	
	public void start() {
		
		log.info "Starting Vital Service Vertx3 instance"
		
		context = vertx.getOrCreateContext()
		
		VitalSigns.get()

		Set<String> appIDs = new HashSet<String>()
		
		Map<String, VitalService> openedServices = [:]
		
		for(VitalService vs : VitalServiceFactory.listOpenServices()) {
			openedServices.put(vs.getName(), vs)	
		}
		
		synchronized (VitalServiceVertx3.class) {
			
			List services = context.config().get("services")
					
			if(services == null) throw new RuntimeException("No 'services' list config param")
			
			for(Object cfgO : services) {
				
				if(!(cfgO) instanceof Map) throw new RuntimeException("Services list config element object must be a map")
				
				Map<String, Object> cfg = cfgO
				
				Object keyO = cfg.get("key")
				if(keyO == null) throw new RuntimeException("No vitalservice 'key' string parameter")
				if(!(keyO instanceof String)) throw new RuntimeException("vitalservice 'key' string parameter")
				String key = keyO
				
				Object profileO = cfg.get('profile')
				if( profileO != null && ! ( profileO instanceof String ) ) throw new RuntimeException("service 'profile' parameter must be a string")
				String profile = (String) profileO
				
				//for inline config
				Object serviceCfgO = cfg.get("config")
				if(serviceCfgO  != null && !(serviceCfgO  instanceof String)) throw new RuntimeException("service 'config' object must be a string")
				String serviceCfgString = serviceCfgO
				
				if(profile == null && serviceCfgString == null) throw new RuntimeException("Expected service 'profile' string or 'config' string")
				
				if(profile != null && serviceCfgString != null) throw new RuntimeException("Cannot use both service 'profile' string and 'config' string")

				String appID = null
				
				VitalServiceConfig serviceConfig = null
				
				if(profile != null) {
					
					ServiceConfigWrapper scw = VitalServiceFactory.getProfileConfig(profile)
					if(scw == null) throw new RuntimeException("Service profile not found: ${profile}")
					serviceConfig = scw.serviceConfig
					if(serviceConfig.getApp() == null) throw new RuntimeException("appID not set in service profile: ${profile}")
					
					appID = scw.app.appID.toString()
					
				} else {
				
					serviceConfig = VitalServiceFactory.parseConfigString(serviceCfgString)
					
					if(serviceConfig.getApp() == null) throw new RuntimeException("No app in parsed service config from string")
					
					appID = serviceConfig.getApp().appID.toString()
					
				}
				
				
				String serviceName = SERVICE_NAME_PREFIX + appID
				
				if( ! appIDs.add(appID) ) throw new RuntimeException("More than 1 service defined for appID: " + appID)
						
				if(registeredServices.containsKey(appID)) {
					log.warn "Vital Service ${serviceName} already registered"
					continue
				}
				
//				VitalServiceFactory.openService()
				
				VitalServiceKey keyObj = new VitalServiceKey().generateURI((VitalApp) null)
				keyObj.key = key
				
				VitalService vitalService = openedServices.get(serviceName)
				if(vitalService != null) {
					log.warn("Vital Service ${serviceName} already opened, registering only")
				} else {
					vitalService = VitalServiceFactory.openService(keyObj, serviceConfig, serviceName)
				}
				
				registeredServices.put(appID, vitalService)
				
				log.info "Vital Service ${serviceName} registered"
				
				
			}
			
		}

		
		//for each worker register more
		
		if(registeredServices.size() == 0) throw new RuntimeException("No vital services registered")
		
		for(VitalService vitalService : registeredServices.values()) {
			
			VitalServiceHandler handler = new VitalServiceHandler(vertx, vitalService)		
			
			String address = ADDRESS_PREFIX + vitalService.getApp().appID.toString()
			
			log.info "Registering vital service handler ${vitalService.getName()} -> ${address}"
			
			
			vertx.eventBus().consumer(address) { Message message ->
	//			message.
				handler.handle(message)
			}
			
			log.info "Handler registered ${vitalService.getName()} -> ${address}"
			
		}
		
	}

	@Override
	public void stop() {
		
		log.info "Stopping Vital Service verticle, closing active service instance..."
		synchronized (VitalServiceVertx3.class) {
			for(VitalService vs : VitalServiceFactory.listOpenServices()) {
				try {
					if(vs.getName().startsWith(SERVICE_NAME_PREFIX)) {
						vs.close()
					}
				} catch(Exception e) {
					log.error( e )
				}
			}
		}
		
		registeredServices.clear()
		
		log.info "Stopped"
				
	}
	
}

