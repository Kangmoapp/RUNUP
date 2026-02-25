package com.example.runup.domain.usecase

import com.example.runup.domain.model.RunRecord
import com.example.runup.domain.repository.RunRecordRepository

class RecordRunningUseCase(
    private val runrecordrepository: RunRecordRepository
) {
    suspend operator fun invoke(runrecord : RunRecord){

    }
}