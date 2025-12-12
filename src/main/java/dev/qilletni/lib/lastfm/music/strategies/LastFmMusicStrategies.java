package dev.qilletni.lib.lastfm.music.strategies;

import dev.qilletni.api.music.strategies.MusicStrategies;

import java.util.Optional;

public class LastFmMusicStrategies implements MusicStrategies<Object, Object> {

    @Override
    public Optional<SearchResolveStrategyProvider<Object, Object>> getSearchResolveStrategyProvider() {
        return Optional.empty();
    }
}
