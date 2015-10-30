package ai.vital.auth.mod

import java.util.HashMap;
import java.util.Map;

import org.githubusercontent.defuse.passwordhash.PasswordHash;
import org.vertx.groovy.core.eventbus.EventBus
import org.vertx.groovy.platform.Verticle
import org.vertx.groovy.core.eventbus.Message;
import org.vertx.java.core.Future;

import ai.vital.auth.queries.Queries;
import ai.vital.domain.CredentialsLogin;
import ai.vital.domain.Login;
import ai.vital.domain.AdminLogin
import ai.vital.domain.SuperAdminLogin
import ai.vital.domain.UserSession;
import ai.vital.service.vertx.VitalServiceMod;
import ai.vital.service.vertx.async.VitalServiceAsyncClient;
import ai.vital.service.vertx.binary.ResponseMessage;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.json.VitalServiceJSONMapper;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalSelectQuery
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.meta.GraphContext;
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VitalSegment
import ai.vital.vitalsigns.model.property.URIProperty;



class VitalAuthManager extends Verticle {

	public final static String error_no_username = 'error_no_username' 
	
	public final static String error_no_password = 'error_no_password'
	
	public final static String error_no_type = 'error_no_type'
	
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
	 
	
	
	public final static String address_login     = "vitalauth.login"
	
	public final static String address_logout    = "vitalauth.logout"
	
	public final static String address_authorise = "vitalauth.authorise"
	

	//keep each map separate ?	
	// sessionID -> login URI
	protected Map<String, String> sessions = null
	// login URI 2 active logins
	protected Map<String, List<LoginInfo>> logins = null

	protected static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000;
	
	protected static final long DEFAULT_ADMIN_SESSION_TIMEOUT = 0;
	
	protected static final long DEFAULT_SUPER_ADMIN_SESSION_TIMEOUT = 0;

	protected long sessionTimeout;
	
	protected long adminSessionTimeout;
	
	protected long superAdminSessionTimeout;

	protected VitalSegment loginsSegment = null
	
	protected VitalSegment adminLoginsSegment = null
	
	protected VitalSegment superAdminLoginsSegment = null
  
	protected int maxSessionsPerUser = 1 
	
	protected Boolean persistentSessions = null
	
	protected Integer expirationProlongMargin = null
	
	protected VitalServiceAsyncClient vitalService = null
	
	private static final class LoginInfo {
		final long timerID;
		final String sessionID;
		final long expirationDate;

		private LoginInfo(long timerID, String sessionID, long expDate) {
			this.timerID = timerID;
			this.sessionID = sessionID;
			this.expirationDate = expDate
		}
		
	}

	/**
	 * Start the busmod
	 */
	public Object start(Future<Void> startedResult) {

		doStart()

		persistentSessions = container.getConfig().get('persistentSessions')
		container.logger.info "Persistent sessions: ${persistentSessions}"
		if(persistentSessions == null) {
			throw new RuntimeException("No persistentSessions param.")	
		}
		
		
		Integer maxSessionsPerUserParam = container.getConfig().get("maxSessionsPerUser")
		
		if(maxSessionsPerUserParam != null) {
			maxSessionsPerUser = maxSessionsPerUserParam.intValue() 
		}
		
		container.logger.info("Max sessions per user: ${maxSessionsPerUser}")
		if(!persistentSessions) {
		
			sessions = new HashMap<>();
			
			logins = new HashMap<>();
				
		}
		
				
		Object timeout = container.getConfig().get('session_timeout')

		if (timeout != null) {
			if (timeout instanceof Long) {
				this.sessionTimeout = (Long)timeout;
			} else if (timeout instanceof Integer) {
				this.sessionTimeout = (Integer)timeout;
			}
		} else {
			this.sessionTimeout = DEFAULT_SESSION_TIMEOUT;
		}

		println "Session Timeout: ${this.sessionTimeout}"
		
		
		Object adminTimeout = container.getConfig().get('admin_session_timeout')
		if(adminTimeout != null) {
			if(adminTimeout instanceof Long) {
				this.adminSessionTimeout = (Long) adminTimeout;
			} else if(timeout instanceof Integer) {
				this.adminSessionTimeout = ((Integer)adminTimeout).longValue()
			}
		} else {
			this.adminSessionTimeout = DEFAULT_ADMIN_SESSION_TIMEOUT
		}
		
		println "Admin Session Timeout: ${this.adminSessionTimeout}"

		
		Object superAdminTimeout = container.getConfig().get('super_admin_session_timeout')
		if(superAdminTimeout != null) {
			if(superAdminTimeout instanceof Long) {
				this.superAdminSessionTimeout = (Long) superAdminTimeout;
			} else if(timeout instanceof Integer) {
				this.superAdminSessionTimeout = ((Integer)superAdminTimeout).longValue()
			}
		} else {
			this.superAdminSessionTimeout = DEFAULT_SUPER_ADMIN_SESSION_TIMEOUT
		}
		
		println "Super Admin Session Timeout: ${this.superAdminSessionTimeout}"
		
		
		expirationProlongMargin = container.getConfig().get('expirationProlongMargin')
		if(expirationProlongMargin == null) {
			throw new RuntimeException("No expirationProlongMargin param")
		}		
		
		if(timeout > 0 && expirationProlongMargin >= timeout) {
			throw new RuntimeException("expiration prolong margin cannot be greater than timeout")
		}
		
		initService() { Throwable exception ->
			
			if(exception != null) { 
				startedResult.setFailure(exception)
				return
			}
			
			checkSegments(startedResult) { ->
			
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
			
			}
			
		}
		
		
		
	}
	
	
	/*
	 * callback(Throwable t) 
	 */
	protected void initService(Closure callback) {
		
		vitalService = new VitalServiceAsyncClient(vertx)
		
		callback(null)
			
	}
	
	protected void doStart() {
		
		 String loginsSegmentString = container.getConfig().get("loginsSegment")
		 if(loginsSegmentString) {
			 loginsSegment = VitalSegment.withId(loginsSegmentString)
			 container.logger.info("loginsSegment: ${loginsSegmentString}")
		 } else {
			 container.logger.info("No 'loginsSegment' param - regular logins disabled")
		 }
		 
		 String adminLoginsSegmentString = container.getConfig().get("adminLoginsSegment")
		 if(adminLoginsSegmentString) {
			 adminLoginsSegment = VitalSegment.withId(adminLoginsSegmentString)
			 container.logger.info("adminLoginsSegment: ${adminLoginsSegmentString}")
		 } else {
			 container.logger.info("No 'adminLoginsSegment' param - admin logins disabled")
		 }
		 
		 String superAdminLoginsSegmentString = container.getConfig().get("superAdminLoginsSegment")
		 if(superAdminLoginsSegmentString) {
			 superAdminLoginsSegment = VitalSegment.withId(superAdminLoginsSegmentString)
			 container.logger.info("superAdminLoginsSegment: ${superAdminLoginsSegmentString}")
		 } else {
			 container.logger.info("No 'superAdminLoginsSegment' param - super admin logins disabled")
		 }
 
		 if(loginsSegment == null && adminLoginsSegment == null && superAdminLoginsSegment == null) {
			 throw new RuntimeException("At least one login type segment must be defined.")
		 }
		 
	}
	
	protected void checkSegments(Future<Void> future, Closure onSegmentsChecked) {
		
		if(loginsSegment != null) {
			
			String segmentID = loginsSegment.segmentID.toString()
			
			getSegment(segmentID) { ResponseMessage rm ->
				
				if(rm.exceptionMessage) {
					future.setFailure(new RuntimeException("Error when checking logins segment: " + rm.exceptionType + " - " + rm.exceptionMessage))
					return
				}
				
				loginsSegment = rm.response
		
				if(loginsSegment == null) {
					future.setFailure(new RuntimeException("Logins segment not found: " + segmentID))
					return
				}
				
				onLoginSegmentChecked(future, onSegmentsChecked)		
			}
			
		} else {
			onLoginSegmentChecked(future, onSegmentsChecked)
		}
		
	}
	
	protected void onLoginSegmentChecked(Future<Void> future, Closure onSegmentsChecked) {
		
		if(adminLoginsSegment != null) {
			
			String segmentID = adminLoginsSegment.segmentID.toString()
			
			getSegment(segmentID) { ResponseMessage rm ->
				
				if(rm.exceptionMessage) {
					future.setFailure(new RuntimeException("Error when checking admin logins segment: " + rm.exceptionType + " - " + rm.exceptionMessage))
					return
				}
				
				adminLoginsSegment = rm.response
				
				if(adminLoginsSegment == null) {
					future.setFailure(new RuntimeException("Admin logins segment not found: " + segmentID))
					return
				}
				
				onAdminLoginSegmentChecked(future, onSegmentsChecked)
				
			}
			
		} else {
		
			onAdminLoginSegmentChecked(future, onSegmentsChecked)
		
		}
		
	}
	
	protected void onAdminLoginSegmentChecked(Future<Void> future, Closure onSegmentsChecked) {
	
		if(superAdminLoginsSegment != null) {
			
			String segmentID = superAdminLoginsSegment.segmentID.toString()
			
			getSegment(segmentID) { ResponseMessage rm ->
				
				if(rm.exceptionMessage) {
					future.setFailure(new RuntimeException("Error when checking super admin logins segment: " + rm.exceptionType + " - " + rm.exceptionMessage))
					return
				}
				
				superAdminLoginsSegment = rm.response
				
				if(superAdminLoginsSegment == null) {
					future.setFailure(new RuntimeException("Super admin logins segment not found: " + segmentID))
					return
				}

				onSegmentsChecked();
								
			} 
			
		} else {
			onSegmentsChecked();
		}
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
	
	protected VitalSegment getLoginsSegment(Message message) {
		
		Map body = message.body()
		
		String type = body.get('type');
		
		if(type == Login.class.simpleName) {
			
			return loginsSegment
			
		} else if(type == AdminLogin.class.simpleName) {
		
			return adminLoginsSegment
		
		} else if(type == SuperAdminLogin.class.simpleName) {

			return superAdminLoginsSegment		
		
		} else {
			message.reply([status: error_unknown_type, message: 'Unknown type: ' + type])
			return
		}
		
	}
	
	protected VitalSegment getSessionsSegment(Message message) {
		return getLoginsSegment(message)
	}
	
	
	protected boolean validateOtherFields(Message message) {
		return true
	}
	
	protected void getSegment(String segmentID, Closure closure) {
		
		vitalService.getSegment(segmentID, closure)
		
	}
	
	protected void executeSelectQuery(VitalSelectQuery selectQuery, Closure closure) {
	
		vitalService.query(selectQuery, closure)
			
	}
	
	protected void generateURI(Class<? extends GraphObject> clazz, Closure closure) {
		
		vitalService.generateURI(clazz, closure)
		
	}
	
	protected void save(VitalSegment targetSegment, List<GraphObject> objects, Closure closure) {
		
		vitalService.save(targetSegment, objects, true, closure)
		
	}
	
	protected delete(List<URIProperty> uris, Closure closure) {
		
		vitalService.delete(uris, closure)
		
	}
	
	protected getRemoteObject(String uri, Closure closure) {
		
		vitalService.get(GraphContext.ServiceWide, URIProperty.withString(uri), true, closure) 
		
	}
	
	protected void doLogin(Message message) {

		Map body = message.body()

		String type = body.get('type')
		
		if(!type) {
			message.reply([status: error_no_type, message: 'No type parameter'])
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
		
		Class cls = null
		
		
		if(type == Login.class.simpleName) {
			
			cls = Login.class
			
		} else if(type == AdminLogin.class.simpleName) {
		
			cls = AdminLogin.class
		
		} else if(type == SuperAdminLogin.class.simpleName) {
		
			cls = SuperAdminLogin.class
		
		} else {
			message.reply([status: error_unknown_type, message: 'Unknown type: ' + type])
			return
		}
		
		if(!validateOtherFields(message)) return;
		
		VitalSegment loginsSegment = getLoginsSegment(message)
		if(loginsSegment == null) return
		
		VitalSegment sessionsSegment = getSessionsSegment(message)
		if(sessionsSegment == null) return
		
//		if(segment == null) {
//			message.reply([status: error_unsupported_type, message: (String) "login type: ${type} unsupported"])
//			return
//		}

		
		final long _timeoutValue = getTimeoutValue(type)
		
		VitalSelectQuery selectQuery = Queries.selectTypedLoginQuery(cls, loginsSegment, username)

		executeSelectQuery(selectQuery) { ResponseMessage selectQueryReply ->

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
				if( l.emailVerified == null || l.emailVerified == false ) {
					message.reply([status: error_email_unverified, message: (String) "The email ${username} is not verified, cannot log in"])
					return
				}
				if( l.active != null && l.active == false ) {
					message.reply([status: error_login_inactive, message: (String) "The login is no longer active"])
					return
				}
			}
			
			if(persistentSessions) {
				
				def onSessionsCountChecked = {
					
					
					generateURI(UserSession.class) { ResponseMessage generateReply ->

						if (generateReply.exceptionMessage) {
							message.reply([status: error_vital_service, message: 'VitalService error: ' + generateReply.exceptionType + ' - ' + generateReply.exceptionMessage])
							return
						}
						
						URIProperty newURI = generateReply.response
						
						UserSession session = new UserSession()
						session.URI = newURI.get()
						
						session.timestamp = System.currentTimeMillis()
						session.sessionType = type
						if(_timeoutValue > 0) {
							session.expirationDate = new Date(System.currentTimeMillis() + _timeoutValue) 
						}
						
						session.loginURI = new URIProperty(login.URI)
						session.sessionID = type + '_' + UUID.randomUUID().toString()
						
						save(sessionsSegment, [session]) { ResponseMessage saveSessionReply ->
							
							if (saveSessionReply.exceptionMessage) {
								message.reply([status: error_vital_service, message: 'VitalService error: ' + saveSessionReply.exceptionType + ' - ' + saveSessionReply.exceptionMessage])
								return
							}
								
							message.reply([status: 'ok', message: 'logged in', sessionID: session.sessionID.toString(), object: VitalServiceJSONMapper.toJSON(login)])
								
						}

						
					}
					
					
					
				};
				
				//if max session per user reached select existing sessions to see if not exceeded
				if(maxSessionsPerUser > 0) {
					
					VitalSelectQuery currentSessionsQuery = Queries.getSessions(sessionsSegment, login.URI, maxSessionsPerUser * 10)

					executeSelectQuery(currentSessionsQuery) { ResponseMessage currentSessionsReply ->
						
						if (currentSessionsReply.exceptionMessage) {
							message.reply([status: error_vital_service, message: 'VitalService error: ' + currentSessionsReply.exceptionType + ' - ' + currentSessionsReply.exceptionMessage])
							return
						}
						
						ResultList sessionsRL = currentSessionsReply.response
						
						if(sessionsRL.status.status != VitalStatus.Status.ok) {
							message.reply([status: error_vital_service, message: 'VitalService error: ' + sessionsRL.status])
							return
						}

						if(sessionsRL.results.size() >= maxSessionsPerUser) {
							
							List<UserSession> sessions = []
							for(GraphObject g : sessionsRL) {
								sessions.add(g)
							}
							
							sessions.sort { UserSession s1, UserSession s2 ->
								
								//expiration date is more important
								Date d1 = s1.expirationDate
								Date d2 = s2.expirationDate
								
								if(d1 == null) d1 = s1.timestamp != null ? new Date(s1.timestamp) : new Date(0L)
								if(d2 == null) d2 = s2.timestamp != null ? new Date(s2.timestamp) : new Date(0L)
								
								return d2.compareTo(d1)
								
							}
							
							List<URIProperty> sessionsToRemove = []
							
							for(int i = maxSessionsPerUser-1; i < sessions.size(); i++) {
								sessionsToRemove.add(URIProperty.withString(sessions.get(i).URI))
							}
							
							//delete old sessions

							delete(sessionsToRemove) { ResponseMessage deleteSessionsReply ->
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
			
				List<LoginInfo> info = logins.get(login.URI)
				if(info != null) {
					if(maxSessionsPerUser > 0 && info.size() > maxSessionsPerUser - 1) {
								
						//sort the list 
						List<LoginInfo> infos = new ArrayList<LoginInfo>(info)
						infos.sort { LoginInfo l1, LoginInfo l2 ->
							return l1.expirationDate.compareTo(l2.expirationDate)	
						}
								
						while(infos.size() > maxSessionsPerUser - 1) {
							//logout any
							LoginInfo expInfo = infos.remove(0)
							logout(message, expInfo.sessionID) { boolean loggedOut->
								
							}
						}
					}
				}
				
				String sessionID = UUID.randomUUID().toString()
				
				Long timerID = null
				
				Long expDate = null
				
				if(_timeoutValue > 0) {
					
					expDate = System.currentTimeMillis() + _timeoutValue
							
					timerID = vertx.setTimer(_timeoutValue) { Long _timerID -> 
						logout(message, sessionID) { boolean loggedOut->
						}
					}
				}
				
				
				//session expires after some time	
				
				//password is hashed, compare hashes
				
				sessions.put(sessionID, login.URI);
				
				//refresh the list
				info = logins.get(login.URI)
				if(info == null) {
					info = Collections.synchronizedList(new ArrayList<LoginInfo>())
					logins.put(login.URI, info);
				} 
				
				info.add(new LoginInfo(timerID, sessionID, expDate))
				
				message.reply([status: 'ok', message: 'logged in', sessionID: sessionID, object: VitalServiceJSONMapper.toJSON(login)])
				
			}
			
		}
	}

	protected void doLogout(Message message) {
		
		Map body = message.body
		
		String sessionID = body.get("sessionID");
		if(!sessionID) {
			message.reply([status: error_no_sessionid_param, message: 'no sessionID param'])
			return
		}
		
		
		if( ! validateOtherFields(message) ) return
		
		VitalSegment sessionsSegment = getSessionsSegment(message)
		if(sessionsSegment == null) return
		
		if (sessionID != null) {
			logout(message, sessionID) { boolean loggedOut ->
				if(loggedOut) {
					message.reply([status: 'ok', message: 'Logged out'])
				} else {
					message.reply([status: error_not_logged_in, message: 'Not logged in'])
				}
			}
		} else {
		}
	}

	protected void logout(Message message, String sessionID, Closure callback) {
		
		if(persistentSessions) {
			
			int uscore = sessionID.indexOf('_')
			
			if(uscore <= 0) {
				callback(false)
				return
			}
			
			String type = sessionID.substring(0, uscore);
			
			Map b = message.body
			if(b.type == null) b.type = type
			
			VitalSegment segment = getSessionsSegment(message)
			if(segment == null) return null
			
			if(type == Login.class.simpleName) {
				
			} else if(type == AdminLogin.class.simpleName) {
				
			} else if(type == SuperAdminLogin.class.simpleName) {
				
			} else {
				container.logger.error("invalid session type: ${type}")
				callback(false)
				return
			}
			
			VitalSelectQuery currentSessionQuery = Queries.getSession(segment, sessionID)

			executeSelectQuery(currentSessionQuery) { ResponseMessage currentSessionsReply ->
				
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
					
					delete([sessionToRemove]) { ResponseMessage deleteSessionsReply ->
						
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
		
			String loginURI = sessions.remove(sessionID);
			if(loginURI == null) {
				callback(false)
				return
			}
			
			List<LoginInfo> infoList = logins.get(loginURI);
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
				logins.remove(loginURI)
			}
			
			callback(true);
			
		}
		
	}

	protected void doAuthorise(Message message) {
		
		String sessionID = ((Map)message.body).get("sessionID");
		
		if (sessionID == null || sessionID.length() == 0) {
			message.reply([status: error_no_sessionid_param, message: 'No session ID parameter'])
			return;
		}
		
		if(persistentSessions) {
			
			doAuthorisePersistent(message, sessionID)
			
		} else {
		
			doAuthoriseVolatile(message, sessionID)
			
		}
		
	}
	
	protected void doAuthorisePersistent(Message message, String sessionID) {
		
		
		int uscore = sessionID.indexOf('_')
		
		if(uscore <= 0) {
			message.reply([status: error_invalid_session_string, message: (String)"invalid session string: ${sessionID}"])
			return
		}
		
		String type = sessionID.substring(0, uscore)
		
		message.body().put('type', type)
		
		if(!validateOtherFields(message)) return
		
		VitalSegment sessionsSegment = getSessionsSegment(message)
		if(sessionsSegment == null) return
		
		VitalSelectQuery currentSessionsQuery = Queries.getSession(sessionsSegment, sessionID)

		executeSelectQuery(currentSessionsQuery) { ResponseMessage currentSessionsReply ->
			
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
			
			Date expDate = session.expirationDate?.getDate()
			if(expDate != null && expDate.time < System.currentTimeMillis()) {
				//expired
				message.reply([status: error_denied, message: 'Session expired, session: ' + sessionID])
				return
				//delete it now ? 
				
			}
			
			String userURI = session.loginURI.get()
			
			if(expDate == null || expirationProlongMargin <= 0) {
				onUserURIObtained(message, sessionID, userURI)
				return
			}
			
			
			//check if expDate needs to be 
			
			long _timeoutValue = getTimeoutValue(session.sessionType?.toString())
			
			//assumed to be in the past
			long lastUpdateTimestamp = expDate.time - _timeoutValue
			
			long ctime = System.currentTimeMillis() 
			
			if( _timeoutValue <= 0 || ( ctime - lastUpdateTimestamp > expirationProlongMargin) ) {
				
				//refresh the session
				session.expirationDate = _timeoutValue > 0 ? new Date( ctime + _timeoutValue ) : null 
				
				save(sessionsSegment, [session]) { ResponseMessage saveSessionReply ->
					
					if (saveSessionReply.exceptionMessage) {
						message.reply([status: error_vital_service, message: 'VitalService error: ' + saveSessionReply.exceptionType + ' - ' + saveSessionReply.exceptionMessage])
						return
					}
					
					ResultList rl = saveSessionReply.response
					
					if(rl.status.status != VitalStatus.Status.ok) {
						message.reply([status: error_vital_service, message: 'VitalService error: ' + rl.status])
						return
					}
					
					onUserURIObtained(message, sessionID, userURI)
					
				}
				
			} else {
			
				onUserURIObtained(message, sessionID, userURI)
				
			}
			
		}
		
	}
	
	protected void doAuthoriseVolatile(Message message, String sessionID) {
		
		String userURI = sessions.get(sessionID);

		// In this basic auth manager we don't do any resource specific authorisation
		// The user is always authorised if they are logged in

		if( ! userURI ) {
			message.reply([status: error_denied, message: 'Session not found/expired, session: ' + userURI])
			return
		}
	
		onUserURIObtained(message, sessionID, userURI)
			
	}
	
	protected void onUserURIObtained(Message message, String sessionID, String userURI) {
		
		
		GraphObject object = VitalSigns.get().getFromCache(userURI)
			
		if(object == null) {
			
			
			getRemoteObject(userURI) { ResponseMessage getObjectMsg ->
				
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
					logout(message, sessionID) { ->
						
					}
					message.reply([status: error_login_no_longer_exists, message: 'Login object no longer exists, URI: ' + userURI])
					return
				}					
				
				if(object instanceof CredentialsLogin) {
					CredentialsLogin l = object
					if( l.emailVerified == null || l.emailVerified == false ) {
						message.reply([status: error_email_unverified, message: (String) "The email ${l.username} is not verified, cannot log in"])
						return
					}
				}
				
				VitalSigns.get().addToCache(object)
				
				message.reply([status: 'ok', sessionID: sessionID, object: VitalServiceJSONMapper.toJSON(object)])
				
			}
			
			return
		}
		
		message.reply([status: 'ok', sessionID: sessionID, object: VitalServiceJSONMapper.toJSON(object)])
		
  }

  protected long getTimeoutValue(String type) {

	  long timeoutValue = 0
	  
	  if(type == Login.class.simpleName) {
		  
		  timeoutValue = this.sessionTimeout
		  
	  } else if(type == AdminLogin.class.simpleName) {
	  
		  timeoutValue = this.adminSessionTimeout
		  
	  } else if(type == SuperAdminLogin.class.simpleName) {
	  
		  timeoutValue = this.superAdminSessionTimeout
		  
	  } 

	  return timeoutValue
	  	  
  }
}
