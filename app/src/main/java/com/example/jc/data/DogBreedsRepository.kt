package com.example.jc.data

import com.example.jc.data.local.DogBreedsDB
import com.example.jc.model.DogBreed
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

class DogBreedsRepository(
    private val db: DogBreedsDB,
    private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun add(breed: DogBreed) = withContext(ioDispatcher) {
        if (db.breeds.add(breed.breedName)) {
            refreshRequired.set(true)
            true
        } else false
    }

    suspend fun delete(breed: DogBreed) = withContext(ioDispatcher) {
        if (db.breeds.removeIf { it == breed.breedName }) {
            refreshRequired.set(true)
            true
        } else false
    }

    private val refreshRequired = AtomicBoolean(true)

    val breeds = flow {
        val myContext = coroutineContext
        refreshBreeds()
        withContext(ioDispatcher + coroutineContext.job) {
            while (kotlin.coroutines.coroutineContext.job.isActive) {
                if (refreshRequired.get()) {
                    withContext(myContext) {
                        refreshBreeds()
                    }
                }
                yield()
                delay(300)
            }
        }
    }

    private suspend fun FlowCollector<List<DogBreed>>.refreshBreeds() {
        emit(db.breeds.map { breed ->
            DogBreed(breed)
        })
        refreshRequired.set(false)
    }

}