
package com.example.demo.service;

import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DemoService {

    private static final Logger logger = LoggerFactory.getLogger(DemoService.class);

    private static final Gauge adsCountGauge = Gauge.build()
            .name("demo_ads_count")
            .help("Current ads count.")
            .register();

    private static final AtomicInteger adsCount = new AtomicInteger(10);
    private static final Random random = new Random();
    private static final String STATE_FILE = "service_state.json";
    private static Timer timer;
    private static Timer errorTimer;

    private static HTTPServer metricsServer;

    @PostConstruct
    public void init() throws IOException {
        DefaultExports.initialize();
        metricsServer = new HTTPServer(1234);

        restoreState();

        scheduleRandomAdsTask();
        scheduleErrorInduction();

        adsCountGauge.set(adsCount.get());
    }

    private void scheduleRandomAdsTask() {
        int delay = random.nextInt(30000); // random between 0 to 30 seconds
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                int currentAds = random.nextInt(11);
                adsCount.set(currentAds);
                adsCountGauge.set(currentAds);
                logger.info("Ads count set to random value: {}", currentAds);

                saveState();

                scheduleRandomAdsTask();
            }
        }, delay);
    }

    private void scheduleErrorInduction() {
        errorTimer = new Timer();
        errorTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.error("*** Inducing error as scheduled after 50 minutes ***");
                System.exit(1);
            }
        }, 50 * 60 * 1000); // 50 mins error
    }

    private static void saveState() {
        try (FileWriter writer = new FileWriter(STATE_FILE)) {
            Gson gson = new Gson();
            State state = new State(adsCount.get());
            gson.toJson(state, writer);
        } catch (IOException e) {
            logger.error("Failed to save state", e);
        }
    }

    private void restoreState() {
        try (FileReader reader = new FileReader(STATE_FILE)) {
            Gson gson = new Gson();
            Type type = new TypeToken<State>() {}.getType();
            State state = gson.fromJson(reader, type);
            adsCount.set(state.getAdsCount());
        } catch (IOException e) {
            adsCount.set(random.nextInt(11));
            logger.warn("Failed to restore state, setting ads count to a random value");
        }
    }

    static class State {
        private int adsCount;

        public State(int adsCount) {
            this.adsCount = adsCount;
        }

        public int getAdsCount() {
            return adsCount;
        }
    }

    @PreDestroy
    public void onShutdown() {
        logger.info("Saving state on shutdown...");
        saveState();
        if (metricsServer != null) {
            metricsServer.stop();
        }
        if (timer != null) {
            timer.cancel();
        }
        if (errorTimer != null) {
            errorTimer.cancel();
        }
    }

    public static int getAdsCount() {
        return adsCount.get();
    }

    public static void setAdsCount(int count) {
        adsCount.set(count);
        adsCountGauge.set(count);
        saveState();
    }
}
