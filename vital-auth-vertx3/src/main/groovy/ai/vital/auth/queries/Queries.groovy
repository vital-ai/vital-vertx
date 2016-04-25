package ai.vital.auth.queries


import ai.vital.domain.CredentialsLogin;
import ai.vital.domain.CredentialsLogin_PropertiesHelper;
import ai.vital.domain.UserSession;
import ai.vital.domain.UserSession_PropertiesHelper;
import ai.vital.query.querybuilder.VitalBuilder;
import ai.vital.vitalservice.query.VitalSelectQuery
import ai.vital.vitalservice.query.VitalSortProperty;
import ai.vital.vitalsigns.model.VitalSegment
import ai.vital.vitalsigns.model.properties.Property_hasUsername;
import ai.vital.vitalsigns.model.property.URIProperty;

class Queries {

	static VitalBuilder builder = new VitalBuilder()
	
	static VitalSelectQuery selectTypedLoginQuery(Class clz, VitalSegment segment, String username) {

		
		VitalSelectQuery query = builder.query {
			
			
			SELECT {
				
				value offset: 0
				
				value limit: 10
				
				value segments: [segment]
			
				AND {
					
					node_constraint { clz }
					
					node_constraint { ((CredentialsLogin_PropertiesHelper)CredentialsLogin.props()).username.equalTo(username) }
					
				}
					
			}
			
		}.toQuery()
				
		return query
		
	}

	static VitalSelectQuery selectTypedLogins(Class, clz, VitalSegment segment, Integer offset, Integer limit) {
		
		VitalSelectQuery query = builder.query {
			
			SELECT {
				
				value offset: offset
				value limit: limit
				
				value segments: [segment]

				value sortProperties: [ VitalSortProperty.get(Property_hasUsername.class, false) ]
								
				node_constraint { clz }
				
			}
			
		}.toQuery()
		
		return query
		
	}
	
	static VitalSelectQuery getSessions(VitalSegment sessionSegment, String loginURI, int limit) {
		
		VitalSelectQuery query = builder.query {
			
			SELECT {
				
				value limit: limit
				
				value offset: 0
				
				value segments : [sessionSegment]
				
				AND {
					
					node_constraint { UserSession.class }
					
					node_constraint { ((UserSession_PropertiesHelper)UserSession.props()).loginURI.equalTo(URIProperty.withString(loginURI)) }
					
				}
				
			}
			
		}.toQuery()
		
		return query
		
	}
	
	static VitalSelectQuery getSession(VitalSegment sessionsSegment, String sessionID) {
		
		VitalSelectQuery query = builder.query {
			
			SELECT {
				
				value offset: 0
				
				value limit: 0
				
				value segments: [sessionsSegment]
				
				node_constraint { UserSession.class }

				node_constraint { ((UserSession_PropertiesHelper)UserSession.props()).sessionID.equalTo(sessionID) }
								
			}
			
		}.toQuery()
		
		return query
		
	}

}
