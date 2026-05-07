package com.runit.runup.data.source.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.runit.runup.data.source.local.dao.UserDao
import com.runit.runup.data.source.local.entity.UserEntity

@Database(entities = [UserEntity::class], version = 2)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}