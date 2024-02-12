package com.example.jc

import android.app.Application
import com.example.jc.data.DogBreedsRepository
import com.example.jc.data.local.DogBreedsDB
import com.example.jc.model.DogBreed
import com.example.instrumentationserver.InstrumentationServer
import com.example.jc.view.main.MainActivityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import timber.log.Timber


class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        val myModule = module {
            // Local Databases
            single { DogBreedsDB() }

            // Repositories
            single { DogBreedsRepository(get(), Dispatchers.IO) }

            // View Models
            viewModel { MainActivityViewModel(get()) }
        }

        startKoin {
            androidContext(this@App)
            modules(listOf(myModule))
        }

        // Instrumentation Server enables REST calls to the exposed classes declared below
        if (BuildConfig.DEBUG) MainScope().launch {
            withContext(Dispatchers.IO) {
                val server = InstrumentationServer()

                // Serve singleton classes injected by DI
                val db: DogBreedsDB by inject()
                val repo: DogBreedsRepository by inject()
                server.addSingleton(db)
                server.addSingleton(repo)

                // Serve also DogBreed class for individual instantiation per call
                server.addClass(DogBreed::class)

                // launch the server on port 8080
                server.start(port = 8080)
            }
        }

    }
}