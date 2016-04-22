package ai.vital.auth.mod

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry

import org.githubusercontent.defuse.passwordhash.PasswordHash;
import org.vertx.groovy.core.Vertx;
import org.vertx.groovy.core.eventbus.EventBus
import org.vertx.groovy.platform.Verticle
import org.vertx.groovy.core.eventbus.Message;
import org.vertx.java.core.Future;

import ai.vital.auth.handlers.VitalAuthoriseHandler;
import ai.vital.auth.handlers.VitalLoginHandler;
import ai.vital.auth.handlers.VitalLogoutHandler;
import ai.vital.auth.mod.AppFilter.Auth;
import ai.vital.auth.mod.AppFilter.Rule
import ai.vital.auth.queries.Queries;
import ai.vital.domain.CredentialsLogin;
import ai.vital.domain.Login;
import ai.vital.domain.AdminLogin
import ai.vital.domain.UserSession;
import ai.vital.service.vertx.VitalServiceMod;
import ai.vital.service.vertx.async.VitalServiceAsyncClient;
import ai.vital.service.vertx.binary.ResponseMessage;
import ai.vital.service.vertx.handler.AbstractVitalServiceHandler;
import ai.vital.vitalservice.VitalService
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.json.VitalServiceJSONMapper;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalSelectQuery
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.meta.GraphContext;
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.VitalSegment
import ai.vital.vitalsigns.model.property.URIProperty;



class VitalAuthManager extends Verticle {

	public final static String error_no_username = 'error_no_username' 
	
	public final static String error_no_password = 'error_no_password'
	
	public final static String error_no_type = 'error_no_type'
	
	public final static String error_no_appid = 'error_no_appid'
	
	public final static String error_app_not_found = 'error_app_not_found'
	
	public final static String error_unknown_type = 'error_unknown_type'
	
	public final static String error_unsupported_type = 'error_unsupported_type'
	 
	public final static String error_vital_service = 'error_vital_service'
	 
	public final static String error_not_logged_in = 'error_not_logged_in'
	 
	public final static String error_login_no_longer_exists = 'error_login_no_longer_exists'
	
	public final static String error_no_sessionid_param = 'error_no_sessionid_param'
	 
	public final static String error_denied = 'error_denied'
	 
	public final static String error_invalid_password = 'error_invalid_password'
	 
	public final static String error_invalid_username = 'error_invalid_username'
	 
	public final static String error_email_unverified = 'error_email_unverified'
	 
	public final static String error_login_inactive = 'error_login_inactive'
	 
	public final static String error_invalid_session_string = 'error_invalid_session_string'
	 
	
	public final static String function_login = 'vitalauth.login'
	
	public final static String function_logout = 'vitalauth.logout'
	
	public final static String function_authorise = 'vitalauth.authorise'
	
	
	public final static String address_login     = "vitalauth.login"
//	
	public final static String address_logout    = "vitalauth.logout"
//	
	public final static String address_authorise = "vitalauth.authorise"
	

	protected static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000;
	
	static Login guestLogin = new Login()
	
	static {
		guestLogin.URI = "urn:guestLogin"
		guestLogin.active = true
		guestLogin.emailVerified = true
		guestLogin.locked = false
		guestLogin.name = "Guest Login"
		guestLogin.password = PasswordHash.createHash("guest")
		guestLogin.username = "guest"
	}
	
	
	static class AuthAppBean {
		
		boolean authEnabled = false
		
		//keep each map separate ?	
		// sessionID -> login URI
		protected Map<String, String> sessions = null
		// login URI 2 active logins
		protected Map<String, List<LoginInfo>> logins = null
		
		protected long sessionTimeout;
		
		protected VitalSegment loginsSegment = null
				
		protected VitalSegment adminLoginsSegment = null
				
		protected VitalSegment superAdminLoginsSegment = null
		
		protected VitalSegment sessionsSegment = null		
		
		protected int maxSessionsPerUser = 1 
				
		protected Boolean persistentSessions = null
		
		//only for 
		protected Boolean mockedLogin = null
		
		//in new version this param means how ofset the session timestamp should be refreshed when accessed
		protected Integer expirationProlongMargin = null
				
		protected VitalServiceAsyncClient vitalService = null
		
		protected VitalService vitalServiceSync = null
		
		protected String appID		

		protected VitalApp app
		
		protected AppFilter filter
		
		protected Vertx vertx		
		
		protected String authorizeAddress
		
		protected CredentialsLogin getGuestLogin() {
			return VitalAuthManager.guestLogin
		}
		
		protected void _initService(Vertx vertx) {
			
			this.vertx = vertx
			
			vitalServiceSync = VitalServiceMod.registeredServices.get(appID)
			if(vitalServiceSync == null) throw new RuntimeException("AppID: ${appID} service instance not registered")
			
			vitalService = new VitalServiceAsyncClient(vertx, VitalApp.withId(appID))
			
			app = vitalServiceSync.getApp()
			
			
		}
		
		protected VitalSegment _getSegment(String segmentID) {
			return vitalServiceSync.getSegment(segmentID)
		}
		
		protected boolean _supportsNormal() { return true }
		
		protected boolean _supportsAdmin() { return false }
		
		protected boolean _supportsSuperAdmin() { return false }
		
		
		protected void _executeSelectQuery(VitalSelectQuery selectQuery, Closure closure) {
		
			vitalService.query(selectQuery, closure)
				
		}
		
		protected void _generateURI(Class<? extends GraphObject> clazz, Closure closure) {
			
			vitalService.generateURI(clazz, closure)
			
		}
		
		protected void _save(VitalSegment targetSegment, List<GraphObject> objects, Closure closure) {
			
			vitalService.save(targetSegment, objects, true, closure)
			
		}
		
		protected void _delete(List<URIProperty> uris, Closure closure) {
			
			vitalService.delete(uris, closure)
			
		}
		
		protected void _getRemoteObject(String uri, Closure closure) {
			
			vitalService.get(GraphContext.ServiceWide, URIProperty.withString(uri), true, closure)
			
		}
		
		protected void _callFunction(String fname, Map<String, Object> params, Closure closure) {
			
			vitalService.callFunction(fname, params, closure)
			
		}
		
		protected VitalSegment _getTargetSegment() {
			return loginsSegment
		}
		
		protected Class<? extends CredentialsLogin> _getSuperAdminLoginClass() { 
			return null
		}
		
		protected void _passMessage(Message msg) {
			
			vertx.eventBus.send(vitalService.address, msg.body()) { Message response ->
				
				msg.reply(response.body())
				
			}
			
//			msg.setMetaClass(
//			vitalService.address
			
		}
		
		protected final void checkSegments() {
			
			if(loginsSegment != null) {
				String sid = loginsSegment.segmentID.toString()
				loginsSegment = _getSegment(sid)
				if(loginsSegment == null) throw new RuntimeException("AppID: ${appID} Logins segment not found: ${sid}")
				
			}
			
			if(adminLoginsSegment != null) {
				String sid = adminLoginsSegment.segmentID.toString()
				adminLoginsSegment = _getSegment(sid)
				if(adminLoginsSegment == null) throw new RuntimeException("AppID: ${appID} Admin Logins segment not found: ${sid}")
			}
			
			if(superAdminLoginsSegment != null) {
				String sid = superAdminLoginsSegment.segmentID.toString()
				superAdminLoginsSegment = _getSegment(sid)
				if(superAdminLoginsSegment == null) throw new RuntimeException("AppID: ${appID} SuperAdmin Logins segment not found: ${sid}")
			}
			
			if(sessionsSegment != null) {
				String sid = sessionsSegment.segmentID.toString()
				sessionsSegment = _getSegment(sid)
				if(sessionsSegment == null) throw new RuntimeException("AppID: ${appID} sessions segment not found: ${sid}")
				
			}
		}
		
		protected VitalSegment getLoginsSegment(Message message) {
			
			Map body = message.body()
			
			String type = body.get('type');
			
			if(type == Login.class.simpleName) {
				
				return loginsSegment
				
			} else if(type == AdminLogin.class.simpleName) {
			
				if(!_supportsAdmin()) {
					message.reply([status: error_unknown_type, message: 'Unsupported type: ' + type])
					return
				}
			
				return adminLoginsSegment
			
			} else if(type == 'SuperAdminLogin') {

				if(!_supportsSuperAdmin()) {
					message.reply([status: error_unknown_type, message: 'Unsupported type: ' + type])
					return
				}
				
				return superAdminLoginsSegment
			
			} else {
				message.reply([status: error_unknown_type, message: 'Unknown type: ' + type])
				return
			}
			
		}
		
	}
	
	static Map<String, AuthAppBean> beans = [:]
	
	protected static boolean initialized = false
	
	List<String> beansToCheck = null
	
	
	private static final class LoginInfo {
		final String sessionID;
		long sessionTimestamp;

		private LoginInfo(String sessionID, long sessionTimestamp) {
			this.sessionID = sessionID;
			this.sessionTimestamp = sessionTimestamp
		}
		
	}

	/**
	 * Start the busmod
	 */
	public Object start(Future<Void> startedResult) {

		synchronized(VitalAuthManager.class) {
			
			if(initialized) {
				startedResult.setResult(new Object())
				return startedResult
			}
			
			initialized = true
			
		}
		
		
		Object appsMapO = container.getConfig().get('apps')
		if(appsMapO == null) throw new RuntimeException("No 'apps' map param")
		if(!(appsMapO instanceof Map)) throw new RuntimeException("'apps' param must be a map")
		
		Map<String, Object> appsMap = appsMapO
		
		if(appsMap.size() == 0) throw new RuntimeException("Apps map must not be empty")
		
		for(Entry<String, Object> e : appsMap.entrySet()) {
			
			
			String appID = e.getKey()
			
			
			container.logger.info("Setting up app: ${appID}")
			
			Object cfgO = e.getValue()
			if(!(cfgO instanceof Map)) throw new RuntimeException("App ${appID} config must be a map")
			
			Map<String, Object> appCfg = cfgO
			String access = appCfg.get('access')
			
			if(!access) throw new RuntimeException("App ${appID} must provide 'access' param")
			
			AuthAppBean bean = createBean(access, appCfg)
			bean.authorizeAddress = getAddress_authorise()
			bean.appID = appID
			
			container.logger.info("App: ${appID} access: ${access}")
			
			
			Boolean authEnabled = appCfg.get('auth_enabled')
			if(authEnabled == null) throw new RuntimeException("No auth_enabled boolean param")
			
			bean.authEnabled = authEnabled
			
			//
			if(authEnabled) {
				
				if(access == 'service') {
					
					if(!bean._supportsNormal()) throw new RuntimeException("This module does not support regular login mode");
					
					if(!bean.mockedLogin) {
						String loginsSegmentParam = appCfg.get('loginsSegment')
						if(!loginsSegmentParam) throw new RuntimeException("App ${appID} must provide logins segment")
						
						bean.loginsSegment = VitalSegment.withId(loginsSegmentParam)
					}
					
				} else if(access == 'admin') {
					
					if(!bean._supportsAdmin()) throw new RuntimeException("This module does not support admin mode");
					
					if(!bean.mockedLogin) {
						String adminLoginsSegmentParam = appCfg.get('adminLoginsSegment')
								if(!adminLoginsSegmentParam) throw new RuntimeException("App ${appID} must provide admin logins segment")
						
						bean.adminLoginsSegment = VitalSegment.withId(adminLoginsSegmentParam)
					}
					
				} else if(access == 'superadmin') {

					String superAdminLoginsSegmentParam = appCfg.get('superAdminLoginsSegment')
					if(!superAdminLoginsSegmentParam) throw new RuntimeException("App ${appID} must provide super admin logins segment")
									
					if(!bean._supportsSuperAdmin()) throw new RuntimeException("This module does not support super admin mode");
					
					bean.superAdminLoginsSegment = VitalSegment.withId(superAdminLoginsSegmentParam)
					
				} else {
					
					throw new RuntimeException("Unknown service access level: ${access}")
					
				}
				
				bean.persistentSessions = appCfg.get('persistentSessions')
						
						if(bean.persistentSessions == null) {
							throw new RuntimeException("App ${appID}: No persistentSessions param.")
						}
				container.logger.info "App: ${appID} persistent sessions: ${bean.persistentSessions}"
				
				
				bean.mockedLogin = appCfg.get('mockedLogin')
				if(bean.mockedLogin == null) bean.mockedLogin = false
				
				if(bean.mockedLogin.booleanValue()) {
					container.logger.warn("vital-auth-mode running in mocked-login mode")
					bean.loginsSegment = null
					bean.adminLoginsSegment = null
					bean.superAdminLoginsSegment = null
				}
				
				
				Integer maxSessionsPerUserParam = appCfg.get("maxSessionsPerUser")
				
				if(maxSessionsPerUserParam != null) {
					bean.maxSessionsPerUser = maxSessionsPerUserParam.intValue()
				}
				container.logger.info("App: ${appID} max sessions per user: ${bean.maxSessionsPerUser}")
				
				
				String sessionsSegment = appCfg.get('sessionsSegment')
				
				if(!bean.persistentSessions) {
					
					bean.sessions = new HashMap<>();
					
					bean.logins = new HashMap<>();
					
				} else {
					
					if(!sessionsSegment) throw new RuntimeException("App: ${appID} no sessionsSegment param, required when persistent sessions = true")
					
					bean.sessionsSegment = VitalSegment.withId(sessionsSegment)
					
				}
				
				
				Object timeout = appCfg.get('session_timeout')
						
				if (timeout != null) {
					if (timeout instanceof Long) {
						bean.sessionTimeout = (Long)timeout;
					} else if (timeout instanceof Integer) {
						bean.sessionTimeout = (Integer)timeout;
					}
				} else {
					bean.sessionTimeout = DEFAULT_SESSION_TIMEOUT;
				}
				
				container.logger.info "App: ${appID} session Timeout: ${bean.sessionTimeout}"
				
				
				Object expirationProlongMargin = appCfg.get('expirationProlongMargin')
				if(expirationProlongMargin == null) {
					throw new RuntimeException("App: ${appID} No expirationProlongMargin param")
				}
				bean.expirationProlongMargin = expirationProlongMargin
						container.logger.info("App: ${appID} expirationProlongMargin: ${bean.expirationProlongMargin}")
						
				if(timeout > 0 && expirationProlongMargin >= timeout) {
					throw new RuntimeException("expiration prolong margin cannot be greater than timeout")
				}
				
			}
			
			
			//filter is a list of rules
			Object filterO = appCfg.get('filter')
			if(filterO == null) {
				throw new RuntimeException("App: ${appID} no filter list defined")
			}
			
			AppFilter filter = AppFilter.fromJsonList(filterO)
			
			bean.filter = filter
			
			if(!authEnabled.booleanValue()) {
				
				for(Rule r : filter.rules) {
				
					if(r instanceof Auth) {
						
						throw new RuntimeException("Cannot use Auth rules in an app without authentication enabled")
						
					}
						
				}
				
			}
			
			
			bean._initService(vertx)
			
			bean.checkSegments()
			
			beans.put(appID, bean)
			
		}
		
		

		EventBus eb = vertx.eventBus
		
		eb.registerHandler(getAddress_login()) { Message message ->
			doLogin(message);
		}
		
		eb.registerHandler(getAddress_logout()) { Message message ->
			doLogout(message);
		}
		
		eb.registerHandler(getAddress_authorise()) { Message message ->
			doAuthorise(message);
		}
		
		startedResult.setResult(new Object())
	
		VitalJSEndpointsManager manager = new VitalJSEndpointsManager(this)
		
		//register the handler directly
		AbstractVitalServiceHandler.commonFunctionHandlers.put(VitalAuthoriseHandler.function_authorise, new VitalAuthoriseHandler([authoriseAddress: getAddress_authorise()]))
		AbstractVitalServiceHandler.commonFunctionHandlers.put(VitalLoginHandler.function_login, new VitalLoginHandler([loginAddress: getAddress_login()]))
		AbstractVitalServiceHandler.commonFunctionHandlers.put(VitalLogoutHandler.function_logout, new VitalLogoutHandler([logoutAddress: getAddress_logout()]))
		
		manager.deployEndpoints()
		
		return new Object()
		
	}
	
	
	
	protected String getAddress_login() {
		return address_login
	}
	
	protected String getAddress_logout() {
		return address_logout
	}
	
	protected String getAddress_authorise() {
		return address_authorise
	}
	
	
	protected boolean validateOtherFields(Message message) {
		return true
	}
	
	protected AuthAppBean createBean(String access, Map<String, Object> appCfg) {
		if(access == 'service') return new AuthAppBean()
		throw new RuntimeException("Unsupported access: ${access}")
	}
	
	protected void doLogin(Message message) {

		Map body = message.body()

		String type = body.get('type')
		
		String appID = body.get('appID')
		
		if(!type) {
			message.reply([status: error_no_type, message: 'No type parameter'])
			return
		}
		
		if(!appID) {
			message.reply([status: error_no_appid, message: 'No appID parameter'])
			return
		}
		
		String username = body.get("username");
		if(!username) {
			message.reply([status: error_no_username, message: 'No username parameter'])
			return
		}

		String password = body.get("password");
		if(!password) {
			message.reply([status: error_no_password, message: 'No password parameter'])
			return
		}
		
		AuthAppBean bean = beans.get(appID)
		if(bean == null) {
			message.reply([status: error_app_not_found, message: "App does not support authentication"])
			return
		}
		
		Class cls = null
		
		
		if(type == Login.class.simpleName) {
			
			if(!bean._supportsNormal()) {
				message.reply([status: error_unknown_type, message: "This app does not support service logins"])
				return
			}
			
			cls = Login.class
			
		} else if(type == AdminLogin.class.simpleName) {
		
			if(!bean._supportsAdmin()) {
				message.reply([status: error_unknown_type, message: "This app does not support admin logins"])
				return
			}
		
			cls = AdminLogin.class
		
		} else if(type == 'SuperAdminLogin') {
		
			if(!bean._supportsSuperAdmin()) {
				message.reply([status: error_unknown_type, message: "This app does not support super admin logins"])
				return
			}
		
			cls = bean._getSuperAdminLoginClass()
		
		} else {
			message.reply([status: error_unknown_type, message: 'Unknown type: ' + type])
			return
		}
		
		if(!validateOtherFields(message)) return;
		
		VitalSegment loginsSegment = bean._getTargetSegment()
//		if(loginsSegment == null) return
		
//		if(segment == null) {
//			message.reply([status: error_unsupported_type, message: (String) "login type: ${type} unsupported"])
//			return
//		}

		
		final long _timeoutValue = bean.sessionTimeout

		def onUserSelectQueryResponse = { ResponseMessage selectQueryReply ->
		
			if( selectQueryReply.exceptionMessage ) {
				message.reply([status: error_vital_service, message: 'VitalService error: ' + selectQueryReply.exceptionType + ' - ' + selectQueryReply.exceptionMessage])
				return
			}
			
			ResultList rs = selectQueryReply.response
			
			if (rs.status.status != VitalStatus.Status.ok) {
				message.reply([status: error_vital_service, message: 'VitalService error: ' + rs.status.toString()])
				return
			}
			
			if( rs.results.size() < 1 ) {
				message.reply([status: error_invalid_username, message: 'User not found: ' + username])
				return
			}
			
			CredentialsLogin login = rs.results[0].graphObject
			
			String savedHashedPasswd = login.password
			
			try {
				if( ! PasswordHash.validatePassword(password, savedHashedPasswd) ) {
					message.reply([status: error_invalid_password, message: 'Invalid password'])
					return
				}
			} catch(Exception e) {
				message.reply([status: error_vital_service, message: 'Password validation error: ' + e.localizedMessage])
				return
			}
			
			//hash login.password
			
			if(login instanceof Login) {
				Login l = login
				if( l.emailVerified == null || l.emailVerified.booleanValue() == false ) {
					message.reply([status: error_email_unverified, message: (String) "The email ${username} is not verified, cannot log in"])
					return
				}
				if( l.active != null && l.active.booleanValue() == false ) {
					message.reply([status: error_login_inactive, message: (String) "The login is no longer active"])
					return
				}
			}
			
			if(bean.persistentSessions) {
				
				def onSessionsCountChecked = {
					
					bean._generateURI(UserSession.class) { ResponseMessage generateReply ->

						if (generateReply.exceptionMessage) {
							message.reply([status: error_vital_service, message: 'VitalService error: ' + generateReply.exceptionType + ' - ' + generateReply.exceptionMessage])
							return
						}
						
						URIProperty newURI = generateReply.response
						
						UserSession session = new UserSession()
						session.URI = newURI.get()
						
						session.timestamp = System.currentTimeMillis()
						session.sessionType = type
						//expiration date no longer in use
//						if(_timeoutValue > 0) {
//							session.expirationDate = new Date(System.currentTimeMillis() + _timeoutValue) 
//						}
						
						session.loginURI = new URIProperty(login.URI)
						session.sessionID = type + '_' + UUID.randomUUID().toString()
						
						bean._save(bean.sessionsSegment, [session]) { ResponseMessage saveSessionReply ->
							
							if (saveSessionReply.exceptionMessage) {
								message.reply([status: error_vital_service, message: 'VitalService error: ' + saveSessionReply.exceptionType + ' - ' + saveSessionReply.exceptionMessage])
								return
							}
								
							message.reply([status: 'ok', message: 'logged in', sessionID: session.sessionID.toString(), object: VitalServiceJSONMapper.toJSON(login)])
								
						}

						
					}
					
					
					
				};
				
				//if max session per user reached select existing sessions to see if not exceeded
				if(bean.maxSessionsPerUser > 0) {
					
					VitalSelectQuery currentSessionsQuery = Queries.getSessions(bean.sessionsSegment, login.URI, bean.maxSessionsPerUser * 10)

					bean._executeSelectQuery(currentSessionsQuery) { ResponseMessage currentSessionsReply ->
						
						if (currentSessionsReply.exceptionMessage) {
							message.reply([status: error_vital_service, message: 'VitalService error: ' + currentSessionsReply.exceptionType + ' - ' + currentSessionsReply.exceptionMessage])
							return
						}
						
						ResultList sessionsRL = currentSessionsReply.response
						
						if(sessionsRL.status.status != VitalStatus.Status.ok) {
							message.reply([status: error_vital_service, message: 'VitalService error: ' + sessionsRL.status])
							return
						}

						if(sessionsRL.results.size() >= bean.maxSessionsPerUser) {
							
							List<UserSession> sessions = []
							for(GraphObject g : sessionsRL) {
								sessions.add(g)
							}
							
							sessions.sort { UserSession s1, UserSession s2 ->
								
								Date d1 = s1.timestamp != null ? new Date(s1.timestamp.longValue()) : new Date(0L)
								Date d2 = s2.timestamp != null ? new Date(s2.timestamp.longValue()) : new Date(0L)
								
								return d2.compareTo(d1)
								
							}
							
							List<URIProperty> sessionsToRemove = []
							
							for(int i = bean.maxSessionsPerUser-1; i < sessions.size(); i++) {
								sessionsToRemove.add(URIProperty.withString(sessions.get(i).URI))
							}
							
							//delete old sessions
							bean._delete(sessionsToRemove) { ResponseMessage deleteSessionsReply ->
								if (deleteSessionsReply.exceptionMessage) {
									message.reply([status: error_vital_service, message: 'VitalService error: ' + deleteSessionsReply.exceptionType + ' - ' + deleteSessionsReply.exceptionMessage])
									return
								}
								onSessionsCountChecked();
							}
							
						} else {
							onSessionsCountChecked();
						}
												
						
						
						
					}
					
				} else {
					onSessionsCountChecked();
				}
				
			} else {
			
				List<LoginInfo> info = bean.logins.get(login.URI)
				if(info != null) {
					if(bean.maxSessionsPerUser > 0 && info.size() > bean.maxSessionsPerUser - 1) {
								
						//sort the list 
						List<LoginInfo> infos = new ArrayList<LoginInfo>(info)
						infos.sort { LoginInfo l1, LoginInfo l2 ->
							return l1.sessionTimestamp.compareTo(l2.sessionTimestamp)	
						}
								
						while(infos.size() > bean.maxSessionsPerUser - 1) {
							//logout any
							LoginInfo expInfo = infos.remove(0)
							bean.sessions.remove(expInfo.sessionID)
						}
					}
				}
				
				String sessionID = UUID.randomUUID().toString()
				
				//session expires after some time	
				
				//password is hashed, compare hashes
				
				bean.sessions.put(sessionID, login.URI);
				
				//refresh the list
				info = bean.logins.get(login.URI)
				if(info == null) {
					info = Collections.synchronizedList(new ArrayList<LoginInfo>())
					bean.logins.put(login.URI, info);
				} 
				
				info.add(new LoginInfo(sessionID, System.currentTimeMillis()))
				
				message.reply([status: 'ok', message: 'logged in', sessionID: sessionID, object: VitalServiceJSONMapper.toJSON(login)])
				
			}
			
		}
		
		if(bean.mockedLogin) {
			
			ResultList rl = new ResultList()
			if(username == "guest") {
				rl.addResult(guestLogin)
			}
			
			//prepare fake response message
			ResponseMessage rm = new ResponseMessage()
			rm.setResponse(rl)
			onUserSelectQueryResponse(rm)
			
		} else {
		
			VitalSelectQuery selectQuery = Queries.selectTypedLoginQuery(cls, loginsSegment, username)
		
			bean._executeSelectQuery(selectQuery, onUserSelectQueryResponse)
			
		}
		
	}

	protected void doLogout(Message message) {
		
		Map body = message.body
		
		String sessionID = body.get("sessionID");
		if(!sessionID) {
			message.reply([status: error_no_sessionid_param, message: 'no sessionID param'])
			return
		}
		
		String appID = body.get('appID')
		if(!appID) {
			message.reply([status: error_no_appid, message: 'no appID param'])
			return
		}
		
		AuthAppBean bean = beans.get(appID)
		if(bean == null) {
			message.reply([status: error_app_not_found, message: "App does not support authentication"])
			return
		}
		
		
		if( ! validateOtherFields(message) ) return
		
		if (sessionID != null) {
			logout(bean, message, sessionID) { boolean loggedOut ->
				if(loggedOut) {
					message.reply([status: 'ok', message: 'Logged out'])
				} else {
					message.reply([status: error_not_logged_in, message: 'Not logged in'])
				}
			}
		} else {
			message.reply([status: 'ok', message: 'Logged out'])
		}
	}

	protected void logout(AuthAppBean bean, Message message, String sessionID, Closure callback) {
		
		if(bean.persistentSessions) {
			
			int uscore = sessionID.indexOf('_')
			
			if(uscore <= 0) {
				callback(false)
				return
			}
			
			String type = sessionID.substring(0, uscore);
			
			Map b = message.body
			if(b.type == null) b.type = type
			
			VitalSegment segment = bean.sessionsSegment
			
			if(type == Login.class.simpleName) {
				
			} else if(type == AdminLogin.class.simpleName) {
				
			} else if(type == 'SuperAdminLogin') {
				
			} else {
				container.logger.error("invalid session type: ${type}")
				callback(false)
				return
			}
			
			VitalSelectQuery currentSessionQuery = Queries.getSession(segment, sessionID)

			bean._executeSelectQuery(currentSessionQuery) { ResponseMessage currentSessionsReply ->
				
				if( currentSessionsReply.exceptionMessage ) {
					callback(false)
					return
				}
				
				ResultList sessionsRL = currentSessionsReply.response
				
				if(sessionsRL.status.status != VitalStatus.Status.ok) {
					callback(false)
					return
				}
				
				if(sessionsRL.results.size() > 0) {
					
					URIProperty sessionToRemove = URIProperty.withString(sessionsRL.results[0].graphObject.URI)
					
					bean._delete([sessionToRemove]) { ResponseMessage deleteSessionsReply ->
						
						if (deleteSessionsReply.exceptionMessage) {
							callback(false)
							return
						}
						
						VitalStatus status = deleteSessionsReply.response
						
						if(status.status != VitalStatus.Status.ok) {
							callback(false)
							return
						}
						
						callback(true)
						
					}
					
				} else {
				
					callback(false)
					
				}
			}
			
		} else {
		
			String loginURI = bean.sessions.remove(sessionID);
			if(loginURI == null) {
				callback(false)
				return
			}
			
			List<LoginInfo> infoList = bean.logins.get(loginURI);
			if(infoList != null)  {
				List<LoginInfo> infos = new ArrayList<LoginInfo>(infoList)
				for(LoginInfo li : infos) {
					if(li.sessionID == sessionID) {
						infoList.remove(li)
						break
					}
				}
			}
			
			if(infoList.size() < 1) {
				bean.logins.remove(loginURI)
			}
			
			callback(true);
			
		}
		
	}

	protected void doAuthorise(Message message) {
		
		Map body = ((Map)message.body)
		
		String sessionID = body.get("sessionID");
		
		if (sessionID == null || sessionID.length() == 0) {
			message.reply([status: error_no_sessionid_param, message: 'No session ID parameter'])
			return;
		}
		
		String appID = body.get('appID')
		if(!appID) {
			message.reply([status: error_no_appid, message: 'no appID param'])
			return
		}
		
		AuthAppBean bean = beans.get(appID)
		if(bean == null) {
			message.reply([status: error_app_not_found, message: "App does not support authentication"])
			return
		}
		
		
		if(bean.persistentSessions) {
			
			doAuthorisePersistent(bean, message, sessionID)
			
		} else {
		
			doAuthoriseVolatile(bean, message, sessionID)
			
		}
		
	}
	
	protected void doAuthorisePersistent(AuthAppBean bean, Message message, String sessionID) {
		
		
		int uscore = sessionID.indexOf('_')
		
		if(uscore <= 0) {
			message.reply([status: error_invalid_session_string, message: (String)"invalid session string: ${sessionID}"])
			return
		}
		
		String type = sessionID.substring(0, uscore)
		
		message.body().put('type', type)
		
		if(!validateOtherFields(message)) return
		
		VitalSegment sessionsSegment = bean.sessionsSegment
		
		VitalSelectQuery currentSessionsQuery = Queries.getSession(sessionsSegment, sessionID)

		bean._executeSelectQuery(currentSessionsQuery) { ResponseMessage currentSessionsReply ->
			
			if(currentSessionsReply.exceptionMessage) {
				message.reply([status: error_vital_service, message: 'VitalService error: ' + currentSessionsReply.exceptionType + ' - ' + currentSessionsReply.exceptionMessage])
				return
			}
			
			ResultList sessionsRL = currentSessionsReply.response
			
			if(sessionsRL.status.status != VitalStatus.Status.ok) {
				message.reply([status: error_vital_service, message: 'VitalService error: ' + sessionsRL.status])
				return
			}
			
			UserSession session = null
			
			for(GraphObject g : sessionsRL) {
				
				session = (UserSession) g
				
			}
			
			if(session == null) {
				message.reply([status: error_denied, message: 'Session not found, session: ' + sessionID])
				return
			}
			
			Long timestamp = session.timestamp ? session.timestamp.longValue() : null
			
			long _timeoutValue = bean.sessionTimeout
			
			if(timestamp != null && _timeoutValue > 0L && ( System.currentTimeMillis() - timestamp.longValue() ) > _timeoutValue) {
				//expired
				message.reply([status: error_denied, message: 'Session expired, session: ' + sessionID])
				return
				//delete it now ? 
			}
			
			String userURI = session.loginURI.get()
			
			//don't refresh session timestamp
			if(bean.expirationProlongMargin <= 0 || timestamp == null || System.currentTimeMillis() - timestamp < bean.expirationProlongMargin) {
				onUserURIObtained(bean, message, sessionID, userURI)
				return
			}
			
			
 			//refresh the session timestamp
			session.timestamp = System.currentTimeMillis() 
				
			bean._save(sessionsSegment, [session]) { ResponseMessage saveSessionReply ->
				
				if (saveSessionReply.exceptionMessage) {
					message.reply([status: error_vital_service, message: 'VitalService error: ' + saveSessionReply.exceptionType + ' - ' + saveSessionReply.exceptionMessage])
					return
				}
				
				ResultList rl = saveSessionReply.response
				
				if(rl.status.status != VitalStatus.Status.ok) {
					message.reply([status: error_vital_service, message: 'VitalService error: ' + rl.status])
					return
				}
				
				onUserURIObtained(bean, message, sessionID, userURI)
				
			}
			
		}
		
	}
	
	protected void doAuthoriseVolatile(AuthAppBean bean, Message message, String sessionID) {
		
		String userURI = bean.sessions.get(sessionID);

		// In this basic auth manager we don't do any resource specific authorisation
		// The user is always authorised if they are logged in

		if( ! userURI ) {
			message.reply([status: error_denied, message: 'Session not found/expired, session: ' + userURI])
			return
		}
	
		onUserURIObtained(bean, message, sessionID, userURI)
			
	}
	
	protected void onUserURIObtained(AuthAppBean bean, Message message, String sessionID, String userURI) {
		
		
		//cache skipped
		GraphObject object = null//VitalSigns.get().getFromCache(userURI)
			
		if(object == null) {
			def onUserObjectClosure = { ResponseMessage getObjectMsg ->
				
//			bean._getRemoteObject(userURI) { ResponseMessage getObjectMsg ->
				
				if (getObjectMsg.exceptionMessage) {
					message.reply([status: error_vital_service, message: 'VitalService error: ' + getObjectMsg.exceptionType + ' - ' + getObjectMsg.exceptionMessage])
					return
				}
			
				ResultList rl = getObjectMsg.response

				if(rl.status.status != VitalStatus.Status.ok) {
					message.reply([status: error_vital_service, message: 'VitalService error: ' + rl.status])
					return
				}
				
				object = rl.first()
				
				if(object == null) {
					logout(bean, message, sessionID) { ->
						
					}
					message.reply([status: error_login_no_longer_exists, message: 'Login object no longer exists, URI: ' + userURI])
					return
				}					
				
				if(object instanceof CredentialsLogin) {
					CredentialsLogin l = object
					if( l.emailVerified == null || l.emailVerified.booleanValue() == false ) {
						message.reply([status: error_email_unverified, message: (String) "The email ${l.username} is not verified, cannot log in"])
						return
					}
				}
				
//				VitalSigns.get().addToCache(object)
				
				message.reply([status: 'ok', sessionID: sessionID, object: VitalServiceJSONMapper.toJSON(object)])
				
			}
			
			
			if(bean.mockedLogin) {
				
				ResultList rl = new ResultList()
	
				CredentialsLogin guestLogin = bean.getGuestLogin()
				
				if(userURI == guestLogin.URI) {
					rl.addResult(guestLogin)
				}
	
				ResponseMessage rm = new ResponseMessage()
				rm.setResponse(rl)
	
				onUserObjectClosure(rm)
				
			} else {
			
				bean._getRemoteObject(userURI, onUserObjectClosure)
			
			}
			
			return
		}
		
		message.reply([status: 'ok', sessionID: sessionID, object: VitalServiceJSONMapper.toJSON(object)])
		
  }

}
