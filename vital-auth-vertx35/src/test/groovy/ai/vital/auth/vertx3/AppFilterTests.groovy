package ai.vital.auth.vertx3

import ai.vital.auth.vertx3.AppFilter;
import ai.vital.auth.vertx3.AppFilter.Allow
import ai.vital.auth.vertx3.AppFilter.Auth
import ai.vital.auth.vertx3.AppFilter.Deny
import ai.vital.auth.vertx3.AppFilter.Rule
import junit.framework.TestCase

class AppFilterTests extends TestCase {

	public void testFilters() {
		
		AppFilter af = AppFilter.fromJsonList([
			[type: 'allow', method: 'ping'],
			[type: 'deny',  method: 'list.*'],
			[type: 'auth',  method: 'callFunction', function: 'f1'],
			[type: 'auth',  method: 'callFunction', function: 'f2'],
		])
		
		assertEquals(4, af.rules.size())
		
		Rule r1 = af.rules[0]
		assertTrue(r1 instanceof Allow)
		assertEquals('ping', r1.methodRegex.pattern())
		assertNull(r1.callFunctionRegex)
		
		assertTrue( r1.methodMatch('ping', null) )
		assertFalse( r1.methodMatch('ping2', null) )
		assertTrue( r1.methodMatch('ping', '123') )
		
		Rule r2 = af.rules[1]
		assertTrue(r2 instanceof Deny)
		assertEquals('list.*', r2.methodRegex.pattern())
		assertNull(r2.callFunctionRegex)
		
		assertTrue( r2.methodMatch('list', null) )
		assertFalse( r2.methodMatch('ping2', null) )
		assertTrue( r2.methodMatch('list1', '123') )
		
		
		Rule r3 = af.rules[2]
		assertTrue(r3 instanceof Auth)
		assertEquals('callFunction', r3.methodRegex.pattern())
		assertEquals('f1', r3.callFunctionRegex.pattern())

		assertTrue( r3.methodMatch('callFunction', 'f1') )
		assertFalse( r3.methodMatch('callFunction', 'f2') )
		assertFalse( r3.methodMatch('ping2', 'f1') )
		assertFalse( r3.methodMatch('callFunction', null) )
				
		Rule r4 = af.rules[3]
		assertTrue(r4 instanceof Auth)
		assertEquals('callFunction', r4.methodRegex.pattern())
		assertEquals('f2', r4.callFunctionRegex.pattern())
		
		
		assertTrue( r4.methodMatch('callFunction', 'f2') )
		assertFalse( r4.methodMatch('callFunction', 'f1') )
		assertFalse( r4.methodMatch('ping2', 'f1') )
		assertFalse( r4.methodMatch('callFunction', null) )
	}
	
}
