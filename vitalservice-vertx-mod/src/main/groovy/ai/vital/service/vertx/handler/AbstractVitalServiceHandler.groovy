package ai.vital.service.vertx.handler

import org.apache.commons.lang3.SerializationUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.vertx.groovy.core.Vertx
import org.vertx.groovy.core.eventbus.Message

import ai.vital.query.querybuilder.VitalBuilder
import ai.vital.service.vertx.binary.PayloadMessage
import ai.vital.service.vertx.binary.ResponseMessage
import ai.vital.service.vertx.handler.functions.VertxListHandlersImpl
import ai.vital.service.vertx.handler.functions.VertxRegisterImpl
import ai.vital.service.vertx.handler.functions.VertxSendToStreamImpl
import ai.vital.service.vertx.handler.functions.VertxStreamListSessionSubscriptionsImpl;
import ai.vital.service.vertx.handler.functions.VertxStreamListStreamSubscribersImpl
import ai.vital.service.vertx.handler.functions.VertxStreamSubscribeImpl
import ai.vital.service.vertx.handler.functions.VertxStreamUnsubscribeImpl;
import ai.vital.service.vertx.handler.functions.VertxUnregisterImpl
import ai.vital.service.vertx.json.VitalServiceJSONMapper
import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalservice.model.App
import ai.vital.vitalservice.model.Organization
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalservice.query.VitalQuery
import ai.vital.vitalsigns.java.VitalJavaSerializationUtils

abstract class AbstractVitalServiceHandler {

	def static vitalBuilder = new VitalBuilder()
	
	private final static Logger log = LoggerFactory.getLogger(AbstractVitalServiceHandler.class)
	
	protected abstract Object handleMethod(String method, Object[] args)
	
	static Map<String, CallFunctionHandler> callFunctionHandlers
	
	static Map<String, Subscription> subscriptions = [:]
	
	protected Vertx vertx
	
	public void handle(Message msg) {

		Map jsonResponse = null

		ResponseMessage binaryResponse = null
		
		Object response = null
				
		try {
			
			Object bodyObj = msg.body()
			
			String method = null
					
					
			Object[] a = null
			
			//json
			if(bodyObj instanceof Map) {
				
				jsonResponse = [:]
				
				Map body = bodyObj
				
				method = body.get("method")
				
				if(! method) throw new RuntimeException("No method string")
				
				List args = body.get("args")
				
				if(args == null) throw new RuntimeException("Args list cannot be null")
				
				List l = []
				
				//convert args into array dynamically
				for(int i = 0 ; i < args.size(); i++) {
					
					Object o = args.get(i)
	
					l.add(VitalServiceJSONMapper.fromJSON(o))
														
				}
				
				a = l.toArray(new Object[l.size()])
				
				
			} else if(bodyObj instanceof byte[]) {
			
				binaryResponse = new ResponseMessage()
				
				Object obj = VitalJavaSerializationUtils.deserialize((byte[])bodyObj)
				
				if(!(obj instanceof PayloadMessage)) {
					throw new RuntimeException("Expected an instanceof ${PayloadMessage.class.canonicalName}")
				}
				
				PayloadMessage bmp = obj
				
				method = bmp.method
				a = bmp.args.toArray()
				
			
			} else {
			
				throw new RuntimeException('Unsupported msg body type: ' + bodyObj?.class.canonicalName);
			
			}
		
			response = handleMethod(method, a)
		
			if(jsonResponse != null) {
				
				if(response != null) {
					
					if(response instanceof VitalStatus) {
						jsonResponse.put('status', ((VitalStatus)response).status.name())
						jsonResponse.put('message',  ((VitalStatus)response).message)
					}
					
					if(response instanceof ResultList) {
						
						ResultList rl = response
						
						VitalStatus status = rl.status
						
						if(status != null) {
							
							jsonResponse.put('status', status.status.name())
							jsonResponse.put('message',  status.message)
							
						}

					}
					
					response = VitalServiceJSONMapper.toJSON(response)		
							
					jsonResponse.put('response', response) 
				}
				
				if(jsonResponse.get('status') == null)  { 
					jsonResponse.put('status', 'ok')
				}
				
			}
			
			if(binaryResponse != null) {
				
				binaryResponse.response = response
				
			}
			
		} catch(Exception e) {
			
			log.warn(e.localizedMessage)
			
			if(binaryResponse != null) {
				
				binaryResponse.exceptionMessage = e.getLocalizedMessage()
				binaryResponse.exceptionType = e.class.canonicalName
				
				if(!binaryResponse.exceptionMessage) binaryResponse.exceptionMessage = "(no exception message)"
				
			}
			
			if(jsonResponse != null) {
				jsonResponse.put('status', 'error')
				jsonResponse.put('exception', true)
				jsonResponse.put('exceptionType', e.class.canonicalName)
				jsonResponse.put('message', e.getLocalizedMessage())
			}
			
			
		}
		
		if(jsonResponse != null) {
			msg.reply(jsonResponse)
		}
		
		if(binaryResponse) {
			
			msg.reply( SerializationUtils.serialize(binaryResponse) )
			
		}
					
	}
	
	public AbstractVitalServiceHandler(Vertx vertx) {
		super();
		this.vertx = vertx
		
		initStaticHandlers()
		
	}
	
	void initStaticHandlers() {
		
		if(callFunctionHandlers != null) return
		
		synchronized (AbstractVitalServiceHandler.class) {
			
			if(callFunctionHandlers != null) return
			
			callFunctionHandlers = Collections.synchronizedMap(new LinkedHashMap<String, CallFunctionHandler>())
			
			callFunctionHandlers.put(CallFunctionHandler.VERTX_REGISTER_HANDLER, new VertxRegisterImpl(this))
			callFunctionHandlers.put(CallFunctionHandler.VERTX_UNREGISTER_HANDLER, new VertxUnregisterImpl(this))
			callFunctionHandlers.put(CallFunctionHandler.VERTX_LIST_HANDLERS, new VertxListHandlersImpl(this))
			//register default ones
			
			callFunctionHandlers.put(CallFunctionHandler.VERTX_SEND_TO_STREAM, new VertxSendToStreamImpl(this))
			callFunctionHandlers.put(CallFunctionHandler.VERTX_STREAM_LIST_SESSION_SUBSCRIPTIONS, new VertxStreamListSessionSubscriptionsImpl(this))
			callFunctionHandlers.put(CallFunctionHandler.VERTX_STREAM_LIST_STREAM_SUBSCRIBERS, new VertxStreamListStreamSubscribersImpl(this))
			callFunctionHandlers.put(CallFunctionHandler.VERTX_STREAM_SUBSCRIBE, new VertxStreamSubscribeImpl(this))
			callFunctionHandlers.put(CallFunctionHandler.VERTX_STREAM_UNSUBSCRIBE, new VertxStreamUnsubscribeImpl(this))
			
		}
		
	}


	protected boolean checkParams(String method, Object[] args, boolean throwException, Class... classes) {
		
		if(classes.length != args.length) {
			if(throwException) throw new RuntimeException("Method: ${method} - arguments count does not match, : ${method}, given: ${args.length}, expected: ${classes.length}")
			return false;
		}
		for(int i = 0 ; i < classes.length; i++) {
			
			if(args[i] == null) continue;
			
			if( ! classes[i].isInstance(args[i]) ) {
				if(throwException) throw new RuntimeException("Method: ${method} - argument ${i} is not an instance of ${classes[i].canonicalName}: ${args[i].class.canonicalName}")
				return false;
			}
		}
		
		return true;
		
	}

	protected void unsupported(String method) { throw new RuntimeException("Method unsupported: $method") }
	
	protected void queryFilter(Object[] args, index) {
		
		if(args.length - 1 < index) throw new RuntimeException("Cannot check query, index outside of params list $index, params list size ${args.length}")
		
		
		Object arg = args[index]
		
		if(arg instanceof VitalQuery) return
		
		if(arg instanceof String || arg instanceof GString) {

			arg = vitalBuilder.queryString { arg }.toQuery()
					
			args[index] = arg	
			
			return
		}
		
		throw new RuntimeException("Unexpected query object type: ${arg.class?.canonicalName}, expected string or a VitalQuery")
		
	}
	
	/**
	 * Default call function logic. 
	 * At first it tries to call special vertx functions for managing handlers.
	 * Next checking if it's a registered handler
	 * Returns null if it should falls back to service call function
	 * @return ResultList if callFunction handled, false to fall back to lower layer  
	 */
	protected ResultList callFunctionLogic(Organization organization, App app, String function, Map<String, Object> params) {
		
		CallFunctionHandler handler = callFunctionHandlers.get(function)
		
		if(handler != null) {
			try {
				
				ResultList rl = handler.callFunction(organization, app, function, params)
				if(rl == null) throw new RuntimeException("Handler ${handler.class.canonicalName} did not return ResultList object")
				return rl
				
			} catch(Exception e) {
			
				ResultList rl = new ResultList()
				rl.setStatus(VitalStatus.withError(e.getLocalizedMessage()))
			
				return rl
			}
		}
		
		return null
		
	}
}
