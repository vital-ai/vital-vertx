package ai.vital.service.vertx.json

import ai.vital.domain.Document
import ai.vital.domain.Edge_hasEntity;
import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalservice.model.App;
import ai.vital.vitalservice.model.Organization
import ai.vital.vitalservice.query.ResultElement;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalGraphQuery;
import ai.vital.vitalservice.query.VitalPathQuery;
import ai.vital.vitalservice.query.VitalQuery;
import ai.vital.vitalservice.query.VitalSelectQuery;
import ai.vital.vitalservice.query.VitalSortProperty;
import ai.vital.vitalservice.segment.VitalSegment;
import ai.vital.vitalsigns.meta.GraphContext
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.property.URIProperty;

import java.util.Map.Entry

import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.json.JSONSerializer

class VitalServiceJSONMapper {

	public static Object toJSON(Object o) {

		if(o == null) return null

		if(o instanceof GraphObject) {
			return JSONSerializer.toJSONMap(o)
		} else if(o instanceof VitalStatus) {
			VitalStatus s = o 
			return [type: VitalStatus.class.simpleName, status: s.status.name(), message: s.message]
		} else if(o instanceof List) {
			List a = []
			for(Object x : o) {
				a.add(toJSON(x))
			}
			return a
		} else if(o instanceof URIProperty) {
			return [type: URIProperty.class.simpleName, uri: ((URIProperty)o).URI]
		} else if(o instanceof GraphContext) {
			return [type: GraphContext.class.simpleName, value: ((GraphContext)o).toString()];
		} else if(o instanceof VitalSegment) {
			//downgrade it ?
			return [type: 'VitalSegment', ID: o.ID]
		} else if(o instanceof ResultList) {
			ResultList rs = o
			Map m = [type: ResultList.class.simpleName, totalResults: rs.totalResults, limit: rs.limit, offset: rs.offset]
			List a = []
			for(ResultElement r : rs.results) {
				Map rm = [type: ResultElement.class.simpleName, score: r.score, graphObject: JSONSerializer.toJSONMap(r.graphObject)]
				a.add(rm)
			}
			m.put("status", toJSON(rs.status))
			m.put("results", a)
			return m
		} else if(o instanceof App) {
			App app = o
			return [type: App.class.simpleName, ID: app.ID, appName: app.appName, organizationID: app.organizationID]
		} else if(o instanceof Organization) {
			Organization org = o
			return [type: Organization.class.simpleName, ID: org.ID, organizationName: org.organizationName]
		} else if(o instanceof Class) {
			return [type: 'class', 'value': ((Class)o).canonicalName]
			
		} else if(o instanceof VitalQuery) {
		
			throw new RuntimeException("VitalQuery json not imeplemented")
		
			/* TODO finish it
			VitalQuery vq = o
			
			Map jo = [
				type: vq.class.canonicalName,
				returnSparqlString: vq.returnSparqlString,
				segments: toJSON(vq.segments)
			]
			
			if(vq instanceof VitalGraphQuery) {
				
				VitalGraphQuery vgq = vq

				jo.putAll([
					limit: vgq.limit,
					offset: vgq.offset,
					payloads: vgq.payloads,
					sortProperties: toJSON(vgq.sortProperties),
					topContainer: toJSON(vgq.topContainer)
				])
								
			} else if(vq instanceof VitalPathQuery) {
			
				VitalPathQuery vpq = vq
				
				jo.putAll([
					arcs: toJSON(vpq.arcs),
					maxdepth: vpq.maxdepth,
					rootArc: toJSON(vpq.rootArc),
					rootURIs: toJSON(vpq.rootURIs)
				])
			
			} else if(vq instanceof VitalSelectQuery) {
			
				VitalSelectQuery vsq = vq
				
				jo.putAll([
					distinct: vsq.distinct,
					distinctExpandProperty: vsq.distinctExpandProperty,
					getDistinctExpandProperty()
					getDistinctFirst()
					getDistinctLast()
					getDistinctSort()
					getLimit()
					getOffset()
					getProjectionOnly()
					getPropertyURI()
					getSortProperties()
					getTopContainer()
					isDistinct()
					isDistinctExpandProperty()
					isDistinctFirst()
					isDistinctLast()
					isProjectionOnly()
					:
				])
				
			
			}
			
		
			
			
			return jo
			*/
		/*	
		} else if(o instanceof VitalQueryContainer) {
			VitalQueryContainer vqc = o
			Map jo = [
				type: VitalQueryContainer.class.simpleName, 
				components: toJSON(vqc.components),
				containerType: vqc.type.toString()
			]
			return jo
		} else if(o instanceof VitalSortProperty) {
			VitalSortProperty vsp = o
			Map jo = [type: VitalSortProperty.class.simpleName, 'propertyURI': vsp.propertyURI, reverse: vsp.reverse]
			return jo
		} else if(o instanceof VitalTypeConstraint) {

			VitalTypeConstraint vtc = o
			//			vtc.comparator

			Class cls = VitalSigns.get().getGroovyClass(((URIPropertyValue)vtc.getValue()).URI)

			Map jo = [type: VitalTypeConstraint.class.simpleName, 'class': cls.canonicalName]
			return jo
		} else if(o instanceof VitalPropertyConstraint) {

			VitalPropertyConstraint vpc = o
			Map jo = [type: VitalPropertyConstraint.class.simpleName, propertyURI: vpc.propertyURI, value: toJSON(vpc.value), comparator: vpc.comparator.toString(), negative: vpc.negative]
			return jo
		*/
		} else {
			//			throw new RuntimeException("Unexpected object to serialize: ${o}")
			return o
		}
	}

	public static Object fromJSON(Object o) {

		if(o instanceof Map) {

			Map jo = o
			String type = jo.get("type")

			if(type == null) {
				Map m = [:]
				for(Entry e : jo.entrySet()) {
					m.put(e.key, fromJSON(e.value))
				}
				return m
			} else if(type == App.class.getSimpleName()) {
				App app = new App();
				app.appName = jo.get("appName")
				app.organizationID = jo.get("organizationID")
				app.ID = jo.get("ID")
				return app
			} else if(type == Organization.class.getSimpleName()) {
				Organization org = new Organization();
				org.organizationName = jo.get('organizationName')
				org.ID = jo.get('ID')
				return org
			} else if(type == URIProperty.class.getSimpleName()) {
				return URIProperty.withString(jo.get("uri"))
			} else if(type == GraphContext.class.getSimpleName()) {
				GraphContext gc = GraphContext.valueOf(jo.get("value"))
				return gc
			} else if(type == VitalSegment.class.simpleName) {
				VitalSegment segment = new VitalSegment()
				segment.ID = jo.get("ID")
				return segment
			} else if(type == 'class') {
				return Class.forName(jo.get('value'))
				/* TODO convert queries
			} else if(type == VitalSelectQuery.class.simpleName) {
				VitalSelectQuery sq = new VitalSelectQuery()
				sq.components = fromJSON(jo.get("components"))
				sq.limit = jo.get("limit")
				sq.offset = jo.get("offset")
				sq.projectionOnly = jo.get("projectionOnly")
				if(jo.get("returnSparqlString") != null) sq.returnSparqlString = jo.get("returnSparqlString")
				sq.segments = fromJSON(jo.get("segments"))
				sq.sortProperties = fromJSON(jo.get("sortProperties"))
				sq.type = Type.valueOf(jo.get("containerType"))
				return sq
			} else if(type == VitalGraphQuery.class.simpleName) {
				VitalGraphQuery vgq = new VitalGraphQuery()
				vgq.pathsElements = fromJSON(jo.get("pathsElements"))
				if(jo.get("returnSparqlString") != null) vgq.returnSparqlString = jo.get("returnSparqlString")
				vgq.rootQuery = fromJSON(jo.get("rootQuery"))
				vgq.segments = fromJSON(jo.get("segments"))
				return vgq
			} else if(type == VitalPathQuery.class.simpleName) {
				VitalPathQuery vpq = new VitalPathQuery()
				vpq.pathsElements = fromJSON(jo.get("pathsElements"))
				if(jo.get("returnSparqlString") != null) vpq.returnSparqlString = jo.get("returnSparqlString")
				vpq.rootUris = fromJSON(jo.get("rootUris"))
				vpq.segments = fromJSON(jo.get("segments"))
				return vpq
			} else if(type == VitalSortProperty.class.simpleName) {
				Boolean reverse = jo.get("reverse")
				if(jo.get("class") != null) {
					Class cls = Class.forName(jo.get("class"))
					String name = jo.get("name")
					VitalSortProperty vsp = new VitalSortProperty(cls, name , reverse)
					return vsp
				} else {
					VitalSortProperty vsp = new VitalSortProperty(jo.get("propertyURI"), reverse)
					return vsp
				}
			} else if(type == VitalQueryContainer.class.simpleName) {
				VitalQueryContainer container = new VitalQueryContainer()
				container.components = fromJSON(jo.get("components"))
				container.type = Type.valueOf(jo.get("containerType"))
				return container;
			} else if(type == VitalPropertyConstraint.class.simpleName) {

				Comparator c = Comparator.valueOf(jo.get("comparator"))
				Object val = fromJSON(jo.get("value"))
				Boolean negative = jo.get("negative");

				if(jo.get("class")) {

					Class cls = Class.forName(jo.get("class"))
					String name = jo.get("name")

					VitalPropertyConstraint vpc = new VitalPropertyConstraint(cls, name, val, c, negative)
					return vpc
				} else {

					String propertyURI = jo.get("propertyURI")

					VitalPropertyConstraint vpc = new VitalPropertyConstraint(propertyURI, val, c, negative)
					return vpc
				}
			} else if(type == VitalTypeConstraint.class.simpleName) {

				Class cls = Class.forName(jo.get("class"))

				VitalTypeConstraint vtc = new VitalTypeConstraint(cls)

				return vtc
				*/
			} else if( type == ResultList.class.simpleName) {
			
				ResultList rs = new ResultList()
				
				rs.limit = jo.get("limit")
				rs.offset = jo.get("offset")
				rs.totalResults = jo.get("totalResults")
				rs.results = fromJSON(jo.get("results"))
				rs.status = fromJSON(jo.get("status"))
				return rs
			} else if( type == ResultElement.class.simpleName ) {
				ResultElement re = new ResultElement(fromJSON(jo.get("graphObject")), jo.get("score"))
				return re
			} else if( type == VitalStatus.class.simpleName ) {
				VitalStatus.Status status = null
				if(jo.get('status') != null) {
					status = VitalStatus.Status.valueOf(jo.get('status'))
				}
				VitalStatus s = new VitalStatus(status, jo.get('message'))
				return s
			} else {
				return ai.vital.vitalsigns.json.JSONSerializer.fromJSONMap(jo)
			}
		} else if(o instanceof List) {
			List array = o
			List l = []
			for(int j = 0; j < array.size(); j++) {
				Object ax = fromJSON(array.get(j))
				l.add(ax)
			}
			return l
		} else {
			//assuming basic type
			return o
		}
	}
	
}
