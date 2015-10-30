package ai.vital.service.vertx.handler

import java.util.Map;

import ai.vital.vitalservice.exception.VitalServiceException;
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.VitalOrganization;

interface AsyncCallFunctionHandler extends ICallFunctionHandler {

	public void callFunction(VitalOrganization organization, VitalApp app, String function, Map<String, Object> params, Closure closure) throws Exception
	
}
