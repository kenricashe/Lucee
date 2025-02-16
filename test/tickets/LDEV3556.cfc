component extends="org.lucee.cfml.test.LuceeTestCase"{
     function run( testResults, testBox ){
        describe( "Test case for LDEV-3556", function() {
            it(title="Using string with hash() and toBease64 member functions", body=function( currentSpec ){
                expect("string".hash()).toBe(hash("string"));
                expect("string".tobase64()).toBe(tobase64("string"));
            });
            it(title="Checking error for Using struct with toBase64() member function", body=function( currentSpec ){
                try {
                    var eMessage =  {}.toBase64();
                }
                catch(any e) {
                    var eMessage = e.message;
                }
                expect(eMessage).toInclude("The function [toBase64] does not exist in the Struct");
            });
            it(title="Checking error for Using struct with hash() member function", body=function( currentSpec ){
                try {
                    var eMessage =  {}.hash();
                }
                catch(any e) {
                    var eMessage = e.message;
                }
                expect(eMessage).toInclude("The function [hash] does not exist in the Struct");
            });
            it(title="Checking error for Using array with toBase64() member function", body=function( currentSpec ){
                try {
                    var eMessage =  [].toBase64();
                }
                catch(any e) {
                    var eMessage = e.message;
                }
                expect(eMessage).toInclude("The function [toBase64] does not exist in the Array");
            });
            it(title="Checking error for Using array with hash() member function", body=function( currentSpec ){
                try {
                    var eMessage =  [].hash();
                }
                catch(any e) {
                    var eMessage = e.message;
                }
                expect(eMessage).toInclude("The function [hash] does not exist in the Array");
            });
        });
     };
}