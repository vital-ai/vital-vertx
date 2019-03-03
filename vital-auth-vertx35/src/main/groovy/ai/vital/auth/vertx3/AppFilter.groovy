package ai.vital.auth.vertx3

import java.util.Map.Entry
import java.util.regex.Pattern;

class AppFilter {

	public static abstract class Rule {
		
		Pattern methodRegex
		
		//mainly for callfunction
		Pattern callFunctionRegex
		
		public boolean methodMatch(String methodName, String scriptName) {
			
			boolean matches = methodRegex.matcher(methodName).matches()
			
			if(!matches)  return matches
			
			if('callFunction' == methodName && callFunctionRegex != null) {
				
				if(!scriptName) return false
				
				return callFunctionRegex.matcher(scriptName).matches()
				
			}
			
			return true
			
			
		}
	}
	
	public static class Allow extends Rule {
		
	}
	
	public static class Auth extends Rule {
		
	}
	
	public static class Deny extends Rule {
		
	}
	
	List<Rule> rules = []
	
	
	static AppFilter fromJsonList(List<Map<String, Object>> map) {
		
		AppFilter f = new AppFilter()
		
		for(Map<String, Object> m : map) {
			
			String t = m.get('type');
			
			if(!t) throw new RuntimeException("No filter rule type")
			
			Rule r = null
			
			if(t.equalsIgnoreCase('allow')) {
				
				r = new Allow()
				
			} else if(t.equalsIgnoreCase('deny')) {
			
				r = new Deny()
			
			} else if(t.equalsIgnoreCase('auth')) {
			
				r = new Auth()
			
			}
			
			String method = m.get('method')
			
			if(!method) throw new RuntimeException("No rule 'method' regex rule")
			
			r.methodRegex = Pattern.compile(method)
			
			String callFunctionRegex = m.get('function')
			
			if(callFunctionRegex) {
				
				r.callFunctionRegex = Pattern.compile(callFunctionRegex)
				
			}
			
			f.rules.add(r)
			
			
		}
		
		if(f.rules.size() < 1) throw new RuntimeException("No rules defined")
		
		return f
		
	}
	
}
