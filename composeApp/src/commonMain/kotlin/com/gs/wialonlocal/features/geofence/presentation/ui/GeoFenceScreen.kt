package com.gs.wialonlocal.features.geofence.presentation.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.strings
import cafe.adriel.voyager.core.screen.Screen
import com.gs.wialonlocal.features.main.presentation.ui.SearchBar
import com.gs.wialonlocal.features.main.presentation.ui.ToolBar
import com.gs.wialonlocal.features.monitoring.presentation.ui.monitoringlist.Groups
import kotlinx.coroutines.launch

class GeoFenceScreen: Screen {
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val pagerState = rememberPagerState { 2 }
        val coroutine = rememberCoroutineScope()
        Column(Modifier.fillMaxSize()) {
            ToolBar(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    SearchBar(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        placeholder = strings.search,
                        onSearch = {

                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    TabRow(
                        pagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        indicator = {

                        }
                    ) {
                        Tab(pagerState.currentPage == 0, onClick = {
                            coroutine.launch {
                                pagerState.scrollToPage(0)
                            }
                        }, text = {
                            Text(
                                strings.geofences,
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        })
                        Tab(pagerState.currentPage == 1, onClick = {
                            coroutine.launch {
                                pagerState.scrollToPage(1)
                            }
                        }, text = {
                            Text(
                                "GROUPS",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        })
                    }
                }


            }
            HorizontalPager(pagerState) { index ->
                if (index == 0) {
                    Geofences(modifier = Modifier.fillMaxSize())
                } else {
                    Groups()
                }
            }
        }
    }
}