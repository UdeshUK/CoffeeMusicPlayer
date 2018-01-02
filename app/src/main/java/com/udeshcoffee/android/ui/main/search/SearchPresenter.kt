package com.udeshcoffee.android.ui.main.search

import android.content.IntentFilter
import com.annimon.stream.Collectors
import com.annimon.stream.Stream
import com.cantrowitz.rxbroadcast.RxBroadcast
import com.udeshcoffee.android.App
import com.udeshcoffee.android.data.MediaRepository
import com.udeshcoffee.android.getService
import com.udeshcoffee.android.model.Song
import com.udeshcoffee.android.playSong
import com.udeshcoffee.android.service.MusicService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Udathari on 9/12/2017.
 */
class SearchPresenter(val view: SearchContract.View,
                      private val mediaRepository: MediaRepository): SearchContract.Presenter {

    val TAG = "SearchPresenter"

    private var broadcastDisposable: Disposable? = null
    private lateinit var disposable: Disposable

    init {
        view.presenter = this
    }

    override fun start() {
        val filter = IntentFilter()
        filter.addAction(MusicService.InternalIntents.METADATA_CHANGED)

        broadcastDisposable = RxBroadcast.fromLocalBroadcast(App.instance, filter)
                .subscribe {
                    when(it.action){
                        MusicService.InternalIntents.METADATA_CHANGED -> {
                            val id = getService()?.currentSong()?.id ?: -1
                            view.setCurrentSong(id)
                        }
                    }
                }
    }

    override fun stop() {
        broadcastDisposable?.let {
            if (!it.isDisposed)
                it.dispose()
        }
    }

    override fun search(query: String) {
        if (query == "") {
            view.populateItems(ArrayList())
            view.populateAlbumItems(ArrayList())
            view.populateArtistItems(ArrayList())
            return
        }
        fetchSongs(query)
        fetchAlbums(query)
        fetchArtist(query)
    }

    private fun fetchSongs(query: String) {
        mediaRepository.getSongs()
                .map { songs ->
                    Stream.of(songs).filter { it.title.contains(query, true) }.collect(Collectors.toList())
                }
                .take(1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{
                    songs -> view.populateItems(songs)
                }
        getService()?.currentSong()?.id?.let { view.setCurrentSong(it) }
    }

    private fun fetchAlbums(query: String) {
        mediaRepository.getAlbums()
                .map { albums ->
                    Stream.of(albums).filter{ it.title.contains(query, true) }.collect(Collectors.toList())
                }
                .take(1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{ view.populateAlbumItems(it) }
    }

    private fun fetchArtist(query: String) {
        mediaRepository.getArtists()
                .map { artists ->
                    Stream.of(artists).filter{ it.name.contains(query, true) }.collect(Collectors.toList())
                }
                .take(1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{ view.populateArtistItems(it) }
    }


    override fun playClick(songs: ArrayList<Song>) {}

    override fun queueClick(songs: ArrayList<Song>) {}

    override fun playNextClick(songs: java.util.ArrayList<Song>) {}

    override fun addToFavoritesClick(songs: java.util.ArrayList<Song>) {}

    override fun addToPlaylistClick(songs: java.util.ArrayList<Song>) {}

    override fun itemClicked(position: Int, allItems: List<Song>) {
        playSong(position, allItems, true)
    }

    override fun itemLongClicked(item: Song) {
        view.showSongLongDialog(item)
    }

    override fun albumItemClicked(position: Int) {
        view.showAlbum(position)
    }

    override fun artistItemClicked(position: Int) {
        view.showArtist(position)
    }

}