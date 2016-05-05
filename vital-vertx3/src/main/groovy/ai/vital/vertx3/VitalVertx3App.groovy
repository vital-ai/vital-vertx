package ai.vital.vertx3

import java.util.Map.Entry

import org.junit.After;
import org.slf4j.Logger
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigObject

import ai.vital.service.vertx3.VitalServiceVertx3
import ai.vital.service.vertx3.async.VitalServiceAsyncClient;
import ai.vital.vertx3.VitalVertx3App.VerticleConfig
import ai.vital.vitalservice.EndpointType;
import ai.vital.vitalservice.VitalService;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classloader.VitalSignsRootClassLoader
import ai.vital.vitalsigns.conf.VitalSignsConfig
import ai.vital.vitalsigns.conf.VitalSignsConfig.DomainsStrategy
import ai.vital.vitalsigns.conf.VitalSignsConfig.DomainsSyncMode;
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.groovy.core.Vertx

class VitalVertx3App {

	String name
	
	public final static String VITALSERVICEVERTX3 = 'groovy:' + VitalServiceVertx3.class.canonicalName
	
	public final static String VITALAUTHVERTX3 = 'groovy:ai.vital.auth.vertx3.VitalAuthManager'
	
	public Map globalConfig
	
	//predefined config objects ? 
	public Map vitalserviceConfig
	
	public Map vitalauthConfig

	DomainsSyncImplementation syncImpl
	
	private final static Logger log = LoggerFactory.getLogger(VitalVertx3App.class)
	
	boolean syncDomains = false
	
	Integer syncDomainsIntervalSeconds
	
	List<VerticleConfig> verticlesList = []
	
	Vertx vertx
	
	File mergedJsonSchemaFileLocation
			
	public static class VerticleConfig {
		
		String name
		
		String deploymentID
		
		boolean worker = false
		
		boolean reloadable = true
		
		int instances = 1
		
		@Override
		public String toString() {
			return "${name} reloadable: ${reloadable} - worker: ${worker} instances: ${instances}"
		}
		
	}
	
	public VitalVertx3App configure(Config config) {
	
		name = config.getString("name")
		
		log.info("Configuring app: ${name}")
		
		syncDomains = config.getBoolean("syncDomains")
		
		log.info("syncDomains: ${syncDomains}")
		
		if(syncDomains) {
			
			syncDomainsIntervalSeconds = config.getInt("syncDomainsIntervalSeconds")
			
			log.info("syncDomainsIntervalSeconds: ${syncDomainsIntervalSeconds}")
			
			if(syncDomainsIntervalSeconds <= 0) {
				throw new RuntimeException("syncDomainsIntervalSeconds must be > 0")
			}
			
			String mergedJsonSchemaFileLocationParam = config.getString("mergedJsonSchemaFileLocation")
			
			mergedJsonSchemaFileLocation = new File(mergedJsonSchemaFileLocationParam)
			
			log.info("mergedJsonSchemaFileLocation: ${mergedJsonSchemaFileLocation}")
			
		}
		
		ConfigList verticlesListInput = config.getList("verticles")
		
		List<Map<String, Object>> unwrappedList = verticlesListInput.unwrapped()
		
		globalConfig = config.root().unwrapped()
		
		vitalserviceConfig = globalConfig.get('vitalservice')
		
		List<VerticleConfig> _l = []
		
		boolean vitalServiceFound = false
		
		//make sure the order is preserved, otherwise add priority property
		int c = 0
		for( Map<String, Object> e : unwrappedList) {
			
			c++
			
			String name = e.get('name')
			
			if(!name) throw new RuntimeException("No verticle name in config #{c}: ${e}")
			
			Map<String, Object> _v = e
			
			VerticleConfig vc = new VerticleConfig()
			
			vc.name = name

			Boolean worker = _v.get('worker')
			
			if(worker != null) {
				vc.worker = worker.booleanValue()
			}			

			Integer instances = _v.get('instances')
			
			if(instances != null) {
				vc.instances = instances.intValue()
				if(vc.instances < 0) throw new RuntimeException("Instances count must be >= 0 : ${vc.instances}, 0 for available current cpu count")
				if(vc.instances == 0) {
					vc.instances = Runtime.getRuntime().availableProcessors()
				}
			}
						
			Boolean reloadable = _v.get("reloadable")
			
			if(reloadable != null) {
				vc.reloadable = reloadable.booleanValue()
			} 

			if(name == VITALSERVICEVERTX3) {
				vitalServiceFound = true
				if(vc.reloadable) throw new RuntimeException("${VITALSERVICEVERTX3} verticle cannot be set as reloadable")
				if(!vc.worker) {
					log.warn("${VITALSERVICEVERTX3} not configured as worker")
				}
				
				//check if config available
				vitalserviceConfig = globalConfig.get('vitalservice')
				if(vitalserviceConfig == null) throw new RuntimeException("${VITALSERVICEVERTX3} verticle requires vitalservice config object")
				
			}
			
			if(name == VITALAUTHVERTX3) {
				
				if(vc.reloadable) throw new RuntimeException("${VITALAUTHVERTX3} verticle cannot be set as reloadable")
				
				vitalauthConfig = globalConfig.get('vitalauth')
				if(vitalauthConfig == null) throw new RuntimeException("${VITALAUTHVERTX3} verticle requires auth config object")
				
			}
			
			_l.add(vc)
						
		}
		
		if(syncDomains && !vitalServiceFound) {
			throw new RuntimeException("SyncDomains may only be enabled with ${VITALSERVICEVERTX3} verticle")
		}
		
//		for(Object l : verticlesListInput) {
//			
//			if(!(l instanceof ConfigObject)) throw new RuntimeException("verticles must contain")
//			
//		}
		
		verticlesList = Collections.unmodifiableList(_l)
				
		log.info("verticles list: [${verticlesList.size()}]")
		
		if(verticlesList.size() == 0) {
			throw new RuntimeException("verticles list must not be empty")
		}
		
		for(VerticleConfig vc : verticlesList) {
			log.info(vc.toString())
		}
		
		//check if it has vitalservice verticle
		
		return this
		
	}
	
	public VitalVertx3App start(String appJar, Handler<Throwable> handler) {
		
		log.info("Staring app ${name}")
		
		this.vertx = Vertx.vertx()
		
		log.info("Initializing VitalSigns singleton...")
		
		VitalSigns vs = VitalSigns.get()
		
		if(syncDomains) {
			
			VitalSignsConfig cfg = vs.getConfig()
			
			if(cfg.domainsStrategy != DomainsStrategy.dynamic) {
				handler.handle(new RuntimeException("Sync domains may only be used with vitalsigns dynamic domains strategy only"))
				return
			}
			
			if(cfg.domainsSyncMode != DomainsSyncMode.pull) {
				handler.handle(new RuntimeException("Sync domains may only be used with vitalsigns domains sync mode set to pull"))
				return
			}
			
			if(cfg.loadDeployedJars) {
				handler.handle(new RuntimeException("Sync domains may only be used with vitalsigns loadDeployedJars set to false"))
				return
			}
			
			if(!cfg.autoLoad) {
				handler.handle(new RuntimeException("Sync domains may only be used with vitalsigns autoLoad set to true"))
				return
			}
			
		}
		
		if(appJar != "local") {
			
			log.info("Starting in dynamic mode from external jar")
			
			VitalSignsRootClassLoader cl = new VitalSignsRootClassLoader(VitalVertx3App.class.getClassLoader(), appJar);
			
			Thread.currentThread().setContextClassLoader(cl);
			
		} else {
		
			log.info("Starting with flat classloader")
		
		}
		
		
		log.info("Deploying verticles")
		deployVerticles(new ArrayList(verticlesList)) { Throwable deployError ->
			
			if(deployError != null) {
				handler.handle(deployError)
				return
			}
			
			if(syncDomains) {

				syncImpl.scheduleSync()
				
				//initially refresh json schema too if not exists
				if(!mergedJsonSchemaFileLocation.exists()) {
					syncImpl.doSyncJSONSchemas(false)
				}
				
			}
			
			handler.handle(null)
			
		}
		
		
		return this
		
	}


	
	
	
	public void undeployReloadableVerticles(Handler<Throwable> endHandler) {
		
		List<VerticleConfig> vcs = []
		
		for(VerticleConfig vc : verticlesList) {
			
			if( vc.reloadable ) {
				vcs.add(vc)
			}
			
		}
		
		undeployVerticles(vcs) { Throwable throwable ->
			
			if(throwable != null) {
				log.error("undeploying reloadable verticles failed: ", throwable)
				endHandler.handle(throwable)
				return
			}
			
			log.info("undeploying reloadable verticles succeeded")
			
			endHandler.handle(null)
		}
	}
	
	public void deployReloadableVerticles(Handler<Throwable> endHandler) {

		List<VerticleConfig> vcs = []
		
		for(VerticleConfig vc : verticlesList) {
			
			if( vc.reloadable ) {
				vcs.add(vc)
			}
			
		}
				
		deployVerticles(vcs) { Throwable deployThrowable ->
				
			if(deployThrowable != null) {
				log.error("deploying reloadable verticles failed: ", deployThrowable)
				endHandler.handle(deployThrowable)
				return
			}
			
			log.info("deploying reloadable verticles succeeded")
				
			endHandler.handle(null)
			
		}
		
	}
	
	public void undeployVerticles(List<VerticleConfig> vcs, Handler<Throwable> endHandler) {
		
		if(vcs.size() == 0) {
			endHandler.handle(null)
			return
		}
		
		VerticleConfig vc = vcs.remove(vcs.size() - 1)
		
		if(vc.deploymentID == null) {
			endHandler.handle(new RuntimeException("Verticle was not deployed ${vc.name}"))
			return
		}
		
		vertx.undeploy(vc.deploymentID) { AsyncResult<String> aRes ->
		
			if(!aRes.succeeded()) {
				log.error("Error when undeploying verticle: ${vc.name}", aRes.cause())
				endHandler.handle(aRes.cause())
				return
			}
			
			vc.deploymentID = null
			
			log.info("Verticle undeployed, ${vc.name}")
		
			undeployVerticles(vcs, endHandler)
			
		}
		
		
	}
	
	public void deployVerticles(List<VerticleConfig> vcs, Handler<Throwable> endHandler) {
		
		if(vcs.size() == 0) {
			
			//no more
			endHandler.handle(null)
			return
			
		}
		
		VerticleConfig vc = vcs.remove(0)
		
		if(vc.deploymentID != null) {
			endHandler.handle(new RuntimeException("Verticle already deployed ${vc.name}, deploymentID: ${vc.deploymentID}"))
			return
		}
		
		Map cfg = globalConfig
		
		if(vc.name == VITALSERVICEVERTX3) {
			cfg = vitalserviceConfig
		} else if(vc.name == VITALAUTHVERTX3) {
			cfg = vitalauthConfig
		}
				
		vertx.deployVerticle(vc.name, [
			worker: vc.worker,
			instances: vc.instances,
			config: cfg
		]){ AsyncResult<String> aRes ->
		
			if(!aRes.succeeded()) {
				log.error("Error when deploying verticle: ${vc.name}", aRes.cause())
				endHandler.handle(aRes.cause())
				return
			}
			
			vc.deploymentID = aRes.result()
			
			log.info("Verticle deployed: ${vc.name}, deploymentID: ${vc.deploymentID}")
		
			
			if(vc.name == VITALSERVICEVERTX3) {
				
				if(syncDomains) {
					
					if( VitalServiceVertx3.registeredServices.size() != 1 ) {
						endHandler.handle(new RuntimeException("In SyncDomains mode expected exactly one registered vitalservice instance, found: ${VitalServiceVertx3.registeredServices.size()}"))
						return
					}
					
					VitalService service = VitalServiceVertx3.registeredServices.entrySet().iterator().next().getValue()

					if(service.getEndpointType() != EndpointType.VITALPRIME) {
						endHandler.handle(new RuntimeException("SyncDomains mode may only be enabled with VitalPrime endpoint type"))
					}
				
					syncImpl = new DomainsSyncImplementation()
					syncImpl.client = new VitalServiceAsyncClient(vertx, service.getApp())
					syncImpl.syncDomainsIntervalSeconds = syncDomainsIntervalSeconds
					syncImpl.vertx = vertx
					syncImpl.app = this
					syncImpl.mergedJsonSchemaFileLocation = mergedJsonSchemaFileLocation
											
				}
				
			}
				
		
			deployVerticles(vcs, endHandler)
			
		}
		
	}
	
}
