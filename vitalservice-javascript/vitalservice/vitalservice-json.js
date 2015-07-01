/**
 * VitalService JSON submodule - validates the messages and transferred objects using json schema
 * TODO - all service objects validation
 */
VitalServiceJson = function() {
	
	if(typeof(tv4) == 'undefined') {
		throw ("missing tv4 library");
	}
	
	if(typeof(vital_core_0_1_0_schema) == 'undefined') {
		throw ("No vital_core_0_1_0_schema - core json schema not loaded");
	}
	
	if(typeof(vital_0_1_0_schema) == 'undefined') {
		throw ("No vital_0_1_0_schema - domain json schema not loaded");
	}
	
	this.dynamicPropertiesClasses = 
	[
	 'http://vital.ai/ontology/vital-core#VITAL_GraphContainerObject',
	 'http://vital.ai/ontology/vital#SparqlBinding'
	];
	
	//TODO add other schemas loaded via some collection?
	var sFiles = [vital_core_0_1_0_schema, vital_0_1_0_schema];
	
	for(var i = 0 ; i < VitalServiceJson.customOntologies.length; i++) {
		sFiles.push(VitalServiceJson.customOntologies[i]);
	}
	
	this.loaded = {};
	
	for(var i = 0 ; i < sFiles.length; i++) {
		
		var schemas = sFiles[i];
		
		for(var j = 0 ; j < schemas.length; j++) {
			
			var schema = schemas[j];
			
			var extendsURI = schema['extends'];
			
			if(extendsURI != null) {
				
//				console.log("Extension to ", extendsURI);
				
				var l = this.loaded[extendsURI];
				
				if(l == null) {
					console.warn("Base schema not found: " + extendsURI);
					continue;
				}
			
				var currentProps = l['properties'];
				
				var newProps = schema['properties'];
				
				$.extend(currentProps, newProps);
				
				tv4.addSchema(extendsURI, l);
				
				this.loaded[extendsURI] = l;
				
			} else {
				
				var uri = schema.id;
				
//				console.log("Loading schema ", uri);
				
				tv4.addSchema(uri, schema);
				
				this.loaded[uri] = schema;
				
			}
			
		}
		
	}
	
}

VitalServiceJson.customOntologies = [];

/**
 * Validates vitalservice json response object
 * returns errorMessage or null if ok
 */
VitalServiceJson.prototype.validateResponse = function(response) {

	if(response == null) return 'null response object - nothing to validate';
	
	//only result list
	if( response.type != 'ResultList' ) {
		return;
	} 
	
	if( response.results == null ) return null;
	
	for(var i = 0 ; i < response.results.length; i++) {
		
		var resultElement = response.results[i];
		
		var graphObject = resultElement.graphObject;

		if(graphObject.type == null) return "Graph object without type!";

		if(this.dynamicPropertiesClasses.indexOf(graphObject.type) >= 0) {
			//dynamic properties objects are allowed
			continue;
		}
		
		var schema = this.loaded[graphObject.type]
		
		if(schema == null) return "No schema found for graph object type: " + graphObject.type; 
		
		
		var valid = tv4.validate(graphObject, schema);
		//console.log("Object valid ? " + valid, obj);
		
		if(!valid) {
			console.error("Object invalid", tv4.error);
			console.error("Object invalid code ", tv4.error.code);
			console.error("Object invalid message ", tv4.error.message);
			console.error("Object invalid dataPath ", tv4.error.dataPath);
			console.error("Object invalid schemaKey ", tv4.error.schemaKey);
			console.error("Object invalid obj Key ", graphObject);
			return "Code: " + tv4.error.code + 
			" Message: " + tv4.error.message +
			" DataPath: " + tv4.error.dataPath + 
			" SchemaKey: " + tv4.error.schemaKey;
		}
	}
	
	console.log("Validation passed, checked " + response.results.length + " objects");
    
    return null;

	
}