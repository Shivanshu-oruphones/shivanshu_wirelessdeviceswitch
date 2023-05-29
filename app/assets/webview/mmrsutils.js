var MMRSUTILS = (function(){

var mIsAndroid = (navigator.userAgent.indexOf("Android") > 0) ||
                 (navigator.userAgent.indexOf("android") > 0);

var mIsIPhone  = (navigator.userAgent.indexOf("iPhone")  > 0) ||
                 (navigator.userAgent.indexOf("iPad")    > 0);      


var DEVICEBRIDGE = getDeviceBridge();

function doTrace(aTag, aText)
{
    if (mIsAndroid)
    {
        DEVICEBRIDGE.itrace(aTag+ "::" +aText);
        return;
    }

    if (console.log)
    {
       console.log(aTag+ "::" +aText);
    }
}

function doWarn(aTag, aText)
{
    if (mIsAndroid)
    {
        DEVICEBRIDGE.iwarn(aTag+ "::" +aText);
        return;
    }

    if (console.warn)
    {
        console.warn(aTag+ "::" +aText);
        return;        
    }

    if (console.log)
    {
        console.log("+++ " +aTag+ "::" +aText);        
    }
}

function doError(aTag, aText)
{
    if (mIsAndroid)
    {
        DEVICEBRIDGE.ierror(aTag+ "::" +aText);
        return;
    }

    if (console.error)
    {
        console.error(aTag+ "::" +aText);
        return;        
    }

    if (console.log)
    {
        console.log("*** " +aTag+ "::" +aText);        
    }
}

function trace(aText) { doTrace(exports.name, aText); }
function warn(aText)  { doWarn (exports.name, aText); }
function error(aText) { doError(exports.name, aText); }

function isAndroidRun()
{
    return mIsAndroid;
}

function isIPhoneRun()
{
    return mIsIPhone;
}

function isNativeRun()
{
    var isNative = mIsAndroid || mIsIPhone;

    return isNative;
}

var MMRSDEVICEWEB = {};

function getDeviceBridge()
{
    if (mIsAndroid)
    {
        return MMRSNATIVE;
    }

    if (mIsIPhone)
    {
        return IOSNATIVE;
    }

    return MMRSDEVICEWEB;
}

//===================================================================================================
// Object Utilities
//===================================================================================================
//
function copyData(aObject)
{
    var copy = null;

    try
    {
        copy = JSON.parse(JSON.stringify(aObject));
    }
    catch (err)
    {
        error("copyData, Exception: " +err);
        copy = null;
    }

    return copy;
}

function contains(aLine, aWord)
{
    if (aLine == null || aWord == null)
    {
        return false;
    }

    var line = aLine.toLowerCase();
    var word = aWord.toLowerCase();

    var wordPos = line.indexOf(word);

    return wordPos != -1;
}


function setElement(aItemId, aContents)
{
    var item = document.getElementById(aItemId);

    if (item == null)
    {
        error("setElement, Cannot find element: " +aItemId);
        return;
    }

    item.innerHTML = aContents;
}

function getElementPosition(aElementId)
{
    var viewPortoffsetY = window.pageYOffset;
    var viewPortoffsetX = window.pageXOffset;

    //trace("findImagePositions, View Port X: " +viewPortoffsetX+ ", Y: " +viewPortoffsetY);

    var area = document.getElementById(aElementId);

    if (area == null)
    {
    	error("getElementPosition, Cannot find: " +aElementId);
        return null;
    }

    var rect = area.getBoundingClientRect();

    var offsetY = viewPortoffsetY + rect.top; // Add on the scroll
    var offsetX = viewPortoffsetX + rect.left;

    var width  = rect.right  - rect.left;
    var height = rect.bottom - rect.top;

    var point = {x: offsetX, y: offsetY, w: width, h: height};

    return point;
}


function getScreenDimensions()
{
    var screenCenter = getElementPosition("centerline");
    var screenFooter = getElementPosition("footer");

    if (screenCenter == null || screenFooter == null)
    {
        error("Cannot find screen layout elements");
        return;
    }    

    var width  = (screenCenter.x + (screenCenter.w / 2)) * 2;
    var height = (screenFooter.y + screenFooter.h) * 2;

//--    trace("getScreenDimensions, Width: " +width+ ", Height: " +height);
 
    var screenSize = { w: width, h: height};

    return screenSize;
}


function getAngularScope(aAngularArea)
{
//--    trace(">> getAngularScope");

    var angularElement = document.getElementById(aAngularArea);

    if (angularElement == null)
    {
        error("getAngularScope, Cannot find angular area element: " +aAngularArea);
        return null;
    }

    var scope = angular.element(angularElement).scope();

    if (scope == null)
    {
        error("getAngularScope, cannot get angular scope in area: " +aAngularArea);
        return null;
    }

//--    trace("<< getAngularScope");    

    return scope;
}


function getOptions(aUrl)
{
    var options = {};

    var locationStr = aUrl;
    if (locationStr == null) locationStr = new String(document.location);
    
    // Ignore any fragments
//--    trace("getOptions, aUrl: " + aUrl);
    var mainUrlAndFragment = locationStr.split('#');
    var locationStr = mainUrlAndFragment[0];

//--    trace("getOptions, Url: " + locationStr);

    var queryPos = locationStr.indexOf('?');

    if (queryPos == -1)
    {
        return options;
    }

    var queryStr = locationStr.slice(queryPos+1);

    var queries = queryStr.split('&');

//--    trace("getOptions, Queries: +" +queryStr+ "+");

    for (var index in queries)
    {
        var thisQuery = queries[index];

        var equalPos = thisQuery.indexOf("=");

        if (equalPos == -1)
        {
            options[thisQuery] = true;
        }
        else
        {
            var key   = thisQuery.substr(0, equalPos);
            var value = thisQuery.substr(equalPos+1);

            //trace("getOptions, Query: " +thisQuery+ ", Key: " +key+ ", Value: " +value+ ",");
            options[key] = value;
        }

//--        trace("getOptions, Key: +" +key+ "+ Value: +" + options[key] + "+");
    }

    return options;
}

function mkStateChangeFn(aHttpRequest, aRequestId, aCallback)
{
    return function()
    {
//--        trace("stateChange, onreadystatechange - Enter (" +aRequestId+ ")"); 
        
        if (aHttpRequest.readyState != 4)
        {
//--            trace("stateChange, onreadystatechange - Not Ready (" +aRequestId+ ")"); 
            return;
        }
    
//--        trace("stateChange, onreadystatechange - Ready (" +aRequestId+ ")"); 
        
        if (aHttpRequest.status == 200)
        {
            var contentType = aHttpRequest.getResponseHeader("Content-Type");
            
//--            trace("stateChange, onreadystatechange - Content Type: " +contentType+ " (" +aRequestId+ ")");

            var text = aHttpRequest.responseText;
            
            if (text == null)
            {
//--                trace("stateChange, onreadystatechange - Unable to Get Returned Data (" +aRequestId+ ")");
                aCallback(aRequestId, false, null);
                return;
            }

//--            trace("stateChange, onreadystatechange - Text Length: " + text.length+ " (" +aRequestId+ ")");
            aCallback(aRequestId, true, text);
            return;
        }

//--        trace("stateChange, onreadystatechange - Unable to Get Url Status: " +aHttpRequest.status+ " (" +aRequestId+ ")");        
        aCallback(aRequestId, false, null);
    }
}


function sendGetRequest(aUrl, aRequestId, aHeaders, aCallback)
{
//--    trace("sendGetRequest, Url: " +aUrl);   

    if (aCallback == null)
    {
        error("sendGetRequest, No call back specified");
        return;
    }

    var httpRequest = null

    if (window.XMLHttpRequest)
    {
//--        trace("sendGetRequest - Window has XMLHttpRequest");
        // code for IE7+, Firefox, Chrome, Opera, Safari
        httpRequest = new XMLHttpRequest();
    }
    else
    {
//--        trace("sendGetRequest - Window does NOT have XMLHttpRequest");
        // code for IE6, IE5
        httpRequest = new ActiveXObject("Microsoft.XMLHTTP");
    }

    httpRequest.onreadystatechange = mkStateChangeFn(httpRequest, aRequestId, aCallback);

    var queryPos   = aUrl.indexOf("?");
    var concatChar = queryPos == -1 ? "?" : ":";

    var dateNow = new Date();
    var timeNow = dateNow.getTime();

    aUrl += concatChar+ "time="  +timeNow;
    
    httpRequest.open("GET", aUrl, true);

    if (aHeaders != null)
    {
        for (var headerKey in aHeaders)
        {
            var headerData = aHeaders[headerKey];

//--            trace("sendGetRequest, Header: " +headerKey+ ", Value: " +headerData);

            httpRequest.setRequestHeader(headerKey, headerData);
        }
    }    

    httpRequest.send();
}

function sendPostRequest(aUrl, aRequestId, aHeaders, aData, aCallback)
{
//--    trace("sendPostRequest, Url: " +aUrl);   
 
    if (aCallback == null)
    {
        error("sendPostRequest, No call back specified");
        return;
    }

    var httpRequest = null

    if (window.XMLHttpRequest)
    {
        //trace("sendPostRequest - Window has XMLHttpRequest");
        // code for IE7+, Firefox, Chrome, Opera, Safari
        httpRequest = new XMLHttpRequest();
    }
    else
    {
        //trace("sendPostRequest - Window does NOT have XMLHttpRequest");
        // code for IE6, IE5
        httpRequest = new ActiveXObject("Microsoft.XMLHTTP");
    }

    httpRequest.onreadystatechange = mkStateChangeFn(httpRequest, aRequestId, aCallback);

    var queryPos  = aUrl.indexOf("?");
    var concatChar = queryPos == -1 ? "?" : ":";

    var dateNow = new Date();
    var timeNow = dateNow.getTime();

    //aUrl += concatChar+ "time="  +timeNow;    

    httpRequest.open("POST", aUrl, true);

    if (aHeaders != null)
    {
        for (var headerKey in aHeaders)
        {
            var headerData = aHeaders[headerKey];

//--            trace("sendPostRequest, Header: " +headerKey+ ", Value: " +headerData);

            httpRequest.setRequestHeader(headerKey, headerData);

        }
    }
    
//--    trace("sendPostRequest, Data: " +aData);

    httpRequest.send(aData);
}

/*
Return the field object from the array that has the matching settingitem value
Returns null if there is no matching field in the array
*/
function getFieldUsingSettingItem(aResponseData, aSettingItemValue) {
    trace(">> getFieldUsingSettingItem : aSettingItemValue: " + aSettingItemValue);
    var matchingField = null,
        currentField,
        currentSettingItemValue,
        fieldIndex,
        fieldArray = aResponseData.fields;

    if ((fieldArray === null) || (fieldArray === undefined)) {
        trace("Could not find field array");
    } else {
        for (fieldIndex = 0; fieldIndex < fieldArray.length; fieldIndex = fieldIndex + 1) {
            currentField = fieldArray[fieldIndex];

            if ((currentField !== null) && (currentField !== undefined)) {
                currentSettingItemValue = currentField.fieldid;

                if ((currentSettingItemValue !== null) && (currentSettingItemValue !== undefined)) {
                    if (currentSettingItemValue === aSettingItemValue) {
                        trace("Got matching field: " + JSON.stringify(currentField));
                        matchingField = currentField;
                        break;
                    }
                }
            }
        }
    }

//--    trace("<< getFieldUsingSettingItem");

    return matchingField;
}
    
// Utility function to search for a field and get the fieldValue as a percentage
// Returns null if any of the following conditions are true:
    // - the field or value could not be found
    // - there is no maximum value (minimum values are assumed to be 0)
function getPercentValueFromResponseData(aResponseData, aSettingItemValue) {
//--    trace(">> getValueFromResponseData");

    var matchingField,
        matchingMaximumNumber = NaN,
        matchingValueNumber = NaN,
        percentValue = null;

    matchingField = getFieldUsingSettingItem(aResponseData, aSettingItemValue);

    if ((matchingField !== null) && (matchingField !== undefined)) {
        matchingValueNumber = Number(matchingField.fieldvalue);
        matchingMaximumNumber = Number(matchingField.fieldmaxvalue);
    }
    
    if ((isNaN(matchingMaximumNumber)) || (isNaN(matchingValueNumber))) {
        trace("Can't get percentage value because there's no value, or no max value");
        percentValue = null;
    } else {
        percentValue = (matchingValueNumber / matchingMaximumNumber) * 100;
        percentValue = Math.ceil(percentValue);
        if (percentValue > 100) {
            percentValue = 100;
        }
    }
    
    return percentValue;
}

// Utility function to search for a field and get the fieldValue
// Returns null if the field or value could not be found, otherwise it returns the value of the field
function getValueFromResponseData(aResponseData, aSettingItemValue) {
//--    trace(">> getValueFromResponseData");

    var matchingField,
        matchingValue = null;

    matchingField = getFieldUsingSettingItem(aResponseData, aSettingItemValue);

    if ((matchingField !== null) && (matchingField !== undefined)) {
        matchingValue = matchingField.fieldvalue;

        if (matchingValue === undefined) {
            matchingValue = null;
        }
    } else {
//--        trace("No matching field");
    }

//--    trace("<< getValueFromResponseData");

    return matchingValue;
}

var exports = {};

exports.name = "mmrsutils";

exports.doTrace = function(aTag, aText) { doTrace(aTag, aText); }
exports.doWarn  = function(aTag, aText) { doWarn (aTag, aText); }
exports.doError = function(aTag, aText) { doError(aTag, aText); }

exports.isIPhoneRun   = function() { return isIPhoneRun(); }
exports.isAndroidRun  = function() { return isAndroidRun(); }
exports.isNativeRun   = function() { return isNativeRun(); }

exports.getDeviceBridge = function() { return getDeviceBridge(); }

exports.copyData   = function(aObject) { return copyData(aObject); }

exports.contains   = function(aLine, aWord) { return contains(aLine, aWord); }

exports.setElement = function(aItemId, aContents) { setElement(aItemId, aContents); }

exports.getAngularScope = function(aAngularArea) { return getAngularScope(aAngularArea); }

exports.getOptions  = function(aUrl) { return getOptions(aUrl); }

exports.getElementPosition  = function(aElementId) { return getElementPosition(aElementId); }

exports.getScreenDimensions = function() { return getScreenDimensions(); }

exports.sendGetRequest  = function(aUrl, aRequestId, aHeaders, aCallback)        { return sendGetRequest (aUrl, aRequestId, aHeaders, aCallback); }
exports.sendPostRequest = function(aUrl, aRequestId, aHeaders, aData, aCallback) { return sendPostRequest(aUrl, aRequestId, aHeaders, aData, aCallback); }

exports.getValueFromResponseData = function(aResponseData, aSettingItemValue) {return getValueFromResponseData(aResponseData, aSettingItemValue); }

exports.getPercentValueFromResponseData = function(aResponseData, aSettingItemValue) {return getPercentValueFromResponseData(aResponseData, aSettingItemValue); }

exports.getFieldUsingSettingItem = function(aResponseData, aSettingItemValue) {return getFieldUsingSettingItem(aResponseData, aSettingItemValue); }

return exports;

}());
