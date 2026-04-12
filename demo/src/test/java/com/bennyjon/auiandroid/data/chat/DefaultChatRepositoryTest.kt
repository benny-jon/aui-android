package com.bennyjon.auiandroid.data.chat

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bennyjon.auiandroid.data.chat.db.ChatDatabase
import com.bennyjon.auiandroid.data.chat.db.ChatMessageDao
import com.bennyjon.auiandroid.data.llm.FakeLlmClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DefaultChatRepositoryTest {

    private lateinit var db: ChatDatabase
    private lateinit var dao: ChatMessageDao
    private lateinit var fakeLlmClient: FakeLlmClient
    private lateinit var repo: DefaultChatRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChatDatabase::class.java,
        ).allowMainThreadQueries().build()

        dao = db.chatMessageDao()
        fakeLlmClient = FakeLlmClient()
        repo = DefaultChatRepository(
            llmClient = fakeLlmClient,
            dao = dao,
            systemPrompt = "You are a test assistant.",
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `sendUserMessage inserts user and assistant messages`() = runTest(testDispatcher) {
        repo.sendUserMessage("conv1", "hi")

        val messages = repo.observeMessages("conv1").first()
        assertEquals(2, messages.size)

        val user = messages[0]
        assertEquals(ChatMessage.Role.USER, user.role)
        assertEquals("hi", user.text)
        assertNull(user.auiResponse)

        val assistant = messages[1]
        assertEquals(ChatMessage.Role.ASSISTANT, assistant.role)
        assertNotNull(assistant.text)
    }

    @Test
    fun `sendUserMessage with AUI response populates auiResponse and rawAuiJson`() = runTest(testDispatcher) {
        // First call returns text-only; second returns text + AUI poll
        repo.sendUserMessage("conv1", "first")
        repo.sendUserMessage("conv1", "second")

        val messages = repo.observeMessages("conv1").first()
        assertEquals(4, messages.size)

        // Second assistant response (index 3) should have AUI
        val assistantWithAui = messages[3]
        assertEquals(ChatMessage.Role.ASSISTANT, assistantWithAui.role)
        assertNotNull(assistantWithAui.text)
        assertNotNull(assistantWithAui.auiResponse)
        assertNotNull(assistantWithAui.rawAuiJson)
    }

    @Test
    fun `clearConversation removes all messages`() = runTest(testDispatcher) {
        repo.sendUserMessage("conv1", "hi")
        assertEquals(2, repo.observeMessages("conv1").first().size)

        repo.clearConversation("conv1")
        assertEquals(0, repo.observeMessages("conv1").first().size)
    }

    @Test
    fun `messages from different conversations are isolated`() = runTest(testDispatcher) {
        repo.sendUserMessage("conv1", "hello")
        repo.sendUserMessage("conv2", "world")

        assertEquals(2, repo.observeMessages("conv1").first().size)
        assertEquals(2, repo.observeMessages("conv2").first().size)
    }

    @Test
    fun `messages are ordered by creation time`() = runTest(testDispatcher) {
        repo.sendUserMessage("conv1", "first")
        repo.sendUserMessage("conv1", "second")

        val messages = repo.observeMessages("conv1").first()
        assertEquals(4, messages.size)

        // Verify ordering: user1, assistant1, user2, assistant2
        assertEquals(ChatMessage.Role.USER, messages[0].role)
        assertEquals("first", messages[0].text)
        assertEquals(ChatMessage.Role.ASSISTANT, messages[1].role)
        assertEquals(ChatMessage.Role.USER, messages[2].role)
        assertEquals("second", messages[2].text)
        assertEquals(ChatMessage.Role.ASSISTANT, messages[3].role)
    }

    @Test
    fun `each message has a unique id`() = runTest(testDispatcher) {
        repo.sendUserMessage("conv1", "hi")

        val messages = repo.observeMessages("conv1").first()
        val ids = messages.map { it.id }.toSet()
        assertEquals(2, ids.size)
    }

    @Test
    fun `clearConversation on empty conversation is a no-op`() = runTest(testDispatcher) {
        repo.clearConversation("nonexistent")
        assertEquals(0, repo.observeMessages("nonexistent").first().size)
    }
}
