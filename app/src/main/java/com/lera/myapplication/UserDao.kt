package com.lera.myapplication


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
// Указываем, что этот интерфейс является DAO (Data Access Object) для Room
@Dao
interface UserDao {
    // Метод для вставки списка пользователей
    @Insert
    fun insertList(data: List<User>)
    //1 объект
    @Insert
    suspend fun insert(user: User)

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)

    // Метод для получения всех пользователей из таблицы users_table,
    // отсортированных по id в порядке возрастания
    @Query("Select * from users_table order by id ASC")
    fun getAll(): List<User>

    // Метод для получения пользователя по логину и паролю
    @Query("Select * from users_table WHERE login=:login and pass=:pass")
    fun getByLoginAndPass(login: String, pass: String): User
}