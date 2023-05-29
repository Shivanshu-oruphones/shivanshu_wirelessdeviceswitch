package com.pervacio.wds.app;

import android.os.Build;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by Billy on 06/04/2017.
 */

public class SRPBridgeWebView implements SRPBridge {
    private static final String kSRPLaunchUrl = "file:///android_asset/webview/mmrssrp.html";

    private WebView mWebView;
    private SRPBridge.Observer mObserver;

    MessengerWebBridgeSRP mMessenger;

    public SRPBridgeWebView(WebView aWebView, SRPBridge.Observer aObserver) {
        mWebView = aWebView;
        mObserver = aObserver;

        mMessenger = new MessengerWebBridgeSRP(new IncomingMessages());

        initialiseWebView(mWebView, kSRPLaunchUrl);
    }

    //==============================================================================================
    // Public Interface
    //==============================================================================================
    //
    @Override
    public void srpClientInitialise(String aUsername, String aPassword) {
        mMessenger.queueDoSRPClientInitialise(aUsername, aPassword);
    }

    @Override
    public void srpServerInitialise(String aSalt, String aUsername, String aPassword) {
        mMessenger.queueDoSRPServerInitialise(aSalt, aUsername, aPassword);
    }

    @Override
    public void srpClientCreateProof(String aServerPublicKey) {
        mMessenger.queueDoSRPClientCreateProof(aServerPublicKey);
    }

    @Override
    public void srpServerPerformClientProofCheck(String aClientPublicKey, String aClientProof) {
        mMessenger.queueDoSRPServerPerformClientProofCheck(aClientPublicKey, aClientProof);
    }

    @Override
    public void srpClientPerformServerProofCheck(String aServerProof) {
        mMessenger.queueDoSRPClientPerformServerProofCheck(aServerProof);
    }


    //==============================================================================================
    // Web View Initialisation
    //==============================================================================================
    //
    private WebView initialiseWebView(WebView aWebView, String aUrl) {
        trace("initialiseWebView, Url: " + aUrl);

        aWebView.clearCache(true);

        //aWebView.setBackgroundColor(Color.TRANSPARENT);
        //aWebView.setVisibility(aWebView.VISIBLE);

        WebSettings settings = aWebView.getSettings();
        settings.setJavaScriptEnabled(true);

        WebViewClient webClient = new SRPWebViewClient();
        aWebView.setWebViewClient(webClient);

        WebViewHandler webHandler = new WebViewHandler();

        aWebView.addJavascriptInterface(webHandler, "MMRSNATIVE");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            aWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        }

        aWebView.loadUrl(aUrl);

        return aWebView;
    }


    //==============================================================================================
    // Send Commands to the WebView
    //==============================================================================================
    //
    private void callJsFunction(String aCall) {
        // log("callJsFunction, Call: " + aCall); // Removed trace, because aCall can be very big

        if (mWebView == null) {
            error("callJsFunction, WebView NOT initialised");
            return;
        }

        mWebView.loadUrl("javascript:" + aCall);
    }

    //==============================================================================================
    // Handle Incoming Messages from Other Threads and Send to the WebView
    //==============================================================================================
    //
    class IncomingMessages implements MessengerWebBridgeSRP.Observer {
        @Override
        public void onSRPLoaded(boolean aSuccess) {
            mObserver.onSRPLoaded();
        }

        @Override
        public void doSRPClientInitialise(String aUsername, String aPassword) {
            //trace("doSRPClientInitialise, User: " +aUsername+ ", Pass: " +aPassword);
            callJsFunction("MMRSSRP.doSRPClientInitialise('" + aUsername + "','" + aPassword + "')");
        }

        @Override
        public void onSRPClientInitialiseResponse(String aSalt, String aClientPublicKey) {
            //trace("onSRPClientInitialiseResponse, Salt: " +aSalt+ ", Client Pub Key: " +aClientPublicKey);
            mObserver.onSRPClientInitialiseResponse(aSalt, aClientPublicKey);
        }

        @Override
        public void doSRPServerInitialise(String aSalt, String aUsername, String aPassword) {
            //trace("doSRPInitServer, User: " +aUsername+ ", Pass: " +aPassword);
            callJsFunction("MMRSSRP.doSRPServerInitialise('" + aSalt + "','" + aUsername + "','" + aPassword + "')");
        }

        @Override
        public void onSRPServerInitialiseResponse(String aServerPublicKey) {
            //trace("onSRPClientInitialiseResponse, Server Pub Key: " +aServerPublicKey);
            mObserver.onSRPServerInitialiseResponse(aServerPublicKey);
        }

        @Override
        public void doSRPClientCreateProof(String aServerPublicKey) {
            //trace("doSRPClientCreateProof, Server Key: " +aServerPublicKey);
            callJsFunction("MMRSSRP.doSRPClientCreateProof('" + aServerPublicKey + "')");
        }

        @Override
        public void onSRPClientCreateProofResponse(String aClientProof) {
            //trace("onSRPClientCreateProofResponse, Client Proof: " +aClientProof);
            mObserver.onSRPClientCreateProofResponse(aClientProof);
        }

        @Override
        public void doSRPClientPerformServerProofCheck(String aServerProof) {
            //trace("doSRPClientPerformServerProofCheck");
            callJsFunction("MMRSSRP.doSRPClientPerformServerProofCheck('" + aServerProof + "')");
        }

        @Override
        public void onSRPClientPerformServerProofCheckResponse(boolean aSuccess, String aClientSharedKey) {
            //trace("onSRPClientPerformServerProofCheckResponse, Success: " +aSuccess+ ", Client Shared Key: " +aClientSharedKey);
            mObserver.onSRPClientPerformServerProofCheckResponse(aSuccess, aClientSharedKey);
        }

        @Override
        public void doSRPServerPerformClientProofCheck(String aClientPublicKey, String aClientProof) {
            //trace("doSRPServerPerformClientProofCheck");
            callJsFunction("MMRSSRP.doSRPServerPerformClientProofCheck('" + aClientPublicKey + "','" + aClientProof + "')");
        }

        @Override
        public void onSRPServerPerformClientProofCheckResponse(boolean aSuccess, String aServerSharedKey, String aServerProof) {
            //trace("onSRPServerPerformClientProofCheckResponse, Success: " +aSuccess+ ", Server Shared Key: " +aServerSharedKey+ ", Server Proof: " +aServerProof);
            mObserver.onSRPServerPerformClientProofCheckResponse(aSuccess, aServerSharedKey, aServerProof);
        }
    }

    //==============================================================================================
    // Handle Commands from the WebView and send via the messenger to get them on the proper thread
    //==============================================================================================
    //
    class WebViewHandler {
        @android.webkit.JavascriptInterface
        public String onSRPClientInitialiseResponse(final String aSalt, final String aClientPublicKey) {
            log("onSRPClientInitialiseResponse, Salt: " + aSalt + ", Client Public Key: " + aClientPublicKey);
            mMessenger.queueOnSRPClientInitialiseResponse(aSalt, aClientPublicKey);
            String result = "";
            return result;
        }

        @android.webkit.JavascriptInterface
        public String onSRPServerInitialiseResponse(final String aServerPublicKey) {
            log("onSRPServerInitialiseResponse, Server Public Key: " + aServerPublicKey);
            mMessenger.queueOnSRPServerInitialiseResponse(aServerPublicKey);
            String result = "";
            return result;
        }

        @android.webkit.JavascriptInterface
        public String onSRPClientCreateProofResponse(final String aClientProof) {
            log("onSRPClientCreateProofResponse, Client Proof: " + aClientProof);
            mMessenger.queueOnSRPClientCreateProofResponse(aClientProof);
            String result = "";
            return result;
        }

        @android.webkit.JavascriptInterface
        public String onSRPServerPerformClientProofCheckResponse(String aSuccess, String aServerSharedKey, String aServerProof) {
            log("onSRPServerPerformClientProofCheckResponse, Success: " + aSuccess + ", Shared Key: " + aServerSharedKey + ", Server Proof: " + aServerProof);

            Boolean success = aSuccess.equals("1");

            mMessenger.queueOnSRPServerPerformClientProofCheckResponse(success, aServerSharedKey, aServerProof);
            String result = "";
            return result;
        }

        @android.webkit.JavascriptInterface
        public String onSRPClientPerformServerProofCheckResponse(String aSuccess, String aClientSharedKey) {
            log("onSRPClientPerformServerProofCheckResponse, aSuccess: " + aSuccess + ", Shared Key: " + aClientSharedKey);

            Boolean success = aSuccess.equals("1");

            mMessenger.queueOnSRPClientPerformServerProofCheckResponse(success, aClientSharedKey);
            String result = "";
            return result;
        }


        @android.webkit.JavascriptInterface
        public void itrace(final String aText) {
            log("ITRACE: " + aText);
        }

        @android.webkit.JavascriptInterface
        public void iwarn(final String aText) {
            warn("IWARN: " + aText);
        }
    }


    //==============================================================================================
    // Control the WebView's Page Loading
    //==============================================================================================
    //
    class SRPWebViewClient extends WebViewClient {
        // The javascript send a page request to indicate its screen is setup
        //
        // Using a URL is a simpler way to receive communication from the webview
        // than the alternative of using a webview handler.
        //
        public boolean shouldOverrideUrlLoading(WebView aWebView, String aUrl) {
//--            log(">> SRPWebViewClient, shouldOverrideUrlLoading: " + aUrl);
            //aWebView.loadUrl(aUrl);

            // The webview progress view has loaded and is ready to updates
            // It may queue these until the screen is in place
            //
            if (aUrl.equals("cmd://ready")) {
//--                log("<< SRPWebViewClient, true");
                return true;
            }

//--            log("<< SRPWebViewClient, false");
            return false;
        }

        public void onPageFinished(WebView aWebView, String aUrl) {
            //log("SRPWebViewClient, onPageFinished: " + aUrl);

            mMessenger.queueSRPLoaded(true);
        }

        public void onReceivedError(WebView aWebView, int aErrorCode, String aDescription, String aFailingUrl) {
            error("SRPWebViewClient, onReceivedError: " + aDescription);
        }
    }


    //==============================================================================================
    // Logging
    //==============================================================================================

    private static String TAG = "SRPBridgeWebView";

    private static void trace(String aMessage) {
        Log.v(TAG, aMessage);
    }

    private static void log(String aMessage) {
        Log.d(TAG, aMessage);
    }

    private static void warn(String aMessage) {
        Log.w(TAG, aMessage);
    }

    private static void error(String aMessage) {
        Log.e(TAG, aMessage);
    }
}
