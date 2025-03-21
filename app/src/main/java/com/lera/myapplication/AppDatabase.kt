package com.lera.myapplication

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

// определяет базу данных Room.
// entities: Список сущностей
// version: Версия базы данных
// false - не экспортировать

@Database(entities = [User::class, Child::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    // Абстрактные методы для получения Data Access Objects
    abstract fun userDao(): UserDao
    abstract fun childDao(): ChildDao

    companion object {
        //  INSTANCE для хранения единственного экземпляра базы данных @Volatile гарантирует видимость изменений для всех потоков.
        @Volatile private var INSTANCE: AppDatabase? = null

        // Функция для получения экземпляра базы данных
        // Обеспечивает потокобезопасность с помощью synchronized
        fun getDatabase(context: Context): AppDatabase =
            // ?: - оператор Elvis.  Если INSTANCE не null, возвращаем его
            INSTANCE ?: synchronized(this) {
                // Если INSTANCE все еще null, создаем базу данных и присваиваем ее INSTANCE
                // also - вызов функции, которая вернет сам объект
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        // Функция для создания бд
        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext, // Контекст приложения
                AppDatabase::class.java, // Класс базы данных
                "child_db") // Имя базы данных
                // Добавляем обратный вызов для выполнения операций при создании базы данных.
                .addCallback(object : RoomDatabase.Callback() {
                    // Вызывается после создания базы данных.
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            getDatabase(context).userDao().insertList(PREPOPULATE_USERS)
                            getDatabase(context).childDao().insertList(PREPOPULATE_CHILD)
                        }
                    }
                })
                .build()

        // Тестовые данные для предварительного заполнения таблицы User.
        val PREPOPULATE_USERS = listOf(
            User("lera", "a"),
            User("ilya", "b"),
        )
        // Тестовые данные для предварительного заполнения таблицы Child.
        val PREPOPULATE_CHILD = listOf(
            Child(1, "anna", Date(120, 10, 10)), // Год, месяц, день. Обратите внимание, что год отсчитывается от 1900
            Child(1, "ivan", Date(122, 10, 15)),
            Child(2, "ivan", Date(121, 11, 11)),
        )
    }
}