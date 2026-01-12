
/*
 * User functions
 */

/**
 * Get the friends of a user. This is unpaged, so it returns the first 50.
 *
 * @param[@type string] user The username of the user to get the friends of
 * @returns[@type lastfm.LastFmResult] The result of the request, with data being a list of [@type core.User]s, and no wrapped data
 */
native fun getFriends(user)

/**
 * Get the friends of a user.
 *
 * @param[@type string] user The username of the user to get the friends of
 * @param[@type lastfm.Page] page The page of the request
 * @returns[@type lastfm.LastFmResult] The result of the request, with data being a list of [@type core.User]s, and no wrapped data
 */
native fun getFriends(user, page)

native fun getUser(user) // TODO

/**
 * Gets the tracks favorited by the user. This is unpaged, so it returns the first 50.
 *
 * @param[@type string] user The username of the user to get the loved tracks of
 * @returns[@type lastfm.LastFmResult] The result of the request, with data being a list of songs, and wrapped data as a list of [@type lastfm.LovedTrack]
 */
native fun getLovedTracks(user)

/**
 * Gets the tracks favorited by the user.
 *
 * @param[@type string] user The username of the user to get the loved tracks of
 * @param[@type lastfm.Page] page The page of the request
 * @returns[@type lastfm.LastFmResult] The result of the request, with data being a list of songs, and wrapped data as a list of [@type lastfm.LovedTrack]
 */
native fun getLovedTracks(user, page)

// TODO: get tags

/**
 * Gets the tracks recently played by the user. This is unpaged, so it returns the most recent 50.
 *
 * @param[@type string] user The username of the user to get the recent tracks of
 * @returns[@type lastfm.LastFmResult] The result of the request, with data being a list of songs, and wrapped data as a list of [@type lastfm.RecentTrack]
 */
native fun getRecentTracks(user)

/**
 * Gets the tracks recently played by the user.
 *
 * @param[@type string] user The username of the user to get the recent tracks of
 * @param[@type lastfm.Page] page The page of the request
 * @param[@type lastfm.DateRange] dateRange The range of dates to get the tracks from
 * @returns[@type lastfm.LastFmResult] The result of the request, with data being a list of songs, and wrapped data as a list of [@type lastfm.RecentTrack]
 */
native fun getRecentTracks(user, page, dateRange)

/**
 * Gets the top albums of a user. This is unpaged, so it returns the all time top 50.
 *
 * @param[@type string] user The username of the user to get the top albums of
 * @returns[@type lastfm.LastFmResult] The result of the request, with data being a list of albums, and wrapped data as a list of [@type lastfm.TopAlbum]
 */
native fun getTopAlbums(user)

/**
 * Gets the top albums of a user.
 *
 * @param[@type string] user The username of the user to get the top albums of
 * @param[@type string] period The period to get the top albums for. Can be "overall", "7day", "1month", "3month", "6month", or "12month"
 * @param[@type lastfm.Page] page The page of the request
 * @returns[@type lastfm.LastFmResult] The result of the request, with data being a list of albums, and wrapped data as a list of [@type lastfm.TopAlbum]
 */
native fun getTopAlbums(user, page, dateRange)

/**
 * Gets the top artists of a user. This is unpaged, so it returns the all time top 50.
 *
 * @param[@type string] user The username of the user to get the top artists of
 * @returns[@type lastfm.LastFmResult] The result of the request, with data being a list of artists, and wrapped data as a list of [@type lastfm.TopArtist]
 */
native fun getTopArtists(user)

/**
 * Gets the top artists of a user.
 *
 * @param[@type string] user The username of the user to get the top artists of
 * @param[@type string] period The period to get the top albums for. Can be "overall", "7day", "1month", "3month", "6month", or "12month"
 * @param[@type lastfm.Page] page The page of the request
 * @returns[@type lastfm.LastFmResult] The result of the request, with data being a list of artists, and wrapped data as a list of [@type lastfm.TopArtist]
 */
native fun getTopArtists(user, page, dateRange)

/**
 * Gets the top tracks of a user. This is unpaged, so it returns the all time top 50.
 *
 * @param[@type string] user The username of the user to get the top tracks of
 * @returns[@type lastfm.LastFmResult] The result of the request, with data being a list of songs, and wrapped data as a list of [@type lastfm.TopTrack]
 */
native fun getTopTracks(user)

/**
 * Gets the top tracks of a user. This is unpaged, so it returns the all time top 50.
 *
 * @param[@type string] user The username of the user to get the top tracks of
 * @param[@type string] period The period to get the top tracks for. Can be "overall", "7day", "1month", "3month", "6month", or "12month"
 * @param[@type lastfm.Page] page The page of the request
 * @returns[@type lastfm.LastFmResult] The result of the request, with data being a list of songs, and wrapped data as a list of [@type lastfm.TopTrack]
 */
native fun getTopTracks(user, period, page)

// TODO: These
//
//native fun getWeeklyAlbumChart(user)
//
//native fun getWeeklyArtistChart(user)
//
//native fun getWeeklyChartList(user)
//
//native fun getWeeklyTrackChart(user)


/**
 * A paged response information.
 */
entity Page {
    /**
     * The page index.
     */
    int page = 1
    
    /**
     * The limit of items per page.
     */
    int count = 50
    
    Page()
}

entity Attr {
    string user
    int totalPages
    int page
    int perPage
    int total
    
    Attr(user, totalPages, page, perPage, total)
}

/**
 * A response from the Last.FM API. This may either contain data from the response, or error information.
 * To ensure standard Qilletni response types such as `song[]` or `Artist[]`, functions will often return direct music
 * types in the `data` field. If the API returned additional information, such as rank number or play count, this is
 * placed in the `wrqappedData`. The type of both `data` and `wrappedData` are specified in the function that returns
 * this entity.
 */
entity LastFmResult {

    string errorMessage
    
    int errorCode
    
    any data
    
    /**
     * For some methods, the track/artist/etc. data may have additional information for each item, such as play count, rank, etc.
     */
    Optional wrappedData
    
    Attr attr
    
    LastFmResult(data, wrappedData, errorCode, errorMessage, attr)
    
    fun isError() {
        return errorCode != -1
    }
}

entity DateRange {
    /**
     * The start date of the range.
     * @type core.Date
     */
    Optional from
    
    /**
     * The end date of the range.
     * @type core.Date
     */
    Optional to
    
    DateRange(from, to)
    
    static fun ofFrom(from) {
        return new DateRange(Optional.fromValue(from), Optional.fromEmpty())
    }
    
    static fun ofTo(to) {
        return new DateRange(Optional.fromEmpty(), Optional.fromValue(to))
    }
    
    static fun of(from, to) {
        return new DateRange(Optional.fromValue(from), Optional.fromValue(to))
    }
    
    static fun ofEmpty() {
        return new DateRange(Optional.fromEmpty(), Optional.fromEmpty())
    }
}

/*
 * Wrapped data
 */
 
// getLovedTracks

entity LovedTrack {
    song track
    int dateUts
    int dateText
    
    LovedTrack(track, dateUts, dateText)
}

// getRecentTracks

entity RecentTrack {
    song track
    boolean nowPlaying
    int dateUts
    string dateText
    
    RecentTrack(track, nowPlaying, dateUts, dateText)
}

// getTopAlbums

entity TopAlbum {
    album albumValue
    int playCount
    int rank
    
    TopAlbum(albumValue, playCount, rank)
}

// getTopArtists

entity TopArtist {
    Artist artist
    int playCount
    int rank
    
    TopArtist(artist, playCount, rank)
}

// getTopTracks

entity TopTrack {
    song track
    int playCount
    int rank
    
    TopTrack(track, playCount, rank)
}
