var eb = new vertx.EventBus(window.location.protocol + '//' + window.location.hostname + ':' + window.location.port + '/eventbus');
eb.onopen = function() {
	console.log("Event bus connected");
};

var method2Args = {
	ping: [],
	callFunction: 
	[
	 'script1',
	 { 
		 param1: 1,
		 param2 : 3,
		 param3: true
	 }
	],
	get:
	[ 
	 { type: 'VitalURI', uri: 'uri1'}, 
	 {type: 'GraphContext', value: 'Container'}, 
	 []
	],
	getExpanded: 
	[
		 { type: 'VitalURI', uri: 'uri2'},
		 { type: 'VitalSegment', id: 'segment1' }
	],
	getExpanded_: 
	[
	 	{ type: 'VitalURI', uri: 'uri2'},
	 	{ type: 'VitalSegment', id: 'segment1' },
	 	[
	 	 [
	 	  {
	 		  type: 'QueryPathElement', edgeTypeURI: 'http://vital.ai/ontology/vital.owl#Edge_hasTopic', destObjectTypeURI: null, direction: 'forward', collectEdges: 'yes', collectDestObjects: 'no'
	 	  },
	 	  {
	 		  type: 'QueryPathElement', edgeTypeURI: 'http://vital.ai/ontology/vital.owl#Edge_hasNormalizedTopic', destObjectTypeURI: null, direction: 'forward', collectEdges: 'yes', collectDestObjects: 'no'
	 	  }
	 	 ],
	 	 [
	 	  {
	 		  type: 'QueryPathElement', edgeTypeURI: 'http://vital.ai/ontology/vital.owl#Edge_hasEntity', destObjectTypeURI: 'http://vital.ai/ontology/vital.owl#Document', direction: 'reverse', collectEdges: 'yes', collectDestObjects: 'no'
	 	  }
	 	 ]
	 	]
	],
	save:
	[
	 { type: 'VitalSegment', id: 'segment1' },
	 { type: 'http://vital.ai/ontology/vital.owl#Document', URI: 'doc1', title: 'Title'}
	],
	save_:
	[
	 { type: 'VitalSegment', id: 'segment1' },
	 [
	  { type: 'http://vital.ai/ontology/vital.owl#Document', URI: 'doc1', title: 'Title'},
	  { type: 'http://vital.ai/ontology/vital.owl#Entity', URI: 'entity1', name: 'Entity 1'}
	 ]
	],
	'delete': 
	[
	 { type: 'VitalURI', uri: 'uri1'}
	],
	'delete_': 
	[
	 [
	  { type: 'VitalURI', uri: 'uri1'},
	  { type: 'VitalURI', uri: 'uri2'}
	 ]
	],
	'deleteExpanded': 
	[
	 { type: 'VitalURI', uri: 'uri1'}
	],
	'deleteExpanded_': 
	[
	 [
	  { type: 'VitalURI', uri: 'uri1'},
	  { type: 'VitalURI', uri: 'uri2'}
	 ]
	],
	generateURI: 
	[
	 {type: 'class', value: 'ai.vital.domain.Document'}
	],
	listSegments:
	[
	],
	selectQuery:
	[
	 {
	  type: 'VitalSelectQuery',
	  offset: 0,
	  limit: 10,
	  projectionOnly: false,
	  segments: 
	  [
	 	{ type: 'VitalSegment', id: 'wordnet' }
	  ],
	  components: 
	  [
	   	{ type: 'VitalPropertyConstraint', 'class': 'ai.vital.domain.NounSynsetNode', name: 'name', value: 'apple', comparator: 'EQ', negative: false }
	  ],
	  sortProperties: []
	 }
	],
	graphQuery: 
	[
	 {
	  type: 'VitalGraphQuery',
	  segments: 
	  [
		{ type: 'VitalSegment', id: 'wordnet' }
	  ],
	  rootUris: ['http://uri.vital.ai/wordnet/NounSynsetNode_1396611318625_1016512'],
	  pathElements:
	  [
	   [
	    {
	 	  type: 'QueryPathElement', edgeTypeURI: 'http://vital.ai/ontology/vital.owl#Edge_WordnetPartMeronym', destObjectTypeURI: null, direction: 'forward', collectEdges: 'yes', collectDestObjects: 'yes'
	    }
	   ],
	   [
	    {
	    	type: 'QueryPathElement', edgeTypeURI: 'http://vital.ai/ontology/vital.owl#Edge_WordnetPartMeronym', destObjectTypeURI: null, direction: 'reverse', collectEdges: 'yes', collectDestObjects: 'yes'
	    }
	   ],
	  ]
	 }
	]

	

	
}

$(function(){
	
	var el = $('#methods');
	
	for(var key in method2Args) {
		var args = method2Args[key]
		
		var mname = key
		
		while(mname.indexOf('_') == mname.length -1) {
			mname = mname.substring(0, mname.length -1);
		}
		
		el.append($('<div>').append(
			$('<button>').text(mname + ' (' + args.length + 'arg' + (args.length == 1 ? '' : 's') + ')').attr('data-method', key)
		));
	}
	
	$('button[data-method]').each(function(){

		var b = $(this);
		
		var method = b.attr('data-method')
		
		var mname = method
		
		while(mname.indexOf('_') == mname.length -1) {
			mname = mname.substring(0, mname.length -1);
		}
		
		b.click(function(event){
			
			console.log('method: ' + mname )
			
			var args = method2Args[method]
			
			eb.send('vitalservice', {method: mname , args: args}, function(result) {
				console.log(result);
			});
			
		});
	})
	
});