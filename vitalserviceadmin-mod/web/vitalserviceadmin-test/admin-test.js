var vitalserviceAdmin = null;

$(function(){
	
	
	console.log("instantiating service...");
	
	vitalserviceAdmin = new VitalServiceAdmin(function(){
		console.log('connected to endpoint');
	}, function(err){
		alert('couln\'t connect to endpoint -' + err);
	});
	
	var methodsEl = $('#methods');
	
	var clickHandler = function(event){
		var b = $(this);
		var methodName = b.text();
		console.log('button-clicked', methodName);
		
		var m = vitalserviceAdmin[methodName];
		
		var margs = method2Params[methodName];
		
		if(margs == null) {
			alert("No test params for " + m);
		}
		
		var copied = margs.slice();
		
		var $method = $('#method');
		
		var $response = $('#response');
		var $error = $('#error');
		
		$method.empty();
		$response.empty();
		$error.empty();
		
		$method.append($('<p>').text("method: " + methodName));
		
		$method.append($('<p>').text("input args:"));
		$method.append($('<p>').text(JSON.stringify(copied)));
		
		var sCB = function(result){
			$response.text(JSON.stringify(result));
		};
		
		var eCB = function(error) {
			$error.text(error);
		};
		
		copied.push(sCB);
		copied.push(eCB);
		
		console.log('method ' + m + ' params', copied);
		m.apply(vitalserviceAdmin, copied);
		
	};
	
	for( var p in vitalserviceAdmin) {
		
	    if(typeof vitalserviceAdmin[p] === "function") {
	    	
	    	methodsEl.append($('<li>')
	    		.append($('<button>').text(p).click(clickHandler))
	    	);
	    	
		}
	}
	
});

var app = {
	type: 'App',
	ID: 'test'
};

var segment = {
	type: 'VitalSegment',
	appId: app.ID,
	id: 'seg1'
};

var method2Params = {
	addApp: [app],
	addSegment: [app, segment, true],
	callFunction: [app, 'function1', {param1: 'p1', param2: 2}],
//	'delete_': [ {type: 'VitalURI', uri: 'urn:x1'} ],
	'delete_': [ app, [ {type: 'VitalURI', uri: 'urn:x1'}, {type: 'VitalURI', uri: 'urn:x2'} ] ],
	deleteObjects: [ app, [{type: 'http://vital.ai/ontology/vital#Entity', URI: 'urn:e1'}, {type: 'http://vital.ai/ontology/vital#Entity', URI: 'urn:e2'}] ],
//	deleteExpanded: [],
//	deleteExpandedInSegments: [],
//	deleteExpandedInSegmentsWithPaths: [],
//	deleteExpandedWithPaths: [],
	generateURI: [ app, {type: 'class', value: 'ai.vital.domain.Document'}],
	get: [ app, {type: 'VitalURI', uri:'urn:x2'} ],
	graphQuery: [app, { 
//		type: 'Vital',
//		
	}],
	listApps: [],
	listSegments: [app],
	ping: [],
	removeApp: [app],
	removeSegment: [app, segment],
	save: [ app, {type: 'VitalSegment', id: 'x'}, {type: 'http://vital.ai/ontology/vital#Entity'} ],
	selectQuery: [ app, {
		type: 'VitalSelectQuery',
		segments: [{type: 'VitalSegment', id: 'wordnet'}],
		components: [{type: 'VitalPropertyConstraint', comparator: 'EQ', value: 'apple', propertyURI: 'http://vital.ai/ontology/vital#hasName'}]
	}]
};

