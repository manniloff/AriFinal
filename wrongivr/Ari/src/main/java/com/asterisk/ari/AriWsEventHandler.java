
package com.asterisk.ari;

import ch.loway.oss.ari4java.generated.Message;
import ch.loway.oss.ari4java.tools.AriCallback;
import ch.loway.oss.ari4java.tools.RestException;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class AriWsEventHandler implements AriCallback<Message> {

    @Inject
    Logger logger;

    @Inject
    Event<Message> event;
    ExecutorService executorService = Executors.newFixedThreadPool(9);

/**
     * Minimum set of events that we should handle for start
     * StasisStart_impl_ari_3_0_0 ChannelStateChange_impl_ari_3_0_0
     * PlaybackStarted_impl_ari_3_0_0 PlaybackFinished_impl_ari_3_0_0
     * ChannelDtmfReceived_impl_ari_3_0_0 ChannelDtmfReceived_impl_ari_3_0_0
     * ChannelDtmfReceived_impl_ari_3_0_0 ChannelDtmfReceived_impl_ari_3_0_0
     * ChannelVarset_impl_ari_3_0_0 ChannelVarset_impl_ari_3_0_0
     * ChannelVarset_impl_ari_3_0_0 ChannelVarset_impl_ari_3_0_0
     * ChannelHangupRequest_impl_ari_3_0_0 StasisEnd_impl_ari_3_0_0
     */

    @Override
    public void onSuccess(final Message result) {
        executorService.execute(() -> {
            event.select(new AriEventBinding() {
                private static final long serialVersionUID = -8285796201390337743L;

                @Override
                public String eventType() {
                    return result.getClass().getSimpleName().split("_")[0].trim();
                }
            }).fire(result);
        });
    }


    @Override
    public void onFailure(RestException e) {
        logger.warn("ws onFailure; {}", e.getMessage());

    }
}

