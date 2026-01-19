package com.zenith.app.data.cloud

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google認証管理クラス
 * Google Sign-InとDrive APIスコープの管理
 */
@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _signInState = MutableStateFlow<GoogleSignInState>(GoogleSignInState.SignedOut)
    val signInState: StateFlow<GoogleSignInState> = _signInState.asStateFlow()

    private val _currentAccount = MutableStateFlow<GoogleSignInAccount?>(null)
    val currentAccount: StateFlow<GoogleSignInAccount?> = _currentAccount.asStateFlow()

    private val googleSignInClient: GoogleSignInClient by lazy {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA)) // アプリデータフォルダのみアクセス
            .build()

        GoogleSignIn.getClient(context, signInOptions)
    }

    init {
        // 既存のサインイン状態をチェック
        checkExistingSignIn()
    }

    /**
     * 既存のサインイン状態を確認
     */
    private fun checkExistingSignIn() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && hasRequiredScopes(account)) {
            _currentAccount.value = account
            _signInState.value = GoogleSignInState.SignedIn(account.email ?: "")
        } else {
            _signInState.value = GoogleSignInState.SignedOut
        }
    }

    /**
     * 必要なスコープが付与されているか確認
     */
    private fun hasRequiredScopes(account: GoogleSignInAccount): Boolean {
        return GoogleSignIn.hasPermissions(account, Scope(DriveScopes.DRIVE_APPDATA))
    }

    /**
     * サインインIntentを取得
     */
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * サインイン結果を処理
     */
    fun handleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.result
            if (account != null) {
                _currentAccount.value = account
                _signInState.value = GoogleSignInState.SignedIn(account.email ?: "")
            } else {
                _signInState.value = GoogleSignInState.Error("サインインに失敗しました")
            }
        } catch (e: Exception) {
            _signInState.value = GoogleSignInState.Error(e.message ?: "サインインエラー")
        }
    }

    /**
     * サインアウト
     */
    suspend fun signOut() {
        try {
            googleSignInClient.signOut()
            _currentAccount.value = null
            _signInState.value = GoogleSignInState.SignedOut
        } catch (e: Exception) {
            _signInState.value = GoogleSignInState.Error(e.message ?: "サインアウトエラー")
        }
    }

    /**
     * アカウントの接続を解除（完全にリンク解除）
     */
    suspend fun revokeAccess() {
        try {
            googleSignInClient.revokeAccess()
            _currentAccount.value = null
            _signInState.value = GoogleSignInState.SignedOut
        } catch (e: Exception) {
            _signInState.value = GoogleSignInState.Error(e.message ?: "接続解除エラー")
        }
    }

    /**
     * サインイン済みかどうか
     */
    fun isSignedIn(): Boolean {
        return _currentAccount.value != null
    }

    /**
     * サインイン状態をリセット
     */
    fun resetError() {
        val account = _currentAccount.value
        if (account != null) {
            _signInState.value = GoogleSignInState.SignedIn(account.email ?: "")
        } else {
            _signInState.value = GoogleSignInState.SignedOut
        }
    }
}

/**
 * Googleサインイン状態
 */
sealed class GoogleSignInState {
    data object SignedOut : GoogleSignInState()
    data class SignedIn(val email: String) : GoogleSignInState()
    data class Error(val message: String) : GoogleSignInState()
}
