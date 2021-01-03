package io.github.wulkanowy.data.repositories

import io.github.wulkanowy.data.db.dao.MobileDeviceDao
import io.github.wulkanowy.data.db.entities.MobileDevice
import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.mappers.mapToEntities
import io.github.wulkanowy.data.mappers.mapToMobileDeviceToken
import io.github.wulkanowy.data.pojos.MobileDeviceToken
import io.github.wulkanowy.sdk.Sdk
import io.github.wulkanowy.utils.init
import io.github.wulkanowy.utils.networkBoundResource
import io.github.wulkanowy.utils.uniqueSubtract
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MobileDeviceRepository @Inject constructor(
    private val mobileDb: MobileDeviceDao,
    private val sdk: Sdk
) {

    fun getDevices(student: Student, semester: Semester, forceRefresh: Boolean) = networkBoundResource(
        shouldFetch = { it.isEmpty() || forceRefresh },
        query = { mobileDb.loadAll(semester.studentId) },
        fetch = {
            sdk.init(student).switchDiary(semester.diaryId, semester.schoolYear)
                .getRegisteredDevices()
                .mapToEntities(semester)
        },
        saveFetchResult = { old, new ->
            mobileDb.deleteAll(old uniqueSubtract new)
            mobileDb.insertAll(new uniqueSubtract old)
        }
    )

    suspend fun unregisterDevice(student: Student, semester: Semester, device: MobileDevice) {
        sdk.init(student).switchDiary(semester.diaryId, semester.schoolYear)
            .unregisterDevice(device.deviceId)

        mobileDb.deleteAll(listOf(device))
    }

    suspend fun getToken(student: Student, semester: Semester): MobileDeviceToken {
        return sdk.init(student).switchDiary(semester.diaryId, semester.schoolYear)
            .getToken()
            .mapToMobileDeviceToken()
    }
}
