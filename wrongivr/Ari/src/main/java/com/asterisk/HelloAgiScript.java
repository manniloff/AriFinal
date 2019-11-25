package com.asterisk;

import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.BaseAgiScript;

public class HelloAgiScript extends BaseAgiScript
{
    public void service(AgiRequest request, AgiChannel channel)
            throws AgiException
    {
        // Answer the channel...
        answer();

        // ...say hello...
        streamFile("welcome");

        // ...and hangup.
        hangup();
    }
}
