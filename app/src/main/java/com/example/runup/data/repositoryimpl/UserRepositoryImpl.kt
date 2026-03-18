package com.example.runup.data.repositoryimpl

import com.example.runup.data.local.UserPreferenceDataSource
import com.example.runup.data.source.local.dao.UserDao
import com.example.runup.data.source.local.entity.UserEntity
import com.example.runup.data.source.remote.user.UserDataSource
import com.example.runup.domain.model.AuthResult
import com.example.runup.domain.model.RunRecord
import com.example.runup.domain.model.UserData
import com.example.runup.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject


class UserRepositoryImpl @Inject constructor(
    private val userdatasource: UserDataSource,
    private val userDao: UserDao,
    private val userPreferenceDataSource: UserPreferenceDataSource
) : UserRepository {
    //firebase

    // 이메일 중복 체크
    override suspend fun checkuseremail(useremail: String): AuthResult<Boolean> {
        return userdatasource.isEmailAlreadyRegistered(useremail)
    }
    // 이메일, PW 저장
    override suspend fun saveUserlogininfo(useremail: String, userpw: String): AuthResult<Boolean> {
        return userdatasource.registerUser(useremail, userpw)
    }
    // 로그인
    override suspend fun login(useremail: String, userpw: String): AuthResult<Boolean> {
        return userdatasource.loginUser(useremail, userpw)
    }
    // 구글로 로그인
    override suspend fun signInWithGoogle(idToken: String): AuthResult<Boolean> {
        return userdatasource.signInWithGoogle(idToken)
    }
    // 사용자 이름 업데이트
    override suspend fun updateUserName(username: String): AuthResult<Boolean> {
        return userdatasource.updateUserName(username)
    }
    // 달리기 목표 저장
    override suspend fun updateUserGoal(goaldistance: Int, goaltime: Int): AuthResult<Boolean> {
        return userdatasource.updateUserGoal(goaldistance, goaltime)
    }
    // 달리기 기록 저장
    override suspend fun saveRunRecord(record: RunRecord): AuthResult<Boolean> {
        return userdatasource.saveRunRecord(record)
    }
    // 내 데이터 가져오기
    override suspend fun getMyUserData(): AuthResult<UserData> {
        return userdatasource.getMyUserData()
    }
    // 사용자 계정 삭제
    override suspend fun deleteUserAccount(userpw: String): AuthResult<Boolean> {
        return userdatasource.deleteUserAccount(userpw)
    }
    // 사용자 목표 가져오기
    override suspend fun getUserGoal(): AuthResult<Pair<Int,Int>> {
        return userdatasource.getUserGoal()
    }

    //Room DB
    // roomdb 에 사용자목표저장
    override suspend fun saveUserGoalToRoom(
        goaldistance: Int,
        goaltime: Int
    ): AuthResult<Boolean> {
        return try {
            //Entity 생성 및 DB 저장
            val userEntity = UserEntity(
                id = 0, // 단일 사용자 데이터 유지
                goalDistance = goaldistance,
                goalTime = goaltime,
            )
            userDao.insertUser(userEntity)
            AuthResult.Success(true)

        } catch (e: Exception) {
            // 3. 실패 시 에러 메시지와 함께 Error 반환
            // 에러 메시지는 프로젝트 상황에 맞게 커스텀 가능합니다.
            AuthResult.Fail(e.message ?: "로컬 DB 저장 중 알 수 없는 오류가 발생했습니다.")
        }
    }

    // 사용자 목표 가져오기
    override suspend fun getUserGoalFromRoom(): AuthResult<Pair<Int, Int>> {
        return try {
            //Dao를 통해 id=0인 유저 데이터 조회
            val userEntity = userDao.getUser()

            if (userEntity != null) {
                //데이터가 있으면 Pair로 묶어서 Success 반환
                val goalPair = Pair(userEntity.goalDistance, userEntity.goalTime)
                AuthResult.Success(goalPair)
            } else {
                // 3. 데이터가 비어있을 경우 (초기 상태) Fail 반환
                AuthResult.Fail("저장된 목표 데이터가 없습니다. 먼저 목표를 설정해주세요.")
            }

        } catch (e: Exception) {
            // 4. DB 접근 오류 등 예외 발생 시 Fail 반환
            AuthResult.Fail(e.message ?: "로컬 데이터를 불러오는 중 오류가 발생했습니다.")
        }
    }

    // 사용자 삭제
    override suspend fun deleteUserGoalFromRoom(): AuthResult<Boolean> {
        return try {
            // id = 0인 데이터를 삭제
            userDao.deleteUserById()
            // 성공 시 true 반환
            AuthResult.Success(true)
        } catch (e: Exception) {
            // 실패 시 에러 메시지와 함께 Fail 반환
            AuthResult.Fail(e.message ?: "데이터 삭제 중 오류가 발생했습니다.")
        }
    }

    override suspend fun updateUserLoginStatus(userLoginStatus: Boolean): AuthResult<Boolean> {
        return try {
            userPreferenceDataSource.updateUserLoginStatus(userLoginStatus)
            AuthResult.Success(true)
        } catch (e: Exception) {
            AuthResult.Fail(e.message ?: "로그인 상태 저장 실패")
        }
    }

    override suspend fun getIsLogin(): AuthResult<Boolean> {
        return try {
            val isLogin = userPreferenceDataSource.getIsLogin().first()
            AuthResult.Success(isLogin)
        } catch (e: Exception) {
            AuthResult.Fail(e.message ?: "로그인 상태 조회 실패")
        }
    }
}