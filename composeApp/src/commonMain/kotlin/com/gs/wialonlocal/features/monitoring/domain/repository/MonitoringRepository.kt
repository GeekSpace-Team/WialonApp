package com.gs.wialonlocal.features.monitoring.domain.repository

import com.gs.wialonlocal.core.network.Resource
import com.gs.wialonlocal.features.monitoring.data.entity.history.CustomFields
import com.gs.wialonlocal.features.monitoring.data.entity.history.GetReportSettings
import com.gs.wialonlocal.features.monitoring.data.entity.history.LoadEventRequest
import com.gs.wialonlocal.features.monitoring.data.entity.history.Trip
import com.gs.wialonlocal.features.monitoring.data.entity.history.TripsResponse
import com.gs.wialonlocal.features.monitoring.domain.model.UnitModel
import kotlinx.coroutines.flow.Flow

interface MonitoringRepository {
    suspend fun getEvents(): Flow<Resource<List<UnitModel>>>
    suspend fun getUpdates(oldEvents: List<UnitModel>): Flow<Resource<List<UnitModel>>>
    suspend fun getReportSettings(itemId: String): Flow<Resource<GetReportSettings>>
    suspend fun loadEvents(req: LoadEventRequest): Flow<Resource<Pair<List<Trip>, List<Trip>>>>
    suspend fun unloadEvents(id: String): Flow<Resource<Unit>>
    suspend fun getEvent(id: String,mode: Int = 0): Flow<Resource<CustomFields>>

}