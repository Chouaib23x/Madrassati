package dz.madrassati.data.repo

import dz.madrassati.data.db.StudentDao
import dz.madrassati.data.db.StudentEntity
import dz.madrassati.domain.model.Student
import dz.madrassati.domain.repository.ClassRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ClassRepositoryImpl @Inject constructor(
    private val studentDao: StudentDao
) : ClassRepository {

    override fun students(): Flow<List<Student>> =
        studentDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun student(id: Long): Student? = studentDao.findById(id)?.toDomain()

    override suspend fun count(): Int = studentDao.count()

    override suspend fun add(name: String, avatar: String, colorHex: Long): Boolean {
        if (studentDao.count() >= 20) return false
        studentDao.insert(
            StudentEntity(
                name = name,
                avatar = avatar,
                colorHex = colorHex,
                sortOrder = studentDao.count() + 1
            )
        )
        return true
    }

    override suspend fun update(id: Long, name: String, avatar: String, colorHex: Long): Boolean {
        studentDao.update(id, name, avatar, colorHex)
        return true
    }

    override suspend fun remove(id: Long) {
        studentDao.delete(id)
    }

    override suspend fun importFromServer(students: List<Student>) {
        studentDao.upsertAll(students.map {
            StudentEntity(
                id = it.id,
                name = it.name,
                avatar = it.avatar,
                colorHex = it.colorHex,
                sortOrder = it.sortOrder
            )
        })
    }

    private fun StudentEntity.toDomain() = Student(id, name, avatar, colorHex, sortOrder)
}
