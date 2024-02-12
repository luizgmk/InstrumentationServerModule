package com.example.jc.view.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jc.data.DogBreedsRepository
import com.example.jc.data.local.DogBreedsDB
import com.example.jc.view.theme.JcTheme
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val vm: MainActivityViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JcTheme {
                // A surface container using the 'background' color from the theme
                Home(vm, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun Home(vm: MainActivityViewModel, modifier: Modifier = Modifier) {

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Greetings(vm)
    }
}

@Composable
private fun Greetings(
    vm: MainActivityViewModel,
    modifier: Modifier = Modifier
) {
    val names = vm.breeds.collectAsStateWithLifecycle(initialValue = listOf())

    Column(
        modifier = modifier
            .padding(vertical = 4.dp)
            .verticalScroll(rememberScrollState())
    ) {
        for (name in names.value) {
            Greeting(name = name.breedName)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Row(modifier = Modifier.padding(24.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(text = name)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    JcTheme {
        Home(
            MainActivityViewModel(
                DogBreedsRepository(DogBreedsDB(), Dispatchers.IO)
            )
        )
    }
}