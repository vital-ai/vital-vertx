package ai.vital.vertx3

import io.vertx.groovy.core.Vertx

import java.nio.charset.StandardCharsets
import java.util.Map.Entry

import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import ai.vital.service.vertx3.async.VitalServiceAsyncClient
import ai.vital.service.vertx3.binary.ResponseMessage
import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.command.patterns.JarFileInfo
import ai.vital.vitalsigns.command.patterns.JsonSchemaFileInfo
import ai.vital.vitalsigns.model.DomainModel

class DomainsSyncImplementation {

	private final static Logger log = LoggerFactory.getLogger(DomainsSyncImplementation.class)
	
	public final static String DOMAIN_MANAGER_DATASCRIPT = "commons/scripts/DomainsManagerScript";
	
	Vertx vertx
	
	VitalServiceAsyncClient client

	Integer syncDomainsIntervalSeconds
	
	VitalVertx3App app
	
	File mergedJsonSchemaFileLocation
	
	Long currentTimerID = null
	
	public void scheduleSync() {
		
		log.info("Scheduling domains sync launch in ${syncDomainsIntervalSeconds} seconds")
		
		currentTimerID = vertx.setTimer(syncDomainsIntervalSeconds.longValue() * 1000L) { timerID ->
					
			currentTimerID = null
			doSyncDomains();
					
					
		}
	}
	

	public void doSyncDomains() {
		
		log.info("Syncing domains")
		
		if(currentTimerID != null) {
			vertx.cancelTimer(currentTimerID)
			currentTimerID = null
		}
		
		Map<String, DomainModel> currentModels = new HashMap<String, DomainModel>()
		Map<String, DomainModel> remoteModels = new HashMap<String, DomainModel>()
		
		//quickly check current and remote domains lists, if change detected sync and reload verticles
		for(DomainModel model : VitalSigns.get().getDomainModels()) {
			currentModels.put(model.URI, currentModels)
		}
		
		client.callFunction(DOMAIN_MANAGER_DATASCRIPT, [action: "listDomainJars"]) { ResponseMessage rm ->
			
			String error = null
			
			ResultList rl = rm.response
			if(rm.exceptionType) {
				error = "${rm.exceptionType} - ${rm.exceptionMessage}"
			} else {
				if( rl.status.status != VitalStatus.Status.ok ) { 
					error = rl.status.toString()
				}
			}
			
			if(error) {
				log.error("Error when listing remote domains: ${error}")
				scheduleSync()
				return;
			}
			
			for(DomainModel r : rl) {
				
				Boolean isActive = r.active
				
				if(isActive == null || isActive.booleanValue() ) {
					remoteModels.put(r.URI, r)
				}				
			}
		
			boolean changeDetected = false
			if(currentModels.size() != remoteModels.size()) {
				log.info("Change detected - different domains count, local ${currentModels.size()} remote ${remoteModels.size()}")
				changeDetected = true
			}

			if(!changeDetected) {
				
				//compare sets
				Set<String> diff1 = new HashSet<String>(currentModels.keySet())
				Set<String> diff2 = new HashSet<String>(remoteModels.keySet())
				
				diff1.removeAll(remoteModels)
				diff2.removeAll(currentModels)
				
				if(diff1.size() > 0 || diff2.size() > 0) {
					log.info("Change detected - local models diff ${diff1}, remote diff ${diff2}")
					changeDetected = true
				}
				
			}
			
			
			//compare hashses
			if(!changeDetected) {
				
				for(Entry<String, DomainModel> e : currentModels.entrySet()) {
					
					String uri = e.getKey()
					String currentHash = e.getValue().domainOWLHash

					DomainModel r = remoteModels.get(uri)
					
					String remoteHash = r.domainOWLHash
					
					if(currentHash == null) {
						log.error("Local domain model owl hash not set: " + uri);
						scheduleSync()
						return
					} 
						
					if(remoteHash == null) {
						log.error("Remote domain model owl hash not set: " + uri);
						scheduleSync()
						return
					}
					
					if( currentHash != remoteHash) {
						log.info("Change detected - different domain owl hash values: ${currentHash} vs ${remoteHash} - ${uri}")
						changeDetected = true
						break
					}
					
				}
				
			}
			
			if(changeDetected) {

				app.undeployReloadableVerticles() { Throwable undeployError ->
					
					if(undeployError) {
						log.error("Error when undeploying current verticles:", undeployError)
						scheduleSync()
						return
					}
					
					//unload verticles
					log.info("Syncing domains via VitalSigns")
					try {
						VitalSigns.get().sync()
					} catch(Exception e) {
						log.error(e.getLocalizedMessage(), e)
						scheduleSync()
						return
					}
					
					log.info("Sync finished")
					
					app.deployReloadableVerticles() { Throwable deployError ->
						
						if(deployError) {
							log.error("Error when loading verticles back:", deployError)
						} else {
							log.info("Verticles reloaded, refreshing json schemas file")
							doSyncJSONSchemas(true)
						}
						
						
						return
						
					}
									
				}
				

				
			}
						
				
		}
		
	}
	
	public void doSyncJSONSchemas(boolean doScheduleSync) {
		
		List<String> domainsOrder = []
		
		//generate domains order
		for(DomainModel dm : VitalSigns.get().getDomainModels()) {
			JarFileInfo jfi = JarFileInfo.fromString(dm.name.toString())
			domainsOrder.add(jfi.domain)
		}
		
		
		client.callFunction(DOMAIN_MANAGER_DATASCRIPT, [action: 'listJsonSchemas', stringContent: true]) { ResponseMessage rm ->
			
			String error = null
			
			ResultList rl = rm.response
			if(rm.exceptionType) {
				error = "${rm.exceptionType} - ${rm.exceptionMessage}"
			} else {
				if( rl.status.status != VitalStatus.Status.ok ) {
					error = rl.status.toString()
				}
			}
			
			if(error) {
				log.error("Error when listing remote domains: ${error}")
				if(doScheduleSync)scheduleSync()
				return;
			}
			
			ResultList jsRL  = rm.response
			
			Map<String, DomainModel> domain2Json = [:]
			
			for(DomainModel m : jsRL) {
				
				String domain = JsonSchemaFileInfo.fromString(m.name.toString()).domain
				
				domain2Json.put(domain, m)
				
			}
			
			BufferedWriter writer = null
			
			try {
				
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mergedJsonSchemaFileLocation), StandardCharsets.UTF_8))
				
				for(String domain : domainsOrder) {
					
					DomainModel dm = domain2Json.get(domain)
					
					if(dm == null) {
						log.error("Json schema not found for domain: ${domain}")
						continue
					}
					
					String schemaContent = dm.domainOWL
					
					writer.write(schemaContent)
					
					writer.write("\n\n")
					
				}
				
				log.info("Json schema generation complete")
			
		
			} finally {
				IOUtils.closeQuietly(writer)
			}
			
			if(doScheduleSync) scheduleSync()
			
		}
		
		/*
		if(VitalSigns.get().getConfig().domainsSyncLocation != DomainsSyncLocation.domainsDirectory) {
			log.warn("Json schema may only be generated with when domainsSyncLocation = domainsDirectory")
			if(scheduleSync) scheduleSync()
			return
		}
		
		File domainJsonSchemaDir = new File(VitalSigns.get().vitalHomePath, "domain-json-schema")
		
		BufferedWriter writer = null
		
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mergedJsonSchemaFileLocation), StandardCharsets.UTF_8))
		
			//json schema is generated from local files, the order is determined by domains
			for(DomainModel dm : VitalSigns.get().getDomainModels()) {
				
				String jarName = dm.name
				
				JarFileInfo jfi = JarFileInfo.fromString(jarName)
				JsonSchemaFileInfo jsfi = JsonSchemaFileInfo.fromJarInfo(jfi)
				
				File jsonSchemaFile = new File(domainJsonSchemaDir, jsfi.toFileName())
				
				if(!jsonSchemaFile.exists()) {
					log.error("Json schema file not found")
					continue
				}

				String contents = FileUtils.readFileToString(jsonSchemaFile, "UTF-8")
				
				writer.write(contents)
				
				writer.write("\n\n")
								
			}

		} finally {
			IOUtils.closeQuietly(writer)
		}
		*/
				
	}
	
}

