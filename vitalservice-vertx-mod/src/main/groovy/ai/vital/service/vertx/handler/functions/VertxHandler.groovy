package ai.vital.service.vertx.handler.functions

import java.util.Map;

import ai.vital.service.vertx.handler.AbstractVitalServiceHandler;
import ai.vital.service.vertx.handler.CallFunctionHandler;
import ai.vital.vitalservice.exception.VitalServiceException;
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException;
import ai.vital.vitalservice.model.App;
import ai.vital.vitalservice.model.Organization;
import ai.vital.vitalservice.query.ResultList;

abstract class VertxHandler implements CallFunctionHandler {

	protected AbstractVitalServiceHandler handler
	
	public VertxHandler(AbstractVitalServiceHandler handler) {
		super();
		this.handler = handler;
	}

	protected String getRequiredStringParam(String n, Map<String, Object> params) {
		
		def v = params.get(n)
		if(v == null) throw new RuntimeException("No '$n' param")
		if(!(v instanceof String)) throw new RuntimeException("'$n' must be a string")
		
		return v
	}

}
