package com.application.bikestreets.api.modals

import com.mapbox.geojson.Point
import com.mapbox.search.SearchResultMetadata
import com.mapbox.search.common.RoutablePoint
import com.mapbox.search.offline.OfflineSearchResult
import com.mapbox.search.record.HistoryRecord
import com.mapbox.search.result.SearchAddress
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchResultType

/**
 * Creating a super type that can take the mapbox search types of
 *  - SearchResult
 *  - OfflineSearchResult
 *  - History
 */
data class Location(
    val id: String,
    val name: String,
    val descriptionText: String?,
    val address: SearchAddress?,
    val routablePoints: List<RoutablePoint>?,
    val categories: List<String>?,
    val makiIcon: String?,
    val coordinate: Point,
    val type: SearchResultType?,
    val metadata: SearchResultMetadata?,
) {
    constructor(searchResult: SearchResult) : this(
        id = searchResult.id,
        name = searchResult.name,
        descriptionText = searchResult.descriptionText,
        address = searchResult.address,
        routablePoints = searchResult.routablePoints,
        categories = searchResult.categories,
        makiIcon = searchResult.makiIcon,
        coordinate = searchResult.coordinate,
        type = null,
        metadata = searchResult.metadata
    )

    constructor(offlineSearchResult: OfflineSearchResult) : this(
        id = offlineSearchResult.id,
        name = offlineSearchResult.name,
        descriptionText = offlineSearchResult.descriptionText,
        address = null,
        routablePoints = offlineSearchResult.routablePoints,
        categories = null,
        makiIcon = null,
        coordinate = offlineSearchResult.coordinate,
        type = null,
        metadata = null
    )

    constructor(historyRecord: HistoryRecord) : this(
        id = historyRecord.id,
        name = historyRecord.name,
        descriptionText = historyRecord.descriptionText,
        address = historyRecord.address,
        routablePoints = historyRecord.routablePoints,
        categories = historyRecord.categories,
        makiIcon = historyRecord.makiIcon,
        coordinate = historyRecord.coordinate,
        type = historyRecord.type,
        metadata = historyRecord.metadata
    )
}
