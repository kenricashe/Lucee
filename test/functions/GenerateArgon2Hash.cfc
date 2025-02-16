component extends="org.lucee.cfml.test.LuceeTestCase"  labels="argon2" skip="true"{
	function run( testResults , testBox ) {
		describe( "test case for GenerateArgon2Hash", function() {
			it(title = "Checking with GenerateArgon2Hash()",skip=getJavaVersion()>20, body = function( currentSpec ) {
				assertEquals(true, Argon2CheckHash('test', GenerateArgon2Hash('test')));
			});

			it(title = "Checking with GenerateArgon2Hash",skip=getJavaVersion()>20, body = function( currentSpec ) {

				assertEquals(true,"#Argon2CheckHash('test', GenerateArgon2Hash('test'))#");

				assertEquals(true,"#Argon2CheckHash('test', GenerateArgon2Hash('test', 'argon2i', 1, 8, 1))#");
						
				try{
					assertEquals(true,"#Argon2CheckHash('test', GenerateArgon2Hash('test', '', 1, 8, 1))#");
					fail("must throw:The Variant should be ARGON2i or ARGON2d or ARGON2id");
				}
				catch(any e){}

				try{
					assertEquals(true,"#Argon2CheckHash('test', GenerateArgon2Hash('test', 'argon2i', 0, 8, 1))#");
					fail("must throw:The parallelism factor value should be between 1 and 10");
				}
				catch(any e){}

				try{
					assertEquals(true,"#Argon2CheckHash('test', GenerateArgon2Hash('test', 'argon2i', 1, 7, 1))#");
					fail("must throw:The memory cost value should be between 8 and 100000");
				}
				catch(any e){}

				try{
					assertEquals(true,"#Argon2CheckHash('test', GenerateArgon2Hash('test', 'argon2i', 1, 8, 0))#");
					fail("must throw:The iterations value should be between 1 and 20");
				}
				catch(any e){}
			});		
		});	
	}
	private function getJavaVersion() {
	    var raw=server.java.version;
	    var arr=listToArray(raw,'.');
	    if(arr[1]==1) // version 1-9
	        return arr[2];
	    return arr[1];
	}
}