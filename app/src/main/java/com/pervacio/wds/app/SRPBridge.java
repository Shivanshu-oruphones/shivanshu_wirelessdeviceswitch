package com.pervacio.wds.app;


/**
 * Created by Billy on 28/02/16.
 */

public interface SRPBridge
{
    // For the sake of speed both the client side and server side methods are implemented
    // in this same interface.
    //
    // The class that implements the Observer will need to provide stubbed methods for those
    // it does not need to implement.
    //
    // I.e. If implementing the Client side, the implementation will need to prvide bodies
    // for onSRPServerInitialiseResponse(), and onSRPServerPerformClientProofCheckResponse()
    // even though these will never be called/

    interface Observer
    {
        // Client and Server must wait until they receive the onSRPLoad callback before initialising
        //
        void onSRPLoaded();

        //Client method callbacks
        //
        void onSRPClientInitialiseResponse(String aSalt, String aClientPublicKey);

        void onSRPClientCreateProofResponse(String aClientProof);

        void onSRPClientPerformServerProofCheckResponse(boolean aSuccess, String aClientSharedKey);

        //Server method callbacks
        //
        void onSRPServerInitialiseResponse(String aServerPublicKey);

        void onSRPServerPerformClientProofCheckResponse(boolean aSuccess, String aServerSharedKey, String aServerProof);

    }

    // Client methods
    //
    void srpClientInitialise(String aUsername, String aPassword);

    void srpClientCreateProof(String aServerPublicKey);

    void srpClientPerformServerProofCheck(String aServerProof);


    //Server method callbacks
    //
    void srpServerInitialise(String aSalt, String aUsername, String aPassword);

    void srpServerPerformClientProofCheck(String aClientPublicKey, String aClientProof);



}
