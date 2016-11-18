package ai.vital.service.vertx3.websocket

import groovy.json.JsonOutput
import groovy.lang.Closure;
import io.vertx.core.Handler
import io.vertx.groovy.core.Vertx
import io.vertx.groovy.core.http.HttpClient
import io.vertx.groovy.core.http.WebSocket
import io.vertx.groovy.core.http.WebSocketFrame

import org.codehaus.jackson.map.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import ai.vital.service.vertx3.async.VitalServiceAsyncClientBase
import ai.vital.service.vertx3.binary.ResponseMessage
import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalservice.json.VitalServiceJSONMapper
import ai.vital.vitalservice.query.VitalQuery;
import ai.vital.vitalsigns.model.VitalApp

/**
 * External Asynchronous version of vital service on vertx over websocket
 * All closures are called with {@link ai.vital.service.vertx.binary.ResponseMessage}
 * @author Derek
 *
 */
class VitalServiceAsyncWebsocketClient extends VitalServiceAsyncClientBase {

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
	
	ObjectMapper objectMapper = null
	
	Long periodicID = null
	
	long pingInterval = 5000L;
	
	//append this to webservice object
	String appSessionID
	
	int reconnectCount = 5
	int reconnectIntervalMillis = 3000
	
	
	private int attempt = 0
	
	private boolean closed = false
	
	private URL url
	
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
						callback(rm)
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
							callback(rm)
							return
						}
						
						responseObject = body.response
					
					}
					
					if(responseObject == null) {
						rm.exceptionType = 'error_no_response'
						rm.exceptionMessage = 'No response object'
						callback(rm)
						return
					}
					
					try {
						
						
						//remove stream name!
						responseObject.remove('streamName')
						
						Object resObj = VitalServiceJSONMapper.fromJSON(responseObject)
						
						rm.response = resObj
						
					} catch(Exception e) {
						log.error(e.localizedMessage, e)
						rm.exceptionType = e.getClass().getCanonicalName()
						rm.exceptionMessage = e.localizedMessage
						callback(rm)
						return
					}
					
					callback(rm)
						
				} else {
				
					log.warn("Unknown msg type: ${mtype}")
				
				}
				
//				println frame.isText()
//
//				log.info("Frame received: ${frame} ${textHandlerID}")
				
				
			}
			
			/*
			webSocket.closeHandler {
				log.info("WebSocket close handler")
				
				if(closed) {
					log.info("client already closed")
//				} else {
//					log.warn("re-opening websocket")
//					openWebSocket()
				}
				
			}
			*/
			
			webSocket.exceptionHandler { Throwable t ->
				log.error("WEBSOCKET EXCEPTION", t)
			}
			
			log.info("WebSocket connection to ${endpointURL} ready")
			
			if(completeHandler != null) {
				completeHandler.handle(null)
				//handler notified only once
				completeHandler = null
			}
			
			if(reconnectHandler != null) {
				reconnectHandler.handle()
			}
			
		}, {Throwable t ->
		
			if(periodicID != null) {
				vertx.cancelTimer(periodicID)
				periodicID = null
			}
		
			if(completeHandler != null) {
				
				log.error("Error when opening a websocket connection: ${t.localizedMessage}", t)
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
				vertx.setTimer(reconnectIntervalMillis) { Long timerID ->
					
					openWebSocket()
					
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
}
