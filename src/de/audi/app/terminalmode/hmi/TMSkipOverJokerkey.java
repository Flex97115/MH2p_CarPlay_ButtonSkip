/*
 * Copyright (c) 2026 Flex97115 (https://github.com/Flex97115)
 * This file is part of MH2p_CarPlay_ButtonSkip, licensed under CC BY-NC-SA 4.0.
 * https://creativecommons.org/licenses/by-nc-sa/4.0/
 * See the LICENSE file in the project root for full license text.
 * NOT FOR COMMERCIAL USE
 *
 * Replaces de.audi.app.terminalmode.hmi.TMSkipOverJokerkey from lsd.jxe
 * (CarPlayButtonSkip.jar is loaded before lsd.jxe on the boot classpath).
 *
 * On the Porsche Macan 95B.2 the steering-wheel programmable button set to "Next track"
 * in the INSTRUMENT CLUSTER is sent as a BAP audio command (DedicatedAudioControl skip)
 * to the audio source in focus. Radio/media have a cluster listener, CarPlay/Android Auto
 * (terminal mode) had none, so the command was dropped.
 *
 * This class registers a CombiBAPServiceTerminalModeListener on the cluster's
 * terminal-mode audio connector (taken from the OSGi bundle context, because
 * IServiceManager.getService() returns a logging decorator):
 *   - skip(+n): 1st press -> wait DOUBLE_TAP_WINDOW_MS; 2nd press in window -> previous
 *               track, otherwise -> next track
 *   - skip(-n): previous track
 *   - every other cluster request gets a "not successful / stopped" answer.
 *
 * Java 1.4 syntax (no generics) to match the unit's J9 VM (class file version 48).
 */
package de.audi.app.terminalmode.hmi;

import de.audi.app.bap.app.AbstractAppConnectorFSG;
import de.audi.app.terminalmode.ITerminalLogger;
import de.audi.app.terminalmode.ITerminalModeComponent;
import de.audi.app.terminalmode.osgi.IServiceManager;
import de.audi.app.terminalmode.osgi.IServiceTracker;
import de.audi.app.terminalmode.smartphone.IPlayerModificationListener;
import de.audi.app.terminalmode.smartphone.ISmartphoneProperties;
import de.audi.atip.interapp.combi.bap.audio.CombiBAPServiceTerminalMode;
import de.audi.atip.interapp.combi.bap.audio.CombiBAPServiceTerminalModeListener;
import de.audi.atip.timer.Timer;
import de.audi.atip.timer.TimerListener;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class TMSkipOverJokerkey implements ITerminalModeComponent {
    private static final String LOGCLASS = "TMSkipOverJokerkey";
    static final long DOUBLE_TAP_WINDOW_MS = 600L;
    private static final int LOG_INFO = 1000000;

    private final ITerminalLogger logger;
    private final IServiceManager serviceManager;
    private final ISmartphoneProperties smartphoneProperties;
    private final IPlayerModificationListener playerModificationListener;
    private volatile IServiceTracker clusterTracker;
    private final TapEngine tapEngine = new TapEngine();

    public TMSkipOverJokerkey(ITerminalLogger iTerminalLogger, IServiceManager iServiceManager,
                              ISmartphoneProperties iSmartphoneProperties,
                              IPlayerModificationListener iPlayerModificationListener) {
        this.serviceManager = iServiceManager;
        this.logger = iTerminalLogger;
        this.smartphoneProperties = iSmartphoneProperties;
        this.playerModificationListener = iPlayerModificationListener;
    }

    private static Object safeGet(de.audi.atip.utils.reactive.properties.Gettable g) {
        try { return g == null ? null : g.get(); } catch (Throwable t) { return "err:" + t; }
    }

    public void init() {
        log("init (CarPlayButtonSkip)");
        try {
            this.clusterTracker = this.serviceManager.createServiceTracker(CombiBAPServiceTerminalMode.class,
                    new ClusterServiceCustomizer());
            this.clusterTracker.open();
        } catch (Throwable t) {
            log("cluster tracker error: " + t);
        }
    }

    public void deinit() {
        if (null != this.clusterTracker) {
            this.clusterTracker.close();
            this.clusterTracker = null;
        }
        this.tapEngine.reset();
    }

    void log(String msg) {
        try {
            this.logger.hmi().log(LOG_INFO, "[" + LOGCLASS + "/CarPlayButtonSkip] " + msg);
        } catch (Throwable t) {
            // never let logging break key handling
        }
    }

    void sendSkip(boolean forward) {
        log("skipButton forward=" + forward + " focus=" + safeGet(smartphoneProperties.getPropertyAudiofocus()));
        try {
            playerModificationListener.skipButton(forward, false);
            playerModificationListener.skipButton(forward, true);
        } catch (Throwable t) {
            log("skipButton error: " + t);
        }
    }

    /** 1 press = next (after the window), 2 presses within the window = previous. */
    class TapEngine implements TimerListener {
        private final Timer tapTimer = new Timer("JokerSkipDoubleTap", DOUBLE_TAP_WINDOW_MS, true, this);
        private boolean tapPending = false;

        void press() {
            boolean doublePress = false;
            synchronized (this) {
                if (this.tapPending) {
                    this.tapPending = false;
                    this.tapTimer.cancel();
                    doublePress = true;
                } else {
                    this.tapPending = true;
                    this.tapTimer.restart();
                }
            }
            if (doublePress) {
                log("double tap -> previous");
                sendSkip(false);
            }
        }

        synchronized void reset() {
            this.tapPending = false;
            this.tapTimer.cancel();
        }

        public void fireTimer(Timer timer) {
            synchronized (this) {
                if (!this.tapPending) {
                    return; // second press already handled
                }
                this.tapPending = false;
            }
            log("single tap -> next");
            sendSkip(true);
        }

        public void cancelTimer(Timer timer) {
        }
    }

    /** Receives the cluster's audio commands while CarPlay / Android Auto is the audio source. */
    class ClusterAudioListener implements CombiBAPServiceTerminalModeListener {
        private final CombiBAPServiceTerminalMode service;

        ClusterAudioListener(CombiBAPServiceTerminalMode service) {
            this.service = service;
        }

        public void skip(int n) {
            log("cluster skip(" + n + ")");
            try {
                if (n > 0) {
                    tapEngine.press();
                } else if (n < 0) {
                    tapEngine.reset();
                    sendSkip(false);
                }
            } finally {
                try { service.skipResult(true); } catch (Throwable t) { log("skipResult error: " + t); }
            }
        }

        public void startSeek(int direction) {
            log("cluster startSeek(" + direction + ") -> not supported");
            try { service.updateSeekStatus(false); } catch (Throwable t) { log("seek error: " + t); }
        }

        public void cancelSeek() {
            log("cluster cancelSeek");
            try { service.updateSeekStatus(false); } catch (Throwable t) { log("seek error: " + t); }
        }

        public void setActiveSourceState(int stateInfo, int scope) {
            log("cluster setActiveSourceState(" + stateInfo + "," + scope + ") ignored");
        }

        public void selectListEntry(int n) { notSupportedList("selectListEntry", n); }
        public void selectListEntryPresetList(int n) { notSupportedList("selectListEntryPresetList", n); }
        public void selectListEntryFavoriteList(int n) { notSupportedList("selectListEntryFavoriteList", n); }
        public void selectListEntrySiriusAlertList(int n) { notSupportedList("selectListEntrySiriusAlertList", n); }
        public void selectListEntryLastStationList(int n) { notSupportedList("selectListEntryLastStationList", n); }

        private void notSupportedList(String what, int n) {
            log("cluster " + what + "(" + n + ") -> not supported");
            try { service.selectListEntryResult(1); } catch (Throwable t) { log("selectListEntryResult error: " + t); }
        }

        public void setPreferredList(int n) {
            log("cluster setPreferredList(" + n + ")");
            try { service.updatePreferredList(n); } catch (Throwable t) { log("updatePreferredList error: " + t); }
        }

        public void getNextListPos(int a, int b) {
            log("cluster getNextListPos(" + a + "," + b + ") -> not supported");
            try { service.getNextListPosResult(1, a, a, 0); } catch (Throwable t) { log("getNextListPosResult error: " + t); }
        }

        public void switchSource(int sourceType, int slot, int partition) {
            log("cluster switchSource(" + sourceType + "," + slot + "," + partition + ") -> not supported");
            try { service.switchSourceResult(1); } catch (Throwable t) { log("switchSourceResult error: " + t); }
        }

        public void activateSource(int n) {
            log("cluster activateSource(" + n + ") -> not supported");
            try { service.activateSourceResult(1); } catch (Throwable t) { log("activateSourceResult error: " + t); }
        }

        public void requestCoverArt(long id, int n) {
            log("cluster requestCoverArt(" + id + "," + n + ") ignored");
        }
    }

    private class ClusterServiceCustomizer implements ServiceTrackerCustomizer {
        private ClusterAudioListener listener;

        public final Object addingService(ServiceReference serviceReference) {
            // IServiceManager.getService() wraps the service in a logging decorator (v5 finding);
            // ask the OSGi bundle context directly to get the real AppConnectorTerminalMode.
            Object service = null;
            try {
                service = serviceManager.getBundleContext().getService(serviceReference);
            } catch (Throwable t) {
                log("bundleContext.getService error: " + t);
            }
            if (service == null) {
                service = serviceManager.getService(serviceReference);
            }
            log("cluster service object: " + service);
            if (!(service instanceof CombiBAPServiceTerminalMode) || !(service instanceof AbstractAppConnectorFSG)) {
                log("cluster service not usable: " + service);
                serviceManager.releaseService(serviceReference);
                return null;
            }
            AbstractAppConnectorFSG connector = (AbstractAppConnectorFSG) service;
            Object current = connector.getAppServiceListener();
            if (current != null) {
                log("cluster terminal-mode listener already set (" + current + "), leave it");
                return service;
            }
            this.listener = new ClusterAudioListener((CombiBAPServiceTerminalMode) service);
            connector.setAppServiceListener(this.listener);
            log("cluster terminal-mode listener registered");
            return service;
        }

        public void modifiedService(ServiceReference serviceReference, Object object) {
        }

        public void removedService(ServiceReference serviceReference, Object object) {
            try {
                if (object instanceof AbstractAppConnectorFSG && this.listener != null) {
                    AbstractAppConnectorFSG connector = (AbstractAppConnectorFSG) object;
                    if (connector.getAppServiceListener() == this.listener) {
                        connector.setAppServiceListener(null);
                    }
                }
            } catch (Throwable t) {
                log("cluster remove error: " + t);
            }
            this.listener = null;
            tapEngine.reset();
            serviceManager.releaseService(serviceReference);
            log("cluster terminal-mode listener removed");
        }
    }
}
