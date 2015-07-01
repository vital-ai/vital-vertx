package ai.vital.service.vertx.json

import ai.vital.domain.Document
import ai.vital.domain.Edge_hasEntity;
import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalservice.factory.Factory;
import ai.vital.vitalservice.model.App;
import ai.vital.vitalservice.model.Customer;
import ai.vital.vitalservice.query.AbstractVitalGraphQuery
import ai.vital.vitalservice.query.AggregationType;
import ai.vital.vitalservice.query.QueryPathElement;
import ai.vital.vitalservice.query.ResultElement;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalGraphQuery;
import ai.vital.vitalservice.query.VitalPathQuery
import ai.vital.vitalservice.query.VitalProperty
import ai.vital.vitalservice.query.VitalPropertyConstraint;
import ai.vital.vitalservice.query.VitalQueryComponent;
import ai.vital.vitalservice.query.VitalSelectAggregationQuery;
import ai.vital.vitalservice.query.VitalSparqlQuery;
import ai.vital.vitalservice.query.VitalTypeConstraint;
import ai.vital.vitalservice.query.VitalPropertyConstraint.Comparator;
import ai.vital.vitalservice.query.VitalQueryContainer;
import ai.vital.vitalservice.query.VitalQueryContainer.Type;
import ai.vital.vitalservice.query.graph.VitalGraphQueryV2
import ai.vital.vitalservice.query.VitalSelectQuery;
import ai.vital.vitalservice.query.VitalSortProperty;
import ai.vital.vitalservice.query.QueryPathElement.CollectDestObjects;
import ai.vital.vitalservice.query.QueryPathElement.CollectEdges;
import ai.vital.vitalservice.query.QueryPathElement.Direction;
import ai.vital.vitalservice.segment.VitalSegment;
import ai.vital.vitalsigns.datatype.VitalURI
import ai.vital.vitalsigns.meta.GraphContext
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.URIPropertyValue

import java.util.Map.Entry

import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.json.JSONSerializer

class VitalServiceJSONMapper {

	public static Object toJSON(Object o) {

		if(o == null) return null

		if(o instanceof GraphObject) {
			return JSONSerializer.toJSONMap(o)
		} else if(o instanceof VitalStatus) {
			return null
		} else if(o instanceof List) {
			List a = []
			for(Object x : o) {
				a.add(toJSON(x))
			}
			return a
		} else if(o instanceof VitalURI) {
			return [type: VitalURI.class.simpleName, uri: ((VitalURI)o).URI]
		} else if(o instanceof java.util.Date) {
			return [type: java.util.Date.class.simpleName, time: ((java.util.Date)o).getTime()]
		} else if(o instanceof GraphContext) {
			return [type: GraphContext.class.simpleName, value: ((GraphContext)o).toString()];
		} else if(o instanceof URIPropertyValue) {
			URIPropertyValue upv = o
			return [type: URIPropertyValue.class.simpleName, uri: upv.URI]
		} else if(o instanceof VitalSegment) {
			//downgrade it ?
			return [type: 'VitalSegment', id: o.id]
		} else if(o instanceof ResultList) {
			ResultList rs = o
			Map m = [type: ResultList.class.simpleName, totalResults: rs.totalResults, limit: rs.limit, offset: rs.offset]
			List a = []
			for(ResultElement r : rs.results) {
				Map rm = [type: ResultElement.class.simpleName, score: r.score, graphObject: JSONSerializer.toJSONMap(r.graphObject)]
				a.add(rm)
			}
			m.put("results", a)
			return m
		} else if(o instanceof App) {
			App app = o
			return [type: App.class.simpleName, ID: app.ID, appName: app.appName, customerID: app.customerID]
		} else if(o instanceof Customer) {
			Customer c = o
			return [type: Customer.class.simpleName, ID: c.ID, customerName: c.customerName]
		} else if(o instanceof Class) {
			return [type: 'class', 'value': ((Class)o).canonicalName]
		} else if(o instanceof VitalSelectQuery) {
			VitalSelectQuery vsq = o
			Map jo = [type: VitalSelectQuery.class.simpleName,
				components: toJSON(vsq.components),
				limit: vsq.limit,
				offset: vsq.offset,
				projectionOnly: vsq.projectionOnly,
				segments: toJSON(vsq.segments),
				sortProperties: toJSON(vsq.sortProperties),
				containerType: vsq.type.toString()
			]
			
			if(o instanceof VitalSelectAggregationQuery){
				
				VitalSelectAggregationQuery vsaq = (VitalSelectAggregationQuery)o;
				
				jo['aggregationType'] = vsaq.aggregationType.name
				
				if(vsaq.vitalProperty) {
					jo['vitalProperty'] = vsaq.vitalProperty.propertyURI
				}
				
			}
			
			return jo
		} else if(o instanceof AbstractVitalGraphQuery) {

			AbstractVitalGraphQuery avgq = o
			Map jo = [type: o.class.simpleName,
					pathsElements: toJSON(avgq.pathsElements),
//					rootUris: toJSON(vgq.rootUris),
					segments: toJSON(avgq.segments),
					returnSparqlString: avgq.returnSparqlString
			]
			
			if(o instanceof VitalGraphQuery) {
				VitalGraphQuery vgq = avgq
				jo['rootQuery'] = toJSON(vgq.rootQuery) 
			} else if(o instanceof VitalGraphQueryV2) {
				throw  new RuntimeException("Not implemented!")
//				jo['rootUris'] = toJSON(vgq.rootUris)
			} else if(o instanceof VitalPathQuery) {
				VitalPathQuery vpq = o
				jo['rootUris'] = toJSON(vpq.rootUris)
			} else if(o instanceof VitalSparqlQuery) {
				VitalSparqlQuery vsq = o;
				jo['sparql'] = vsq.sparql 
			}
			
			return jo
		} else if(o instanceof QueryPathElement) {
			QueryPathElement qpe = o
			Map jo = [type: QueryPathElement.class.simpleName,
				collectDestObjects: qpe.collectDestObjects.toString(),
				edgeTypeURI: qpe.edgeTypeURI,
				destTypeURI: qpe.destObjectTypeURI,
				direction: qpe.direction.toString(),
				collectEdges: qpe.collectEdges.toString(),
				collectDestObjects: qpe.collectDestObjects.toString(),
				components: toJSON(qpe.components),
				terminatingURI: qpe.terminatingURI,
				containerType: qpe.type.toString()
			]
			return jo
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
				app.customerID = jo.get("customerID")
				app.ID = jo.get("ID")
				return app
			} else if(type == Customer.class.getSimpleName()) {
				Customer c = new Customer();
				c.customerName = jo.get('customerName')
				c.ID = jo.get('ID')
				return c
			} else if(type == VitalURI.class.getSimpleName()) {
				return VitalURI.withString(jo.get("uri"))
			}  else if(type == java.util.Date.class.simpleName) {
				return new java.util.Date((long)jo.get('time'))
			} else if(type == URIPropertyValue.class.simpleName) {
				return new URIPropertyValue(jo.get("uri"))
			} else if(type == GraphContext.class.getSimpleName()) {
				GraphContext gc = GraphContext.valueOf(jo.get("value"))
				return gc
			} else if(type == VitalSegment.class.simpleName) {
				VitalSegment segment = new VitalSegment()
				segment.id = jo.get("id")
				return segment
			} else if(type == QueryPathElement.class.simpleName) {
				//rdf types to match graph objects domain ?
				Class edgeType = VitalSigns.get().getGroovyClass(jo.get("edgeTypeURI"))
				Class destType = jo.get("destObjectTypeURI") ? VitalSigns.get().getGroovyClass(jo.get("destObjectTypeURI")) : null
				Direction dir = Direction.valueOf(jo.get("direction"))
				CollectEdges ce = CollectEdges.valueOf(jo.get("collectEdges"))
				CollectDestObjects cde = CollectDestObjects.valueOf(jo.get("collectDestObjects"))
				QueryPathElement qpe = new QueryPathElement(edgeType, destType, dir, ce, cde)
				qpe.components = fromJSON(jo.get("components"))
				qpe.terminatingURI = jo.get("terminatingURI")
				qpe.type = Type.valueOf(jo.get("containerType"))
				return qpe
			} else if(type == 'class') {
				return Class.forName(jo.get('value'))
			} else if(type == VitalSelectQuery.class.simpleName || type == VitalSelectAggregationQuery.class.simpleName) {

				VitalSelectQuery sq = type == VitalSelectAggregationQuery.class.simpleName  ? new VitalSelectAggregationQuery() : new VitalSelectQuery()
				sq.components = fromJSON(jo.get("components"))
				if( jo.get("limit") != null ) {
					sq.limit = jo.get("limit")
				} 
				if(jo.get("offset") != null) {
					sq.offset = jo.get("offset") 
				}
				if(jo.get("projectionOnly") !=null) {
					sq.projectionOnly = jo.get("projectionOnly") 
				}
				sq.segments = fromJSON(jo.get("segments"))
				if(jo.get("sortProperties")) {
					sq.sortProperties = fromJSON(jo.get("sortProperties"))
				}
				if(jo.get("containerType") != null) {
					sq.type = Type.valueOf(jo.get("containerType"))
				}
				
				if(sq instanceof VitalSelectAggregationQuery) {
					VitalSelectAggregationQuery vsaq = sq
					vsaq.aggregationType = AggregationType.valueOf(jo.get('aggregationType'))
					Object vp = jo.get('vitalProperty');
					if(vp) {
						if(vp instanceof String) {
							vsaq.vitalProperty = new VitalProperty(vp) 
						} else if(vp instanceof Map){
							vsaq.vitalProperty = new VitalProperty(vp['class'], vp['name'])
						}
					}
				}
				
				return sq
			} else if(type == VitalGraphQuery.class.simpleName || type == VitalSparqlQuery.class.simpleName
			|| type == VitalPathQuery.class.simpleName || type == VitalGraphQueryV2.class.simpleName) {
				AbstractVitalGraphQuery avgq = null;
				if(type == VitalGraphQuery.class.simpleName ) {
					avgq = new VitalGraphQuery()
					VitalGraphQuery vgq = avgq
					vgq.rootQuery = fromJSON(jo.get('rootQuery'))
				} else if(type == VitalSparqlQuery.class.simpleName) {
					avgq = new VitalSparqlQuery()
					VitalSparqlQuery vsq = avgq
					vsq.sparql = jo.get('sparql')
				} else if(type == VitalPathQuery.class.simpleName) {
					avgq = new VitalPathQuery()
					VitalPathQuery vpq = avgq
					vpq.rootUris = fromJSON(jo.get("rootUris"))
				} else if(type == VitalGraphQueryV2.class.simpleName) {
//					avgq = new VitalGraphQueryV2()
					throw new RuntimeException("Not implemeted!")
				}
				
				// ? : new VitalGraphQuery()
				avgq.pathsElements = fromJSON(jo.get("pathsElements"))
				avgq.segments = fromJSON(jo.get("segments"))
				avgq.returnSparqlString = jo.get('returnSparqlString')
				
				return avgq
				
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
				if(negative == null) negative = false

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
				//			} else if(type == Q)
			} else if( type == ResultList.class.simpleName) {
			
				ResultList rs = new ResultList()
				
				rs.limit = jo.get("limit")
				rs.offset = jo.get("offset")
				rs.totalResults = jo.get("totalResults")
				rs.results = fromJSON(jo.get("results"))
				
				return rs
			} else if( type == ResultElement.class.simpleName ) {
				ResultElement re = new ResultElement(fromJSON(jo.get("graphObject")), jo.get("score"))
				return re
			} else {
				GraphObject g = ai.vital.vitalsigns.json.JSONSerializer.fromJSONMap(jo)
				if(g == null) throw new RuntimeException("No graph object deserialized from ${jo}")
				return g
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
	
	public static void main(String[] args) {

		Map x = VitalServiceJSONMapper.toJSON(VitalURI.withString('xxx'));
		
		Object y = VitalServiceJSONMapper.fromJSON(x);
				
		VitalGraphQuery graphQuery = new VitalGraphQuery()
		graphQuery.rootUris = ['root']
		graphQuery.segments = [VitalSegment.withId('segment1')]
		graphQuery.pathsElements = [
			[
				new QueryPathElement(Edge_hasEntity.class, null, Direction.reverse, CollectEdges.yes, CollectDestObjects.yes)
			]
		]
		
		Map json = VitalServiceJSONMapper.toJSON(graphQuery)
		
		VitalGraphQuery vgq = VitalServiceJSONMapper.fromJSON(json)
		
//		ResultList rs = Factory.getVitalService().graphQuery(vgq)
//		println rs
		
		Document doc= new Document()
		doc.URI = 'd1'
		doc.title = 't'
		
		Edge_hasEntity edge = new Edge_hasEntity()
		edge.URI = 'uri1'
		edge.sourceURI = 'x1'
		edge.destinationURI = 'z1'
		
		
		Map m = VitalServiceJSONMapper.toJSON([method: 'm1', args:[[doc, edge]]]);
		
		Object fromJSON = VitalServiceJSONMapper.fromJSON(m)
		
		println fromJSON
	}
}
