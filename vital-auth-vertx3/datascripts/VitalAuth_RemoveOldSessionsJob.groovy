package commons.scripts

import java.io.Serializable;
import java.util.Map

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import ai.vital.domain.UserSession
import ai.vital.domain.UserSession_PropertiesHelper;
import ai.vital.prime.groovy.TimeUnit;
import ai.vital.prime.groovy.VitalPrimeGroovyJob
import ai.vital.prime.groovy.VitalPrimeGroovyScript
import ai.vital.prime.groovy.VitalPrimeScriptInterface
import ai.vital.query.querybuilder.VitalBuilder
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalservice.query.VitalSelectQuery;
import ai.vital.vitalsigns.model.VitalSegment
import ai.vital.vitalsigns.model.property.URIProperty;;

class VitalAuth_RemoveOldSessionsJob implements VitalPrimeGroovyScript, VitalPrimeGroovyJob {

	/** CONFIG **/
	
	long maxAge = 30L * 24L * 3600L * 1000L 
	
	String segmentID = "sessions" 
	
	/** END OF CONFIG **/
	
	private final static Logger log = LoggerFactory.getLogger(VitalAuth_RemoveOldSessionsJob.class)
	
	@Override
	public boolean startAtPrettyTime() {
		return false;
	}

	@Override
	public int getInterval() {
		return 30;
	}

	@Override
	public TimeUnit getIntervalTimeUnit() {
		return TimeUnit.DAY;
	}

	@Override
	public void executeJob(VitalPrimeScriptInterface scriptInterface, Map<String, Serializable> jobDataMap) {
		
		ResultList rl = executeScript(scriptInterface, [:])
		
	}

	@Override
	public ResultList executeScript(VitalPrimeScriptInterface scriptInterface, Map<String, Object> parameters) {

		long timestamp = ( System.currentTimeMillis() - maxAge )
		
		log.info("Removing sessions older than: ${new Date(timestamp)}")
		
		ResultList rl = new ResultList()
		
		int removed = 0
		
		try {
			
			VitalSegment segment = scriptInterface.getSegment(segmentID)
			if(segment == null) throw new Exception("Segment not found: ${segmentID}")
			
			boolean keepGoing = true
			
			int limit = 1000
			
			while(keepGoing) {
				
				VitalSelectQuery sq = new VitalBuilder().query {
					
					SELECT {
						
						value offset: 0
						
						value limit: limit
							
						value segments: [segment]
						
						node_constraint { UserSession.class }			
						
						node_constraint { ((UserSession_PropertiesHelper) UserSession.props()).timestamp.lessThan(timestamp) }
						
					}
					
				}.toQuery()
				
				ResultList queryRL = scriptInterface.query(sq)
				
				if(queryRL.status.status != VitalStatus.Status.ok) {
					throw new Exception("Sessions query error: ${queryRL.status.message}")
				}
				
				List<URIProperty> uris = []
				
				for(UserSession us : queryRL) {
					
					uris.add(URIProperty.withString( us.URI) )
					
				}
				
				log.info("Selected ${uris.size()} sessions")
				
				if(uris.size() > 0) {
					
					VitalStatus deleteStatus = scriptInterface.delete(uris)
					if(deleteStatus.status != VitalStatus.Status.ok) {
						throw new Exception("Sessions delete error: ${deleteStatus.message}")
					}
					
					removed += ( deleteStatus.getSuccesses() != null ? deleteStatus.getSuccesses().intValue() : uris.size() )
					
					log.info("Deleted ${deleteStatus.successes} sessions")
					
				}
				
				if(uris.size() >= limit) {
					
					keepGoing = true
					
				} else {
				
					keepGoing = false
				
				}
				
			}
			
			
		} catch(Exception e) {
			rl.status = VitalStatus.withError(e.localizedMessage)
		}
		
		log.info("Total session deleted: ${removed}")
		
		rl.status.successes = removed
		
		return rl;
	}

}
