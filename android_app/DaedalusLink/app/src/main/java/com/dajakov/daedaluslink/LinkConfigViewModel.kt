package com.dajakov.daedaluslink

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class LinkConfigViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = LinkConfigDatabase.getDatabase(application).linkConfigDao()
    val allConfigs: Flow<List<LinkConfig>> = dao.getAllLinkConfigs()

    fun insertLinkConfig(linkConfig: LinkConfig) {
        viewModelScope.launch {
            dao.insertLinkConfig(linkConfig)
        }
    }

    suspend fun getLinkConfigById(linkId: Int): LinkConfig? {
        return dao.getLinkConfigById(linkId)
    }

    fun deleteLinkConfig(linkConfig: LinkConfig) {
        viewModelScope.launch {
            dao.deleteLinkConfig(linkConfig)
        }
    }
}

