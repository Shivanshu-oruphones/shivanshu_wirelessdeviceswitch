package com.pervacio.wds.app;

/**
 * Created by Billy on 22/02/16.
 *
 * This is a utility class for sending messages to the UI thread.
 *
 * Other threads call the required queuing function (e.g. "queueSetPairCodes") and this message
 * is sent to the UI thread with the corresponding receptor method being invoked on the UI thread
 * (e.g. "doSetPairCodes").
 *
 * The receptor class on the UI thread implements the "Observer" interface
 *
 */
public class MessengerWebBridgeSRP extends MessengerBase
{
    public interface Observer // Implemented on the UI thread
    {
        void onSRPLoaded(boolean aSuccess);

        void doSRPClientInitialise(String aUsername, String aPassword);
        void onSRPClientInitialiseResponse(String aSalt, String aClientPublicKey);

        void doSRPServerInitialise(String aSalt, String aUsername, String aPassword);
        void onSRPServerInitialiseResponse(String aServerPublicKey);

        void doSRPClientCreateProof(String aServerPublicKey);
        void onSRPClientCreateProofResponse(String aClientProof);

        void doSRPClientPerformServerProofCheck(String aServerProof);
        void onSRPClientPerformServerProofCheckResponse(boolean aSuccess, String aClientSharedKey);

        void doSRPServerPerformClientProofCheck(String aClientPublicKey, String aClientProof);
        void onSRPServerPerformClientProofCheckResponse(boolean aSucces, String aServerSharedKey, String aServerProof);
    }

    private final static int MESSAGE_ID_SRP_LOADED                          = 1;

    private final static int MESSAGE_ID_CLIENT_INIT                         = 10;
    private final static int MESSAGE_ID_CLIENT_INIT_RESPONSE                = 11;

    private final static int MESSAGE_ID_SERVER_INIT                         = 12;
    private final static int MESSAGE_ID_SERVER_INIT_RESPONSE                = 13;

    private final static int MESSAGE_ID_CLIENT_CREATE_PROOF                 = 20;
    private final static int MESSAGE_ID_CLIENT_CREATE_PROOF_RESPONSE        = 21;

    private final static int MESSAGE_ID_CLIENT_PERFORM_SERVER_PROOF_CHECK          = 22;
    private final static int MESSAGE_ID_CLIENT_PERFORM_SERVER_PROOF_CHECK_RESPONSE = 23;

    private final static int MESSAGE_ID_SERVER_PERFORM_CLIENT_PROOF_CHECK          = 30;
    private final static int MESSAGE_ID_SERVER_PERFORM_CLIENT_PROOF_CHECK_RESPONSE = 31;

    //private Handler         mHandler;
    private Observer        mObserver;

    public MessengerWebBridgeSRP(Observer aObserver)
    {
        mObserver = aObserver;
        //mHandler  = new Handler(Looper.getMainLooper());
    }

    //============================================================================================
    // Public Functions for Queuing Requests
    //============================================================================================
    //
    public void queueSRPLoaded(Boolean aSuccess)
    {
        queueMessage(MESSAGE_ID_SRP_LOADED, aSuccess);
    }

    public void queueDoSRPClientInitialise(String aUsername, String aPassword)
    {
        String[] payload = {aUsername, aPassword};
        queueMessage(MESSAGE_ID_CLIENT_INIT, payload);
    }

    public void queueOnSRPClientInitialiseResponse(final String aSalt, final String aClientPublicKey)
    {
        String[] payload = {aSalt, aClientPublicKey};
        queueMessage(MESSAGE_ID_CLIENT_INIT_RESPONSE, payload);
    }

    public void queueDoSRPServerInitialise(String aSalt, String aUsername, String aPassword)
    {
        String[] payload = {aSalt, aUsername, aPassword};
        queueMessage(MESSAGE_ID_SERVER_INIT, payload);
    }

    public void queueOnSRPServerInitialiseResponse(final String aServerPublicKey)
    {
        queueMessage(MESSAGE_ID_SERVER_INIT_RESPONSE, aServerPublicKey);
    }

    public void queueDoSRPClientCreateProof(String aServerPublicKey)
    {
        queueMessage(MESSAGE_ID_CLIENT_CREATE_PROOF, aServerPublicKey);
    }

    public void queueOnSRPClientCreateProofResponse(String aClientProof)
    {
        queueMessage(MESSAGE_ID_CLIENT_CREATE_PROOF_RESPONSE, aClientProof);
    }

    public void queueDoSRPClientPerformServerProofCheck(String aServerProof)
    {
        queueMessage(MESSAGE_ID_CLIENT_PERFORM_SERVER_PROOF_CHECK, aServerProof);
    }

    public void queueOnSRPClientPerformServerProofCheckResponse(Boolean aSuccess, String aClientSharedKey)
    {
        Object[] payload = {aSuccess, aClientSharedKey};
        queueMessage(MESSAGE_ID_CLIENT_PERFORM_SERVER_PROOF_CHECK_RESPONSE, payload);
    }

    public void queueDoSRPServerPerformClientProofCheck(String aClientPublicKey, String aClientProof)
    {
        String[] payload = {aClientPublicKey, aClientProof};
        queueMessage(MESSAGE_ID_SERVER_PERFORM_CLIENT_PROOF_CHECK, payload);
    }

    public void queueOnSRPServerPerformClientProofCheckResponse(Boolean aSuccess, String aServerSharedKey, String aServerProof)
    {
        Object[] payload = {aSuccess, aServerSharedKey, aServerProof};
        queueMessage(MESSAGE_ID_SERVER_PERFORM_CLIENT_PROOF_CHECK_RESPONSE, payload);
    }

    //============================================================================================
    // Send the message to the UI thread
    //============================================================================================
    //
    protected void processMessage(int aMessageId, Object aMessage)
    {
        switch (aMessageId)
        {
            case MESSAGE_ID_SRP_LOADED:
            {
                Boolean success = (Boolean) aMessage;
                mObserver.onSRPLoaded(success);
            }
            break;

            case MESSAGE_ID_CLIENT_INIT:
                {
                    String[] payload = (String[]) aMessage;
                    String username = payload[0];
                    String password = payload[1];

                    mObserver.doSRPClientInitialise(username, password);
                }
                break;

            case MESSAGE_ID_CLIENT_INIT_RESPONSE:
            {
                String[] payload = (String[]) aMessage;
                String salt = payload[0];
                String clientPublicKey = payload[1];

                mObserver.onSRPClientInitialiseResponse(salt, clientPublicKey);
            }
            break;

            case MESSAGE_ID_SERVER_INIT:
            {
                String[] payload = (String[]) aMessage;
                String salt     = payload[0];
                String username = payload[1];
                String password = payload[2];

                mObserver.doSRPServerInitialise(salt, username, password);
            }
            break;

            case MESSAGE_ID_SERVER_INIT_RESPONSE:
            {
                String serverPublicKey = (String) aMessage;
                mObserver.onSRPServerInitialiseResponse(serverPublicKey);
            }
            break;

            case MESSAGE_ID_CLIENT_CREATE_PROOF:
            {
                String serverPublicKey = (String) aMessage;
                mObserver.doSRPClientCreateProof(serverPublicKey);
            }
            break;

            case MESSAGE_ID_CLIENT_CREATE_PROOF_RESPONSE:
            {
                String clientProof = (String) aMessage;
                mObserver.onSRPClientCreateProofResponse(clientProof);
            }
            break;

            case MESSAGE_ID_CLIENT_PERFORM_SERVER_PROOF_CHECK:
            {
                String serverProof = (String) aMessage;
                mObserver.doSRPClientPerformServerProofCheck(serverProof);
            }
            break;

            case MESSAGE_ID_CLIENT_PERFORM_SERVER_PROOF_CHECK_RESPONSE:
            {
                Object[] payload = (Object[]) aMessage;
                Boolean success         = (Boolean) payload[0];
                String  clientSharedKey = (String)  payload[1];

                mObserver.onSRPClientPerformServerProofCheckResponse(success, clientSharedKey);
            }
            break;

            case MESSAGE_ID_SERVER_PERFORM_CLIENT_PROOF_CHECK:
            {
                String[] payload = (String[]) aMessage;
                String clientPublicKey  = payload[0];
                String clientProof      = payload[1];

                mObserver.doSRPServerPerformClientProofCheck(clientPublicKey, clientProof);
            }
            break;

            case MESSAGE_ID_SERVER_PERFORM_CLIENT_PROOF_CHECK_RESPONSE:
            {
                Object[] payload = (Object[]) aMessage;
                Boolean success         = (Boolean) payload[0];
                String  serverSharedKey = (String)  payload[1];
                String  serverProof     = (String)  payload[2];

                mObserver.onSRPServerPerformClientProofCheckResponse(success, serverSharedKey, serverProof);
            }
            break;

            default:
                error("processMessage, Bad Message Id: " +aMessageId);
                break;
        }
    }

    //==============================================================================================
    // Logging
    //==============================================================================================

    static String TAG = "MessengerWebBridgeSRP";
}
