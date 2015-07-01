package ai.vital.service.vertx.test

import groovy.transform.TypeChecked.TypeCheckingInfo;

import org.vertx.groovy.core.eventbus.Message;
import org.vertx.groovy.platform.Verticle

import ai.vital.lucene.memory.service.VitalServiceLuceneMemory;
import ai.vital.service.vertx.VitalServiceMod
import ai.vital.vitalservice.factory.Lock
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalservice.query.VitalPropertyConstraint
import ai.vital.vitalservice.query.VitalQueryContainer
import ai.vital.vitalservice.query.VitalSortProperty
import ai.vital.vitalservice.query.VitalPropertyConstraint.Comparator
import ai.vital.vitalservice.query.VitalSelectQuery
import ai.vital.vitalservice.query.VitalQueryContainer.Type
import ai.vital.vitalservice.query.VitalTypeConstraint
import ai.vital.domain.Document

import ai.vital.service.vertx.json.VitalServiceJSONMapper

import ai.vital.vitalservice.segment.VitalSegment

class TestVerticle extends Verticle {

	@Override
	public Object start() {

		vertx.eventBus.send(VitalServiceMod.ADDRESS, [method: 'listSegments', args: []]) { Message response ->
			
			Object body = response.body()
			
			println body
			
		}
		
//		VitalServiceLuceneMemory memory = new VitalServiceLuceneMemory(new Lock(), VitalServiceLuceneMemory.defaultCustomer, VitalServiceLuceneMemory.defaultApp);
//		memory.save(null, null)
		
		
		VitalSelectQuery selectQuery = new VitalSelectQuery()
		selectQuery.offset = 0
		selectQuery.limit = 10
		selectQuery.projectionOnly = false
		selectQuery.segments = [ VitalSegment.withId('user') ]
		
		String filter = "derek@vital.ai";
		
		selectQuery.type = VitalQueryContainer.Type.or
		
		VitalQueryContainer qc1 = new VitalQueryContainer()
		qc1.type = VitalQueryContainer.Type.and
		qc1.components = [
			new VitalTypeConstraint(Document.class),
			new VitalPropertyConstraint(Document.class, 'title', filter.toLowerCase(), Comparator.EQ, false)
		]
		
		VitalQueryContainer qc2 = new VitalQueryContainer()
		qc2.type = VitalQueryContainer.Type.and
		qc2.components = [
			new VitalTypeConstraint(Document.class),
			new VitalPropertyConstraint(Document.class, 'body', filter.toLowerCase(), Comparator.CONTAINS_CASE_INSENSITIVE, false)
		]
		
		selectQuery.components = [
			qc1,
			qc2
		]
		
		selectQuery.sortProperties = [new VitalSortProperty(VitalSortProperty.RELEVANCE, false)]
		
		
		
		Map jo = [method: 'selectQuery', args: [ VitalServiceJSONMapper.toJSON(selectQuery)]]
		
		vertx.eventBus.send(VitalServiceMod.ADDRESS, jo) { Message response ->
			
			Object body = response.body()
			
			println body
			
			Map selectRS = response.body()
			
			println selectRS
			if( selectRS.get('status') != "ok" ) {
				throw new RuntimeException("xx");
			}

			ResultList rs = VitalServiceJSONMapper.fromJSON(selectRS.get('response'))
			println rs
		}
		
		
		
		return super.start();
	}

	
}
