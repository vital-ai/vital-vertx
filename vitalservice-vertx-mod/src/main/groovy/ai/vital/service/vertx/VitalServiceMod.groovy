package ai.vital.service.vertx

import groovy.transform.TypeChecked.TypeCheckingInfo;

import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.platform.Verticle;

import ai.vital.service.vertx.handler.CallFunctionHandler;
import ai.vital.service.vertx.handler.VitalServiceHandler;
import ai.vital.vitalservice.VitalService;
import ai.vital.vitalservice.config.VitalServiceConfig;
import ai.vital.vitalservice.factory.VitalServiceFactory;
import ai.vital.vitalservice.factory.VitalServiceFactory.ServiceConfigWrapper;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalServiceKey;

/**
 * Registers a set of vitalservice instances
 * @author Derek
 */
public class VitalServiceMod extends Verticle {
	
	public static String ADDRESS_PREFIX = "vitalservice."
	
	public static String SERVICE_NAME_PREFIX = 'vitalservicevertx.'
	
	public static Map<String, VitalService> registeredServices = [:]
	
	public Object start() {
		
		container.logger.info "Starting Vital Service Mod instance"
		
		VitalSigns.get()

		Set<String> appIDs = new HashSet<String>()
		
		Map<String, VitalService> openedServices = [:]
		
		for(VitalService vs : VitalServiceFactory.listOpenServices()) {
			openedServices.put(vs.getName(), vs)	
		}
		
		synchronized (VitalServiceMod.class) {
			
			List services = container.config.get("services")
					
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
					container.logger.warn "Vital Service ${serviceName} already registered"
					continue
				}
				
//				VitalServiceFactory.openService()
				
				VitalServiceKey keyObj = new VitalServiceKey().generateURI((VitalApp) null)
				keyObj.key = key
				
				VitalService vitalService = openedServices.get(serviceName)
				if(vitalService != null) {
					container.logger.warn("Vital Service ${serviceName} already opened, registering only")
				} else {
					vitalService = VitalServiceFactory.openService(keyObj, serviceConfig, serviceName)
				}
				
				registeredServices.put(appID, vitalService)
				
				container.logger.info "Vital Service ${serviceName} registered"
				
				
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

		
		//for each worker register more
		
		if(registeredServices.size() == 0) throw new RuntimeException("No vital services registered")
		
		for(VitalService vitalService : registeredServices.values()) {
			
			VitalServiceHandler handler = new VitalServiceHandler(vertx, vitalService)		
			
			String address = ADDRESS_PREFIX + vitalService.getApp().appID.toString()
			
			container.logger.info "Registering vital service handler ${vitalService.getName()} -> ${address}"
			
			
			vertx.eventBus.registerHandler(address) { Message message ->
	//			message.
				handler.handle(message)
			}
			
			container.logger.info "Handler registered ${vitalService.getName()} -> ${address}"
			
		}
		
		return new Object()
						
//vertx.eventBus.registerHandler("vitalservice") { message ->
//	message.reply("pong!")
//	container.logger.info("Sent back pong groovy!")
//}
		

		
	}

	@Override
	public Object stop() {
		
			container.logger.info "Stopping Vital Service module, closing active service instance..."
			synchronized (VitalServiceMod.class) {
				for(VitalService vs : VitalServiceFactory.listOpenServices()) {
					try {
						if(vs.getName().startsWith(SERVICE_NAME_PREFIX)) {
							vs.close()
						}
					} catch(Exception e) {
						container.logger.error( e )
					}
				}
			}
		
		registeredServices.clear()
		
		container.logger.info "Stopped"
		return new Object()
				
	}
	
	
	
	
	
}

