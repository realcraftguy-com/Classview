package com.realcraftguy.classview.student

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.realcraftguy.classview.student.data.Membership
import com.realcraftguy.classview.student.data.Profile
import com.realcraftguy.classview.student.data.ServerError
import com.realcraftguy.classview.student.data.Settings
import com.realcraftguy.classview.student.data.Status
import com.realcraftguy.classview.student.data.StudentRepository
import com.realcraftguy.classview.student.data.StudentStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class Screen { Start, Profile, Join, Joined, Hints, Server }

enum class ConnectionState { Unknown, Connected, Offline }

/** Kurzlebiger Zustand der Oberfläche (nicht gespeichert). */
data class Transient(
    val screen: Screen? = null,
    val code: String = "",
    val codeHint: String? = null,
    val joinBusy: Boolean = false,
    val joinError: String? = null,
    val connection: ConnectionState = ConnectionState.Unknown,
    val offlineMessage: String? = null,
    /** Hinweis auf der Startseite, z. B. „Du wurdest aus der Klasse entfernt.“ */
    val notice: String? = null,
    val serverBusy: Boolean = false,
    val serverError: String? = null,
    val profileSaving: Boolean = false,
)

data class UiState(
    val loaded: Boolean = false,
    val profile: Profile = Profile(),
    val membership: Membership? = null,
    val settings: Settings? = null,
    val t: Transient = Transient(),
) {
    /** Ohne ausdrückliche Navigation: beigetreten → Klassenansicht, sonst Startseite. */
    val screen: Screen get() = t.screen ?: if (membership != null) Screen.Joined else Screen.Start
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val repo = StudentRepository(StudentStore(app))
    private val transient = MutableStateFlow(Transient())
    private var refreshJob: Job? = null

    val state: StateFlow<UiState> = combine(
        repo.store.profile, repo.store.membership, repo.store.settings, transient,
    ) { profile, membership, settings, t -> UiState(true, profile, membership, settings, t) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    private fun update(block: (Transient) -> Transient) = transient.update(block)

    fun navigate(screen: Screen) = update { it.copy(screen = screen, joinError = null, serverError = null) }
    fun back() = update { it.copy(screen = null, joinError = null, serverError = null) }
    fun dismissNotice() = update { it.copy(notice = null) }

    // ------------------------------------------------------------------ Verbindung

    /** Solange die App sichtbar ist, alle 30 s prüfen. TODO: Phase 5 – ersetzt durch den WebSocket. */
    fun onResume() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(30_000)
            }
        }
    }

    fun onPause() {
        refreshJob?.cancel()
    }

    private suspend fun refresh() {
        when (val s = repo.refresh()) {
            Status.NotJoined -> Unit
            Status.Connected -> update { it.copy(connection = ConnectionState.Connected, offlineMessage = null) }
            is Status.Offline -> update { it.copy(connection = ConnectionState.Offline, offlineMessage = s.message) }
            is Status.Removed -> update { Transient(notice = s.message) }
        }
    }

    // ------------------------------------------------------------------ Beitreten

    fun onCodeChange(raw: String) {
        val (code, hint) = StudentRepository.sanitizeCode(raw)
        update { it.copy(code = code, codeHint = hint, joinError = null) }
    }

    fun join() {
        val code = transient.value.code
        if (code.length != 6 || transient.value.joinBusy) return
        update { it.copy(joinBusy = true, joinError = null) }
        viewModelScope.launch {
            try {
                repo.join(code)
                val seenHints = repo.store.currentSettings().hintsSeen
                update {
                    Transient(screen = if (seenHints) null else Screen.Hints, connection = ConnectionState.Connected)
                }
            } catch (e: ServerError) {
                update { it.copy(joinBusy = false, joinError = e.message) }
            }
        }
    }

    fun leave() {
        viewModelScope.launch {
            val ok = repo.leave()
            update {
                Transient(notice = if (ok) "Du hast die Klasse verlassen." else "Server nicht erreichbar – du wurdest nur auf diesem Tablet abgemeldet.")
            }
        }
    }

    // ------------------------------------------------------------------ Profil

    fun saveProfile(name: String, preset: String?) {
        update { it.copy(profileSaving = true) }
        viewModelScope.launch {
            repo.saveProfile(name, preset)
            update { it.copy(profileSaving = false, screen = null) }
        }
    }

    fun pickPhoto(uri: Uri) {
        viewModelScope.launch {
            runCatching { repo.setPhoto(uri) }.onFailure { e ->
                update { it.copy(notice = "Das Foto konnte nicht geladen werden (${e.message}).") }
            }
        }
    }

    fun removePhoto() {
        viewModelScope.launch { repo.removePhoto() }
    }

    // ------------------------------------------------------------------ Server-Adresse

    fun saveServer(input: String) {
        update { it.copy(serverBusy = true, serverError = null) }
        viewModelScope.launch {
            try {
                repo.setServer(input)
                update { it.copy(serverBusy = false, screen = Screen.Join) }
            } catch (e: ServerError) {
                update { it.copy(serverBusy = false, serverError = e.message) }
            }
        }
    }

    // ------------------------------------------------------------------ Hinweise

    fun finishHints() {
        viewModelScope.launch {
            repo.store.setHintsSeen()
            update { it.copy(screen = null) }
        }
    }

    fun setAutostartDone(done: Boolean) = viewModelScope.launch { repo.store.setAutostartDone(done) }
    fun setLockDone(done: Boolean) = viewModelScope.launch { repo.store.setLockDone(done) }

    suspend fun awaitLoaded() = state.first { it.loaded }
}
