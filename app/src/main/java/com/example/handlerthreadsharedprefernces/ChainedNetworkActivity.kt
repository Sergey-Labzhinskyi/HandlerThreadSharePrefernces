package com.example.handlerthreadsharedprefernces

import android.annotation.SuppressLint
import android.os.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast

class ChainedNetworkActivity : AppCompatActivity() {

    private val DIALOG_LOADING = 0
    private val SHOW_LOADING = 1
    private val DISMISS_LOADING = 2


    /**
     * 1. Обработчик, который обрабатывает сообщения в UI-потоке.
     */
    private val dialogHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            Log.d("ChainedNetworkActivity", "dialogHandler handleMessage()")
            when (msg.what) {
                SHOW_LOADING -> {
                    Log.d("ChainedNetworkActivity", "dialogHandler SHOW_LOADING")
                    Toast.makeText(
                        applicationContext,
                        "SHOW_LOADING", Toast.LENGTH_LONG
                    ).show()
                }
                DISMISS_LOADING -> {
                    Log.d("ChainedNetworkActivity", "dialogHandler DISMISS_LOADING")
                    Toast.makeText(
                        applicationContext,
                        "DISMISS_LOADING", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private inner class NetworkHandler : HandlerThread(
        "NetworkHandler",
        Process.THREAD_PRIORITY_BACKGROUND
    ) {

        private val STATE_A = 1
        private val STATE_B = 2
        private lateinit var handler: Handler

        override fun onLooperPrepared() {
            super.onLooperPrepared()
            Log.d("ChainedNetworkActivity", "NetworkHandler onLooperPrepared()")
            handler = object : Handler(looper) {
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    when (msg.what) {
                        /**
                         * 2. Первый сетевой вызов, который инициализуется в onCreate(). Он передает
                         * сообщения в UI-поток, где создается Toast, отображающий ход выполнения
                         * задачи. После успешного завершения сетевой операции ее результат
                         * передается во вторую задачу(STATE_B)
                         */
                        STATE_A -> {
                            Log.d("ChainedNetworkActivity", "NetworkHandler STATE_A")
                            dialogHandler.sendEmptyMessage(SHOW_LOADING)
                            val result = networkOperation1()
                            sendMessage(obtainMessage(STATE_B, result)) //?: dialogHandler.sendEmptyMessage(DISMISS_LOADING)
                        }
                        /**
                         * 3. Выполнение второй сетевой операции.
                         */
                        STATE_B -> {
                            Log.d("ChainedNetworkActivity", "NetworkHandler STATE_B")
                            networkOperation2(msg.obj as String)
                            dialogHandler.sendEmptyMessage(DISMISS_LOADING)
                        }
                    }
                }
            }
            fetchDataFromNetwork()
        }

        /**
         * 4. Инициализация сетевого вызова при запуске фонового потока.
         */
        private fun fetchDataFromNetwork() {
            Log.d("ChainedNetworkActivity", "NetworkHandler fetchDataFromNetwork()")
            handler.sendEmptyMessage(STATE_A)
        }

        private fun networkOperation2(data: String) {
            Log.d("ChainedNetworkActivity", "NetworkHandler networkOperation2()")
            SystemClock.sleep(2000)
        }

        private fun networkOperation1(): String {
            Log.d("ChainedNetworkActivity", "NetworkHandler networkOperation1(()")
            SystemClock.sleep(2000)
            return "A networkOperation1"
        }
    }

    private lateinit var thread: NetworkHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chained_network)
        Log.d("ChainedNetworkActivity", "onCreate()")
        thread = NetworkHandler()
        thread.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        thread.quit()
    }
}