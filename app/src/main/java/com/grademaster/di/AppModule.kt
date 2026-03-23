package com.grademaster.di

import android.content.Context
import androidx.room.Room
import com.grademaster.data.local.AppPreferences
import com.grademaster.data.local.GradeMasterDatabase
import com.grademaster.data.local.StudentDao
import com.grademaster.data.local.VaultSessionDao
import com.grademaster.data.local.appDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGradeMasterDatabase(@ApplicationContext context: Context): GradeMasterDatabase =
        Room.databaseBuilder(
            context,
            GradeMasterDatabase::class.java,
            GradeMasterDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideStudentDao(db: GradeMasterDatabase): StudentDao = db.studentDao()

    @Provides
    fun provideVaultSessionDao(db: GradeMasterDatabase): VaultSessionDao = db.vaultSessionDao()

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences =
        AppPreferences(context.appDataStore)

    /* @Provides
    @ApplicationContext
    fun provideContext(@ApplicationContext context: Context): Context = context */
}
