package com.example.adremover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.adremover.core.AdRemoverEngine
import com.example.adremover.model.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val engine = AdRemoverEngine(application)
    
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()
    
    private val _processState = MutableStateFlow<AdRemoverEngine.ProcessState>(AdRemoverEngine.ProcessState.Idle)
    val processState: StateFlow<AdRemoverEngine.ProcessState> = _processState.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    var currentPackage: String? = null
        private set
    
    init {
        refreshApps()
    }
    
    fun refreshApps() {
        viewModelScope.launch {
            _apps.value = engine.getInstalledApps()
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun removeAd(packageName: String) {
        if (_isProcessing.value) return
        
        viewModelScope.launch {
            _isProcessing.value = true
            currentPackage = packageName
            
            engine.processApp(packageName) { state ->
                _processState.value = state
            }
            
            _isProcessing.value = false
            currentPackage = null
        }
    }
    
    fun analyzeApp(packageName: String) {
        if (_isProcessing.value) return
        
        viewModelScope.launch {
            _isProcessing.value = true
            _processState.value = AdRemoverEngine.ProcessState.Analyzing("正在分析...")
            
            val result = engine.analyzeApp(packageName)
            
            if (result != null) {
                if (result.hasAds) {
                    val adNames = result.detectedAds.joinToString(", ") { it.signature.name }
                    _processState.value = AdRemoverEngine.ProcessState.Analyzing("发现广告: $adNames")
                } else {
                    _processState.value = AdRemoverEngine.ProcessState.Analyzing("未发现已知广告SDK")
                }
            } else {
                _processState.value = AdRemoverEngine.ProcessState.Error("分析失败")
            }
            
            _isProcessing.value = false
        }
    }
    
    fun clearState() {
        _processState.value = AdRemoverEngine.ProcessState.Idle
    }
}
