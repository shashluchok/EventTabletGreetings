package com.example.android.eventtabletgreetings

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel


class MainActivity : AppCompatActivity() {

    private var isGreeting = false
    var greetingsJob: Job? = null

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectToChat()
        disableUserInteraction()


    }

    private fun greet(userInfo: UserInfo) {
        lifecycleScope.launch(Dispatchers.IO) {
            /* val userInfo = usersChannel.receive()*/
            val standTitleFirst = (100 until 1000).random()
            val standTitleSecond = (100 until 1000).random()

            greetingsJob?.cancel()
            greetingsJob = null

            launch(Dispatchers.Main) {
                greetings_card_cl.apply {
                    animate().alpha(0f)
                        .x(resources.displayMetrics.widthPixels.toFloat())
                        .setDuration(600)
                }
            }
            delay(600)

            launch(Dispatchers.Main) {
                greetings_card_cl.apply {
                    animate().x(0f)
                        .setDuration(0)
                    animate()
                        .alpha(1f)
                        .setDuration(600)
                    greeting_card_user_name.text = userInfo.userName
                    greeting_card_user_hint_stands.text = (String.format(
                        resources.getString(R.string.stand_titles),
                        standTitleFirst,
                        standTitleSecond
                    ))
                    /*Handler(Looper.getMainLooper()).postDelayed({
                        (greeting_card_iv.background as AnimatedVectorDrawable).start()
                    }, 700)*/
                }
            }
            greetingsJob = launch(Dispatchers.IO) {
                delay(15000)
                if (isActive) {
                    greetingsJob?.cancel()
                    greetingsJob = null
                    launch(Dispatchers.Main) {
                        greetings_card_cl.apply {
                            animate().alpha(0f)
                                .x(resources.displayMetrics.widthPixels.toFloat())
                                .setDuration(600)
                        }
                    }
                }
            }
        }
    }


//    private val usersChannel = Channel<UserInfo>(Channel.UNLIMITED, BufferOverflow.SUSPEND)

    suspend fun DefaultClientWebSocketSession.outputMessages() {

        for (message in incoming) {
            try {
                message as? Frame.Text ?: continue
                val text = message.readText().toString()
                val userInfo = Gson().fromJson<UserInfo>(text, UserInfo::class.java)
                greet(userInfo = userInfo)
//                usersChannel.send(userInfo)
            } catch (e: Exception) {
                Log.v("NEW MESSAGE", "Error while receiving: " + e.localizedMessage)
            }
        }

    }


    private fun connectToChat() {
        val client = HttpClient(CIO) {
            install(WebSockets)
            engine {
                requestTimeout = 100000L
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {

            try {
                client.ws(
                    method = HttpMethod.Get,
                    host = "lfprototypegreetings.herokuapp.com",
                    path = "/eventAuthNumberOne"
                ) {

                    val messageOutputRoutine = launch { outputMessages() }
                    val userInputRoutine = launch { inputMessages() }

                    userInputRoutine.join()
                    messageOutputRoutine.cancelAndJoin()
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                client.close()
            }
        }
    }

    suspend fun DefaultClientWebSocketSession.inputMessages() {
        while (true) {

        }
    }

    private fun disableUserInteraction() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun enableUserInteraction() {
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

}


data class UserInfo(
    val userName: String,
)
