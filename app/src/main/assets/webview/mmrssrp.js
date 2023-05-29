/*jslint browser: true*/
/*global $, jQuery, alert, angular, MMRSUTILS, MMRSCLIENTGATEWAY, MMRSCONFIG, MMRSCHARTUTILS*/
var MMRSSRP = (function () {

    var mClient = new jsrp.client();
    var mServer = new jsrp.server();

    var BRIDGE = MMRSUTILS.getDeviceBridge();

    function onLoad()
    {
        note(">> onLoad");

        if (! MMRSUTILS.isNativeRun())
        {
            BRIDGE = SRPTESTBRIDGE;
            BRIDGE.test();
        }

        note("<< onLoad");
    }

    //==============================================================================
    // Asynchronous counter part of the above public interface.
    // The main body of the funtion is run asynchronously as it would otherwise
    // block the calling native thread and the response that we send is done 
    // while that is blocked. This happens on iOS.
    //==============================================================================
    //

    function doSRPClientInitialiseA(aUsername, aPassword)
    {
        //trace(">> doSRPClientInitialiseA, User: " +aUsername+ ", Pass: " +aPassword);

        mClient.init({ username: aUsername, password: aPassword }, function ()
        {
            // Generate a salt on the client (this could be done on the server if that is better)
            mClient.srp.generateSalt(function (saltErr, salt)
            {            
                if (saltErr !== null)
                {
                    error('doSRPClientInitialiseA, Unable to generate salt');
                }

                //trace("doSRPClientInitialiseA, Salt Type: " +typeof(salt));
            
                mClient.setSalt(salt);

                var clientPublicKey = mClient.getPublicKey();   

                //trace("doSRPClientInitialiseA, Client Pub Key: " +clientPublicKey);

                var saltHex = mClient.getSalt();

                //trace("<< doSRPClientInitialiseA, Salt: " +saltHex);

                BRIDGE.onSRPClientInitialiseResponse(saltHex, clientPublicKey);   
                return;
            });
        });
    }

    function doSRPServerInitialiseA(aSaltHex, aUsername, aPassword)
    {
        //trace(">> doSRPServerInitialiseA, Salt: " +aSaltHex+ ", User: " +aUsername+ ", Pass: " +aPassword);

        var salt = aSaltHex; // hexToBinArray(aSaltHex);

        mServer.init({ salt: salt, username: aUsername, password: aPassword }, function ()
        {
            mServer.setSalt(aSaltHex);

            var serverPublicKey = mServer.getPublicKey();

            //trace("<< doSRPServerInitialiseA, Server Pub Key: " +serverPublicKey);

            BRIDGE.onSRPServerInitialiseResponse(serverPublicKey);
        });        
    }    

    function doSRPClientCreateProofA(aServerPublicKey)
    {
        //trace(">> doSRPClientCreateProofA, Server Pub Key: " +aServerPublicKey);

        mClient.setServerPublicKey(aServerPublicKey);

        var proof = mClient.getProof();

        //trace("<< doSRPClientCreateProofA, Client Proof: " +proof);

        BRIDGE.onSRPClientCreateProofResponse(proof);
    }

    function doSRPServerPerformClientProofCheckA(aClientPublicKey, aClientProof)
    {
        //trace(">> doSRPServerPerformClientProofCheckA, Client Pub Key: " +aClientPublicKey+ ", Client Proof: " +aClientProof);

        mServer.setClientPublicKey(aClientPublicKey);

        var result = mServer.checkClientProof(aClientProof);

        var resultStr = result ? "1" : "0";

        var serverSharedKey = "";
        var serverProof     = ""

        if (result)
        {
            serverSharedKey = mServer.getSharedKey();
            serverProof     = mServer.getProof();
        }

        //trace("<< doSRPServerPerformClientProofCheckA, Result: " +resultStr+ ", Server Shared Key: " +serverSharedKey+ ", Server Proof: " +serverProof);   

        BRIDGE.onSRPServerPerformClientProofCheckResponse(resultStr, serverSharedKey, serverProof);
    }

    function doSRPClientPerformServerProofCheckA(aServerProof)
    {
        //trace(">> doSRPClientPerformServerProofCheckA, Server Proof: " +aServerProof);

        var result = mClient.checkServerProof(aServerProof);
        var resultStr = result ? "1" : "0";        
        
        //trace("*** doSRPClientPerformServerProofCheck, Client - Proof Result: " +result);   

        var clientSharedKey = "";

        if (result)
        {
            clientSharedKey = mClient.getSharedKey();
        }


        //trace("<< doSRPClientPerformServerProofCheckA, Result: " +resultStr+ ", Client Shared Key: " +clientSharedKey);

        BRIDGE.onSRPClientPerformServerProofCheckResponse(resultStr, clientSharedKey);       
    }

    /* Unused

    function binArrayToHex(aArray)
    {
        var result = "";

        var length = aArray.length;

        //trace("binArrayToHex, Length: " +length);

        for (var index = 0; index < length; index++)
        {
            var value = aArray[index];

            var hex = value.toString(16).toUpperCase();

            //trace("binArrayToHex, Index: " +index+ ", Value: " +value+ ", Hex: " +hex);

            result += hex;
        }

        return result;
    }

    function hexToBinArray(aHex)
    {
        var result = [];

        var hexLength = aHex.length;

        //trace("hexToBinArray, Length: " +hexLength);

        for (var index = 0; index < hexLength; index += 2)
        {
            var thisHex = aHex.substr(index, 2)

            var thisByte = parseInt(thisHex, 16);

            //trace("hexToBinArray, Index: " +index+ ", Hex: " +thisHex+ ", Value: " +thisByte);

            result.push(thisByte);
        }

        return result;
    }
    */

    //=====================================================================================
    // Testing from web browser
    //=====================================================================================
    //

    var SRPTESTBRIDGE = {};

    SRPTESTBRIDGE.test = function()
    {
        exports.doSRPClientInitialise("usergood", "passgood"); // Callback is: onSRPClientReady
    }

    SRPTESTBRIDGE.onSRPClientInitialiseResponse = function(aSalt, aClientPublicKey)
    {
        trace("onSRPClientInitialiseResponse, Client - Public: " +aClientPublicKey);

        SRPTESTBRIDGE.clientPublicKey = aClientPublicKey;

        exports.doSRPServerInitialise(aSalt, "usergood", "passgood"); // Callback is: onSRPServerReady
    }

    SRPTESTBRIDGE.onSRPServerInitialiseResponse = function(aServerPublicKey)
    {
        trace("onSRPServerInitialiseResponse, Server - Public: " +aServerPublicKey);

        exports.doSRPClientCreateProof(aServerPublicKey);
    }

    SRPTESTBRIDGE.onSRPClientCreateProofResponse = function(aClientProof)
    {
        trace("doSRPClientCreateProof, Client - Proof: " +aClientProof);

        exports.doSRPServerPerformClientProofCheck(SRPTESTBRIDGE.clientPublicKey , aClientProof);        
    }

    SRPTESTBRIDGE.onSRPServerPerformClientProofCheckResponse = function(aResult, aSharedKey, aServerProof)
    {
        trace("onSRPServerPerformClientProofCheckResponse, Server - Result: " +aResult+ ", Shared Key: " +aSharedKey+ ", Server Proof: " +aServerProof);

        exports.doSRPClientPerformServerProofCheck(aServerProof);        
    }

    SRPTESTBRIDGE.onSRPClientPerformServerProofCheckResponse = function(aResult, aSharedKey)
    {
        trace("onSRPClientPerformServerProofCheckResponse, Client - Result: " +aResult+ ", Shared Key: " +aSharedKey);

        trace("-- END --");
    }
    //
    //=====================================================================================



    //====================================================================================
    // Logging
    //====================================================================================
    //
    function trace(aText) { MMRSUTILS.doTrace("mmrssrp", aText);  }
    function note(aText)  { MMRSUTILS.doTrace("mmrssrp", aText);  }
    function warn(aText)  { MMRSUTILS.doWarn("mmrssrp", aText);   }
    function error(aText) { MMRSUTILS.doError("mmrssrp", aText);  }    

    //====================================================================================
    // EXPORTS
    //
    // Each these functions return the result by called their corresponding
    // response function in the native bridge
    //
    // E.g.
    // doSRPClientInitialise              calls onSRPClientInitialiseResponse
    // doSRPServerPerformClientProofCheck calls onSRPServerPerformClientProofCheckResponse
    //
    //==============================================================================

    var exports = {};

    exports.name = "mmrssrp";

    exports.onLoad = function()   { onLoad(); }

    exports.doSRPClientInitialise = function(aUsername, aPassword)
    {
        setTimeout(doSRPClientInitialiseA, 0, aUsername, aPassword);
    }

    exports.doSRPServerInitialise = function(aSalt, aUsername, aPassword)
    {
        setTimeout(doSRPServerInitialiseA, 0, aSalt, aUsername, aPassword);
    }
     
    exports.doSRPClientCreateProof = function(aServerPublicKey)
    {
        setTimeout(doSRPClientCreateProofA, 0, aServerPublicKey);
    }

    exports.doSRPClientPerformServerProofCheck = function(aServerProof)
    {
        setTimeout(doSRPClientPerformServerProofCheckA, 0, aServerProof);
    }

    exports.doSRPServerPerformClientProofCheck = function(aClientPublicKey, aClientProof)
    {
        setTimeout(doSRPServerPerformClientProofCheckA, 0, aClientPublicKey, aClientProof);
    }    



    return exports;

}());
