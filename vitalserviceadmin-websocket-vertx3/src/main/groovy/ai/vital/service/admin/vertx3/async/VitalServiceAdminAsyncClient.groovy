package ai.vital.service.admin.vertx3.async

import static ai.vital.vitalservice.VitalServiceConstants.NO_TRANSACTION
import groovy.json.JsonOutput
import io.vertx.core.Handler
import io.vertx.groovy.core.Vertx
import io.vertx.groovy.core.http.HttpClient
import io.vertx.groovy.core.http.WebSocket
import io.vertx.groovy.core.http.WebSocketFrame

import org.codehaus.jackson.map.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import ai.vital.service.vertx3.binary.ResponseMessage
import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalservice.json.VitalServiceJSONMapper

/**
 * Asynchronous version of vital service on vertx
 * All closures are called with {@link ai.vital.service.vertx.binary.ResponseMessage}
 * @author Derek
 *
 */
class VitalServiceAdminAsyncWebsocketClient extends VitalServiceAdminAsyncClientBase {

	private final static Logger log = LoggerFactory.getLogger(VitalServiceAdminAsyncWebsocketClient.class)
	
	String address
	
	String endpointURL = null
	
	Handler<Throwable> completeHandler
	
	HttpClient httpClient
	
	WebSocket webSocket
	
	Map<String, Closure> callbacksMap = [:]
	
	ObjectMapper objectMapper = null
	
	Long periodicID = null
	
	long pingInterval = 5000L;
	
	//append this to webservice object
	String appSessionID
	
	/**
	 * @param address, 'vitalserviceadmin' or 'endpoint.[appId]' if behind auth filter
	 */
	VitalServiceAdminAsyncWebsocketClient(Vertx vertx, String address, String endpointURL) {
		super(vertx)
		this.address = address
		this.vertx = vertx
		this.endpointURL = endpointURL
		
		objectMapper = new ObjectMapper()
		
	}
	
	protected void sendPing() {
		webSocket.writeFinalTextFrame(JsonOutput.toJson([type: 'ping']))
		log.info("Ping sent")
	}
	
	public void	connect(Handler<Throwable> completeHandler) {
		
		this.completeHandler = completeHandler
		
		URL url = new URL(endpointURL)
		
		int port = url.getPort()
		
		if(port < 0) {
			port = url.getDefaultPort()
		}
		
		Map opts = [ ssl: url.getProtocol().equalsIgnoreCase('https') ]
		
		httpClient = vertx.createHttpClient(opts)

		httpClient.websocket(port, url.getHost(), url.getPath(), {WebSocket ws ->
			
			webSocket = ws
			
			sendPing()
			
			periodicID = vertx.setPeriodic(pingInterval) { Long periodicID ->
				this.periodicID = periodicID
				sendPing()
			}
			
			webSocket.frameHandler { WebSocketFrame frame ->
				
//				if( log.isDebugEnabled() ) log.debug("Frame received: ${frame}")
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
					
					Closure callback = callbacksMap.remove(replyAddress)
					
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
					
					Map responseObject = body.response
					
					if(responseObject == null) {
						rm.exceptionType = 'errir_no_.response'
						rm.exceptionMessage = 'No response object'
						callback(rm)
						return
					}
					
					try {
						
						Object resObj = VitalServiceJSONMapper.fromJSON(responseObject)
						
						rm.response = resObj
						
					} catch(Exception e) {
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
			
			log.info("WebSocket connection to ${endpointURL} ready")
			
			completeHandler.handle(null)
			
		}, {Throwable t ->
		
			log.error("Error when opening a websocket connection: ${t.localizedMessage}", t)
		
			completeHandler.handle(t)
			
		})
		
	}
	
	public VitalStatus closeWebsocket()  {
		
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
		
		if(vertx != null) {
			try {
				vertx.close()
			} catch(Exception e) {
				log.error("Error when closing vertx: ${e.localizedMessage}", e)
			}
			this.vertx = null
		}
		
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
	
	}

	public void query(String queryString, Closure closure) {
		impl(closure, 'query', [queryString])
	}

	
}
