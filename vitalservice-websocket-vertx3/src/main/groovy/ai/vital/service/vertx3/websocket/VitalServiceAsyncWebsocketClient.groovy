package ai.vital.service.vertx3.websocket

import groovy.json.JsonOutput
import groovy.lang.Closure;
import io.vertx.core.Handler
import io.vertx.groovy.core.Vertx
import io.vertx.groovy.core.http.HttpClient
import io.vertx.groovy.core.http.WebSocket
import io.vertx.groovy.core.http.WebSocketFrame

import java.util.Map;
import java.util.Map.Entry

import org.codehaus.jackson.map.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import ai.vital.service.vertx3.async.VitalServiceAsyncClientBase
import ai.vital.service.vertx3.binary.ResponseMessage
import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalservice.json.VitalServiceJSONMapper
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalQuery;
import ai.vital.vitalsigns.model.VITAL_Node
import ai.vital.vitalsigns.model.VitalApp

/**
 * External Asynchronous version of vital service on vertx over websocket
 * All closures are called with {@link ai.vital.service.vertx.binary.ResponseMessage}
 * @author Derek
 *
 */
class VitalServiceAsyncWebsocketClient extends VitalServiceAsyncClientBase {

	
	public final static String GROOVY_REGISTER_STREAM_HANDLER = 'groovy-register-stream-handler';
	
	public final static String GROOVY_UNREGISTER_STREAM_HANDLER = 'groovy-unregister-stream-handler';
	
	public final static String GROOVY_LIST_STREAM_HANDLERS = 'groovy-list-stream-handlers';
	
	public final static String VERTX_STREAM_SUBSCRIBE = 'vertx-stream-subscribe';
	
	public final static String VERTX_STREAM_UNSUBSCRIBE = 'vertx-stream-unsubscribe';
	
	
	private final static Logger log = LoggerFactory.getLogger(VitalServiceAsyncWebsocketClient.class)
	
	String endpointURL = null
	
	Handler<Throwable> completeHandler	
	
	//returns reconnect count
	Handler<Integer> connectionErrorHandler
	
	//this handler gets notified of reconnection event
	Handler<Void> reconnectHandler
	
	HttpClient httpClient
	
	WebSocket webSocket
	
	Map<String, Closure> callbacksMap = [:]
	Map<String, Long> callbacks2Timers = [:]
	
	
	
	Map<String, Closure> streamCallbacksMap = [:]
	
	Map<String, Closure> registeredHandlers = [:]
	
	Map<String, Closure> currentHandlers = [:]
	
	boolean eventbusListenerActive = false
	Closure eventbusHandler = null
	
	ObjectMapper objectMapper = null
	
	Long periodicID = null
	
	long pingInterval = 5000L;
	
	String sessionID
	
	//append this to webservice object (auth)
	String appSessionID
	
	int reconnectCount = 5
	int reconnectIntervalMillis = 3000
	
	
	private int attempt = 0
	
	private boolean closed = false
	
	private URL url
	
	Long reopeningTimer = null 
	
	/**
	 * 
	 * @param vertx - will *not* be closed when closeWebsocket is called or connect / reconnect exceptions occur
	 * @param app
	 * @param addressPrefix 'vitalservice.' or 'endpoint.'
	 * @param endpointURL
	 * @param reconnectCount
	 * @param reconnectIntervalMillis
	 * 
	 */
	VitalServiceAsyncWebsocketClient(Vertx vertx, VitalApp app, String addressPrefix, String endpointURL, int reconnectCount, int reconnectIntervalMillis) {
		super(vertx)
		if(app == null) throw new NullPointerException("Null app")
		String appID = app.appID?.toString()
		if(appID == null) throw new NullPointerException("Null appID")
		this.address = addressPrefix + appID
		this.vertx = vertx
		this.endpointURL = endpointURL
		if(reconnectCount < 0 ) throw new Exception("reconnectCount must be >= 0")
		if(reconnectIntervalMillis < 1) throw new Exception("reconnectIntervalMillis must be > 0")
		this.reconnectCount = reconnectCount
		this.reconnectIntervalMillis = reconnectIntervalMillis
		objectMapper = new ObjectMapper()
		
		sessionID = UUID.randomUUID().toString()
		
	}
	
	VitalServiceAsyncWebsocketClient(Vertx vertx, VitalApp app, String addressPrefix, String endpointURL) {
		this(vertx, app, addressPrefix, endpointURL, 5, 3000)
	}
	
	protected void sendPing() {
		if(webSocket != null) {
			webSocket.writeFinalTextFrame(JsonOutput.toJson([type: 'ping']))
			log.debug("Ping sent")
		}
	}
	
	
	private openWebSocket() {
		
		if(periodicID != null) {
			vertx.cancelTimer(periodicID)
			periodicID = null
		}
		
		int port = url.getPort()
		
		if(port < 0) {
			port = url.getDefaultPort()
		}
		
		httpClient.websocket(port, url.getHost(), url.getPath(), {WebSocket ws ->
			
			reopeningTimer = null
			attempt = 0
			webSocket = ws
			
			periodicID = vertx.setPeriodic(pingInterval) { Long periodicID ->
				this.periodicID = periodicID
				sendPing()
			}
			
			webSocket.frameHandler { WebSocketFrame frame ->
				
				if( log.isDebugEnabled() ) log.debug("Frame received: ${frame}")
//				callbacksMap.get()
				String response = frame.textData();
				Map envelope = objectMapper.readValue(response, LinkedHashMap.class)
				
				String mtype = envelope.type
				
				if(mtype == 'rec') {
				
					String replyAddress = envelope.address
					
					if(!replyAddress) {
						log.error("Received response without replyAddress")
						return
					}
					
					Closure callback = streamCallbacksMap.get(replyAddress)

					boolean isStream = callback != null
					
					if(callback == null) {
						callback = callbacksMap.remove(replyAddress)
						Long timerID = callbacks2Timers.remove(replyAddress)
						if(timerID != null) {
							vertx.cancelTimer(timerID)
						}
					}
					
					
					if(callback == null) {
						log.warn("Callback not found for address ${replyAddress} - timed out")
						return
					}
					
					Map body = envelope.body
					
					ResponseMessage rm = new ResponseMessage()
							
					if(!body) {
						rm.exceptionType = 'error_no_body'
						rm.exceptionMessage = "No body in response message"
						if(!isStream) {
							callback(rm)
						} else {
							log.error(rm.exceptionType + ' - ' + rm.exceptionMessage)
						}
						return
					}
					
					Map responseObject = null
					
					
					//no wrapping code
					if(body.get('_type') != null) {

						responseObject = body
						
					} else {
					
						String status = body.status
						if(status == null) status = "error_no_status"
						
						String msg = body.message
						if(!msg) msg = "(empty error message)"
						
						if(!status.equalsIgnoreCase('ok')) {
							rm.exceptionType = status
							rm.exceptionMessage = msg
							if(!isStream) {
								callback(rm)
							} else {
								log.error(rm.exceptionType + ' - ' + rm.exceptionMessage)
							}
							return
						}
						
						responseObject = body.response
					
					}
					
					if(responseObject == null) {
						rm.exceptionType = 'error_no_response'
						rm.exceptionMessage = 'No response object'
						if(!isStream) {
							callback(rm)
						} else {
							log.error(rm.exceptionType + ' - ' + rm.exceptionMessage)
						}
						return
					}
					
					//remove stream name!
					String streamName = responseObject.remove('streamName')
					
					if(isStream && !streamName) {
						rm.exceptionType = 'error_no_stream_name_in_stream_response'
						rm.exceptionMessage = 'No streamName in stream response'
						log.error(rm.exceptionType + ' - ' + rm.exceptionMessage)
						return
					}
					
					try {
						
						Object resObj = VitalServiceJSONMapper.fromJSON(responseObject)
						
						rm.response = resObj
						
					} catch(Exception e) {
						log.error(e.localizedMessage, e)
						rm.exceptionType = e.getClass().getCanonicalName()
						rm.exceptionMessage = e.localizedMessage
						if(!isStream) {
							callback(rm)
						} else {
							log.error(rm.exceptionType + ' - ' + rm.exceptionMessage)
						}
						return
					}
					
					if(isStream) {
						if(!(rm.response instanceof ResultList)) {
							log.error("Stream handler expects only result list messages")
							return
						}
						callback(streamName, rm.response)
					} else {
						callback(rm)
					}
					
						
				} else {
				
					log.warn("Unknown msg type: ${mtype}, ${envelope}")
				
				}
				
//				println frame.isText()
//
//				log.info("Frame received: ${frame} ${textHandlerID}")
				
				
			}
			
			webSocket.closeHandler {
				log.info("WebSocket close handler called")
				
				if(closed) {
					log.info("client already closed - that's ok")
				} else {
					if(reopeningTimer != null) {
						log.error("websocket closed, but already a re-open timer set")
					} else {
						log.error("websocket closed, re-opening")
						openWebSocket()
					}
				}
				
			}
			
			webSocket.exceptionHandler { Throwable t ->
				log.error("WEBSOCKET EXCEPTION", t)
			}
			
			log.info("WebSocket connection to ${endpointURL} ready")
			
			if(completeHandler != null) {
				completeHandler.handle(null)
				//handler notified only once
				completeHandler = null
			}
			
			
			
			if(currentHandlers.size() > 0) {
			
				List keys = new ArrayList(currentHandlers.keySet())
				log.info("Re-subscribing ${keys.size()} stream handlers: ${keys}")
				
				callFunctionSuper(VERTX_STREAM_SUBSCRIBE, [streamNames: keys, sessionID: this.sessionID]) { ResponseMessage res ->
					
					if(res.exceptionType) {
						log.error("Error when re-subscribing to streams: " + res.exceptionType + ' - ' + res.exceptionMessage)
						return
					}
					
					ResultList resRL = res.response
					if(resRL.status.status != VitalStatus.Status.ok) {
						log.error("Error when re-subscribing to streams: " + resRL.status.message)
						return
					}
					
					if( true /*! this.eventbusListenerActive */) {
						
						log.info("Also refreshing inactive eventbus listener")
						
						this.eventbusHandler = createNewHandler();
		//				_this.eb.registerHandler('stream.'+ _this.sessionID, _this.eventbusHandler);
						String address = 'stream.'+ this.sessionID
						this.webSocket.writeFinalTextFrame(JsonOutput.toJson([
							type: 'register',
							address: address,
							headers: [:]
						]))
						
						
						streamCallbacksMap.put(address, this.eventbusHandler)
						
						this.eventbusListenerActive = true
						
					}
					
					if(reconnectHandler != null) {
						reconnectHandler.handle()
					}
					
				}
				
			} else {
			
				if(reconnectHandler != null) {
					reconnectHandler.handle()
				}
			
			}
			
			/*
			var currentKeys = [];
			
			for ( var key in _this.currentHandlers ) {
				currentKeys.push(key);
			}
			
			if(currentKeys.length > 0) {
				
				if(VITAL_LOGGING) { console.log('refreshing session handlers: ', currentKeys); }
				
				var args = [VitalServiceWebsocketImpl.VERTX_STREAM_SUBSCRIBE, {streamNames: currentKeys, sessionID: _this.sessionID}];
				if(_this.admin) {
					//insert null app
					args.splice(0, 0, null);
				}
				//re-register it ?
				_this.callMethod('callFunction', args, function(successRL){
					
					if(!_this.eventbusListenerActive) {
						
						_this.eventbusHandler = _this.createNewHandler();
						_this.eb.registerHandler('stream.'+ _this.sessionID, _this.eventbusHandler);
						_this.eventbusListenerActive = true;
						
					}
					
					
				}, function(errorResponse){
					console.error(errorResponse);
				});
					
			}
			*/
			
		}, {Throwable t ->

			log.error("Error when (re-)opening a websocket connection: ${t.localizedMessage}", t)
				
			if(periodicID != null) {
				vertx.cancelTimer(periodicID)
				periodicID = null
			}
		
			if(completeHandler != null) {
				
				closeWebsocket()
				completeHandler.handle(t)
				completeHandler = null
				
			} else {
			
				//just close the websocket connection
				if(this.webSocket != null) {
					try {
						this.webSocket.close()
					} catch(Exception e) {
					}
					
					this.webSocket = null
					
				}
				
				if(attempt >= reconnectCount) {
					log.error("Error when reopening a websocket connection: ${t.localizedMessage}, attempt ${attempt} of ${reconnectCount}, notifying error handler", t)
					closed = true
					closeWebsocket()
					connectionErrorHandler.handle(attempt)
					return
				}

				attempt++
				log.error("Error when reopening a websocket connection: ${t.localizedMessage}, attempt ${attempt} of ${reconnectCount} retrying in ${reconnectIntervalMillis} milliseconds", t)
				
				//just keep retrying after
				reopeningTimer = vertx.setTimer(reconnectIntervalMillis) { Long timerID ->
					openWebSocket()
					reopeningTimer = null
					
				}
			
			}
			
		})
		
	}
	
	/**
	 * Connects to an endpoint. 
	 * Complete handler gets notified of success (null throwable), error otherwise
	 * The connection error handler is triggered when N reconnect attempts have failed
	 */
	public void	connect(Handler<Throwable> completeHandler, Handler<Void> connectionErrorHandler) {
		
		if(completeHandler == null) throw new NullPointerException("complete handler must not be null")
		if(connectionErrorHandler == null) throw new NullPointerException("connectionErrorHandler must not be null")
		this.completeHandler = completeHandler
		this.connectionErrorHandler = connectionErrorHandler
		
		url = new URL(endpointURL)
		
		Map opts = [ ssl: url.getProtocol().equalsIgnoreCase('https') ]
		
		httpClient = vertx.createHttpClient(opts)

		openWebSocket()
		
	}
	
	public VitalStatus closeWebsocket()  {
		
		if( this.closed ) {
			return VitalStatus.withOKMessage("Client already closed");
		}
		
		this.closed = true
		
		if(periodicID != null) {
			vertx.cancelTimer(periodicID)
			periodicID = null
		}
		
		VitalStatus status = null
		
		//just close the websocket connection
		if(this.webSocket != null) {
		
			try {
				
				this.webSocket.close()
				
//						status = VitalStatus.withOKMessage("async websocket client closed")
				
				
			} catch(Exception e) {

				log.error("Error when closing websocket: ${e.localizedMessage}", e)
//						status = VitalStatus.withError("Error when closing websocket: ${e.localizedMessage}")
			
			}
			
			this.webSocket = null
			
		}
		
		
		if(this.httpClient != null) {
			try {
				httpClient.close()
			} catch(Exception e) {
				log.error("Error when closing http client: ${e.localizedMessage}", e)
			}
			this.httpClient = null
		}
		
//		if(vertx != null) {
//			try {
//				vertx.close()
//			} catch(Exception e) {
//				log.error("Error when closing vertx: ${e.localizedMessage}", e)
//			}
//			this.vertx = null
//		}
		
		status = VitalStatus.withOKMessage("async websocket client closed")
		
		return status;
		
	}
	
	@Override
	protected void impl(Closure closure, String method, List args) {
	
		if(vertx == null) {
			ResponseMessage rm = new ResponseMessage("error_client_closed", "The async client is already closed")
			closure(rm)
			return
		}
		
		if(this.webSocket == null) {
			ResponseMessage res = new ResponseMessage("error_websocket_failed", "Websocket connection unavailable, retry")
			closure(res)
			return
		}
	
		if(method == 'close') {
			
			VitalStatus status = closeWebsocket()
			
			ResponseMessage res = new ResponseMessage()
			res.response = status
			
			closure(res)
		
			return
				
		}
	
		//serialize request
		
		//construct a json object and serialize it, binary format to be supported
		
		
		Map data = [
			method: method,
			args: args,
			sessionID: this.appSessionID
		]
		
		String replyAddress = UUID.randomUUID().toString() //makeUUID();
		
		Map envelope = [
			type: 'send',
			address: this.address,
			headers: [:],//mergeHeaders(this.defaultHeaders, headers),
			body: VitalServiceJSONMapper.toJSON(data),
			replyAddress: replyAddress 
			/*
			var replyAddress = makeUUID();
			envelope.replyAddress = replyAddress;
			this.replyHandlers[replyAddress] = callback;
			*/
		];
	
		callbacksMap.put(replyAddress, closure)	
	
//			byte[] bytes = SerializationUtils.serialize(new PayloadMessage(method, args))
//			Buffer buffer = Buffer.buffer(bytes.length)
//			((io.vertx.core.buffer.Buffer)buffer.getDelegate()).appendBytes(bytes)
		this.webSocket.writeFinalTextFrame(JsonOutput.toJson(envelope))
		
		long _timerID = vertx.setTimer(30000) { Long timerID ->
			
			callbacks2Timers.remove(replyAddress)
			
			if( callbacksMap.remove(replyAddress) != null ) {
				ResponseMessage res = new ResponseMessage('error_request_timeout', "Request timed out (30000ms)")
				closure(res)
			}
			
		}
	
	}

	public void query(String queryString, Closure closure) {
		impl(closure, 'query', [queryString])
	}

	private void callFunctionSuper(String function, Map<String, Object> arguments, Closure closure) {
		super.callFunction(function, arguments, closure)
	}
	
	@Override
	public void callFunction(String function, Map<String, Object> arguments, Closure closure) {

		if(function == GROOVY_LIST_STREAM_HANDLERS) {
			this.listStreamHandlers(arguments, closure)
			return
		} else if(function == GROOVY_REGISTER_STREAM_HANDLER) {
			this.registerStreamHandler(arguments, closure)
			return
		} else if(function == GROOVY_UNREGISTER_STREAM_HANDLER) {
			this.unregisterStreamHandler(arguments, closure);
			return
		} else if(function == VERTX_STREAM_SUBSCRIBE) {
			this.streamSubscribe(arguments, closure)
			return
		} else if(function == VERTX_STREAM_UNSUBSCRIBE) {
			this.streamUnsubscribe(arguments, closure)
			return
		}
		
		super.callFunction(function, arguments, closure)
		
	}
	
	private void listStreamHandlers(Map<String, Object> params, Closure closure) {
		
		ResultList rl = new ResultList()
		
		for(Entry<String, Closure> entry : this.registeredHandlers.entrySet()) {
			
			String key = entry.getKey()
			
			VITAL_Node g = new VITAL_Node()
			g.URI = 'handler:' + key
			g.active = this.currentHandlers.containsKey(key)
			g.name = key

			rl.addResult(g)
			
		}

		ResponseMessage rm = new ResponseMessage()
		rm.response = rl
		
		closure(rl) 		
		
	}
	
	private ResponseMessage errorResponseMessage(Closure closure, String exceptionType, String exceptionMessage) {
		ResponseMessage rm = new ResponseMessage()
		rm.exceptionType = exceptionType
		rm.exceptionMessage = exceptionMessage
		return rm
	}
	
	private void registerStreamHandler(Map<String, Object> arguments, Closure closure) {
		
		def streamName = arguments.streamName;
		if(streamName == null) {
			errorResponseMessage(closure, 'error_missing_param_stream_name', "No 'streamName' param")
			return
		}
		
		if(!(streamName instanceof String)) {
			errorResponseMessage(closure, 'error_stream_name_param_type', "streamName param must be a string: " + streamName.getClass().getCanonicalName())
			return
		}
		
		def handlerFunction = arguments.handlerFunction;
		
		if(handlerFunction == null) {
			errorResponseMessage(closure, 'error_missing_param_handler_function', "No 'handlerFunction' param");
			return;
		}
		
		if(!(handlerFunction instanceof Closure)) {
			errorResponseMessage(closure, 'error_handler_function_param_type', "handlerFunction param must be a closure: " + handlerFunction.getClass().getCanonicalName())
			return
		}
		
		
		if( this.registeredHandlers.containsKey(streamName) ) {
			errorResponseMessage(closure, 'error_stream_handler_already_registered', "Handler for stream " + streamName + " already registered.");
			return;
		}
		
		this.registeredHandlers.put(streamName, handlerFunction)
//		
		ResultList rl = new ResultList()
		rl.status = VitalStatus.withOKMessage('Handler for stream ' + streamName + ' registered successfully')

		ResponseMessage rm = new ResponseMessage()
		rm.response = rl
		closure(rm)
		
	}
	
	private void unregisterStreamHandler(Map<String, Object> arguments, Closure closure) {
		
		def streamName = arguments.streamName;
		if(streamName == null) {
			errorResponseMessage(closure, 'error_missing_param_stream_name', "No 'streamName' param")
			return
		}
		
		if(!(streamName instanceof String)) {
			errorResponseMessage(closure, 'error_stream_name_param_type', "streamName param must be a string: " + streamName.getClass().getCanonicalName())
			return
		}
		
		Closure currentHandler = this.registeredHandlers.get(streamName)
		
		if(currentHandler == null) {
			errorResponseMessage(closure, 'error_stream_handler_not_registered', "No handler for stream " + streamName + " registered")
			return;
		}
		
		if(this.currentHandlers.containsKey(streamName)) {
			errorResponseMessage(closure, 'error_handler_in_use', "Handler in use " + streamName)
			return
		}
		
		registeredHandlers.remove(streamName)
		
		ResultList rl = new ResultList()
		rl.status = VitalStatus.withOKMessage('Handler for stream ' + streamName + ' unregistered successfully')

		ResponseMessage rm = new ResponseMessage()
		rm.response = rl
		closure(rm)
		
	}
	
	private void streamSubscribe(Map<String, Object> arguments, Closure closure) {
		
		//first check if we are able to
		def streamName = arguments.streamName
		if(streamName == null) {
			errorResponseMessage(closure, 'error_missing_param_stream_name', "No 'streamName' param")
			return
		}
		
		if(!(streamName instanceof String)) {
			errorResponseMessage(closure, 'error_stream_name_param_type', "streamName param must be a string: " + streamName.getClass().getCanonicalName())
			return
		}

		Closure currentHandler = this.registeredHandlers.get(streamName)
		
		if(currentHandler == null) {
			errorResponseMessage(closure, 'error_stream_handler_not_registered', "No handler for stream " + streamName + " registered")
			return;
		}
		
		Closure activeHandler = this.currentHandlers.get(streamName)
		
		if(activeHandler != null) {
			errorResponseMessage(closure, 'error_stream_handler_already_subscribed', "Handler for stream " + streamName + " already subscribed")
			return;
		}
		
		
		super.callFunction(VERTX_STREAM_SUBSCRIBE, [streamNames: [streamName], sessionID: this.sessionID]) { ResponseMessage res ->
			
			if(res.exceptionType) {
				closure(res)
				return
			}
			
			ResultList resRL = res.response
			if(resRL.status.status != VitalStatus.Status.ok) {
				closure(res)
				return
			}
			

			if(! this.eventbusListenerActive ) {
				
				this.eventbusHandler = createNewHandler();
//				_this.eb.registerHandler('stream.'+ _this.sessionID, _this.eventbusHandler);
				String address = 'stream.'+ this.sessionID
				this.webSocket.writeFinalTextFrame(JsonOutput.toJson([
					type: 'register',
					address: address,
					headers: [:]
				]))
				
				
				streamCallbacksMap.put(address, this.eventbusHandler)
				
				this.eventbusListenerActive = true
			}
			
			this.currentHandlers.put(streamName, currentHandler)
			
			ResultList rl = new ResultList()
			rl.status = VitalStatus.withOKMessage('Successfully subscribed to stream ' + streamName)

			ResponseMessage rm = new ResponseMessage()
			rm.response = rl
			closure(rm)
			
		}
		
	}

	private Closure createNewHandler() {

		Closure wrapperHandler = { String streamName, ResultList rl ->

			Closure handler = this.currentHandlers.get(streamName)
			
			if(handler == null) {
				log.warn("Received a message for non-existing stream handler: " + streamName)
				return
				
			}			
			
			handler(rl)
			
		};
		
		return wrapperHandler;
		
	}
		
	private void streamUnsubscribe(Map<String, Object> arguments, Closure closure) {
		
		def streamName = arguments.streamName
		if(streamName == null) {
			errorResponseMessage(closure, 'error_missing_param_stream_name', "No 'streamName' param")
			return
		}
		
		if(!(streamName instanceof String)) {
			errorResponseMessage(closure, 'error_stream_name_param_type', "streamName param must be a string: " + streamName.getClass().getCanonicalName())
			return
		}
		
		Closure activeHandler = this.currentHandlers.get(streamName)
		
		if( activeHandler == null ) {
			errorResponseMessage(closure, 'error_no_subscribed_stream_handlers', "No handler subscribed to stream " + streamName);
			return;
		}
		
		super.callFunction(VERTX_STREAM_UNSUBSCRIBE, [streamNames: [streamName], sessionID: this.sessionID]) { ResponseMessage res ->
			
			if(res.exceptionType) {
				closure(res)
				return
			}
			
			ResultList resRL = res.response
			if(resRL.status.status != VitalStatus.Status.ok) {
				closure(res)
				return
			}
			
			this.currentHandlers.remove(streamName)
			
			if(this.currentHandlers.size() < 1) {

				this.streamCallbacksMap.clear()
				
//				_this.eb.unregisterHandler('stream.'+ _this.sessionID, _this.eventbusHandler);
				String address = 'stream.'+ this.sessionID
				this.webSocket.writeFinalTextFrame(JsonOutput.toJson([
					type: 'unregister',
					address: address,
					headers: [:]
				]))
				
				this.eventbusListenerActive = false;
				
			}
			
			
			ResultList rl = new ResultList()
			rl.status = VitalStatus.withOKMessage('Successfully unsubscribed from stream ' + streamName)

			ResponseMessage rm = new ResponseMessage()
			rm.response = rl
			closure(rm)
			
		}
		
	}	
	
}
