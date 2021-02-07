package com.example.handlerthreadsharedprefernces

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class SharedPreferencesActivity : AppCompatActivity() {

    /**
     * Вывод считаного значения в TextView
     *
     * 1. Обработчик Handler для UI-потока, используется фоновым потоком для
     * взаемодествия с UI-потоком
     */
    private val uiHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            Log.d("SharedActivity", "uiHandler handleMessage() ${msg.what} thread ${Thread.currentThread()}")
            if (msg.what == 0) {
                msg.obj?.let {
                    val i = it as Int
                    tvNumber.text = i.toString()
            }
            }
        }
    }

    /**
     * 2. Фоновый поток который читает и записывает значение SharedPreferences
     */
    private inner class SharedPreferenceThread : HandlerThread(
            "SharedPreferenceThread", Process.THREAD_PRIORITY_BACKGROUND) {

        private val KEY = "KEY"
        private var pref: SharedPreferences = getSharedPreferences("LocalPrefs", MODE_PRIVATE)
        private val READ = 1
        private val WRITE = 2
        private lateinit var handler: Handler

        override fun onLooperPrepared() {
            super.onLooperPrepared()
            Log.d("SharedActivity", "onLooperPrepared() ${currentThread()}")
            handler = object : Handler(looper) {
                override fun handleMessage(msg: Message) {
                    Log.d("SharedActivity", "onLooperPrepared handleMessage() $msg.what")
                    when (msg.what) {
                        READ -> {
                            Log.d("SharedActivity", "onLooperPrepared READ")
                            uiHandler.sendMessage(uiHandler.obtainMessage(
                                    0, pref.getInt(KEY, 0)))
                        }
                        WRITE -> {
                            Log.d("SharedActivity", "onLooperPrepared WRITE")
                            val editor = pref.edit()
                            editor.putInt(KEY, msg.obj as Int)
                            editor.apply()
                        }
                    }
                }
            }
        }

         fun read() {
             Log.d("SharedActivity", "read()")
             handler.sendEmptyMessage(READ)
        }

         fun write(i: Int){
             Log.d("SharedActivity", "write()")
             handler.sendMessage(Message.obtain(Message.obtain(uiHandler, WRITE, i)))
        }
    }

    private var count = 0
    private lateinit var thread: SharedPreferenceThread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("SharedActivity", "onCreate()")
        thread = SharedPreferenceThread()
        /**
         * 3. Запуск фонового потока.
         */
        thread.start()

        btnRead.setOnClickListener {
            onButtonClickRead()
        }
        btnWrite.setOnClickListener {
            onButtonClickWrite()
        }
    }

    /**
     * Запись пустого значения из UI-потока
     */
    private fun onButtonClickWrite(){
        thread.write(count++)
    }

    /**
     * Инициализация чтения из UI-потока
     */
    private fun onButtonClickRead(){
        thread.read()
    }

    /**
     * Необходимо обеспечить завершение фонового потока
     * одновременно с завершением компонента Activity
     */
    override fun onDestroy() {
        super.onDestroy()
        thread.quit()
    }
}