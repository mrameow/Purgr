package com.purgr.app

import android.app.Application
import android.content.IntentSender
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

enum class Screen { Swipe, Review, Trash, Settings }

class PurgrViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MediaRepository(app)
    private val store = DecisionStore(app)

    var screen by mutableStateOf(Screen.Swipe)
    var loading by mutableStateOf(true)
        private set

    /** Photos/videos not yet swiped, in order. The first element is the card on top. */
    val queue = mutableStateListOf<MediaItem>()

    /** Marked for deletion this session. Nothing is deleted from the phone until the review is confirmed. */
    val marked = mutableStateListOf<MediaItem>()

    /** Items in [marked] that the user rescued during review. */
    val rescued = mutableStateListOf<Long>()

    val trashed = mutableStateListOf<MediaItem>()
    var totalCount by mutableStateOf(0)
        private set

    private val keptIds = HashSet<Long>()
    private val history = ArrayList<Pair<MediaItem, Boolean>>() // item to "kept?" for undo
    private var trashChunks = ArrayDeque<List<MediaItem>>()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            loading = true
            val all = repo.loadLibrary()
            val kept = store.kept()
            val pending = store.pending()
            totalCount = all.size
            keptIds.clear()
            keptIds += kept
            queue.clear()
            marked.clear()
            rescued.clear()
            history.clear()
            for (item in all) {
                when (item.id) {
                    in pending -> marked += item
                    in kept -> Unit
                    else -> queue += item
                }
            }
            persist()
            loading = false
        }
    }

    fun swipe(keep: Boolean) {
        val item = queue.removeFirstOrNull() ?: return
        history += item to keep
        if (keep) keptIds += item.id else marked += item
        persist()
    }

    val canUndo get() = history.isNotEmpty()

    fun undo() {
        val (item, keep) = history.removeLastOrNull() ?: return
        if (keep) keptIds -= item.id else marked.removeAll { it.id == item.id }
        queue.add(0, item)
        persist()
    }

    fun toggleRescue(item: MediaItem) {
        if (!rescued.remove(item.id)) rescued += item.id
    }

    val toDelete get() = marked.filter { it.id !in rescued }

    /** Splits the confirmed deletions into chunks so each system dialog stays a sane size. */
    fun beginTrash(): IntentSender? {
        trashChunks = ArrayDeque(toDelete.chunked(CHUNK))
        return nextTrashRequest()
    }

    fun nextTrashRequest(): IntentSender? {
        val chunk = trashChunks.firstOrNull() ?: return null
        return repo.trashRequest(chunk, trash = true)
    }

    /** Called when the system dialog for the current chunk was approved. */
    fun onChunkTrashed() {
        val chunk = trashChunks.removeFirstOrNull() ?: return
        val ids = chunk.mapTo(HashSet()) { it.id }
        marked.removeAll { it.id in ids }
        history.removeAll { it.first.id in ids }
        persist()
        if (trashChunks.isEmpty()) finishReview()
    }

    fun cancelTrash() {
        trashChunks.clear()
    }

    private fun finishReview() {
        keptIds += rescued
        marked.removeAll { it.id in rescued }
        rescued.clear()
        persist()
        screen = Screen.Swipe
    }

    /** Nothing to delete, only rescues. */
    fun finishWithoutDeleting() = finishReview()

    fun loadTrash() {
        viewModelScope.launch {
            trashed.clear()
            trashed += repo.loadTrashed()
        }
    }

    fun restoreRequest(items: List<MediaItem>): IntentSender = repo.trashRequest(items, trash = false)
    fun deleteForeverRequest(items: List<MediaItem>): IntentSender = repo.deleteRequest(items)

    /** Forget all remembered decisions and start over with the full library. */
    fun startOver() {
        store.save(emptySet(), emptySet())
        reload()
    }

    private fun persist() = store.save(keptIds, marked.mapTo(HashSet()) { it.id })

    private companion object {
        const val CHUNK = 300
    }
}
