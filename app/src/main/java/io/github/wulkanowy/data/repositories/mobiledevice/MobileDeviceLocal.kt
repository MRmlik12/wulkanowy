package io.github.wulkanowy.data.repositories.mobiledevice

import io.github.wulkanowy.data.db.dao.MobileDeviceDao
import io.github.wulkanowy.data.db.entities.MobileDevice
import io.github.wulkanowy.data.db.entities.Semester
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MobileDeviceLocal @Inject constructor(private val mobileDb: MobileDeviceDao) {

    suspend fun saveDevices(devices: List<MobileDevice>) {
        mobileDb.insertAll(devices)
    }

    suspend fun deleteDevices(devices: List<MobileDevice>) {
        mobileDb.deleteAll(devices)
    }

    fun getDevices(semester: Semester): Flow<List<MobileDevice>> {
        return mobileDb.loadAll(semester.studentId)
    }
}
