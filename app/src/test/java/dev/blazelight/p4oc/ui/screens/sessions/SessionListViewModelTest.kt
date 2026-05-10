package dev.blazelight.p4oc.ui.screens.sessions

import dev.blazelight.p4oc.data.remote.mapper.MessageMapper
import dev.blazelight.p4oc.data.session.SessionRepositoryImpl
import dev.blazelight.p4oc.fakes.FakeWorkspaceClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionListViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createSession_cancellationDoesNotSetError() = runTest {
        val client = FakeWorkspaceClient().apply {
            createSessionFailure = CancellationException("leaving screen")
        }
        val repository = SessionRepositoryImpl(
            client = client,
            messageMapper = MessageMapper(Json { ignoreUnknownKeys = true }),
            dispatcher = dispatcher,
        )
        val viewModel = SessionListViewModel(repository)

        viewModel.createSession(title = "new")

        assertNull(viewModel.uiState.value.error)
        repository.close()
    }
}
