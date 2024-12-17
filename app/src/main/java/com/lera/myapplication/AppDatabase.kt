package com.lera.myapplication

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Date

@Database(entities = [User::class, Child::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase(){
    abstract fun userDao(): UserDao
    abstract fun childDao(): ChildDao


    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext,
                AppDatabase::class.java, "child_db")
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        ioThread {
                            getDatabase(context).userDao().insertList(PREPOPULATE_USERS)
                            getDatabase(context).childDao().insertList(PREPOPULATE_CHILD)
                        }
                    }
                })
                .build()

        val PREPOPULATE_USERS = listOf(
            User("lera", "a"),
            User("ilya", "b"),
        )
        val PREPOPULATE_CHILD = listOf(
            Child(1, "anna", Date(120, 10, 10)),
            Child(1, "ivan", Date(122, 10, 15)),
            Child(2, "ivan", Date(121, 11, 11)),
        )
    }


}