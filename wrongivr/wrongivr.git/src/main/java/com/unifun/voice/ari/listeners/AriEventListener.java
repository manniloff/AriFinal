package com.unifun.voice.ari.listeners;

import ch.loway.oss.ari4java.generated.Message;
import ch.loway.oss.ari4java.tools.RestException;

public interface AriEventListener {
	void onEvent(Message message);

	void onFailedEvent(RestException e);
}
