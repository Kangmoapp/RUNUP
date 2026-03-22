package com.example.runup.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.runup.data.source.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM user_table WHERE id = 0")
    fun getUserFlow(): Flow<UserEntity?>

    @Query("DELETE FROM user_table WHERE id = 0")
    suspend fun deleteUserById()
}