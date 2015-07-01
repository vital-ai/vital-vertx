var vitalservice = null;

$(function(){
	
	
	console.log("instantiating service...");
	
	vitalservice = new VitalService(function(){
		console.log('connected to endpoint');
	}, function(err){
		alert('couln\'t connect to endpoint -' + err);
	});
	
	var methodsEl = $('#methods');
	
	var clickHandler = function(event){
		var b = $(this);
		var methodName = b.text();
		console.log('button-clicked', methodName);
		
		var m = vitalservice[methodName];
		
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
		
		m.apply(vitalservice, copied);
		
	};
	
	for( var p in vitalservice) {
		
	    if(typeof vitalservice[p] === "function") {
	    	
	    	methodsEl.append($('<li>')
	    		.append($('<button>').text(p).click(clickHandler))
	    	);
	    	
		}
	}
	
});

var method2Params = {
	callFunction: ['function1', {param1: 'p1', param2: 2}],
	'delete_': [{type: 'VitalURI', uri: 'urn:x1'}],
	deleteObjects: [ [{type: 'ai.vital.domain.Entity', URI: 'urn:e1'}, {type: 'ai.vital.domain.Entity', URI: 'urn:e2'}] ],
//	deleteExpanded: [],
//	deleteExpandedInSegments: [],
//	deleteExpandedInSegmentsWithPaths: [],
//	deleteExpandedWithPaths: [],
	generateURI: [{type: 'class', value: 'ai.vital.domain.Document'}],
	get: [ {type: 'VitalURI', uri:'urn:x2'} ],
	graphQuery: [],
	listSegments: [],
	ping: [],
	save: [ {type: 'VitalSegment', id: 'x'}, {type: 'ai.vital.domain.Entity'} ],
	selectQuery: []
}

