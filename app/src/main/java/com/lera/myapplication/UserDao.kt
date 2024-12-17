package com.lera.myapplication


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {
    @Insert
    fun insertList(data: List<User>)

    @Insert
    suspend fun insert(user: User)

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)

    @Query("Select * from users_table order by id ASC")
    fun getAll(): List<User>

    @Query("Select * from users_table WHERE login=:login and pass=:pass")
    fun getByLoginAndPass(login: String, pass: String): User
}