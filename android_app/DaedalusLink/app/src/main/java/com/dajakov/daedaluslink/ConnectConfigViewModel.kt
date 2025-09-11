package com.dajakov.daedaluslink

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ConnectConfigViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = ConnectConfigDatabase.getDatabase(application).connectConfigDao()
    val allConfigs: Flow<List<ConnectConfig>> = dao.getAllConfigs()

    fun insertConfig(config: ConnectConfig) {
        viewModelScope.launch {
            dao.insertConfig(config)
        }
    }

    fun deleteConfig(config: ConnectConfig) {
        viewModelScope.launch {
            dao.deleteConfig(config)
        }
    }
}