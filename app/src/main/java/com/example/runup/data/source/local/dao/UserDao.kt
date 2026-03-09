package com.example.runup.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.runup.data.source.local.entity.UserEntity

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM user_table WHERE id = 0")
    suspend fun getUser(): UserEntity?

    @Query("DELETE FROM user_table WHERE id = 0")
    suspend fun deleteUserById()

    @Query("UPDATE user_table SET isLogin = :loginStatus WHERE id = 0")
    suspend fun updateLoginStatus(loginStatus: Boolean)

    @Query("SELECT isLogin FROM user_table WHERE id = 0")
    suspend fun getIsLogin(): Boolean
}