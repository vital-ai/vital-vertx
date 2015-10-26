package ai.vital.service.vertx.handler.functions

import java.util.Map;

import org.mortbay.jetty.servlet.AbstractSessionManager.Session;

import ai.vital.service.vertx.handler.AbstractVitalServiceHandler;
import ai.vital.service.vertx.handler.Subscription;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.exception.VitalServiceException;
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalOrganization

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class VertxStreamUnsubscribeImpl extends VertxHandler {

	private final static Logger log = LoggerFactory.getLogger(VertxStreamUnsubscribeImpl.class)
	
	public VertxStreamUnsubscribeImpl(AbstractVitalServiceHandler handler) {
		super(handler);
	}

	@Override
	public ResultList callFunction(VitalOrganization organization, VitalApp app,
			String function, Map<String, Object> params)
			throws VitalServiceUnimplementedException, VitalServiceException {
				
		List<String> streamNames = params.get('streamNames')
		
		if(streamNames == null || streamNames.isEmpty()) throw new RuntimeException("No 'streamNames' param")
		
		String sessionID = getRequiredStringParam('sessionID', params)
		
		int removed = 0
		
		synchronized(handler.subscriptions) {
			
			
			Subscription s = handler.subscriptions.get(sessionID)
			
			if(s != null) {
				
				for(String streamName : streamNames) {
					
					if ( s.streamNames.remove(streamName) ) removed ++
					
				}
				
				
				if(s.streamNames.isEmpty()) {
					
					handler.subscriptions.remove(sessionID)
					
					
				} else {
				
					s.timestamp = System.currentTimeMillis()
				
				}
				
				
			}
			
		}
		
		log.info("Unsubscribed sessionID: $sessionID, streams: $streamNames")
		
		ResultList rl = new ResultList()
		rl.setTotalResults(removed);
		rl.setStatus(VitalStatus.withOKMessage('Subscriptions removed ' + removed))
		
		return rl;
		
	}
	
}
