package com.example.runup.data.repositoryimpl

import com.example.runup.domain.model.RunRecord
import com.example.runup.domain.model.UserData
import com.example.runup.domain.repository.RunRecordRepository
import com.example.runup.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject

class RunRecordRepositoryImpl @Inject constructor (
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
): RunRecordRepository {

}