package com.lera.myapplication

import java.util.concurrent.Executors
// ExecutorService с одним потоком для выполнения асинхронных задач.
private val IO_EXECUTOR = Executors.newSingleThreadExecutor()
// Функция ioThread принимает функцию f без параметров и возвращающего значения (() -> Unit) и запускает её в отдельном потоке
fun ioThread(f : () -> Unit) {
    IO_EXECUTOR.execute(f)
}