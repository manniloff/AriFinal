package com.asterisk.ari;

import ch.loway.oss.ari4java.ARI;
import ch.loway.oss.ari4java.AriFactory;
import ch.loway.oss.ari4java.generated.ActionEvents;
import ch.loway.oss.ari4java.tools.ARIException;
import com.asterisk.model.AriSettings;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import lombok.Data;
import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@ApplicationScoped
public class AriManager {

    Map<String, ARI> ariInterfaces = new ConcurrentHashMap<>();
    @Inject
    Logger logger;

    ARI ari;
    @Inject
    AriWsEventHandler ariHandler;
    List<AriSettings> ariSettings;


    void init(@Observes StartupEvent event) {
        logger.info("Initiating ARI...");
        logger.info("Retrieve settings from data base...");
        logger.info("Initialisation of tmpDatabase... ");
        ariSettings = getList();
        ariSettings.forEach(this::setupARI);
        //tmpDatabase.initializeTmpDatabase();
    }

    void stop(@Observes(during = TransactionPhase.BEFORE_COMPLETION) ShutdownEvent event) {
        logger.info("The application is stopping...");
        ariInterfaces.forEach((s, ari1) -> {
            try {
                close(ari1);
            } catch (ARIException e) {
                logger.warn("Something went wrong while closing ws connection", e);
            }
        });
    }

    private void close(ARI ari) throws ARIException {
        // TODO find how to properly deallocate Asterisk REST interface
        ARI.sleep(500);
        ari.cleanup();
    }

    private ARI buildAri(AriSettings settings) throws ARIException, URISyntaxException {
        return AriFactory.nettyHttp(settings.getAriUrl(), settings.getUser(), settings.getPassword(), settings.getAriVersion(), settings.getApplicationName());
    }

    private void setupARI(AriSettings ariSettings) {
        try {
            logger.info("Set up new ARI interface {} with url {} and ari_version {}", ariSettings.getApplicationName(),
                    ariSettings.getAriUrl(), ariSettings.getAriVersion());
            ari = buildAri(ariSettings);
            ariInterfaces.put(ariSettings.getApplicationName(), ari);
            initWebSocketConnection(ari);

        } catch (Exception e) {
            logger.warn("Unable to obtain ari instance: {}", Arrays.toString(e.getStackTrace()));
        }
    }

    protected List<AriSettings> getList() {
        List<AriSettings> ariSettings = new ArrayList<>();
        ariSettings.add(new AriSettings("http://192.168.1.11:8088/", "unifun", "unifun", "Unifun-ARI",ch.loway.oss.ari4java.AriVersion.ARI_4_0_0));
        return ariSettings;
    }

    private void initWebSocketConnection(ARI ari) throws ARIException {
        logger.info("Connecting as ari application {}", ari.getAppName());
        ari.getActionImpl(ActionEvents.class).eventWebsocket(ari.getAppName(), false, ariHandler);
    }
}
