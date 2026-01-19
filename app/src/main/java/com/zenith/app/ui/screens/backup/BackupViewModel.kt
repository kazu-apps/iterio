package com.zenith.app.ui.screens.backup

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenith.app.data.cloud.CloudBackupInfo
import com.zenith.app.data.cloud.GoogleAuthManager
import com.zenith.app.data.cloud.GoogleSignInState
import com.zenith.app.data.cloud.NoBackupFoundException
import com.zenith.app.domain.model.ImportResult
import com.zenith.app.domain.model.SubscriptionStatus
import com.zenith.app.domain.usecase.BackupUseCase
import com.zenith.app.domain.usecase.CloudBackupUseCase
import com.zenith.app.domain.usecase.PremiumRequiredException
import com.zenith.app.ui.premium.PremiumManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BackupState {
    data object Idle : BackupState()
    data object Exporting : BackupState()
    data object Importing : BackupState()
    data class ExportSuccess(val message: String) : BackupState()
    data class ImportSuccess(val result: ImportResult) : BackupState()
    data class Error(val message: String) : BackupState()
}

sealed class CloudBackupState {
    data object Idle : CloudBackupState()
    data object Uploading : CloudBackupState()
    data object Downloading : CloudBackupState()
    data class UploadSuccess(val info: CloudBackupInfo) : CloudBackupState()
    data class DownloadSuccess(val result: ImportResult) : CloudBackupState()
    data class Error(val message: String) : CloudBackupState()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupUseCase: BackupUseCase,
    private val cloudBackupUseCase: CloudBackupUseCase,
    private val googleAuthManager: GoogleAuthManager,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    private val _cloudBackupState = MutableStateFlow<CloudBackupState>(CloudBackupState.Idle)
    val cloudBackupState: StateFlow<CloudBackupState> = _cloudBackupState.asStateFlow()

    private val _cloudBackupInfo = MutableStateFlow<CloudBackupInfo?>(null)
    val cloudBackupInfo: StateFlow<CloudBackupInfo?> = _cloudBackupInfo.asStateFlow()

    val googleSignInState: StateFlow<GoogleSignInState> = googleAuthManager.signInState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GoogleSignInState.SignedOut
        )

    val subscriptionStatus: StateFlow<SubscriptionStatus> = premiumManager.subscriptionStatus
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SubscriptionStatus()
        )

    val isPremium: StateFlow<Boolean> = subscriptionStatus
        .map { it.isPremium }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    init {
        // サインイン状態が変わったらDriveを初期化してバックアップ情報を取得
        viewModelScope.launch {
            googleAuthManager.currentAccount.collect { account ->
                if (account != null) {
                    cloudBackupUseCase.initializeDrive(account)
                    fetchCloudBackupInfo()
                }
            }
        }
    }

    // ローカルバックアップ
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.Exporting

            backupUseCase.exportBackup(uri).fold(
                onSuccess = {
                    _backupState.value = BackupState.ExportSuccess("バックアップが完了しました")
                },
                onFailure = { e ->
                    _backupState.value = when (e) {
                        is PremiumRequiredException -> BackupState.Error("Premium機能です")
                        else -> BackupState.Error(e.message ?: "エクスポートに失敗しました")
                    }
                }
            )
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.Importing

            backupUseCase.importBackup(uri).fold(
                onSuccess = { result ->
                    _backupState.value = BackupState.ImportSuccess(result)
                },
                onFailure = { e ->
                    _backupState.value = when (e) {
                        is PremiumRequiredException -> BackupState.Error("Premium機能です")
                        else -> BackupState.Error(e.message ?: "インポートに失敗しました")
                    }
                }
            )
        }
    }

    fun resetState() {
        _backupState.value = BackupState.Idle
    }

    // クラウドバックアップ
    fun getGoogleSignInIntent(): Intent {
        return googleAuthManager.getSignInIntent()
    }

    fun handleGoogleSignInResult(data: Intent?) {
        googleAuthManager.handleSignInResult(data)
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            googleAuthManager.signOut()
            _cloudBackupInfo.value = null
            cloudBackupUseCase.cleanup()
        }
    }

    fun uploadToCloud() {
        viewModelScope.launch {
            _cloudBackupState.value = CloudBackupState.Uploading

            cloudBackupUseCase.uploadToCloud().fold(
                onSuccess = { info ->
                    _cloudBackupInfo.value = info
                    _cloudBackupState.value = CloudBackupState.UploadSuccess(info)
                },
                onFailure = { e ->
                    _cloudBackupState.value = when (e) {
                        is PremiumRequiredException -> CloudBackupState.Error("Premium機能です")
                        else -> CloudBackupState.Error(e.message ?: "アップロードに失敗しました")
                    }
                }
            )
        }
    }

    fun downloadFromCloud() {
        viewModelScope.launch {
            _cloudBackupState.value = CloudBackupState.Downloading

            cloudBackupUseCase.downloadFromCloud().fold(
                onSuccess = { result ->
                    _cloudBackupState.value = CloudBackupState.DownloadSuccess(result)
                },
                onFailure = { e ->
                    _cloudBackupState.value = when (e) {
                        is PremiumRequiredException -> CloudBackupState.Error("Premium機能です")
                        is NoBackupFoundException -> CloudBackupState.Error("クラウドにバックアップがありません")
                        else -> CloudBackupState.Error(e.message ?: "ダウンロードに失敗しました")
                    }
                }
            )
        }
    }

    private fun fetchCloudBackupInfo() {
        viewModelScope.launch {
            cloudBackupUseCase.getCloudBackupInfo().fold(
                onSuccess = { info ->
                    _cloudBackupInfo.value = info
                },
                onFailure = { /* ignore */ }
            )
        }
    }

    fun resetCloudState() {
        _cloudBackupState.value = CloudBackupState.Idle
    }

    fun startTrial() {
        viewModelScope.launch {
            premiumManager.startTrial()
        }
    }
}
