package dev.qilletni.lib.lastfm;

import dev.qilletni.api.lang.types.AlbumType;
import dev.qilletni.api.lang.types.EntityType;
import dev.qilletni.api.lang.types.JavaType;
import dev.qilletni.api.lang.types.SongType;
import dev.qilletni.api.lang.types.conversion.TypeConverter;
import dev.qilletni.api.lang.types.entity.EntityInitializer;
import dev.qilletni.api.music.factories.AlbumTypeFactory;
import dev.qilletni.api.music.factories.SongTypeFactory;
import dev.qilletni.lib.lastfm.music.LastFmMusicFetcher;
import dev.qilletni.lib.lastfm.music.api.LastFmAPI;
import dev.qilletni.lib.lastfm.music.api.Page;
import dev.qilletni.lib.lastfm.music.api.Period;
import dev.qilletni.lib.lastfm.music.api.responses.DateRange;
import dev.qilletni.lib.lastfm.music.api.responses.LastFmResponse;
import dev.qilletni.lib.lastfm.music.api.responses.reusable.UserResponseAttr;

import java.time.LocalDate;
import java.util.Optional;

public class LastFmFunctions {
    
    private final LastFmAPI lastFmAPI;
    private final EntityInitializer entityInitializer;
    private final SongTypeFactory songTypeFactory;
    private final AlbumTypeFactory albumTypeFactory;
    private final TypeConverter typeConverter;

    public LastFmFunctions(EntityInitializer entityInitializer, SongTypeFactory songTypeFactory, AlbumTypeFactory albumTypeFactory, TypeConverter typeConverter) {
        this.entityInitializer = entityInitializer;
        this.songTypeFactory = songTypeFactory;
        this.albumTypeFactory = albumTypeFactory;
        this.typeConverter = typeConverter;
        this.lastFmAPI = LastFmAPI.getInstance();
    }

    public EntityType getFriends(String user) {
        var friendsResponse = lastFmAPI.getFriends(user, new Page()).join();
        if (friendsResponse.isError()) {
            return createErrorResult(friendsResponse);
        }

        var friends = friendsResponse.getResponse().friends();
        
        var userList = friends.user().stream().map(friend -> entityInitializer.initializeEntity("User", friend.url(), friend.name())).toList();
        return createResult(userList, null, friends.attr());
    }

    public EntityType getFriends(String user, EntityType pageEntity) {
        var page = createPageFromEntity(pageEntity);
        var friendsAPIResponse = lastFmAPI.getFriends(user, page).join();
        if (friendsAPIResponse.isError()) {
            return createErrorResult(friendsAPIResponse);
        }
        
        var friends = friendsAPIResponse.getResponse().friends();

        var userList = friends.user().stream().map(friend -> entityInitializer.initializeEntity("User", friend.url(), friend.name())).toList();
        return createResult(userList, null, friends.attr());
    }

    public void getUser(String user) {
//        lastFmAPI.getUserInfo() // TODO
    }

    public EntityType getLovedTracks(String user) {
        return fetchLovedTracks(user, new Page());
    }

    public EntityType getLovedTracks(String user, EntityType pageEntity) {
        return fetchLovedTracks(user, createPageFromEntity(pageEntity));
    }
    
    private EntityType fetchLovedTracks(String user, Page page) {
        var lovedTracksResponse = lastFmAPI.getLovedTracks(user, page).join();
        if (lovedTracksResponse.isError()) {
            return createErrorResult(lovedTracksResponse);
        }

        var lovedTracks = lovedTracksResponse.getResponse().lovedtracks();

        var lovedTrackListData = lovedTracks.track().stream()
                .map(lovedTrackResponse -> {
                    var track = LastFmMusicFetcher.createTrack(lovedTrackResponse);
                    SongType song = songTypeFactory.createSongFromTrack(track);
                    long dateUts = -1;
                    var dateText = "";

                    if (lovedTrackResponse.date() != null) {
                        dateUts = lovedTrackResponse.date().uts();
                        dateText = lovedTrackResponse.date().text();
                    }

                    return new LovedTrackWrapper(song, dateUts, dateText);
                }).toList();

        var lovedTrackList = lovedTrackListData.stream()
                .map(LovedTrackWrapper::track)
                .toList();

        var wrappedLovedTrackList = lovedTrackListData.stream()
                .map(lovedTrack -> typeConverter.convertFromRecordToEntity("LovedTrack", lovedTrack))
                .toList();
        
        return createResult(lovedTrackList, wrappedLovedTrackList, lovedTracks.attr());
    }

    public EntityType getRecentTracks(String user) {
        return fetchRecentTracks(user, new Page(), new DateRange());
    }

    public EntityType getRecentTracks(String user, EntityType pageEntity, EntityType dateRangeEntity) {
        var page = createPageFromEntity(pageEntity);
        var dateRange = createDateRangeFromEntity(dateRangeEntity);
        
        return fetchRecentTracks(user, page, dateRange);
    }

    private EntityType fetchRecentTracks(String user, Page page, DateRange dateRange) {
        var recentTracksResponse = lastFmAPI.getRecentTracks(user, false, dateRange, page).join();
        if (recentTracksResponse.isError()) {
            return createErrorResult(recentTracksResponse);
        }

        var recentTracks = recentTracksResponse.getResponse().recenttracks();

        // TODO: These don't contain the album, should they be fetched manually individually?
        //       that seems too intense for just getting the album
        
        var recentTrackListData = recentTracks.track().stream()
                .map(recentTrackResponse -> {
                            var track = LastFmMusicFetcher.createTrack(recentTrackResponse);
                            SongType song = songTypeFactory.createSongFromTrack(track);
                            boolean nowPlaying = false;
                            long dateUts = -1;
                            var dateText = "";
                            
                            if (recentTrackResponse.attr() != null) {
                                nowPlaying = recentTrackResponse.attr().nowPlaying();
                            }
                            
                            if (recentTrackResponse.date() != null) {
                                dateUts = recentTrackResponse.date().uts();
                                dateText = recentTrackResponse.date().text();
                            }
                            
                            return new RecentTrackWrapper(song, nowPlaying, dateUts, dateText);
                        }).toList();

        var recentTrackList = recentTrackListData.stream()
                .map(RecentTrackWrapper::track)
                .toList();

        var wrappedRecentTrackList = recentTrackListData.stream()
                .map(topTrack -> typeConverter.convertFromRecordToEntity("RecentTrack", topTrack))
                .toList();
        
        return createResult(recentTrackList, wrappedRecentTrackList, recentTracks.attr());
    }

    public EntityType getTopAlbums(String user) {
        return fetchTopAlbums(user, Period.UNSET, new Page());
    }

    public EntityType getTopAlbums(String user, String periodString, EntityType pageEntity) {
        var page = createPageFromEntity(pageEntity);
        var period = Period.fromString(periodString);

        return fetchTopAlbums(user, period, page);
    }
    
    public EntityType fetchTopAlbums(String user, Period period, Page pageEntity) {
        var topAlbumsResponse = lastFmAPI.getTopAlbums(user, period, pageEntity).join();
        if (topAlbumsResponse.isError()) {
            return createErrorResult(topAlbumsResponse);
        }
        
        var topAlbums = topAlbumsResponse.getResponse().topalbums();
        
        var topAlbumListData = topAlbums.album().stream()
                .map(topAlbumResponse -> {
                    var album = LastFmMusicFetcher.createAlbum(topAlbumResponse);
                    AlbumType albumType = albumTypeFactory.createAlbumFromTrack(album);
                    var playCount = -1;
                    if (topAlbumResponse.playcount() != null) {
                        playCount = topAlbumResponse.playcount();
                    }
                    
                    var rank = -1;
                    if (topAlbumResponse.attr() != null) {
                        rank = topAlbumResponse.attr().rank();
                    }
                    
                    return new TopAlbumWrapper(albumType, playCount, rank);
                })
                .toList();
        
        var topAlbumList = topAlbumListData.stream()
                .map(TopAlbumWrapper::album)
                .toList();
        
        var wrappedTopAlbumList = topAlbumListData.stream()
                .map(topAlbum -> typeConverter.convertFromRecordToEntity("TopAlbum", topAlbum))
                .toList();
        
        return createResult(topAlbumList, wrappedTopAlbumList, topAlbums.attr());
    }

    public EntityType getTopArtists(String user) {
        return fetchTopArtists(user, Period.UNSET, new Page());
    }

    public EntityType getTopArtists(String user, String periodString, EntityType pageEntity) {
        var page = createPageFromEntity(pageEntity);
        var period = Period.fromString(periodString);
        
        return fetchTopArtists(user, period, page);
    }
    
    public EntityType fetchTopArtists(String user, Period period, Page pageEntity) {
        var topArtistsResponse = lastFmAPI.getTopArtists(user, period, pageEntity).join();
        if (topArtistsResponse.isError()) {
            return createErrorResult(topArtistsResponse);
        }

        var topArtists = topArtistsResponse.getResponse().topartists();

        var topArtistListData = topArtists.artist().stream()
                .map(topArtistResponse -> {
                    var artist = LastFmMusicFetcher.createArtist(topArtistResponse);
                    var artistType = entityInitializer.initializeEntity("Artist", artist.getId(), artist.getName());
                    
                    var playCount = -1;
                    if (topArtistResponse.playcount() != null) {
                        playCount = topArtistResponse.playcount();
                    }

                    var rank = -1;
                    if (topArtistResponse.attr() != null) {
                        rank = topArtistResponse.attr().rank();
                    }
                    
                    return new TopArtistWrapper(artistType, playCount, rank);
                })
                .toList();

        var topArtistList = topArtistListData.stream()
                .map(TopArtistWrapper::artist)
                .toList();

        var wrappedTopArtistList = topArtistListData.stream()
                .map(topArtist -> typeConverter.convertFromRecordToEntity("TopArtist", topArtist))
                .toList();

        return createResult(topArtistList, wrappedTopArtistList, topArtists.attr());
    }

    public EntityType getTopTracks(String user) {
        return fetchTopTracks(user, Period.UNSET, new Page());
    }

    public EntityType getTopTracks(String user, String periodString, EntityType pageEntity) {
        var page = createPageFromEntity(pageEntity);
        var period = Period.fromString(periodString);
        
        return fetchTopTracks(user, period, page);
    }

    private EntityType fetchTopTracks(String user, Period period, Page pageEntity) {
        var topTracksResponse = lastFmAPI.getTopTracks(user, period, pageEntity).join();
        if (topTracksResponse.isError()) {
            return createErrorResult(topTracksResponse);
        }

        var topTracks = topTracksResponse.getResponse().toptracks();
        
        var topTrackListData = topTracks.track().stream()
                .map(topTrackResponse -> {
                    var track = LastFmMusicFetcher.createTrack(topTrackResponse);
                    SongType song = songTypeFactory.createSongFromTrack(track);
                    var playCount = -1;
                    if (topTrackResponse.playcount() != null) {
                        playCount = topTrackResponse.playcount();
                    }
                    
                    var rank = -1;
                    if (topTrackResponse.attr() != null) {
                        rank = topTrackResponse.attr().rank();
                    }
                    
                    return new TopTrackWrapper(song, playCount, rank);
                })
                .toList();
        
        var topTrackList = topTrackListData.stream()
                .map(TopTrackWrapper::track)
                .toList();
        
        var wrappedTopTrackList = topTrackListData.stream()
                .map(topTrack -> typeConverter.convertFromRecordToEntity("TopTrack", topTrack))
                .toList();
        
        return createResult(topTrackList, wrappedTopTrackList, topTracks.attr());
    }

    public void getWeeklyAlbumChart(String user) {
        // TODO: Implement this method
    }

    public void getWeeklyArtistChart(String user) {
        // TODO: Implement this method
    }

    public void getWeeklyChartList(String user) {
        // TODO: Implement this method
    }

    public void getWeeklyTrackChart(String user) {
        // TODO: Implement this method
    }
    
    // Utility methods for native methods
    
    private EntityType createEmptyOptional() {
        return entityInitializer.initializeEntity("Optional", Optional.empty(), false);
    }
    
    private EntityType createOptional(Object object) {
        return entityInitializer.initializeEntity("Optional", object, true);
    }
    
    private EntityType createResult(Object data, Object wrappedData, UserResponseAttr attr) {
        var wrappedOptional = wrappedData == null ? createEmptyOptional() : createOptional(wrappedData);
        return entityInitializer.initializeEntity("LastFmResult", data, wrappedOptional, -1, "", createAtr(attr));
    }
    
    private EntityType createErrorResult(LastFmResponse<?> lastFmResponse) {
        return createResult(lastFmResponse.getErrorResponse().error(), lastFmResponse.getErrorResponse().message());
    }
    
    private EntityType createResult(int errorCode, String errorMessage) {
        return entityInitializer.initializeEntity("LastFmResult", 0, createEmptyOptional(), errorCode, errorMessage, createEmptyAtr());
    }
    
    private Page createPageFromEntity(EntityType entityType) {
        return typeConverter.convertFromEntityToRecord(entityType, Page.class);
    }
    
    private DateRange createDateRangeFromEntity(EntityType entityType) {
        var fromOptional = entityType.getEntityScope().<EntityType>lookup("from").getValue(); // DateRange.from
        var fromDateOptional = getDateFromOptional(fromOptional);
        
        var toOptional = entityType.getEntityScope().<EntityType>lookup("to").getValue();
        var toDateOptional = getDateFromOptional(toOptional);


        return new DateRange(fromDateOptional, toDateOptional);
    }
    
    private Optional<LocalDate> getDateFromOptional(EntityType entityType) {
        var dateOptional = entityType.getEntityScope().<JavaType>lookup("_value").getValue().getOptionalReference(EntityType.class); // DateRange.from._value
        
        return dateOptional.map(optionalValue -> {
            optionalValue.validateType("Date");
            return optionalValue.getEntityScope().<JavaType>lookup("_date").getValue().getReference(LocalDate.class); // DateRange.from._value.date
        });
    }
    
    private EntityType createAtr(UserResponseAttr attr) {
        return typeConverter.convertFromRecordToEntity("Attr", attr);
    }
    
    private EntityType createEmptyAtr() {
        return entityInitializer.initializeEntity("Attr", "", 0, 0, 0, 0);
    }

    private record LovedTrackWrapper(SongType track, long dateUts, String dateText) {}
    
    private record RecentTrackWrapper(SongType track, boolean nowPlaying, long dateUts, String dateText) {}

    private record TopAlbumWrapper(AlbumType album, int playCount, int rank) {}

    private record TopArtistWrapper(EntityType artist, int playCount, int rank) {}
    
    private record TopTrackWrapper(SongType track, int playCount, int rank) {}
    
}