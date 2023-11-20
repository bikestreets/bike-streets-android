package com.application.bikestreets.composables.bottomsheet

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.getString
import androidx.core.view.isVisible
import com.application.bikestreets.R
import com.application.bikestreets.api.modals.Location
import com.application.bikestreets.utils.getSearchOptions
import com.mapbox.search.ApiType
import com.mapbox.search.ResponseInfo
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.offline.OfflineResponseInfo
import com.mapbox.search.offline.OfflineSearchEngine
import com.mapbox.search.offline.OfflineSearchEngineSettings
import com.mapbox.search.offline.OfflineSearchResult
import com.mapbox.search.record.HistoryRecord
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import com.mapbox.search.ui.adapter.engines.SearchEngineUiAdapter
import com.mapbox.search.ui.view.CommonSearchViewConfiguration
import com.mapbox.search.ui.view.DistanceUnitType
import com.mapbox.search.ui.view.SearchMode
import com.mapbox.search.ui.view.SearchResultsView

private lateinit var searchEngineUiAdapter: SearchEngineUiAdapter

@Composable
fun SearchOptions(
    modifier: Modifier = Modifier,
    onSearchOptionSelected: (Location) -> Unit,
    newSearchQuery: String,
) {
    // This XML breaks the compose previewer, hide from preview
    if (!LocalInspectionMode.current) {
        val context = LocalContext.current

        val apiType = ApiType.GEOCODING

        val searchEngine = SearchEngine.createSearchEngineWithBuiltInDataProviders(
            apiType = apiType,
            settings = SearchEngineSettings(getString(context, R.string.mapbox_access_token))
        )

        val offlineSearchEngine = OfflineSearchEngine.create(
            OfflineSearchEngineSettings(getString(context, R.string.mapbox_access_token))
        )

        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                SearchResultsView(ctx).apply {
                    initialize(
                        SearchResultsView.Configuration(
                            CommonSearchViewConfiguration(
                                DistanceUnitType.IMPERIAL
                            )
                        )
                    )
                    isVisible = true

                    searchEngineUiAdapter = SearchEngineUiAdapter(
                        view = this,
                        searchEngine = searchEngine,
                        offlineSearchEngine = offlineSearchEngine,
                    )

                    searchEngineUiAdapter.searchMode = SearchMode.AUTO
                    searchEngineUiAdapter.addSearchListener(object :
                        SearchEngineUiAdapter.SearchListener {

                        override fun onSuggestionsShown(
                            suggestions: List<SearchSuggestion>,
                            responseInfo: ResponseInfo
                        ) {
                            // Nothing to do
                        }

                        override fun onSearchResultsShown(
                            suggestion: SearchSuggestion,
                            results: List<SearchResult>,
                            responseInfo: ResponseInfo
                        ) {
                            // Do nothing
                        }

                        override fun onOfflineSearchResultsShown(
                            results: List<OfflineSearchResult>,
                            responseInfo: OfflineResponseInfo
                        ) {
                            // Nothing to do
                        }

                        override fun onSuggestionSelected(searchSuggestion: SearchSuggestion): Boolean {
                            return false
                        }

                        override fun onSearchResultSelected(
                            searchResult: SearchResult,
                            responseInfo: ResponseInfo
                        ) {
                            val location = Location(searchResult)
                            onSearchOptionSelected(location)
                        }

                        override fun onOfflineSearchResultSelected(
                            searchResult: OfflineSearchResult,
                            responseInfo: OfflineResponseInfo
                        ) {
                            onSearchOptionSelected(Location(searchResult))
                        }

                        override fun onError(e: Exception) {
                            Log.e(javaClass.simpleName, "Mapbox Search Error: $e")
                        }

                        override fun onFeedbackItemClick(responseInfo: ResponseInfo) {
                            // Not used
                        }

                        override fun onHistoryItemClick(historyRecord: HistoryRecord) {
                            onSearchOptionSelected(Location(historyRecord))
                        }

                        override fun onPopulateQueryClick(
                            suggestion: SearchSuggestion,
                            responseInfo: ResponseInfo
                        ) {
                            // Not used
                        }
                    })

                }
            },
            update = { view ->
                searchEngineUiAdapter.search(newSearchQuery, getSearchOptions())
            }
        )
    }
}