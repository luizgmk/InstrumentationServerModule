package com.example.jc.view.main

import androidx.lifecycle.ViewModel
import com.example.jc.data.DogBreedsRepository
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import timber.log.Timber

class MainActivityViewModel(
    repository: DogBreedsRepository
) : ViewModel() {

    val breeds = repository.breeds
        .onStart {
            Timber.d("MainActivityViewModel :: Flow collection started (FlowCollector hash: ${this.hashCode()})")
        }
        .onCompletion {
            Timber.d("MainActivityViewModel :: Flow collection completed (FlowCollector hash: ${this.hashCode()})")
        }

}