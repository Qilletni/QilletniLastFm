package dev.qilletni.lib.lastfm;

import dev.qilletni.api.auth.ServiceProvider;
import dev.qilletni.api.exceptions.config.ConfigInitializeException;
import dev.qilletni.api.lib.persistence.PackageConfig;
import dev.qilletni.api.music.MusicCache;
import dev.qilletni.api.music.MusicFetcher;
import dev.qilletni.api.music.MusicTypeConverter;
import dev.qilletni.api.music.StringIdentifier;
import dev.qilletni.api.music.factories.AlbumTypeFactory;
import dev.qilletni.api.music.factories.CollectionTypeFactory;
import dev.qilletni.api.music.factories.SongTypeFactory;
import dev.qilletni.api.music.orchestration.TrackOrchestrator;
import dev.qilletni.api.music.play.PlayActor;
import dev.qilletni.api.music.strategies.MusicStrategies;
import dev.qilletni.lib.lastfm.database.HibernateUtil;
import dev.qilletni.lib.lastfm.music.LastFmMusicTypeConverter;
import dev.qilletni.lib.lastfm.music.LastFmMusicCache;
import dev.qilletni.lib.lastfm.music.LastFmMusicFetcher;
import dev.qilletni.lib.lastfm.music.LastFmStringIdentifier;
import dev.qilletni.lib.lastfm.music.auth.LastFmAuthorizer;
import dev.qilletni.lib.lastfm.music.play.ReroutablePlayActor;
import dev.qilletni.lib.lastfm.music.strategies.LastFmMusicStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class LastFmServiceProvider implements ServiceProvider {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LastFmServiceProvider.class);

    private PackageConfig packageConfig;
    private LastFmMusicCache musicCache;
    private MusicFetcher musicFetcher;
    private TrackOrchestrator trackOrchestrator;
    private MusicTypeConverter musicTypeConverter;
    private StringIdentifier stringIdentifier;
    private LastFmAuthorizer authorizer;
    private LastFmMusicStrategies musicStrategies;
    private PlayActor playActor;

    private static ServiceProvider serviceProviderInstance;

    @Override
    public CompletableFuture<Void> initialize(BiFunction<PlayActor, MusicCache, TrackOrchestrator> defaultTrackOrchestratorFunction, PackageConfig packageConfig) {
        this.packageConfig = packageConfig;
        initConfig();

        authorizer = new LastFmAuthorizer(packageConfig, packageConfig.getOrThrow("apiKey"), packageConfig.getOrThrow("apiSecret"));

        return authorizer.authorizeLastFm().thenAccept(lastFmAPI -> {
            var lastFmMusicFetcher = new LastFmMusicFetcher();
            musicFetcher = lastFmMusicFetcher;
            musicCache = new LastFmMusicCache(lastFmMusicFetcher);
            playActor = new ReroutablePlayActor();
            trackOrchestrator = defaultTrackOrchestratorFunction.apply(playActor, musicCache);
            
            musicTypeConverter = new LastFmMusicTypeConverter(musicCache);
            musicStrategies = new LastFmMusicStrategies();

            serviceProviderInstance = this;
        });
    }

    @Override
    public void shutdown() {
    }

    @Override
    public String getName() {
        return "LastFm";
    }

    @Override
    public MusicCache getMusicCache() {
        return Objects.requireNonNull(musicCache, "ServiceProvider#initialize must be invoked to initialize MusicCache");
    }

    @Override
    public MusicFetcher getMusicFetcher() {
        return Objects.requireNonNull(musicFetcher, "ServiceProvider#initialize must be invoked to initialize MusicFetcher");
    }

    @Override
    public TrackOrchestrator getTrackOrchestrator() {
        return Objects.requireNonNull(trackOrchestrator, "ServiceProvider#initialize must be invoked to initialize TrackOrchestrator");
    }

    @Override
    public MusicStrategies<?, ?> getMusicStrategies() {
        return Objects.requireNonNull(musicStrategies, "ServiceProvider#initialize must be invoked to initialize MusicStrategies");
    }

    @Override
    public MusicTypeConverter getMusicTypeConverter() {
        return Objects.requireNonNull(musicTypeConverter, "ServiceProvider#initialize must be invoked to initialize MusicTypeConverter");
    }

    @Override
    public StringIdentifier getStringIdentifier(SongTypeFactory songTypeFactory, CollectionTypeFactory collectionTypeFactory, AlbumTypeFactory albumTypeFactory) {
        if (stringIdentifier == null) {
            return stringIdentifier = new LastFmStringIdentifier(musicCache, songTypeFactory, collectionTypeFactory, albumTypeFactory);
        }

        return stringIdentifier;
    }

    @Override
    public PlayActor getPlayActor() {
        return Objects.requireNonNull(playActor, "ServiceProvider#initialize must be invoked to initialize PlayActor");
    }

    private void initConfig() {
        packageConfig.loadConfig();

        var requiredOptions = List.of("apiKey", "apiSecret", "dbUrl", "dbUsername", "dbPassword");
        var allFound = true;

        for (var option : requiredOptions) {
            if (packageConfig.get(option).isEmpty()) {
                allFound = false;
                LOGGER.error("Required config value '{}' not found in Last.Fm config", option);
            }
        }

        if (!allFound) {
            throw new ConfigInitializeException("Last.Fm config is missing required options, aborting");
        }

        HibernateUtil.initializeSessionFactory(packageConfig.getOrThrow("dbUrl"), packageConfig.getOrThrow("dbUsername"), packageConfig.getOrThrow("dbPassword"));
    }

    public static ServiceProvider getServiceProviderInstance() {
        Objects.requireNonNull(serviceProviderInstance, "ServiceProvider#initialize must be invoked to initialize ServiceProvider");
        return serviceProviderInstance;
    }

}
