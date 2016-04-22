package ai.vital.service.vertx3.handler.functions

import java.util.Map;

import ai.vital.domain.Organization
import ai.vital.service.vertx3.handler.AbstractVitalServiceHandler;
import ai.vital.service.vertx3.handler.Subscription;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.exception.VitalServiceException;
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalOrganization

import org.slf4j.Logger
import org.slf4j.LoggerFactory 

class VertxStreamSubscribeImpl extends VertxHandler {

	private final static Logger log = LoggerFactory.getLogger(VertxStreamSubscribeImpl.class)
	
	public VertxStreamSubscribeImpl(AbstractVitalServiceHandler handler) {
		super(handler);
	}

	@Override
	public ResultList callFunction(VitalOrganization organization, VitalApp app,
			String function, Map<String, Object> params, Map<String, Object> sessionParams)
			throws VitalServiceUnimplementedException, VitalServiceException {

		List<String> streamNames = params.get('streamNames')

		if(streamNames == null || streamNames.isEmpty()) throw new RuntimeException("No 'streamNames' param")
				
		String sessionID = getRequiredStringParam('sessionID', params)
		
		//check if already exists
		
		int subscribed =  0
		
		synchronized (handler.subscriptions) {
			
			Subscription s = handler.subscriptions.get(sessionID)
			
			if(s == null) {
				
				s = new Subscription()
				s.sessionID = sessionID
				handler.subscriptions.put(sessionID, s)
				
			}
			
			for(String streamName : streamNames) {
				if( s.streamNames.add(streamName) ) subscribed ++ 
			}
			
			s.timestamp = System.currentTimeMillis()
			
		}
				
		log.info("Subscribed sessionID: $sessionID, streams: $streamNames")
		
		ResultList rl = new ResultList()
		rl.setTotalResults(subscribed)
		rl.setStatus(VitalStatus.withOKMessage("Subscription added: ${subscribed}"))
		
		return rl;
	}

}
