package ai.vital.service.vertx.handler.functions

import java.util.Map

import org.vertx.java.core.json.JsonObject;

import ai.vital.service.vertx.handler.AbstractVitalServiceHandler;
import ai.vital.service.vertx.handler.Subscription;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.exception.VitalServiceException;
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException;
import ai.vital.vitalservice.json.VitalServiceJSONMapper;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalOrganization

class VertxSendToStreamImpl extends VertxHandler {

	public VertxSendToStreamImpl(AbstractVitalServiceHandler handler) {
		super(handler);
	}

	@Override
	public ResultList callFunction(VitalOrganization organization, VitalApp app,
			String function, Map<String, Object> params, Map<String, Object> sessionParams)
			throws VitalServiceUnimplementedException, VitalServiceException {

		String streamName = getRequiredStringParam('streamName', params)
		
		//optional session
		List<String> sessionIDs = params.get('sessionIDs')
		
		if(!streamName) throw new RuntimeException("No 'streamName' param")
		
		def message = params.get('message')
		
		if(message == null) throw new RuntimeException("no 'message'")
		
		if(!(message instanceof ResultList)) throw new RuntimeException("'message' must be a ResultList object")
		
		//it is supposed to send a result list
		
		def json = VitalServiceJSONMapper.toJSON(message)
		
		JsonObject jsonObj = new JsonObject(json)
		jsonObj.putString('streamName', streamName)
		
		int i = 0
		
		synchronized (handler.subscriptions) {

			if(sessionIDs && sessionIDs.size() > 0) {
				
				for(String sessionID : sessionIDs) {
					
					Subscription s = handler.subscriptions.get(sessionID)
					
					if(s && s.streamNames.contains(streamName)) {
						handler.vertx.eventBus.send('stream.' + s.sessionID, jsonObj)
						i++;
						
					}
					
				}
				
			} else {
			
				for(Subscription s : handler.subscriptions.values()) {
					
					if(s.streamNames.contains(streamName)) {
						
						handler.vertx.eventBus.send('stream.' + s.sessionID, jsonObj)
						
						i++;
						
					}
				
				}
						
			}
			
		}
		
		ResultList rl = new ResultList()
		rl.setTotalResults(i)
		rl.setStatus(VitalStatus.withOKMessage("Sent $i messages"))
				
		return rl;
		
	}

}
